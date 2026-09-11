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
  formatOtherSelection,
} from '../validation'
import { ReviewEditFooter } from '../ReviewEditFooter'
import {
  AppHeader,
  FixedFooter,
  PrimaryButton,
  SummaryCard,
  TextField,
} from '../../../components/ui'

export interface SupplierSmartDetailsScreenProps {
  readonly journey: InquiryJourney
}

export function SupplierSmartDetailsScreen({ journey }: SupplierSmartDetailsScreenProps) {
  const { draft, updateDraft, goBack, advance, editing, startEdit, finishEdit, cancelEdit } =
    journey
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [optionalOpen, setOptionalOpen] = useState(false)

  const missing = getMissingSupplierFields(draft.supplier)
  const departments = getDepartmentsByIds(draft.departmentIds)
  const productTypes = getProductTypesByIds(draft.productTypeIds)
  const categoryValue = [
    ...departments.map((d) => d.name),
    formatOtherSelection(
      draft.supplier.otherCategory,
      copy.supplier.otherCategory,
      draft.supplier.otherCategoryDetail,
    ),
  ]
    .filter(Boolean)
    .join(', ')
  const typeValue = [
    ...productTypes.map((p) => p.name),
    formatOtherSelection(
      draft.supplier.otherProductType,
      copy.supplier.otherProductType,
      draft.supplier.otherProductTypeDetail,
    ),
  ]
    .filter(Boolean)
    .join(', ')

  const updateSupplier = (field: keyof typeof draft.supplier, value: string) => {
    updateDraft({
      supplier: { ...draft.supplier, [field]: value },
    })
  }

  const handleContinue = () => {
    const fieldErrors = validateSupplierSmartDetails(draft.supplier)
    setErrors(fieldErrors)
    if (Object.keys(fieldErrors).length === 0) {
      if (editing) {
        finishEdit()
        return
      }
      advance()
    }
  }

  return (
    <div className="inquiry-app">
      <AppHeader
        showBack
        onBack={goBack}
        stepLabel={editing ? copy.common.edit : copy.common.stepOf(3, 4)}
      />

      <main className="inquiry-main inquiry-main--with-header">
        <h1 className="screen-title" style={{ fontSize: '1.25rem' }}>
          {copy.supplier.smartDetailsTitle}
        </h1>
        <p className="screen-subtitle section-gap">
          {copy.supplier.smartDetailsSubtitle}
        </p>

        <SummaryCard
          title={copy.supplier.selectedTaxonomy}
          editLabel={copy.common.edit}
          onEdit={() => startEdit('supplier-departments')}
          rows={[
            { label: 'Categories', value: categoryValue || '—' },
            { label: 'Product types', value: typeValue || '—' },
          ]}
        />

        <SummaryCard
          title={copy.supplier.fromCard}
          editLabel={copy.common.edit}
          onEdit={() => startEdit('contact-confirm')}
          rows={[
            { label: 'Company', value: draft.supplier.companyName || '—' },
            { label: 'Contact', value: draft.contact.fullName || '—' },
            { label: 'Work email', value: draft.contact.workEmail || '—' },
            { label: 'Mobile', value: formatPhone(draft.contact) || '—' },
            ...(draft.supplier.locationFromCard
              ? [{ label: 'Location', value: draft.supplier.locationFromCard }]
              : []),
          ]}
        />

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

      {editing ? (
        <ReviewEditFooter
          canSave={Object.keys(validateSupplierSmartDetails(draft.supplier)).length === 0}
          onCancel={cancelEdit}
          onSave={handleContinue}
        />
      ) : (
        <FixedFooter note={copy.supplier.savedSelections}>
          <PrimaryButton onClick={handleContinue}>
            {copy.supplier.continueReview}
          </PrimaryButton>
        </FixedFooter>
      )}
    </div>
  )
}
