import { createEmptyDraft, type InquiryDraft } from './types'

export type EntryChannel = 'EXHIBITION_QR' | 'WEBSITE' | 'DIRECT'

export interface EntryContext {
  entryChannel: EntryChannel
  campaignCode: string | null
  staffAssisted: boolean
  sharedDevice: boolean
}

/**
 * Stall QR: ?c=POC-STALL-1 or ?campaign=POC-STALL-1
 * Website: /web or ?channel=website
 * Direct: ?channel=direct
 * Staff assist: ?assist=1
 * Shared tablet default on for exhibition QR; ?shared=0 for personal resume.
 */
export function parseEntryContext(
  search = window.location.search,
  pathname = window.location.pathname,
): EntryContext {
  const params = new URLSearchParams(search)
  const channelParam = (params.get('channel') ?? '').trim().toLowerCase()
  const campaignCode =
    (params.get('c') ?? params.get('campaign') ?? '').trim() || null
  const staffAssisted =
    params.get('assist') === '1' || params.get('assist') === 'true'
  const path = pathname.replace(/\/+$/, '') || '/'

  let entryChannel: EntryChannel = 'EXHIBITION_QR'
  if (channelParam === 'website' || path === '/web') {
    entryChannel = 'WEBSITE'
  } else if (channelParam === 'direct') {
    entryChannel = 'DIRECT'
  } else if (campaignCode) {
    entryChannel = 'EXHIBITION_QR'
  }

  const sharedForced =
    params.get('shared') === '1' || params.get('shared') === 'true'
  const personalForced =
    params.get('shared') === '0' || params.get('shared') === 'false'
  const sharedDevice = personalForced
    ? false
    : sharedForced || entryChannel === 'EXHIBITION_QR' || staffAssisted

  return {
    entryChannel,
    campaignCode: entryChannel === 'EXHIBITION_QR' ? campaignCode : null,
    staffAssisted,
    sharedDevice,
  }
}

const POINTER_KEY = 'sarv-inquiry-pointer-v1'
const LEGACY_DRAFT_KEY = 'sarv-inquiry-draft-v2'

export const sessionPointerPort = {
  loadId(): string | null {
    try {
      const raw = sessionStorage.getItem(POINTER_KEY)
      if (!raw) return null
      const parsed = JSON.parse(raw) as { id?: string }
      return typeof parsed.id === 'string' ? parsed.id : null
    } catch {
      return null
    }
  },

  saveId(id: string): void {
    sessionStorage.setItem(POINTER_KEY, JSON.stringify({ id }))
  },

  clear(): void {
    sessionStorage.removeItem(POINTER_KEY)
  },
}

export function clearLegacyLocalDraft(): void {
  try {
    localStorage.removeItem(LEGACY_DRAFT_KEY)
  } catch {
    // ignore
  }
}

export function createSeedDraft(entry: EntryContext): InquiryDraft {
  return {
    ...createEmptyDraft(),
    entryChannel: entry.entryChannel,
  }
}
