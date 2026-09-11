/**
 * Staff/admin REST + sessionStorage Basic header. Used by both /staff and /admin.
 */
import { withPublicBase } from '../../lib/publicPath'

const STAFF_AUTH_KEY = 'sarv-staff-basic-v1'

export function staffAuthHeader(): string | null {
  return sessionStorage.getItem(STAFF_AUTH_KEY)
}

export function setStaffAuth(email: string, password: string): void {
  sessionStorage.setItem(STAFF_AUTH_KEY, btoa(`${email}:${password}`))
}

export function clearStaffAuth(): void {
  sessionStorage.removeItem(STAFF_AUTH_KEY)
}

async function staffFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const auth = staffAuthHeader()
  const headers = new Headers(init.headers)
  if (auth) {
    headers.set('Authorization', `Basic ${auth}`)
  }
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  return fetch(withPublicBase(`/api/v1/staff${path}`), { ...init, headers })
}

export interface StaffMe {
  id: string
  email: string
  displayName: string
  roles: string[]
}

export interface StaffRole {
  code: string
  name: string
  description: string | null
}

export interface StaffAccount {
  id: string
  email: string
  displayName: string
  status: string
  roles: string[]
  createdAt: string | null
  updatedAt: string | null
}

export interface StaffAccountWrite {
  email: string
  displayName: string
  password?: string | null
  roles: string[]
  status: string
}

export interface TradingProduct {
  id: string
  name: string
  listedForBuyers: boolean
  active: boolean
}

export interface TradingSupplier {
  id: string
  sourceKind: 'OFFLINE' | 'PORTAL' | string
  portalInquiryId: string | null
  companyName: string
  contactName: string | null
  email: string | null
  phone: string | null
  websiteUrl: string | null
  notes: string | null
  status: string
  products: TradingProduct[]
  suggestedProductNames: string[]
}

export interface PortalSupplierCandidate {
  inquiryId: string
  referenceCode: string
  companyName: string | null
  personName: string | null
  reviewState: string
}

export interface OfflineSupplierWrite {
  companyName: string
  contactName?: string | null
  email?: string | null
  phone?: string | null
  websiteUrl?: string | null
  notes?: string | null
}

export interface SupplierReview {
  id: string
  referenceCode: string
  submittedAt: string | null
  reviewState: string
  productionState: string
  deliveryState: string | null
  websiteUrl: string | null
  capabilityNotes: string | null
  approvedAt: string | null
  approvedByUserId: string | null
  companyName: string | null
  personName: string | null
  email: string | null
  phone: string | null
}

export interface BuyerLead {
  id: string
  referenceCode: string
  submittedAt: string | null
  leadState: string
  marketingNotes: string | null
  companyName: string | null
  personName: string | null
  email: string | null
  phone: string | null
  requirement: string | null
  deliveryState: string | null
}

export interface ExportJob {
  id: string
  scope: string
  state: string
  originalFilename: string | null
  mediaType: string | null
  byteSize: number | null
  expiresAt: string | null
  generatedAt: string | null
  failureReason: string | null
}

async function readError(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string }
    if (body.message) return body.message
  } catch {
    /* ignore */
  }
  if (response.status === 401) return 'Sign in required.'
  if (response.status === 403) return 'This role cannot use that action.'
  return `Request failed (${response.status})`
}

export const staffApi = {
  async me(): Promise<StaffMe> {
    const response = await staffFetch('/me')
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<StaffMe>
  },

  async suppliers(): Promise<SupplierReview[]> {
    const response = await staffFetch('/suppliers')
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<SupplierReview[]>
  },

  async decide(id: string, decision: string, notes: string): Promise<SupplierReview> {
    const response = await staffFetch(`/suppliers/${id}/decisions`, {
      method: 'POST',
      body: JSON.stringify({ decision, notes }),
    })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<SupplierReview>
  },

  async buyers(): Promise<BuyerLead[]> {
    const response = await staffFetch('/buyers')
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<BuyerLead[]>
  },

  async saveBuyerNotes(id: string, notes: string): Promise<BuyerLead> {
    const response = await staffFetch(`/buyers/${id}/notes`, {
      method: 'POST',
      body: JSON.stringify({ notes }),
    })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<BuyerLead>
  },

  async createExport(): Promise<ExportJob> {
    const response = await staffFetch('/exports', { method: 'POST' })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<ExportJob>
  },

  async downloadExport(id: string): Promise<void> {
    const response = await staffFetch(`/exports/${id}/file`)
    if (!response.ok) throw new Error(await readError(response))
    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'purchase-leads.xlsx'
    link.click()
    URL.revokeObjectURL(url)
  },

  async syncFinishedGoods(): Promise<{
    state: string
    rowsUpserted: number
    rowsDeactivated: number
    message: string
  }> {
    const response = await staffFetch('/finished-goods/sync', { method: 'POST' })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<{
      state: string
      rowsUpserted: number
      rowsDeactivated: number
      message: string
    }>
  },

  async finishedGoodsCount(): Promise<number> {
    const response = await staffFetch('/finished-goods/count')
    if (!response.ok) throw new Error(await readError(response))
    const body = (await response.json()) as { active: number }
    return body.active
  },

  async listRoles(): Promise<StaffRole[]> {
    const response = await staffFetch('/roles')
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<StaffRole[]>
  },

  async listUsers(): Promise<StaffAccount[]> {
    const response = await staffFetch('/users')
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<StaffAccount[]>
  },

  async createUser(body: StaffAccountWrite): Promise<StaffAccount> {
    const response = await staffFetch('/users', {
      method: 'POST',
      body: JSON.stringify(body),
    })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<StaffAccount>
  },

  async updateUser(id: string, body: StaffAccountWrite): Promise<StaffAccount> {
    const response = await staffFetch(`/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<StaffAccount>
  },

  async deactivateUser(id: string): Promise<StaffAccount> {
    const response = await staffFetch(`/users/${id}`, { method: 'DELETE' })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<StaffAccount>
  },

  async listTradingSuppliers(): Promise<TradingSupplier[]> {
    const response = await staffFetch('/trading-suppliers')
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<TradingSupplier[]>
  },

  async portalCandidates(): Promise<PortalSupplierCandidate[]> {
    const response = await staffFetch('/trading-suppliers/portal-candidates')
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<PortalSupplierCandidate[]>
  },

  async createOfflineSupplier(body: OfflineSupplierWrite): Promise<TradingSupplier> {
    const response = await staffFetch('/trading-suppliers', {
      method: 'POST',
      body: JSON.stringify(body),
    })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<TradingSupplier>
  },

  async linkPortalSupplier(inquiryId: string): Promise<TradingSupplier> {
    const response = await staffFetch('/trading-suppliers/from-portal', {
      method: 'POST',
      body: JSON.stringify({ inquiryId }),
    })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<TradingSupplier>
  },

  async updateTradingSupplier(
    id: string,
    body: OfflineSupplierWrite & { status?: string },
  ): Promise<TradingSupplier> {
    const response = await staffFetch(`/trading-suppliers/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<TradingSupplier>
  },

  async deactivateTradingSupplier(id: string): Promise<TradingSupplier> {
    const response = await staffFetch(`/trading-suppliers/${id}`, { method: 'DELETE' })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<TradingSupplier>
  },

  async addTradingProduct(
    supplierId: string,
    body: { name: string; listedForBuyers: boolean },
  ): Promise<TradingProduct> {
    const response = await staffFetch(`/trading-suppliers/${supplierId}/products`, {
      method: 'POST',
      body: JSON.stringify(body),
    })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<TradingProduct>
  },

  async updateTradingProduct(
    supplierId: string,
    productId: string,
    body: { name: string; listedForBuyers: boolean },
  ): Promise<TradingProduct> {
    const response = await staffFetch(`/trading-suppliers/${supplierId}/products/${productId}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<TradingProduct>
  },

  async deactivateTradingProduct(supplierId: string, productId: string): Promise<TradingProduct> {
    const response = await staffFetch(`/trading-suppliers/${supplierId}/products/${productId}`, {
      method: 'DELETE',
    })
    if (!response.ok) throw new Error(await readError(response))
    return response.json() as Promise<TradingProduct>
  },
}
