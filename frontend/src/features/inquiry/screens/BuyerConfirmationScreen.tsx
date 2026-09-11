/**
 * Buy receipt. No assumed availability or SLA.
 */
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import { formatPhone } from '../validation'
import { AppHeader, PrimaryButton } from '../../../components/ui'

export interface BuyerConfirmationScreenProps {
  readonly journey: InquiryJourney
}

export function BuyerConfirmationScreen({ journey }: BuyerConfirmationScreenProps) {
  const { draft, restart, entry } = journey
  const restartLabel = entry.sharedDevice ? copy.common.nextVisitor : copy.common.restart

  return (
    <div className="inquiry-app">
      <AppHeader />

      <main className="inquiry-main inquiry-main--with-header">
        <section className="confirm-hero section-gap">
          <div className="confirm-hero__mark" aria-hidden>
            ✓
          </div>
          <h1 className="screen-title screen-title--display">
            {copy.buyer.confirmationTitle}
          </h1>
          <p className="screen-subtitle" style={{ margin: '0 auto' }}>
            {copy.buyer.confirmationBody}
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
              <p className="card-row-label">{copy.buyer.whatYouNeed}</p>
              <p className="card-row-value">{draft.buyer.requirement}</p>
              {draft.buyer.otherProduct &&
              draft.buyer.otherProductDetail.trim() &&
              draft.buyer.otherProductDetail.trim() !== draft.buyer.requirement.trim() ? (
                <p className="card-row-value" style={{ marginTop: 8 }}>
                  {copy.buyer.otherProduct}: {draft.buyer.otherProductDetail.trim()}
                </p>
              ) : null}
            </div>
          </div>
          {draft.buyer.attachments.length > 0 ? (
            <div className="card-row">
              <div>
                <p className="card-row-label">{copy.buyer.attachmentsTitle}</p>
                <p className="card-row-value">
                  {draft.buyer.attachments.map((file) => file.name).join(', ')}
                </p>
              </div>
            </div>
          ) : null}
          <div className="card-row">
            <div>
              <p className="card-row-label">Contact</p>
              <p className="card-row-value">
                {draft.contact.fullName} · {draft.contact.workEmail} ·{' '}
                {formatPhone(draft.contact)}
              </p>
            </div>
          </div>
        </div>

        <PrimaryButton onClick={restart}>{restartLabel}</PrimaryButton>
      </main>
    </div>
  )
}
