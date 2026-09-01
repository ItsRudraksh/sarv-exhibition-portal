import type { InquiryDraft } from './types'

const STORAGE_KEY = 'sarv-inquiry-draft-v1'

export interface InquiryDraftPort {
  load(): InquiryDraft | null
  save(draft: InquiryDraft): void
  clear(): void
}

function stripPreviewUrls(draft: InquiryDraft): InquiryDraft {
  return {
    ...draft,
    cardFront: draft.cardFront
      ? { name: draft.cardFront.name, size: draft.cardFront.size, type: draft.cardFront.type }
      : null,
    cardBack: draft.cardBack
      ? { name: draft.cardBack.name, size: draft.cardBack.size, type: draft.cardBack.type }
      : null,
    supplier: {
      ...draft.supplier,
      catalogueFile: draft.supplier.catalogueFile
        ? {
            name: draft.supplier.catalogueFile.name,
            size: draft.supplier.catalogueFile.size,
            type: draft.supplier.catalogueFile.type,
          }
        : null,
    },
  }
}

export const localStorageDraftPort: InquiryDraftPort = {
  load(): InquiryDraft | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) return null
      return JSON.parse(raw) as InquiryDraft
    } catch {
      return null
    }
  },

  save(draft: InquiryDraft): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(stripPreviewUrls(draft)))
  },

  clear(): void {
    localStorage.removeItem(STORAGE_KEY)
  },
}
