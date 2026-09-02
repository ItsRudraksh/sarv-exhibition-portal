import { copy } from './copy'
import { useInquiryJourney } from './useInquiryJourney'
import { CardCaptureScreen } from './screens/CardCaptureScreen'
import { ContactConfirmScreen } from './screens/ContactConfirmScreen'
import { IntentSelectionScreen } from './screens/IntentSelectionScreen'
import { SupplierDepartmentsScreen } from './screens/SupplierDepartmentsScreen'
import { SupplierProductTypesScreen } from './screens/SupplierProductTypesScreen'
import { SupplierSmartDetailsScreen } from './screens/SupplierSmartDetailsScreen'
import { SupplierReviewScreen } from './screens/SupplierReviewScreen'
import { SupplierConfirmationScreen } from './screens/SupplierConfirmationScreen'
import { BuyerNeedScreen } from './screens/BuyerNeedScreen'
import { BuyerReviewScreen } from './screens/BuyerReviewScreen'
import { BuyerConfirmationScreen } from './screens/BuyerConfirmationScreen'

export function InquiryApp() {
  const journey = useInquiryJourney()

  const handleRestart = () => {
    if (window.confirm(copy.common.restartConfirm)) {
      journey.restart()
    }
  }

  if (!journey.ready) {
    return (
      <div className="portal-viewport">
        <p className="screen-subtitle" style={{ padding: 24 }}>
          Loading inquiry…
        </p>
      </div>
    )
  }

  const screen = (() => {
    switch (journey.draft.currentStep) {
      case 'card-capture':
        return <CardCaptureScreen journey={journey} />
      case 'contact-confirm':
        return <ContactConfirmScreen journey={journey} />
      case 'intent-selection':
        return <IntentSelectionScreen journey={journey} />
      case 'supplier-departments':
        return <SupplierDepartmentsScreen journey={journey} />
      case 'supplier-product-types':
        return <SupplierProductTypesScreen journey={journey} />
      case 'supplier-smart-details':
        return <SupplierSmartDetailsScreen journey={journey} />
      case 'supplier-review':
        return <SupplierReviewScreen journey={journey} />
      case 'supplier-confirmation':
        return <SupplierConfirmationScreen journey={journey} />
      case 'buyer-need':
        return <BuyerNeedScreen journey={journey} />
      case 'buyer-review':
        return <BuyerReviewScreen journey={journey} />
      case 'buyer-confirmation':
        return <BuyerConfirmationScreen journey={journey} />
      default:
        return <CardCaptureScreen journey={journey} />
    }
  })()

  return (
    <>
      <div className="restart-bar">
        <button type="button" onClick={handleRestart} aria-label={copy.common.restart}>
          {copy.common.restart}
        </button>
      </div>
      <p className="sr-only" role="status">
        {copy.prototypeBanner}
      </p>
      {!journey.apiAvailable ? (
        <p className="field-hint" style={{ textAlign: 'center', margin: '8px 18px 0' }}>
          {copy.prototypeBanner}
        </p>
      ) : null}
      <div className="portal-viewport">{screen}</div>
    </>
  )
}
