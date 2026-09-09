import type { BuyerDetails, ContactDetails, InquiryDraft, SupplierDetails } from './types'

export interface FieldErrors {
  [key: string]: string
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validateContact(contact: ContactDetails): FieldErrors {
  const errors: FieldErrors = {}

  if (!contact.fullName.trim()) {
    errors.fullName = 'Full name is required.'
  }

  if (!contact.workEmail.trim()) {
    errors.workEmail = 'Work email is required.'
  } else if (!EMAIL_RE.test(contact.workEmail.trim())) {
    errors.workEmail = 'Enter a valid work email address.'
  }

  if (!contact.mobileNumber.trim()) {
    errors.mobileNumber = 'Mobile number is required.'
  } else if (contact.mobileNumber.replace(/\D/g, '').length < 6) {
    errors.mobileNumber = 'Enter a valid mobile number.'
  }

  if (!contact.countryCode.trim()) {
    errors.countryCode = 'Country code is required.'
  }

  return errors
}

export function isContactValid(contact: ContactDetails): boolean {
  return Object.keys(validateContact(contact)).length === 0
}

export function validateSupplierDepartments(
  departmentIds: string[],
  otherCategory: boolean,
): FieldErrors {
  if (departmentIds.length === 0 && !otherCategory) {
    return { departments: 'Select at least one category, or choose Other.' }
  }
  return {}
}

export function validateSupplierProductTypes(
  productTypeIds: string[],
  otherProductType: boolean,
  capabilityNotes: string,
): FieldErrors {
  const errors: FieldErrors = {}
  if (!capabilityNotes.trim()) {
    errors.capabilityNotes = 'Describe what you can supply.'
  }
  if (productTypeIds.length === 0 && !otherProductType && !capabilityNotes.trim()) {
    errors.productTypes = 'Select a product type, Other, or describe what you can supply.'
  }
  return errors
}

export function getMissingSupplierFields(supplier: SupplierDetails): string[] {
  const missing: string[] = []
  if (!supplier.companyName.trim()) missing.push('companyName')
  return missing
}

export function validateSupplierSmartDetails(supplier: SupplierDetails): FieldErrors {
  const errors: FieldErrors = {}
  if (!supplier.companyName.trim()) {
    errors.companyName = 'Company name is required.'
  }
  return errors
}

export function hasSupportingFiles(supplier: SupplierDetails): boolean {
  if (supplier.catalogueFile !== null) return true
  return supplier.attachments.some((file) => file.name.trim().length > 0)
}

export function validateSupplierReview(supplier: SupplierDetails): FieldErrors {
  const errors: FieldErrors = {}
  const hasWebsite = supplier.websiteUrl.trim().length > 0
  const hasFiles = hasSupportingFiles(supplier)

  if (!hasWebsite && !hasFiles) {
    errors.catalogue = 'Add a supporting file or a website URL — at least one is required.'
  }

  if (hasWebsite) {
    try {
      const url = supplier.websiteUrl.startsWith('http')
        ? supplier.websiteUrl
        : `https://${supplier.websiteUrl}`
      new URL(url)
    } catch {
      errors.websiteUrl = 'Enter a valid website URL.'
    }
  }

  return errors
}

export function validateBuyerNeed(buyer: BuyerDetails): FieldErrors {
  const errors: FieldErrors = {}
  if (!buyer.requirement.trim()) {
    errors.requirement = 'Describe the product or requirement to continue.'
  }
  for (const row of buyer.finishedGoods) {
    if (!row.quantity.trim()) {
      errors[`qty-PHARMA_ERP-${row.finishedGoodId}`] =
        'Enter a quantity for each selected product.'
    }
  }
  for (const row of buyer.tradingProducts) {
    if (!row.quantity.trim()) {
      errors[`qty-TRADING-${row.tradingProductId}`] =
        'Enter a quantity for each selected product.'
    }
  }
  return errors
}

export function isSupplierDraftComplete(draft: InquiryDraft): boolean {
  return (
    isContactValid(draft.contact) &&
    (draft.departmentIds.length > 0 || draft.supplier.otherCategory) &&
    draft.supplier.capabilityNotes.trim().length > 0 &&
    draft.supplier.companyName.trim().length > 0 &&
    (draft.supplier.websiteUrl.trim().length > 0 || hasSupportingFiles(draft.supplier))
  )
}

export function formatPhone(contact: ContactDetails): string {
  return `${contact.countryCode} ${contact.mobileNumber}`.trim()
}
