import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, applyExtractionProposals, inquiryApi } from './api'
import {
  clearLegacyLocalDraft,
  createSeedDraft,
  parseEntryContext,
  sessionPointerPort,
  type EntryContext,
} from './entryContext'
import {
  BUYER_STEPS,
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
  const [entry] = useState<EntryContext>(() => parseEntryContext())
  const [draft, setDraft] = useState<InquiryDraft>(() => createSeedDraft(entry))
  const [ready, setReady] = useState(false)
  const [apiAvailable, setApiAvailable] = useState(false)
  const [connectionLost, setConnectionLost] = useState(() => !navigator.onLine)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [cardSuggestions, setCardSuggestions] = useState(false)
  const [campaignLabel, setCampaignLabel] = useState<string | null>(null)
  const draftRef = useRef(draft)
  const skipNextSave = useRef(true)
  const entryRef = useRef(entry)

  useEffect(() => {
    draftRef.current = draft
  }, [draft])

  useEffect(() => {
    entryRef.current = entry
  }, [entry])

  useEffect(() => {
    clearLegacyLocalDraft()
  }, [])

  useEffect(() => {
    const onOffline = () => {
      setConnectionLost(true)
      setApiAvailable(false)
    }
    const onOnline = () => {
      setConnectionLost(false)
      void (async () => {
        try {
          await inquiryApi.loadTaxonomy()
          setApiAvailable(true)
          if (draftRef.current.lifecycleState === 'DRAFT') {
            await inquiryApi.save(draftRef.current)
          }
        } catch {
          setApiAvailable(false)
          setConnectionLost(true)
        }
      })()
    }
    window.addEventListener('offline', onOffline)
    window.addEventListener('online', onOnline)
    return () => {
      window.removeEventListener('offline', onOffline)
      window.removeEventListener('online', onOnline)
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    const entry = entryRef.current

    const hydrate = async () => {
      try {
        await inquiryApi.loadTaxonomy()
        if (entry.campaignCode) {
          try {
            const campaign = await inquiryApi.getCampaign(entry.campaignCode)
            if (!cancelled) setCampaignLabel(campaign.label)
          } catch {
            if (!cancelled) {
              setSubmitError('That exhibition QR is not recognised. Ask stall staff for help.')
            }
          }
        }

        const pointerId = sessionPointerPort.loadId()
        if (pointerId) {
          const remote = await inquiryApi.get(pointerId)
          if (cancelled) return
          if (remote) {
            setApiAvailable(true)
            setConnectionLost(false)
            let merged = remote
            try {
              const extraction = await inquiryApi.latestExtraction(merged.id)
              merged = applyExtractionProposals(merged, extraction)
              setCardSuggestions(
                Boolean(extraction?.fields?.some((f) => f.reviewState === 'PENDING')),
              )
            } catch {
              setCardSuggestions(false)
            }
            sessionPointerPort.saveId(merged.id)
            setDraft(merged)
            setReady(true)
            return
          }
          sessionPointerPort.clear()
        }

        const created = await inquiryApi.create({
          entryChannel: entry.entryChannel,
          campaignCode: entry.campaignCode,
          staffAssisted: entry.staffAssisted,
        })
        if (cancelled) return
        setApiAvailable(true)
        setConnectionLost(false)
        sessionPointerPort.saveId(created.id)
        setDraft(created)
      } catch {
        if (cancelled) return
        setApiAvailable(false)
        setConnectionLost(!navigator.onLine)
        setDraft(createSeedDraft(entry))
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
    if (draft.id) {
      sessionPointerPort.saveId(draft.id)
    }
    if (skipNextSave.current) {
      skipNextSave.current = false
      return
    }
    if (!apiAvailable || draft.lifecycleState !== 'DRAFT') return
    const handle = window.setTimeout(() => {
      void inquiryApi.save(draftRef.current).catch(() => {
        setApiAvailable(false)
        setConnectionLost(true)
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
        sessionPointerPort.saveId(saved.id)
        return
      } catch (error) {
        setSubmitError(error instanceof ApiError ? error.message : 'Could not save contact details.')
        setConnectionLost(true)
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
      if (!apiAvailable) {
        setSubmitError(
          'No connection to the portal API. Reconnect to submit — a receipt is only issued online.',
        )
        return
      }
      const saved = await inquiryApi.submit(draftRef.current)
      skipNextSave.current = true
      setDraft(saved)
      sessionPointerPort.saveId(saved.id)
    } catch (error) {
      setSubmitError(
        error instanceof ApiError
          ? error.message
          : 'Could not submit. Check that the Java API is running.',
      )
      setConnectionLost(true)
      setApiAvailable(false)
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
        setSubmitError('Photo kept on this device only until the connection returns.')
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
      let next: InquiryDraft = { ...draftRef.current, [field]: meta }
      try {
        const extraction = await inquiryApi.latestExtraction(draftRef.current.id)
        next = applyExtractionProposals(next, extraction)
        setCardSuggestions(Boolean(extraction?.fields?.some((f) => f.reviewState === 'PENDING')))
      } catch {
        setCardSuggestions(false)
      }
      skipNextSave.current = true
      setDraft(next)
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
        setSubmitError('Catalogue kept on this device only until the connection returns.')
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
      // Offline or already recorded
    }
  }, [apiAvailable])

  const restart = useCallback(() => {
    sessionPointerPort.clear()
    clearLegacyLocalDraft()
    skipNextSave.current = true
    setSubmitError(null)
    setCardSuggestions(false)
    const entry = entryRef.current
    if (apiAvailable || navigator.onLine) {
      void inquiryApi
        .create({
          entryChannel: entry.entryChannel,
          campaignCode: entry.campaignCode,
          staffAssisted: entry.staffAssisted,
        })
        .then((created) => {
          sessionPointerPort.saveId(created.id)
          setApiAvailable(true)
          setConnectionLost(false)
          setDraft(created)
        })
        .catch(() => {
          setApiAvailable(false)
          setDraft(createSeedDraft(entry))
        })
      return
    }
    setDraft(createSeedDraft(entry))
  }, [apiAvailable])

  const canGoBack =
    draft.lifecycleState === 'DRAFT' &&
    getPreviousStep(draft) !== null &&
    draft.currentStep !== 'card-capture'

  return {
    draft,
    ready,
    apiAvailable,
    connectionLost,
    submitting,
    submitError,
    cardSuggestions,
    campaignLabel,
    entry,
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
