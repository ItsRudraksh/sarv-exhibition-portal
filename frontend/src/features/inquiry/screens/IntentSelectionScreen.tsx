import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import type { InquiryRoute } from '../types'
import {
  AppHeader,
  ChevronRightIcon,
  ShoppingBagIcon,
  StorefrontIcon,
} from '../../../components/ui'

export interface IntentSelectionScreenProps {
  readonly journey: InquiryJourney
}

export function IntentSelectionScreen({ journey }: IntentSelectionScreenProps) {
  const { goBack, selectRoute, goToStep } = journey

  const handleRoute = (route: InquiryRoute) => {
    selectRoute(route)
  }

  return (
    <div className="inquiry-app">
      <AppHeader showBack onBack={goBack} />

      <main className="inquiry-main inquiry-main--with-header">
        <div
          className="notice section-gap"
          style={{ borderLeft: '2px solid var(--color-sarv-blue)' }}
        >
          <div>
            <p style={{ margin: 0 }}>{copy.intent.saved}</p>
            <button
              type="button"
              className="btn-text"
              style={{ padding: 0, marginTop: 4 }}
              onClick={() => goToStep('contact-confirm')}
            >
              {copy.intent.changeContact}
            </button>
          </div>
        </div>

        <section className="section-gap">
          <h1 className="screen-title screen-title--display">{copy.intent.title}</h1>
          <p className="screen-subtitle">{copy.intent.subtitle}</p>
        </section>

        <div style={{ borderTop: '1px solid var(--color-glass-border)', borderBottom: '1px solid var(--color-glass-border)' }}>
          <button
            type="button"
            className="route-row"
            onClick={() => handleRoute('SUPPLIER')}
          >
            <div className="route-icon">
              <StorefrontIcon />
            </div>
            <div style={{ flex: 1 }}>
              <h2>{copy.intent.sellTitle}</h2>
              <p>{copy.intent.sellDesc}</p>
            </div>
            <ChevronRightIcon />
          </button>
          <button
            type="button"
            className="route-row"
            onClick={() => handleRoute('PURCHASE')}
          >
            <div className="route-icon">
              <ShoppingBagIcon />
            </div>
            <div style={{ flex: 1 }}>
              <h2>{copy.intent.buyTitle}</h2>
              <p>{copy.intent.buyDesc}</p>
            </div>
            <ChevronRightIcon />
          </button>
        </div>
      </main>

      <footer className="intent-footer">
        <p className="step-label step-label--muted" style={{ marginBottom: 12 }}>
          {copy.intent.footer}
        </p>
        <div style={{ display: 'flex', gap: 16, justifyContent: 'center', flexWrap: 'wrap' }}>
          <span className="step-label step-label--muted">{copy.intent.policyUnavailable}</span>
        </div>
      </footer>
    </div>
  )
}
