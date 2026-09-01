import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import { formatPhone } from '../validation'
import { AppHeader, PrimaryButton } from '../../../components/ui'

export interface BuyerConfirmationScreenProps {
  readonly journey: InquiryJourney
}

export function BuyerConfirmationScreen({ journey }: BuyerConfirmationScreenProps) {
  const { draft, restart } = journey

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
            {copy.buyer.confirmationTitle}
          </h1>
          <p className="screen-subtitle" style={{ margin: '0 auto' }}>
            {copy.buyer.confirmationBody}
          </p>
        </section>

        <div className="card section-gap">
          <div className="card-row">
            <div>
              <p className="card-row-label">{copy.buyer.whatYouNeed}</p>
              <p className="card-row-value">{draft.buyer.requirement}</p>
            </div>
          </div>
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

        <PrimaryButton onClick={restart}>{copy.common.restart}</PrimaryButton>
      </main>
    </div>
  )
}
