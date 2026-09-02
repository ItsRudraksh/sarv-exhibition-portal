import { setLiveTaxonomy } from './taxonomy'
import type { CardFileMeta, InquiryDraft } from './types'
import { createEmptyDraft } from './types'

const STORAGE_KEY = 'sarv-inquiry-draft-v2'
const API_BASE = '/api/v1'

export interface InquiryDraftPort {
  load(): InquiryDraft | null
  save(draft: InquiryDraft): void
  clear(): void
}

export interface StoredFileAsset {
  id: string
  inquiryId: string
  purpose: string
  originalFilename: string
  mediaType: string
  byteSize: number
  securityScanState: string
  processingState: string
}

function fileSnapshot(file: CardFileMeta | null): CardFileMeta | null {
  if (!file) return null
  return {
    name: file.name,
    size: file.size,
    type: file.type,
    assetId: file.assetId,
  }
}

function stripPreviewUrls(draft: InquiryDraft): InquiryDraft {
  return {
    ...draft,
    cardFront: fileSnapshot(draft.cardFront),
    cardBack: fileSnapshot(draft.cardBack),
    supplier: {
      ...draft.supplier,
      catalogueFile: fileSnapshot(draft.supplier.catalogueFile),
    },
  }
}

function withFileUrls(draft: InquiryDraft): InquiryDraft {
  const id = draft.id
  const attach = (file: CardFileMeta | null, asImage: boolean): CardFileMeta | null => {
    if (!file) return null
    if (!asImage || !file.assetId) return file
    return {
      ...file,
      previewUrl: file.previewUrl ?? `${API_BASE}/inquiries/${id}/files/${file.assetId}`,
    }
  }
  return {
    ...draft,
    cardFront: attach(draft.cardFront, true),
    cardBack: attach(draft.cardBack, true),
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

class ApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

async function parseJson(response: Response): Promise<unknown> {
  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    ...init,
  })
  const body = await parseJson(response)
  if (!response.ok) {
    const message =
      body && typeof body === 'object' && 'message' in body
        ? String((body as { message: string }).message)
        : `Request failed (${response.status})`
    throw new ApiError(message)
  }
  return body as T
}

function asDraft(payload: InquiryDraft): InquiryDraft {
  return withFileUrls({
    ...createEmptyDraft(),
    ...payload,
    contact: { ...createEmptyDraft().contact, ...payload.contact },
    supplier: { ...createEmptyDraft().supplier, ...payload.supplier },
    buyer: {
      ...createEmptyDraft().buyer,
      ...payload.buyer,
      specifications: {
        ...createEmptyDraft().buyer.specifications,
        ...payload.buyer?.specifications,
      },
    },
    departmentIds: payload.departmentIds ?? [],
    productTypeIds: payload.productTypeIds ?? [],
  })
}

export const inquiryApi = {
  async create(draft?: InquiryDraft): Promise<InquiryDraft> {
    const created = await request<InquiryDraft>('/inquiries', {
      method: 'POST',
      body: JSON.stringify(draft ? stripPreviewUrls(draft) : {}),
    })
    return asDraft(created)
  },

  async get(id: string): Promise<InquiryDraft | null> {
    const response = await fetch(`${API_BASE}/inquiries/${id}`)
    if (response.status === 404) return null
    const body = await parseJson(response)
    if (!response.ok) {
      throw new ApiError('Could not load inquiry')
    }
    return asDraft(body as InquiryDraft)
  },

  async save(draft: InquiryDraft): Promise<InquiryDraft> {
    const saved = await request<InquiryDraft>(`/inquiries/${draft.id}`, {
      method: 'PATCH',
      body: JSON.stringify(stripPreviewUrls(draft)),
    })
    return asDraft(saved)
  },

  async confirmContact(draft: InquiryDraft): Promise<InquiryDraft> {
    const saved = await request<InquiryDraft>(`/inquiries/${draft.id}/contact`, {
      method: 'POST',
      body: JSON.stringify(stripPreviewUrls(draft)),
    })
    return asDraft(saved)
  },

  async submit(draft: InquiryDraft): Promise<InquiryDraft> {
    const saved = await request<InquiryDraft>(`/inquiries/${draft.id}/submit`, {
      method: 'POST',
      body: JSON.stringify(stripPreviewUrls(draft)),
    })
    return asDraft(saved)
  },

  async loadTaxonomy(): Promise<void> {
    const [departments, productTypes] = await Promise.all([
      request<{ id: string; name: string }[]>('/taxonomy/departments'),
      request<{ id: string; name: string; departmentIds: string[] }[]>('/taxonomy/product-types'),
    ])
    setLiveTaxonomy(departments, productTypes)
  },

  fileUrl(inquiryId: string, assetId: string): string {
    return `${API_BASE}/inquiries/${inquiryId}/files/${assetId}`
  },

  async uploadFile(
    inquiryId: string,
    file: Blob,
    purpose: 'BUSINESS_CARD' | 'CATALOGUE_ORIGINAL',
    side?: 'front' | 'back',
    filename?: string,
  ): Promise<StoredFileAsset> {
    const body = new FormData()
    body.append('file', file, filename ?? (file instanceof File ? file.name : 'upload'))
    const params = new URLSearchParams({ purpose })
    if (side) params.set('side', side)
    const response = await fetch(`${API_BASE}/inquiries/${inquiryId}/files?${params.toString()}`, {
      method: 'POST',
      body,
    })
    const parsed = await parseJson(response)
    if (!response.ok) {
      const message =
        parsed && typeof parsed === 'object' && 'message' in parsed
          ? String((parsed as { message: string }).message)
          : `Upload failed (${response.status})`
      throw new ApiError(message)
    }
    return parsed as StoredFileAsset
  },

  async recordConsent(inquiryId: string, purpose: string, decision: 'GRANTED' | 'DECLINED' | 'REVOKED') {
    return request(`/inquiries/${inquiryId}/consents`, {
      method: 'POST',
      body: JSON.stringify({ purpose, decision }),
    })
  },
}

export { ApiError }
