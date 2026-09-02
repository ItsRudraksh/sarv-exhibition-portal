import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, inquiryApi, localStorageDraftPort } from './api'
import {
  BUYER_STEPS,
  createEmptyDraft,
  SHARED_STEPS,
  SUPPLIER_STEPS,
  type CardFileMeta,
  type CardSide,
  type InquiryDraft,
  type InquiryRoute,
  type InquiryStep,
} from './types'

function getStepOrder(route: InquiryRoute | null): InquiryStep[] {
  if (route === 'SUPPLIER') return [...SHARED_STEPS, ...SUPPLIER_STEPS]
  if (route === 'PURCHASE') return [...SHARED_STEPS, ...BUYER_STEPS]
  return SHARED_STEPS
}

function getPreviousStep(draft: InquiryDraft): InquiryStep | null {
  const steps = getStepOrder(draft.route)
  const idx = steps.indexOf(draft.currentStep)
  if (idx <= 0) return null
  return steps[idx - 1] ?? null
}

function getNextStepForRoute(
  current: InquiryStep,
  route: InquiryRoute,
): InquiryStep {
  const steps = getStepOrder(route)
  const idx = steps.indexOf(current)
  return steps[idx + 1] ?? current
}

export function useInquiryJourney() {
  const [draft, setDraft] = useState<InquiryDraft>(() => createEmptyDraft())
  const [ready, setReady] = useState(false)
  const [apiAvailable, setApiAvailable] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const draftRef = useRef(draft)
  const skipNextSave = useRef(true)

  useEffect(() => {
    draftRef.current = draft
  }, [draft])

  useEffect(() => {
    let cancelled = false

    const hydrate = async () => {
      const local = localStorageDraftPort.load()
      try {
        await inquiryApi.loadTaxonomy()
        if (local?.id) {
          const remote = await inquiryApi.get(local.id)
          if (cancelled) return
          if (remote) {
            setApiAvailable(true)
            setDraft({
              ...remote,
              cardFront: local.cardFront?.previewUrl ? local.cardFront : remote.cardFront,
              cardBack: local.cardBack?.previewUrl ? local.cardBack : remote.cardBack,
            })
            setReady(true)
            return
          }
        }
        const created = await inquiryApi.create(local ?? createEmptyDraft())
        if (cancelled) return
        setApiAvailable(true)
        setDraft(created)
      } catch {
        if (cancelled) return
        setApiAvailable(false)
        setDraft(local ?? createEmptyDraft())
      } finally {
        if (!cancelled) setReady(true)
      }
    }

    void hydrate()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!ready) return
    localStorageDraftPort.save(draft)
    if (skipNextSave.current) {
      skipNextSave.current = false
      return
    }
    if (!apiAvailable || draft.lifecycleState !== 'DRAFT') return
    const handle = window.setTimeout(() => {
      void inquiryApi.save(draftRef.current).catch(() => {
        setApiAvailable(false)
      })
    }, 400)
    return () => window.clearTimeout(handle)
  }, [draft, ready, apiAvailable])

  const updateDraft = useCallback((patch: Partial<InquiryDraft>) => {
    setDraft((prev) => ({ ...prev, ...patch }))
  }, [])

  const goToStep = useCallback((step: InquiryStep) => {
    setDraft((prev) => ({ ...prev, currentStep: step }))
  }, [])

  const goBack = useCallback(() => {
    setDraft((prev) => {
      const prevStep = getPreviousStep(prev)
      if (!prevStep) return prev
      return { ...prev, currentStep: prevStep }
    })
  }, [])

  const selectRoute = useCallback((route: InquiryRoute) => {
    setDraft((prev) => ({
      ...prev,
      route,
      currentStep:
        route === 'SUPPLIER' ? 'supplier-departments' : 'buyer-need',
    }))
  }, [])

  const advanceAfterContact = useCallback(async () => {
    setSubmitError(null)
    const current = {
      ...draftRef.current,
      contactConfirmed: true,
      currentStep: 'intent-selection' as const,
    }
    if (apiAvailable) {
      try {
        const saved = await inquiryApi.confirmContact(current)
        skipNextSave.current = true
        setDraft(saved)
        return
      } catch (error) {
        setSubmitError(error instanceof ApiError ? error.message : 'Could not save contact details.')
        return
      }
    }
    setDraft(current)
  }, [apiAvailable])

  const advance = useCallback(() => {
    setDraft((prev) => {
      if (!prev.route && prev.currentStep === 'intent-selection') return prev
      if (!prev.route) return prev
      const next = getNextStepForRoute(prev.currentStep, prev.route)
      return { ...prev, currentStep: next }
    })
  }, [])

  const submit = useCallback(async () => {
    setSubmitting(true)
    setSubmitError(null)
    try {
      if (apiAvailable) {
        const saved = await inquiryApi.submit(draftRef.current)
        skipNextSave.current = true
        setDraft(saved)
        localStorageDraftPort.save(saved)
        return
      }
      setDraft((prev) => {
        const next: InquiryDraft = {
          ...prev,
          lifecycleState: 'SUBMITTED',
          submittedAt: new Date().toISOString(),
          currentStep:
            prev.route === 'SUPPLIER' ? 'supplier-confirmation' : 'buyer-confirmation',
        }
        localStorageDraftPort.save(next)
        return next
      })
    } catch (error) {
      setSubmitError(
        error instanceof ApiError
          ? error.message
          : 'Could not submit. Check that the Java API is running.',
      )
    } finally {
      setSubmitting(false)
    }
  }, [apiAvailable])

  const uploadCard = useCallback(
    async (side: CardSide, blob: Blob, filename: string, localMeta: CardFileMeta) => {
      setSubmitError(null)
      const field = side === 'front' ? 'cardFront' : 'cardBack'
      if (!apiAvailable) {
        setDraft((prev) => ({ ...prev, [field]: localMeta }))
        return
      }
      const asset = await inquiryApi.uploadFile(
        draftRef.current.id,
        blob,
        'BUSINESS_CARD',
        side,
        filename,
      )
      const meta: CardFileMeta = {
        name: asset.originalFilename,
        size: asset.byteSize,
        type: asset.mediaType,
        assetId: asset.id,
        previewUrl: localMeta.previewUrl ?? inquiryApi.fileUrl(draftRef.current.id, asset.id),
      }
      skipNextSave.current = true
      setDraft((prev) => ({ ...prev, [field]: meta }))
    },
    [apiAvailable],
  )

  const uploadCatalogue = useCallback(
    async (file: File) => {
      setSubmitError(null)
      const local: CardFileMeta = { name: file.name, size: file.size, type: file.type }
      if (!apiAvailable) {
        setDraft((prev) => ({
          ...prev,
          supplier: { ...prev.supplier, catalogueFile: local },
        }))
        return
      }
      const asset = await inquiryApi.uploadFile(
        draftRef.current.id,
        file,
        'CATALOGUE_ORIGINAL',
        undefined,
        file.name,
      )
      skipNextSave.current = true
      setDraft((prev) => ({
        ...prev,
        supplier: {
          ...prev.supplier,
          catalogueFile: {
            name: asset.originalFilename,
            size: asset.byteSize,
            type: asset.mediaType,
            assetId: asset.id,
          },
        },
      }))
    },
    [apiAvailable],
  )

  const declineCardConsent = useCallback(async () => {
    if (!apiAvailable) return
    try {
      await inquiryApi.recordConsent(
        draftRef.current.id,
        'BUSINESS_CARD_EXTRACTION',
        'DECLINED',
      )
    } catch {
      // Offline or already recorded — visitor can still continue without a card.
    }
  }, [apiAvailable])

  const restart = useCallback(() => {
    localStorageDraftPort.clear()
    skipNextSave.current = true
    setSubmitError(null)
    if (apiAvailable) {
      void inquiryApi.create(createEmptyDraft()).then((created) => {
        setDraft(created)
      })
      return
    }
    setDraft(createEmptyDraft())
  }, [apiAvailable])

  const canGoBack =
    draft.lifecycleState === 'DRAFT' &&
    getPreviousStep(draft) !== null &&
    draft.currentStep !== 'card-capture'

  return {
    draft,
    ready,
    apiAvailable,
    submitting,
    submitError,
    updateDraft,
    goToStep,
    goBack,
    selectRoute,
    advanceAfterContact,
    advance,
    submit,
    uploadCard,
    uploadCatalogue,
    declineCardConsent,
    restart,
    canGoBack,
    getStepOrder: () => getStepOrder(draft.route),
  }
}

export type InquiryJourney = ReturnType<typeof useInquiryJourney>
