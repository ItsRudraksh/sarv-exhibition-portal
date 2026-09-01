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

export function validateSupplierDepartments(departmentIds: string[]): FieldErrors {
  if (departmentIds.length === 0) {
    return { departments: 'Select at least one department.' }
  }
  return {}
}

export function validateSupplierProductTypes(productTypeIds: string[]): FieldErrors {
  if (productTypeIds.length === 0) {
    return { productTypes: 'Select at least one product type.' }
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

export function validateSupplierReview(supplier: SupplierDetails): FieldErrors {
  const errors: FieldErrors = {}
  const hasWebsite = supplier.websiteUrl.trim().length > 0
  const hasCatalogue = supplier.catalogueFile !== null

  if (!hasWebsite && !hasCatalogue) {
    errors.catalogue = 'Add a catalogue file or a website URL — at least one is required.'
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
  return errors
}

export function isSupplierDraftComplete(draft: InquiryDraft): boolean {
  return (
    isContactValid(draft.contact) &&
    draft.departmentIds.length > 0 &&
    draft.productTypeIds.length > 0 &&
    draft.supplier.companyName.trim().length > 0 &&
  (draft.supplier.websiteUrl.trim().length > 0 || draft.supplier.catalogueFile !== null)
  )
}

export function formatPhone(contact: ContactDetails): string {
  return `${contact.countryCode} ${contact.mobileNumber}`.trim()
}
