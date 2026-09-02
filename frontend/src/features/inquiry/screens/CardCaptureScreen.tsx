import { useCallback, useRef, useState } from 'react'
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import type { CardSide } from '../types'
import {
  FixedFooter,
  Logo,
  Notice,
  PrimaryButton,
} from '../../../components/ui'
import {
  canUseLiveCamera,
  prepareImageForPreview,
  revokeCardPreview,
} from '../../../lib/imageProcessing'
import { LiveCameraSheet } from './LiveCameraSheet'

export interface CardCaptureScreenProps {
  readonly journey: InquiryJourney
}

export function CardCaptureScreen({ journey }: CardCaptureScreenProps) {
  const { draft, updateDraft, goToStep, uploadCard, declineCardConsent } = journey
  const fileRef = useRef<HTMLInputElement>(null)
  const pendingSideRef = useRef<CardSide>('front')
  const processingRef = useRef(false)
  const [processingSide, setProcessingSide] = useState<CardSide | null>(null)
  const [cameraSide, setCameraSide] = useState<CardSide | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [consentGranted, setConsentGranted] = useState(Boolean(draft.cardFront || draft.cardBack))

  const resetFileInput = () => {
    if (fileRef.current) fileRef.current.value = ''
  }

  const processBlob = async (side: CardSide, source: Blob) => {
    if (processingRef.current) return
    if (!consentGranted) {
      setError(copy.cardCapture.consentRequired)
      return
    }
    processingRef.current = true
    setError(null)
    setProcessingSide(side)
    try {
      const meta = await prepareImageForPreview(source)
      await uploadCard(side, source, meta.name, meta)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : copy.cardCapture.processingFailed)
    } finally {
      processingRef.current = false
      setProcessingSide(null)
      setCameraSide(null)
      resetFileInput()
    }
  }

  const requireConsent = (action: () => void) => {
    if (!consentGranted) {
      setError(copy.cardCapture.consentRequired)
      return
    }
    action()
  }

  const openFilePicker = (side: CardSide, mode: 'camera' | 'upload') => {
    pendingSideRef.current = side
    const input = fileRef.current
    if (!input) return
    if (mode === 'camera') {
      input.setAttribute('capture', 'environment')
    } else {
      input.removeAttribute('capture')
    }
    input.click()
  }

  const openCamera = (side: CardSide) => {
    setError(null)
    pendingSideRef.current = side
    if (canUseLiveCamera()) {
      setCameraSide(side)
      return
    }
    openFilePicker(side, 'camera')
  }

  const handleLiveUnavailable = useCallback(() => {
    setCameraSide(null)
    const input = fileRef.current
    if (!input) return
    input.setAttribute('capture', 'environment')
    input.click()
  }, [])

  const clearPhoto = (side: CardSide) => {
    if (side === 'front') {
      revokeCardPreview(draft.cardFront)
      updateDraft({ cardFront: null })
      return
    }
    revokeCardPreview(draft.cardBack)
    updateDraft({ cardBack: null })
  }

  const frontReady = draft.cardFront !== null

  return (
    <div className="inquiry-app">
      <header className="entry-header">
        <Logo variant="hero" />
        <span className="step-label">Start</span>
      </header>

      <main className="inquiry-main inquiry-main--centered-intro" style={{ paddingTop: 0 }}>
        <section className="section-gap">
          <h1 className="screen-title" style={{ fontSize: '1.5rem' }}>
            {copy.cardCapture.title}
          </h1>
          <p className="screen-subtitle">
            {copy.cardCapture.subtitle}
          </p>
        </section>

        <label className="notice" style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
          <input
            type="checkbox"
            checked={consentGranted}
            onChange={(e) => {
              setConsentGranted(e.target.checked)
              setError(null)
            }}
            style={{ width: 20, height: 20, marginTop: 2, flexShrink: 0 }}
          />
          <span>
            <strong style={{ display: 'block', marginBottom: 4 }}>{copy.cardCapture.consentTitle}</strong>
            {copy.cardCapture.consentBody}
          </span>
        </label>

        {error ? (
          <div className="notice notice--error" role="alert">
            <p>{error}</p>
          </div>
        ) : null}

        <section className="stack-gap section-gap">
          <CardSlot
            label={copy.cardCapture.frontLabel}
            slotLabel="1 OF 2"
            file={draft.cardFront}
            onCamera={() => requireConsent(() => openCamera('front'))}
            onUpload={() => requireConsent(() => openFilePicker('front', 'upload'))}
            onClear={() => clearPhoto('front')}
            active
            processing={processingSide === 'front'}
          />

          <CardSlot
            label={copy.cardCapture.backLabel}
            slotLabel="2 OF 2"
            file={draft.cardBack}
            onCamera={() => requireConsent(() => openCamera('back'))}
            onUpload={() => requireConsent(() => openFilePicker('back', 'upload'))}
            onClear={() => clearPhoto('back')}
            active={frontReady}
            inactiveMessage={copy.cardCapture.backLocked}
            processing={processingSide === 'back'}
          />

          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            className="sr-only"
            onChange={(e) => {
              const file = e.target.files?.[0]
              resetFileInput()
              if (file) void processBlob(pendingSideRef.current, file)
            }}
          />
        </section>

        <div style={{ textAlign: 'center', marginBottom: 'var(--space-section)' }}>
          <button
            type="button"
            className="btn-text"
            onClick={() => {
              void (async () => {
                await declineCardConsent()
                goToStep('contact-confirm')
              })()
            }}
            disabled={processingSide !== null}
          >
            {copy.cardCapture.continueWithout}
          </button>
        </div>

        <Notice>
          <p>{copy.cardCapture.qrNote}</p>
        </Notice>
      </main>

      {(draft.cardFront || draft.cardBack) && (
        <FixedFooter>
          <PrimaryButton
            onClick={() => goToStep('contact-confirm')}
            disabled={processingSide !== null}
          >
            {copy.common.continue}
          </PrimaryButton>
        </FixedFooter>
      )}

      {cameraSide ? (
        <LiveCameraSheet
          onCapture={(blob) => void processBlob(cameraSide, blob)}
          onCancel={() => setCameraSide(null)}
          onUnavailable={handleLiveUnavailable}
        />
      ) : null}
    </div>
  )
}

interface CardSlotProps {
  readonly label: string
  readonly slotLabel: string
  readonly file: { name: string; previewUrl?: string } | null
  readonly onCamera: () => void
  readonly onUpload: () => void
  readonly onClear: () => void
  readonly active?: boolean
  readonly inactiveMessage?: string
  readonly processing?: boolean
}

function CardSlot({
  label,
  slotLabel,
  file,
  onCamera,
  onUpload,
  onClear,
  active = true,
  inactiveMessage,
  processing = false,
}: CardSlotProps) {
  return (
    <div style={{ opacity: active ? 1 : 0.5 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          marginBottom: 'var(--space-label)',
          padding: '0 4px',
        }}
      >
        <span style={{ fontWeight: 500 }}>{label}</span>
        <span className="step-label step-label--muted">{slotLabel}</span>
      </div>
      <div className={`capture-slot${active ? '' : ' capture-slot--inactive'}`}>
        {file?.previewUrl ? (
          <img src={file.previewUrl} alt="" className="capture-slot__preview" />
        ) : active ? (
          <div className="capture-slot__actions">
            <button
              type="button"
              className="btn btn-primary"
              onClick={onCamera}
              disabled={processing}
            >
              {copy.cardCapture.useCamera}
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onUpload}
              disabled={processing}
            >
              {copy.cardCapture.upload}
            </button>
          </div>
        ) : (
          <p style={{ color: 'var(--color-measured-slate)', textAlign: 'center' }}>
            {inactiveMessage}
          </p>
        )}
        {file?.previewUrl ? (
          <>
            <button
              type="button"
              className="capture-slot__clear"
              aria-label={copy.cardCapture.removePhoto}
              onClick={onClear}
              disabled={processing}
            >
              <ClearIcon />
            </button>
            <div className="capture-slot__replace">
              <button
                type="button"
                className="capture-slot__chip"
                onClick={onCamera}
                disabled={processing}
              >
                {copy.cardCapture.retakePhoto}
              </button>
              <button
                type="button"
                className="capture-slot__chip"
                onClick={onUpload}
                disabled={processing}
              >
                {copy.cardCapture.upload}
              </button>
            </div>
          </>
        ) : null}
        {processing ? (
          <div className="capture-slot__processing" aria-live="polite">
            {copy.cardCapture.processing}
          </div>
        ) : null}
      </div>
    </div>
  )
}

function ClearIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path
        d="M4 4l8 8M12 4l-8 8"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
    </svg>
  )
}
