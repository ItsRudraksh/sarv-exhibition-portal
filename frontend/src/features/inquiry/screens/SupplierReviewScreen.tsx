/**
 * Sell submit: website or supporting file required. Edit returns without replaying Continue.
 */
import { useState } from 'react'
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import {
  getDepartmentsByIds,
  getProductTypesByIds,
} from '../taxonomy'
import { validateSupplierReview, formatPhone, formatOtherSelection } from '../validation'
import { InquiryAttachments } from '../InquiryAttachments'
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
  const {
    draft,
    updateDraft,
    goBack,
    submit,
    startEdit,
    submitting,
    submitError,
    uploadAttachments,
    removeAttachment,
    apiAvailable,
  } = journey
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [uploading, setUploading] = useState(false)

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

  const handleFiles = (list: FileList) => {
    setUploading(true)
    void uploadAttachments(list)
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
          editLabel={copy.common.edit}
          onEdit={() => startEdit('contact-confirm')}
          rows={[
            { label: 'Company', value: draft.supplier.companyName },
            { label: 'Contact', value: draft.contact.fullName },
            { label: 'Email', value: draft.contact.workEmail },
            { label: 'Mobile', value: formatPhone(draft.contact) },
          ]}
        />

        <SummaryCard
          title={copy.supplier.supplyCapability}
          editLabel={copy.common.edit}
          onEdit={() => startEdit('supplier-departments')}
          rows={[
            { label: 'Categories', value: categoryValue || '—' },
            { label: 'Product types', value: typeValue || '—' },
            {
              label: copy.supplier.capabilityLabel,
              value: draft.supplier.capabilityNotes.trim() || '—',
            },
          ]}
        />

        <section className="section-gap">
          <h2 style={{ margin: '0 0 12px', fontSize: '1rem', fontWeight: 600 }}>
            {copy.supplier.supportingInfo}
          </h2>
          <div className="stack-gap">
            <InquiryAttachments
              title={copy.supplier.attachmentsTitle}
              hint={copy.supplier.attachmentsHint}
              files={draft.supplier.attachments}
              uploading={uploading}
              error={errors.catalogue}
              apiAvailable={apiAvailable}
              onAdd={handleFiles}
              onRemove={(file) => void removeAttachment(file)}
            />

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
