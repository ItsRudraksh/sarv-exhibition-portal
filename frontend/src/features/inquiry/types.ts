export type InquiryRoute = 'SUPPLIER' | 'PURCHASE'

export type LifecycleState = 'DRAFT' | 'SUBMITTED'

export type InquiryStep =
  | 'card-capture'
  | 'contact-confirm'
  | 'intent-selection'
  | 'supplier-departments'
  | 'supplier-product-types'
  | 'supplier-smart-details'
  | 'supplier-review'
  | 'supplier-confirmation'
  | 'buyer-need'
  | 'buyer-review'
  | 'buyer-confirmation'

export type CardSide = 'front' | 'back'

export interface CardFileMeta {
  name: string
  size: number
  type: string
  previewUrl?: string
  assetId?: string
}

export interface ContactDetails {
  fullName: string
  workEmail: string
  countryCode: string
  mobileNumber: string
}

export interface SupplierDetails {
  companyName: string
  websiteUrl: string
  jobTitle: string
  locationFromCard: string
  catalogueFile: CardFileMeta | null
  otherCategory: boolean
  otherProductType: boolean
  otherCategoryDetail: string
  otherProductTypeDetail: string
  capabilityNotes: string
  attachments: CardFileMeta[]
}

export type PharmacopoeialStandard = 'IP' | 'USP' | 'BP' | 'EP'

export interface BuyerSpecifications {
  quantity: string
  packSize: string
  standard: PharmacopoeialStandard | ''
  neededByDate: string
  notes: string
}

export interface BuyerFinishedGoodSelection {
  finishedGoodId: string
  quantity: string
  /** Client display only; server persists id + quantity. */
  name?: string
}

export interface BuyerTradingProductSelection {
  tradingProductId: string
  quantity: string
  name?: string
}

export interface BuyerDetails {
  requirement: string
  productAreaSearch: string
  finishedGoods: BuyerFinishedGoodSelection[]
  tradingProducts: BuyerTradingProductSelection[]
  specifications: BuyerSpecifications
  attachments: CardFileMeta[]
  otherProduct: boolean
  otherProductDetail: string
}

export interface InquiryDraft {
  id: string
  lifecycleState: LifecycleState
  currentStep: InquiryStep
  route: InquiryRoute | null
  entryChannel: 'EXHIBITION_QR' | 'WEBSITE' | 'DIRECT'
  cardFront: CardFileMeta | null
  cardBack: CardFileMeta | null
  cardQrPayloadInternal: string | null
  contact: ContactDetails
  supplier: SupplierDetails
  departmentIds: string[]
  productTypeIds: string[]
  buyer: BuyerDetails
  contactConfirmed: boolean
  submittedAt: string | null
  referenceCode: string | null
}

/** In-memory only — not persisted. Lets Edit from review return without replaying Continue. */
export interface ReviewEditSession {
  returnTo: InquiryStep
  snapshot: InquiryDraft
  entryStep: InquiryStep
}

export function cloneInquiryDraft(draft: InquiryDraft): InquiryDraft {
  return structuredClone(draft)
}

function createDraftId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }

  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    const bytes = new Uint8Array(16)
    crypto.getRandomValues(bytes)
    bytes[6] = (bytes[6] & 0x0f) | 0x40
    bytes[8] = (bytes[8] & 0x3f) | 0x80
    const hex = [...bytes].map((b) => b.toString(16).padStart(2, '0')).join('')
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
  }

  return `draft-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
}

export const ATTACHMENT_MAX_BYTES = 5 * 1024 * 1024
export const ATTACHMENT_MAX_COUNT = 10

export const createEmptyDraft = (): InquiryDraft => ({
  id: createDraftId(),
  lifecycleState: 'DRAFT',
  currentStep: 'card-capture',
  route: null,
  entryChannel: 'EXHIBITION_QR',
  cardFront: null,
  cardBack: null,
  cardQrPayloadInternal: null,
  contact: {
    fullName: '',
    workEmail: '',
    countryCode: '+91',
    mobileNumber: '',
  },
  supplier: {
    companyName: '',
    websiteUrl: '',
    jobTitle: '',
    locationFromCard: '',
    catalogueFile: null,
    otherCategory: false,
    otherProductType: false,
    otherCategoryDetail: '',
    otherProductTypeDetail: '',
    capabilityNotes: '',
    attachments: [],
  },
  departmentIds: [],
  productTypeIds: [],
  buyer: {
    requirement: '',
    productAreaSearch: '',
    finishedGoods: [],
    tradingProducts: [],
    specifications: {
      quantity: '',
      packSize: '',
      standard: '',
      neededByDate: '',
      notes: '',
    },
    attachments: [],
    otherProduct: false,
    otherProductDetail: '',
  },
  contactConfirmed: false,
  submittedAt: null,
  referenceCode: null,
})

export const SUPPLIER_STEPS: InquiryStep[] = [
  'supplier-departments',
  'supplier-product-types',
  'supplier-smart-details',
  'supplier-review',
  'supplier-confirmation',
]

export const BUYER_STEPS: InquiryStep[] = [
  'buyer-need',
  'buyer-review',
  'buyer-confirmation',
]

export const SHARED_STEPS: InquiryStep[] = [
  'card-capture',
  'contact-confirm',
  'intent-selection',
]
