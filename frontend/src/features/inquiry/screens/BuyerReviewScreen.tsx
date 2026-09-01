import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import { formatPhone } from '../validation'
import {
  AppHeader,
  FixedFooter,
  Notice,
  PrimaryButton,
} from '../../../components/ui'

export interface BuyerReviewScreenProps {
  readonly journey: InquiryJourney
}

function formatSpecs(draft: InquiryJourney['draft']): string {
  const s = draft.buyer.specifications
  const parts: string[] = []
  if (s.quantity) parts.push(`Quantity: ${s.quantity}`)
  if (s.packSize) parts.push(`Pack size: ${s.packSize}`)
  if (s.standard) parts.push(`Standard: ${s.standard}`)
  if (s.neededByDate) parts.push(`Needed by: ${s.neededByDate}`)
  if (s.notes) parts.push(`Notes: ${s.notes}`)
  return parts.length > 0 ? parts.join(' · ') : copy.buyer.noSpecs
}

export function BuyerReviewScreen({ journey }: BuyerReviewScreenProps) {
  const { draft, goBack, submit, goToStep } = journey
  const fromCard = draft.cardFront !== null || draft.cardBack !== null

  return (
    <div className="inquiry-app">
      <AppHeader
        showBack
        onBack={goBack}
        stepLabel={copy.common.stepOf(2, 2)}
        subLabel="Review"
        progress={1}
      />

      <main className="inquiry-main inquiry-main--with-subheader">
        <section className="section-gap">
          <h1 className="screen-title">{copy.buyer.reviewTitle}</h1>
          <p className="screen-subtitle">{copy.buyer.reviewSubtitle}</p>
          <p className="field-hint" style={{ marginTop: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
            <span aria-hidden>☁</span> {copy.common.savedAutomatically}
          </p>
        </section>

        <section className="card section-gap" style={{ padding: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
            <h2 className="step-label step-label--muted">{copy.buyer.whatYouNeed}</h2>
            <button type="button" className="btn-text" onClick={() => goToStep('buyer-need')}>
              {copy.common.edit}
            </button>
          </div>
          <p style={{ margin: '0 0 16px', fontSize: '1.125rem', fontWeight: 600 }}>
            {draft.buyer.requirement}
          </p>
          <div style={{ borderTop: '1px solid var(--color-glass-border)', paddingTop: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <div>
                <p className="card-row-label">{copy.buyer.specifications}</p>
                <p style={{ margin: 0, color: 'var(--color-measured-slate)', fontStyle: 'italic' }}>
                  {formatSpecs(draft)}
                </p>
              </div>
              <button type="button" className="btn-text" onClick={() => goToStep('buyer-need')}>
                {copy.common.edit}
              </button>
            </div>
          </div>
        </section>

        <section className="card section-gap" style={{ padding: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h2 className="step-label step-label--muted">{copy.buyer.contactDetails}</h2>
            {fromCard ? <span className="badge">{copy.buyer.savedFromCard}</span> : null}
          </div>
          <div className="stack-gap">
            <div>
              <p className="card-row-label">Name</p>
              <p style={{ margin: 0 }}>{draft.contact.fullName}</p>
            </div>
            <div>
              <p className="card-row-label">Work email</p>
              <p style={{ margin: 0 }}>{draft.contact.workEmail}</p>
            </div>
            <div>
              <p className="card-row-label">Mobile</p>
              <p style={{ margin: 0 }}>{formatPhone(draft.contact)}</p>
            </div>
          </div>
          <div style={{ borderTop: '1px solid var(--color-glass-border)', marginTop: 16, paddingTop: 16, textAlign: 'center' }}>
            <button type="button" className="btn-text" onClick={() => goToStep('contact-confirm')}>
              {copy.buyer.editContact}
            </button>
          </div>
        </section>

        <Notice>
          <p>{copy.buyer.followUpNote}</p>
        </Notice>
      </main>

      <FixedFooter note={copy.buyer.savedUntilSubmit}>
        <PrimaryButton onClick={submit}>{copy.buyer.submit}</PrimaryButton>
      </FixedFooter>
    </div>
  )
}
