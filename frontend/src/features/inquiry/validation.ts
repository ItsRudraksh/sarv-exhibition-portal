/**
 * Client submit/continue rules. Keep aligned with backend InquiryRules.
 */
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

/** Continue on sell categories: listed, Other+details, or capability notes. */
export function validateSupplierDepartments(
  departmentIds: string[],
  otherCategory: boolean,
  otherCategoryDetail: string,
  capabilityNotes: string,
): FieldErrors {
  if (otherCategory && !otherCategoryDetail.trim()) {
    return { otherCategory: 'Describe the other category.' }
  }
  if (departmentIds.length === 0 && !otherCategory && !capabilityNotes.trim()) {
    return { departments: 'Select a category, choose Other, or describe what you can supply.' }
  }
  return {}
}

/** Continue on sell product types: listed, Other+details, or capability notes. */
export function validateSupplierProductTypes(
  productTypeIds: string[],
  otherProductType: boolean,
  otherProductTypeDetail: string,
  capabilityNotes: string,
): FieldErrors {
  if (otherProductType && !otherProductTypeDetail.trim()) {
    return { otherProductType: 'Describe the other product type.' }
  }
  if (productTypeIds.length === 0 && !otherProductType && !capabilityNotes.trim()) {
    return { productTypes: 'Select a product type, choose Other, or describe what you can supply.' }
  }
  return {}
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

/** Buy continue: requirement text, or Other with details. Quantity per selected row. */
export function validateBuyerNeed(buyer: BuyerDetails): FieldErrors {
  const errors: FieldErrors = {}
  if (!buyer.requirement.trim() && !(buyer.otherProduct && buyer.otherProductDetail.trim())) {
    errors.requirement = 'Describe the product or requirement to continue.'
  }
  if (buyer.otherProduct && !buyer.otherProductDetail.trim()) {
    errors.otherProduct = 'Describe the other product.'
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
    (draft.departmentIds.length > 0 ||
      (draft.supplier.otherCategory && draft.supplier.otherCategoryDetail.trim().length > 0) ||
      draft.supplier.capabilityNotes.trim().length > 0) &&
    (draft.productTypeIds.length > 0 ||
      (draft.supplier.otherProductType &&
        draft.supplier.otherProductTypeDetail.trim().length > 0) ||
      draft.supplier.capabilityNotes.trim().length > 0) &&
    draft.supplier.companyName.trim().length > 0 &&
    (draft.supplier.websiteUrl.trim().length > 0 || hasSupportingFiles(draft.supplier))
  )
}

/** Review line for Other: `Label: details` when checked. */
export function formatOtherSelection(
  selected: boolean,
  label: string,
  detail: string,
): string {
  if (!selected) return ''
  const trimmed = detail.trim()
  return trimmed ? `${label}: ${trimmed}` : label
}

export function formatPhone(contact: ContactDetails): string {
  return `${contact.countryCode} ${contact.mobileNumber}`.trim()
}
