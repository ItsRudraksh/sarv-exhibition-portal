/**
 * I want to sell vs I want to buy. Sets draft.route then advances.
 */
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import type { InquiryRoute } from '../types'
import {
  AppHeader,
  ChevronRightIcon,
  ShoppingBagIcon,
  StorefrontIcon,
} from '../../../components/ui'
import { BrandArt } from '../../../components/BrandArt'

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

        <section className="intent-hero section-gap">
          <BrandArt />
          <div className="intent-hero__copy">
            <h1 className="screen-title screen-title--display">{copy.intent.title}</h1>
            <p className="screen-subtitle">{copy.intent.subtitle}</p>
          </div>
        </section>

        <div className="route-grid">
          <button
            type="button"
            className="route-card"
            onClick={() => handleRoute('SUPPLIER')}
          >
            <div className="route-icon">
              <StorefrontIcon />
            </div>
            <div className="route-card__copy">
              <h2>{copy.intent.sellTitle}</h2>
              <p>{copy.intent.sellDesc}</p>
            </div>
            <ChevronRightIcon />
          </button>
          <button
            type="button"
            className="route-card route-card--buy"
            onClick={() => handleRoute('PURCHASE')}
          >
            <div className="route-icon">
              <ShoppingBagIcon />
            </div>
            <div className="route-card__copy">
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
        <a className="brand-chrome__site intent-footer__link" href={copy.brand.siteUrl} target="_blank" rel="noreferrer">
          {copy.brand.siteLink}
        </a>
      </footer>
    </div>
  )
}
