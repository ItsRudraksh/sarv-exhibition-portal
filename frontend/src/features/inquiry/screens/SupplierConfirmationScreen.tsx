/**
 * Sell receipt. Not an approval or vendor onboarding.
 */
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import {
  getDepartmentsByIds,
  getProductTypesByIds,
} from '../taxonomy'
import { formatPhone, formatOtherSelection } from '../validation'
import { AppHeader, PrimaryButton } from '../../../components/ui'

export interface SupplierConfirmationScreenProps {
  readonly journey: InquiryJourney
}

export function SupplierConfirmationScreen({ journey }: SupplierConfirmationScreenProps) {
  const { draft, restart, entry } = journey
  const restartLabel = entry.sharedDevice ? copy.common.nextVisitor : copy.common.restart
  const departments = getDepartmentsByIds(draft.departmentIds)
  const productTypes = getProductTypesByIds(draft.productTypeIds)

  return (
    <div className="inquiry-app">
      <AppHeader />

      <main className="inquiry-main inquiry-main--with-header">
        <section className="confirm-hero section-gap">
          <div className="confirm-hero__mark" aria-hidden>
            ✓
          </div>
          <h1 className="screen-title screen-title--display">
            {copy.supplier.confirmationTitle}
          </h1>
          <p className="screen-subtitle" style={{ margin: '0 auto' }}>
            {copy.supplier.confirmationBody}
          </p>
          {draft.referenceCode ? (
            <p className="field-hint" style={{ marginTop: 12 }}>
              {copy.common.receiptReference}: {draft.referenceCode}
            </p>
          ) : null}
        </section>

        <div className="card section-gap">
          <div className="card-row">
            <div>
              <p className="card-row-label">Company</p>
              <p className="card-row-value">{draft.supplier.companyName}</p>
            </div>
          </div>
          <div className="card-row">
            <div>
              <p className="card-row-label">Contact</p>
              <p className="card-row-value">
                {draft.contact.fullName} · {formatPhone(draft.contact)}
              </p>
            </div>
          </div>
          <div className="card-row">
            <div>
              <p className="card-row-label">Categories</p>
              <p className="card-row-value">
                {[
                  ...departments.map((d) => d.name),
                  formatOtherSelection(
                    draft.supplier.otherCategory,
                    copy.supplier.otherCategory,
                    draft.supplier.otherCategoryDetail,
                  ),
                ]
                  .filter(Boolean)
                  .join(', ') || '—'}
              </p>
            </div>
          </div>
          <div className="card-row">
            <div>
              <p className="card-row-label">Product types</p>
              <p className="card-row-value">
                {[
                  ...productTypes.map((p) => p.name),
                  formatOtherSelection(
                    draft.supplier.otherProductType,
                    copy.supplier.otherProductType,
                    draft.supplier.otherProductTypeDetail,
                  ),
                ]
                  .filter(Boolean)
                  .join(', ') || '—'}
              </p>
            </div>
          </div>
          {draft.supplier.capabilityNotes ? (
            <div className="card-row">
              <div>
                <p className="card-row-label">{copy.supplier.capabilityLabel}</p>
                <p className="card-row-value">{draft.supplier.capabilityNotes}</p>
              </div>
            </div>
          ) : null}
          {draft.supplier.websiteUrl ? (
            <div className="card-row">
              <div>
                <p className="card-row-label">Website</p>
                <p className="card-row-value">{draft.supplier.websiteUrl}</p>
              </div>
            </div>
          ) : null}
          {draft.supplier.attachments.length > 0 ? (
            <div className="card-row">
              <div>
                <p className="card-row-label">{copy.supplier.attachmentsTitle}</p>
                <p className="card-row-value">
                  {draft.supplier.attachments.map((file) => file.name).join(', ')}
                </p>
              </div>
            </div>
          ) : null}
        </div>

        <PrimaryButton onClick={restart}>{restartLabel}</PrimaryButton>
      </main>
    </div>
  )
}
