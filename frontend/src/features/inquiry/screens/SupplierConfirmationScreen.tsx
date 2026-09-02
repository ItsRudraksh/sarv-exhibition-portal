import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import {
  getDepartmentsByIds,
  getProductTypesByIds,
} from '../taxonomy'
import { formatPhone } from '../validation'
import { AppHeader, PrimaryButton } from '../../../components/ui'

export interface SupplierConfirmationScreenProps {
  readonly journey: InquiryJourney
}

export function SupplierConfirmationScreen({ journey }: SupplierConfirmationScreenProps) {
  const { draft, restart } = journey
  const departments = getDepartmentsByIds(draft.departmentIds)
  const productTypes = getProductTypesByIds(draft.productTypeIds)

  return (
    <div className="inquiry-app">
      <AppHeader />

      <main className="inquiry-main inquiry-main--with-header">
        <section className="section-gap" style={{ textAlign: 'center' }}>
          <div
            style={{
              width: 56,
              height: 56,
              borderRadius: '50%',
              background: 'var(--color-blue-mist)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 16px',
              color: 'var(--color-sarv-blue)',
              fontSize: '1.5rem',
            }}
            aria-hidden
          >
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
              <p className="card-row-label">Departments</p>
              <p className="card-row-value">
                {departments.map((d) => d.name).join(', ')}
              </p>
            </div>
          </div>
          <div className="card-row">
            <div>
              <p className="card-row-label">Product types</p>
              <p className="card-row-value">
                {productTypes.map((p) => p.name).join(', ')}
              </p>
            </div>
          </div>
          {draft.supplier.websiteUrl ? (
            <div className="card-row">
              <div>
                <p className="card-row-label">Website</p>
                <p className="card-row-value">{draft.supplier.websiteUrl}</p>
              </div>
            </div>
          ) : null}
          {draft.supplier.catalogueFile ? (
            <div className="card-row">
              <div>
                <p className="card-row-label">Catalogue</p>
                <p className="card-row-value">{draft.supplier.catalogueFile.name}</p>
              </div>
            </div>
          ) : null}
        </div>

        <PrimaryButton onClick={restart}>{copy.common.restart}</PrimaryButton>
      </main>
    </div>
  )
}
