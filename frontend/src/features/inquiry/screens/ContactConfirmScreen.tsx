import { useState } from 'react'
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import { validateContact } from '../validation'
import {
  AppHeader,
  FixedFooter,
  Notice,
  PrimaryButton,
  TextField,
} from '../../../components/ui'

const COUNTRY_CODES = [
  { value: '+91', label: '+91 (IN)' },
  { value: '+1', label: '+1 (US)' },
  { value: '+44', label: '+44 (UK)' },
  { value: '+61', label: '+61 (AU)' },
  { value: '+49', label: '+49 (DE)' },
]

export interface ContactConfirmScreenProps {
  readonly journey: InquiryJourney
}

export function ContactConfirmScreen({ journey }: ContactConfirmScreenProps) {
  const { draft, updateDraft, goBack, advanceAfterContact, goToStep } = journey
  const [errors, setErrors] = useState<Record<string, string>>({})
  const fromCard = draft.cardFront !== null || draft.cardBack !== null

  const handleContinue = () => {
    const fieldErrors = validateContact(draft.contact)
    setErrors(fieldErrors)
    if (Object.keys(fieldErrors).length === 0) {
      advanceAfterContact()
    }
  }

  const updateContact = (field: keyof typeof draft.contact, value: string) => {
    updateDraft({
      contact: { ...draft.contact, [field]: value },
    })
  }

  return (
    <div className="inquiry-app">
      <AppHeader
        showBack
        onBack={goBack}
        subLabel="DETAILS CHECK"
      />

      <main className="inquiry-main inquiry-main--with-subheader">
        <section className="section-gap">
          <h1 className="screen-title">{copy.contact.title}</h1>
          <p className="screen-subtitle">{copy.contact.subtitle}</p>
          {fromCard ? (
            <div className="badge" style={{ marginTop: 12 }}>
              {copy.contact.fromCard}
            </div>
          ) : null}
        </section>

        <section className="stack-gap section-gap">
          <TextField
            id="fullName"
            label="Full name"
            value={draft.contact.fullName}
            onChange={(v) => updateContact('fullName', v)}
            required
            error={errors.fullName}
          />
          <TextField
            id="workEmail"
            label="Work email"
            type="email"
            value={draft.contact.workEmail}
            onChange={(v) => updateContact('workEmail', v)}
            required
            error={errors.workEmail}
          />
          <div className="field">
            <label htmlFor="mobileNumber">Mobile number *</label>
            <div style={{ display: 'flex', gap: 8 }}>
              <select
                id="countryCode"
                value={draft.contact.countryCode}
                onChange={(e) => updateContact('countryCode', e.target.value)}
                style={{ width: 110, flexShrink: 0 }}
                aria-label="Country code"
              >
                {COUNTRY_CODES.map((c) => (
                  <option key={c.value} value={c.value}>
                    {c.label}
                  </option>
                ))}
              </select>
              <input
                id="mobileNumber"
                type="tel"
                value={draft.contact.mobileNumber}
                onChange={(e) => updateContact('mobileNumber', e.target.value)}
                aria-invalid={!!errors.mobileNumber}
                aria-describedby={errors.mobileNumber ? 'mobile-error' : undefined}
                style={{ flex: 1 }}
              />
            </div>
            {errors.mobileNumber ? (
              <p id="mobile-error" className="field-error" role="alert">
                {errors.mobileNumber}
              </p>
            ) : null}
          </div>
        </section>

        <Notice>
          <p>{copy.contact.savedNote}</p>
        </Notice>
      </main>

      <FixedFooter>
        {fromCard ? (
          <button
            type="button"
            className="btn-text"
            onClick={() => goToStep('card-capture')}
          >
            {copy.contact.retake}
          </button>
        ) : null}
        <PrimaryButton onClick={handleContinue}>{copy.contact.continue}</PrimaryButton>
      </FixedFooter>
    </div>
  )
}
