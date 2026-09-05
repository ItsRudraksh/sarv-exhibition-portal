import { setLiveTaxonomy } from './taxonomy'
import type { CardFileMeta, InquiryDraft } from './types'
import { createEmptyDraft } from './types'
import type { EntryChannel } from './entryContext'

const API_BASE = '/api/v1'

export interface CreateInquiryOptions {
  id?: string
  entryChannel?: EntryChannel
  campaignCode?: string | null
  staffAssisted?: boolean
}

export interface CampaignInfo {
  id: string
  code: string
  label: string
  landingRoute: string
  exhibitionId: string | null
  active: boolean
}

export interface AppMeta {
  poc: boolean
  referencePrefix: string
  stage: string
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

export interface ExtractedFieldProposal {
  id: string
  fieldKey: string
  proposedValueText: string | null
  confidenceScore: number | null
  reviewState: string
}

export interface ExtractionResult {
  id: string
  sessionId: string
  inquiryId: string
  feature: string
  state: string
  inputAssetId: string | null
  providerModelReference: string | null
  cardQrDetected: boolean
  completedAt: string | null
  fields: ExtractedFieldProposal[]
}

/** Prefill empty contact/supplier fields from PENDING proposals. Never overwrites typed values. */
export function applyExtractionProposals(
  draft: InquiryDraft,
  extraction: ExtractionResult | null,
): InquiryDraft {
  if (!extraction?.fields?.length || draft.contactConfirmed) {
    return draft
  }
  const pending = extraction.fields.filter((f) => f.reviewState === 'PENDING' && f.proposedValueText)
  if (pending.length === 0) {
    return draft
  }
  const byKey = new Map(pending.map((f) => [f.fieldKey, f.proposedValueText!.trim()]))
  const fill = (current: string, key: string) => {
    if (current.trim()) return current
    return byKey.get(key) ?? current
  }
  return {
    ...draft,
    contact: {
      fullName: fill(draft.contact.fullName, 'full_name'),
      workEmail: fill(draft.contact.workEmail, 'work_email'),
      countryCode: fill(draft.contact.countryCode, 'country_code'),
      mobileNumber: fill(draft.contact.mobileNumber, 'mobile_number'),
    },
    supplier: {
      ...draft.supplier,
      companyName: fill(draft.supplier.companyName, 'company_name'),
      jobTitle: fill(draft.supplier.jobTitle, 'job_title'),
      locationFromCard: fill(draft.supplier.locationFromCard, 'location_from_card'),
    },
  }
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
  async create(options?: CreateInquiryOptions): Promise<InquiryDraft> {
    const created = await request<InquiryDraft>('/inquiries', {
      method: 'POST',
      body: JSON.stringify({
        id: options?.id,
        entryChannel: options?.entryChannel ?? 'EXHIBITION_QR',
        campaignCode: options?.campaignCode ?? null,
        staffAssisted: options?.staffAssisted ?? false,
      }),
    })
    return asDraft(created)
  },

  async getCampaign(code: string): Promise<CampaignInfo> {
    return request<CampaignInfo>(`/campaigns/${encodeURIComponent(code)}`)
  },

  async getMeta(): Promise<AppMeta> {
    return request<AppMeta>('/meta')
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

  async latestExtraction(inquiryId: string): Promise<ExtractionResult | null> {
    const response = await fetch(`${API_BASE}/inquiries/${inquiryId}/extractions/latest`)
    if (response.status === 404) return null
    const body = await parseJson(response)
    if (!response.ok) {
      throw new ApiError('Could not load card suggestions')
    }
    return body as ExtractionResult
  },
}

export { ApiError }
