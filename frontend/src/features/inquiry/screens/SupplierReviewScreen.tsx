import { useRef, useState } from 'react'
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import {
  getDepartmentsByIds,
  getProductTypesByIds,
} from '../taxonomy'
import { validateSupplierReview, formatPhone } from '../validation'
import {
  AppHeader,
  FixedFooter,
  PrimaryButton,
  SummaryCard,
  TextField,
} from '../../../components/ui'

export interface SupplierReviewScreenProps {
  readonly journey: InquiryJourney
}

export function SupplierReviewScreen({ journey }: SupplierReviewScreenProps) {
  const { draft, updateDraft, goBack, submit, goToStep, submitting, submitError, uploadCatalogue, apiAvailable } = journey
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [uploading, setUploading] = useState(false)
  const fileRef = useRef<HTMLInputElement>(null)

  const departments = getDepartmentsByIds(draft.departmentIds)
  const productTypes = getProductTypesByIds(draft.productTypeIds)

  const handleFile = (file: File | undefined) => {
    if (!file) return
    setUploading(true)
    void uploadCatalogue(file)
      .then(() => {
        setErrors((e) => {
          const next = { ...e }
          delete next.catalogue
          return next
        })
      })
      .catch((caught: unknown) => {
        setErrors((e) => ({
          ...e,
          catalogue: caught instanceof Error ? caught.message : copy.cardCapture.processingFailed,
        }))
      })
      .finally(() => setUploading(false))
  }

  const handleSubmit = () => {
    const fieldErrors = validateSupplierReview(draft.supplier)
    setErrors(fieldErrors)
    if (Object.keys(fieldErrors).length === 0) {
      void submit()
    }
  }

  return (
    <div className="inquiry-app">
      <AppHeader showBack onBack={goBack} stepLabel={copy.common.stepOf(4, 4)} />

      <main className="inquiry-main inquiry-main--with-header">
        <h1 className="screen-title" style={{ fontSize: '1.25rem' }}>
          {copy.supplier.reviewTitle}
        </h1>
        <p className="screen-subtitle section-gap">{copy.supplier.reviewSubtitle}</p>

        <SummaryCard
          title={copy.supplier.companyContacts}
          onEdit={() => goToStep('supplier-smart-details')}
          rows={[
            { label: 'Company', value: draft.supplier.companyName },
            { label: 'Contact', value: draft.contact.fullName },
            { label: 'Email', value: draft.contact.workEmail },
            { label: 'Mobile', value: formatPhone(draft.contact) },
          ]}
        />

        <SummaryCard
          title={copy.supplier.supplyCapability}
          onEdit={() => goToStep('supplier-departments')}
          rows={[
            { label: 'Departments', value: departments.map((d) => d.name).join(', ') },
            { label: 'Product types', value: productTypes.map((p) => p.name).join(', ') },
          ]}
        />

        <section className="section-gap">
          <h2 style={{ margin: '0 0 12px', fontSize: '1rem', fontWeight: 600 }}>
            {copy.supplier.supportingInfo}
          </h2>
          <div className="stack-gap">
            <div className="card" style={{ padding: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
                <div>
                  <p style={{ margin: 0, fontWeight: 600, fontSize: '0.875rem' }}>
                    {copy.supplier.catalogue}
                  </p>
                  <p className="step-label step-label--muted" style={{ marginTop: 4 }}>
                    {copy.supplier.catalogueHint}
                  </p>
                  {draft.supplier.catalogueFile ? (
                    <p style={{ margin: '8px 0 0', fontSize: '0.875rem' }}>
                      {draft.supplier.catalogueFile.name} —{' '}
                      {draft.supplier.catalogueFile.assetId || apiAvailable
                        ? copy.common.uploaded
                        : copy.common.localFileOnly}
                    </p>
                  ) : null}
                </div>
                <button
                  type="button"
                  className="btn btn-secondary"
                  style={{ width: 'auto', minHeight: 36, fontSize: '0.875rem' }}
                  disabled={uploading}
                  onClick={() => fileRef.current?.click()}
                >
                  {uploading ? copy.cardCapture.uploading : 'Add catalogue'}
                </button>
              </div>
              <input
                ref={fileRef}
                type="file"
                accept=".pdf,image/*"
                className="sr-only"
                onChange={(e) => handleFile(e.target.files?.[0])}
              />
            </div>

            <TextField
              id="websiteUrl"
              label={copy.supplier.website}
              value={draft.supplier.websiteUrl}
              onChange={(v) =>
                updateDraft({ supplier: { ...draft.supplier, websiteUrl: v } })
              }
              placeholder="https://"
              hint={copy.supplier.websiteHint}
              error={errors.websiteUrl}
            />

            {errors.catalogue ? (
              <p className="field-error" role="alert">
                {errors.catalogue}
              </p>
            ) : null}
            {submitError ? (
              <p className="field-error" role="alert">
                {submitError}
              </p>
            ) : null}

            <p className="field-hint">{copy.supplier.websiteOrCatalogue}</p>
          </div>
        </section>
      </main>

      <FixedFooter note={copy.supplier.reviewQueueNote}>
        <PrimaryButton onClick={handleSubmit} disabled={submitting}>
          {submitting ? 'Submitting…' : copy.supplier.submit}
        </PrimaryButton>
      </FixedFooter>
    </div>
  )
}
