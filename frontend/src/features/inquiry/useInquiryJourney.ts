import { useCallback, useEffect, useState } from 'react'
import { localStorageDraftPort } from './api'
import {
  BUYER_STEPS,
  createEmptyDraft,
  SHARED_STEPS,
  SUPPLIER_STEPS,
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
  const [draft, setDraft] = useState<InquiryDraft>(() => {
    return localStorageDraftPort.load() ?? createEmptyDraft()
  })

  useEffect(() => {
    if (draft.lifecycleState === 'DRAFT') {
      localStorageDraftPort.save(draft)
    }
  }, [draft])

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

  const advanceAfterContact = useCallback(() => {
    setDraft((prev) => ({
      ...prev,
      contactConfirmed: true,
      currentStep: 'intent-selection',
    }))
  }, [])

  const advance = useCallback(() => {
    setDraft((prev) => {
      if (!prev.route && prev.currentStep === 'intent-selection') return prev
      if (!prev.route) return prev
      const next = getNextStepForRoute(prev.currentStep, prev.route)
      return { ...prev, currentStep: next }
    })
  }, [])

  const submit = useCallback(() => {
    setDraft((prev) => ({
      ...prev,
      lifecycleState: 'SUBMITTED',
      submittedAt: new Date().toISOString(),
      currentStep:
        prev.route === 'SUPPLIER'
          ? 'supplier-confirmation'
          : 'buyer-confirmation',
    }))
  }, [])

  const restart = useCallback(() => {
    localStorageDraftPort.clear()
    setDraft(createEmptyDraft())
  }, [])

  const canGoBack =
    draft.lifecycleState === 'DRAFT' &&
    getPreviousStep(draft) !== null &&
  draft.currentStep !== 'card-capture'

  return {
    draft,
    updateDraft,
    goToStep,
    goBack,
    selectRoute,
    advanceAfterContact,
    advance,
    submit,
    restart,
    canGoBack,
    getStepOrder: () => getStepOrder(draft.route),
  }
}

export type InquiryJourney = ReturnType<typeof useInquiryJourney>
