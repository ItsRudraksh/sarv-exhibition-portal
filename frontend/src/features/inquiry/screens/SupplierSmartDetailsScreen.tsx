import { useState } from 'react'
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import {
  getDepartmentsByIds,
  getProductTypesByIds,
} from '../taxonomy'
import {
  getMissingSupplierFields,
  validateSupplierSmartDetails,
  formatPhone,
} from '../validation'
import {
  AppHeader,
  FixedFooter,
  PrimaryButton,
  TextField,
} from '../../../components/ui'

export interface SupplierSmartDetailsScreenProps {
  readonly journey: InquiryJourney
}

export function SupplierSmartDetailsScreen({ journey }: SupplierSmartDetailsScreenProps) {
  const { draft, updateDraft, goBack, advance } = journey
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [optionalOpen, setOptionalOpen] = useState(false)

  const missing = getMissingSupplierFields(draft.supplier)
  const departments = getDepartmentsByIds(draft.departmentIds)
  const productTypes = getProductTypesByIds(draft.productTypeIds)

  const updateSupplier = (field: keyof typeof draft.supplier, value: string) => {
    updateDraft({
      supplier: { ...draft.supplier, [field]: value },
    })
  }

  const handleContinue = () => {
    const fieldErrors = validateSupplierSmartDetails(draft.supplier)
    setErrors(fieldErrors)
    if (Object.keys(fieldErrors).length === 0) {
      advance()
    }
  }

  return (
    <div className="inquiry-app">
      <AppHeader
        showBack
        onBack={goBack}
        stepLabel={copy.common.stepOf(3, 4)}
      />

      <main className="inquiry-main inquiry-main--with-header">
        <h1 className="screen-title" style={{ fontSize: '1.25rem' }}>
          {copy.supplier.smartDetailsTitle}
        </h1>
        <p className="screen-subtitle section-gap">
          {copy.supplier.smartDetailsSubtitle}
        </p>

        <section className="section-gap">
          <h3 className="step-label step-label--muted" style={{ marginBottom: 12 }}>
            Selected taxonomy
          </h3>
          <div className="card section-gap">
            <div className="card-row">
              <div>
                <p className="card-row-label">Departments</p>
                <p className="card-row-value">
                  {departments.map((d) => d.name).join(', ') || '—'}
                </p>
              </div>
            </div>
            <div className="card-row">
              <div>
                <p className="card-row-label">Product types</p>
                <p className="card-row-value">
                  {productTypes.map((p) => p.name).join(', ') || '—'}
                </p>
              </div>
            </div>
          </div>
        </section>

        <section className="section-gap">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
            <h3 className="step-label step-label--muted">{copy.supplier.fromCard}</h3>
          </div>
          <div className="card">
            <PreviewRow label="Company" value={draft.supplier.companyName || '—'} />
            <PreviewRow label="Contact" value={draft.contact.fullName || '—'} />
            <PreviewRow label="Work email" value={draft.contact.workEmail || '—'} />
            <PreviewRow label="Mobile" value={formatPhone(draft.contact) || '—'} />
            {draft.supplier.locationFromCard ? (
              <PreviewRow label="Location" value={draft.supplier.locationFromCard} />
            ) : null}
          </div>
        </section>

        <section className="section-gap">
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 16,
              marginBottom: 16,
            }}
          >
            <div style={{ flex: 1, height: 1, background: 'var(--color-glass-border)' }} />
            <h3 className="step-label step-label--muted">{copy.supplier.onlyMissing}</h3>
            <div style={{ flex: 1, height: 1, background: 'var(--color-glass-border)' }} />
          </div>

          {missing.length === 0 ? (
            <div
              className="card"
              style={{
                padding: 16,
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                background: 'var(--color-surface-container-low)',
              }}
            >
              <span style={{ color: 'var(--color-sarv-blue)' }} aria-hidden>✓</span>
              <p style={{ margin: 0, fontWeight: 500 }}>{copy.supplier.allFound}</p>
            </div>
          ) : (
            <div className="stack-gap">
              {missing.includes('companyName') ? (
                <TextField
                  id="companyName"
                  label="Company name"
                  value={draft.supplier.companyName}
                  onChange={(v) => updateSupplier('companyName', v)}
                  required
                  error={errors.companyName}
                />
              ) : null}
            </div>
          )}
        </section>

        <div className="accordion section-gap">
          <button
            type="button"
            className="accordion__trigger"
            aria-expanded={optionalOpen}
            onClick={() => setOptionalOpen(!optionalOpen)}
          >
            <div>
              <div style={{ fontWeight: 500 }}>{copy.supplier.optionalCompany}</div>
              <div style={{ fontSize: '0.875rem', color: 'var(--color-measured-slate)', marginTop: 4 }}>
                {copy.supplier.optionalCompanyHint}
              </div>
            </div>
            <span className={`accordion__chevron${optionalOpen ? ' accordion__chevron--open' : ''}`}>
              ▼
            </span>
          </button>
          {optionalOpen ? (
            <div className="accordion__content">
              <TextField
                id="jobTitle"
                label="Job title"
                value={draft.supplier.jobTitle}
                onChange={(v) => updateSupplier('jobTitle', v)}
              />
              <TextField
                id="locationFromCard"
                label="Location (from card or business address)"
                value={draft.supplier.locationFromCard}
                onChange={(v) => updateSupplier('locationFromCard', v)}
              />
            </div>
          ) : null}
        </div>
      </main>

      <FixedFooter note={copy.supplier.savedSelections}>
        <PrimaryButton onClick={handleContinue}>
          {copy.supplier.continueReview}
        </PrimaryButton>
      </FixedFooter>
    </div>
  )
}

function PreviewRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="card-row">
      <div style={{ flex: 1, overflow: 'hidden' }}>
        <p className="card-row-label">{label}</p>
        <p className="card-row-value">{value}</p>
      </div>
    </div>
  )
}
