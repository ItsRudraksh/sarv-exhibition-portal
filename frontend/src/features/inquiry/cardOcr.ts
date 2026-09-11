/**
 * Local visiting-card assist: client QR (jsQR) + printed OCR (Tesseract.js).
 * Worker/core/lang are loaded from local Vite assets — not a cloud AI vendor.
 * QR payloads are contact suggestions only; never redirect or preview a URL.
 */

import jsQR from 'jsqr'
import { createWorker, PSM, type Worker } from 'tesseract.js'
import workerPath from 'tesseract.js/dist/worker.min.js?url'
import corePath from 'tesseract.js-core/tesseract-core-simd-lstm.wasm.js?url'
import { parseContactPayload, type CardOcrProposal } from './cardContactPayload'

export type { CardOcrProposal }

export interface CardExtractResult {
  proposals: CardOcrProposal[]
  source: 'qr' | 'ocr' | 'none'
  /** Short status for UI when nothing usable was found. */
  detail?: string
}

const EMAIL_RE = /[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i
const PHONE_RE = /(?:\+|00)?[0-9][0-9\s().-]{7,18}[0-9]/
const TITLE_RE =
  /\b(manager|director|officer|engineer|executive|head|lead|ceo|cto|cfo|md|vp|president|scientist|analyst)\b/i

export const CLIENT_OCR_PROVIDER = 'tesseract-js-v1'

let sharedWorker: Worker | null = null
let workerPromise: Promise<Worker> | null = null

export function parseCardOcrText(rawText: string): CardOcrProposal[] {
  const fields: CardOcrProposal[] = []
  if (!rawText?.trim()) return fields
  const text = rawText.replace(/\u00a0/g, ' ').trim()

  const email = text.match(EMAIL_RE)
  if (email) add(fields, 'work_email', email[0])

  const phoneMatch = text.match(PHONE_RE)
  if (phoneMatch && !phoneMatch[0].includes('@')) {
    addPhone(fields, phoneMatch[0])
  }

  let fullName: string | null = null
  let company: string | null = null
  let title: string | null = null
  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim().replace(/\s+/g, ' ')
    if (!line || line.length > 80) continue
    if (EMAIL_RE.test(line) || PHONE_RE.test(line)) continue
    if (/^(www\.|https?:\/\/|tel[:\s]|email[:\s]|e-mail[:\s]|ph[:\s]|mob[:\s]|mobile[:\s])/i.test(line)) {
      continue
    }
    if (TITLE_RE.test(line) && !title) {
      title = line
      continue
    }
    if (looksLikePersonName(line) && !fullName) {
      fullName = line
      continue
    }
    if (looksLikeCompany(line) && !company) {
      company = line
    }
  }
  add(fields, 'full_name', fullName)
  add(fields, 'company_name', company)
  add(fields, 'job_title', title)
  return fields
}

/** QR proposals win on key conflict; OCR fills gaps. */
export function mergeCardProposals(
  preferred: CardOcrProposal[],
  fallback: CardOcrProposal[],
): CardOcrProposal[] {
  const byKey = new Map<string, string>()
  for (const f of preferred) {
    if (f.proposedValueText?.trim()) byKey.set(f.fieldKey, f.proposedValueText.trim())
  }
  for (const f of fallback) {
    if (!byKey.has(f.fieldKey) && f.proposedValueText?.trim()) {
      byKey.set(f.fieldKey, f.proposedValueText.trim())
    }
  }
  return [...byKey.entries()].map(([fieldKey, proposedValueText]) => ({
    fieldKey,
    proposedValueText,
  }))
}

export function proposalsFromExtractionFields(
  fields:
    | { fieldKey: string; reviewState?: string; proposedValueText?: string | null }[]
    | undefined,
): CardOcrProposal[] {
  if (!fields?.length) return []
  return fields
    .filter((f) => (!f.reviewState || f.reviewState === 'PENDING') && f.proposedValueText?.trim())
    .map((f) => ({
      fieldKey: f.fieldKey,
      proposedValueText: f.proposedValueText!.trim(),
    }))
}

export function extractionHasContactHints(proposals: CardOcrProposal[]): boolean {
  return proposals.some((f) =>
    ['full_name', 'work_email', 'mobile_number'].includes(f.fieldKey),
  )
}

/**
 * Extract contact proposals from a card image: try QR first, then local OCR.
 */
export async function extractCardContact(image: Blob): Promise<CardExtractResult> {
  const frame = await blobToImageData(image)
  const qr = jsQR(frame.data, frame.width, frame.height, { inversionAttempts: 'attemptBoth' })
  if (qr?.data?.trim()) {
    const fromQr = parseContactPayload(qr.data)
    if (extractionHasContactHints(fromQr) || fromQr.length > 0) {
      return { proposals: fromQr, source: 'qr' }
    }
  }

  try {
    const ocrBlob = await preprocessForOcr(image)
    const worker = await getWorker()
    await worker.setParameters({ tessedit_pageseg_mode: PSM.SINGLE_BLOCK })
    const {
      data: { text },
    } = await worker.recognize(ocrBlob)
    const proposals = parseCardOcrText(text ?? '')
    if (extractionHasContactHints(proposals) || proposals.length > 0) {
      return { proposals, source: 'ocr' }
    }
    return {
      proposals: [],
      source: 'none',
      detail: text?.trim()
        ? 'Text was found but contact fields could not be parsed. Enter them manually.'
        : 'No readable text found on this photo. Try a clearer, well-lit shot.',
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : 'OCR failed'
    return {
      proposals: [],
      source: 'none',
      detail: `Could not read the card (${message}). Enter details manually.`,
    }
  }
}

/** @deprecated Prefer extractCardContact */
export async function recognizeCardImage(image: Blob): Promise<CardOcrProposal[]> {
  const result = await extractCardContact(image)
  return result.proposals
}

async function getWorker(): Promise<Worker> {
  if (sharedWorker) return sharedWorker
  if (!workerPromise) {
    workerPromise = createWorker('eng', 1, {
      workerPath,
      corePath,
      // Served from frontend/public/tessdata/eng.traineddata
      langPath: `${import.meta.env.BASE_URL}tessdata`,
      gzip: false,
    }).then((w) => {
      sharedWorker = w
      return w
    })
  }
  return workerPromise
}

async function blobToImageData(blob: Blob): Promise<ImageData> {
  const bitmap = await createImageBitmap(blob)
  try {
    const canvas = document.createElement('canvas')
    canvas.width = bitmap.width
    canvas.height = bitmap.height
    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    if (!ctx) throw new Error('Canvas unavailable')
    ctx.drawImage(bitmap, 0, 0)
    return ctx.getImageData(0, 0, canvas.width, canvas.height)
  } finally {
    bitmap.close()
  }
}

/** Upscale small shots and boost contrast for OCR. */
async function preprocessForOcr(source: Blob): Promise<Blob> {
  const bitmap = await createImageBitmap(source)
  try {
    const minEdge = 900
    const scale = Math.max(1, minEdge / Math.min(bitmap.width, bitmap.height))
    const width = Math.round(bitmap.width * scale)
    const height = Math.round(bitmap.height * scale)
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d', { alpha: false })
    if (!ctx) throw new Error('Canvas unavailable')
    ctx.filter = 'grayscale(1) contrast(1.25) brightness(1.05)'
    ctx.drawImage(bitmap, 0, 0, width, height)
    ctx.filter = 'none'
    return await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (b) => (b ? resolve(b) : reject(new Error('OCR preprocess failed'))),
        'image/jpeg',
        0.92,
      )
    })
  } finally {
    bitmap.close()
  }
}

function looksLikePersonName(line: string): boolean {
  if (!/^[A-Za-z][A-Za-z .'-]{1,60}$/.test(line)) return false
  const parts = line.split(/\s+/)
  return parts.length >= 2 && parts.length <= 5
}

function looksLikeCompany(line: string): boolean {
  if (line.length < 3) return false
  const lower = line.toLowerCase()
  if (
    lower.includes('ltd') ||
    lower.includes('limited') ||
    lower.includes('pvt') ||
    lower.includes('inc') ||
    lower.includes('llc') ||
    lower.includes('pharma') ||
    lower.includes('labs') ||
    lower.includes('bio') ||
    lower.includes('corp') ||
    lower.includes('private') ||
    lower.includes('company')
  ) {
    return true
  }
  return line === line.toUpperCase() && /^[A-Z0-9 &.,'-]{3,60}$/.test(line)
}

function addPhone(fields: CardOcrProposal[], raw: string) {
  let digits = raw.replace(/[^0-9+]/g, '')
  if (digits.startsWith('00')) digits = `+${digits.slice(2)}`
  if (digits.startsWith('+91') && digits.length > 3) {
    add(fields, 'country_code', '+91')
    add(fields, 'mobile_number', digits.slice(3))
    return
  }
  if (digits.startsWith('+1') && digits.length > 2) {
    add(fields, 'country_code', '+1')
    add(fields, 'mobile_number', digits.slice(2))
    return
  }
  if (digits.startsWith('+44') && digits.length > 3) {
    add(fields, 'country_code', '+44')
    add(fields, 'mobile_number', digits.slice(3))
    return
  }
  if (digits.startsWith('+')) {
    add(fields, 'mobile_number', digits)
    return
  }
  if (digits.length === 10) {
    add(fields, 'country_code', '+91')
    add(fields, 'mobile_number', digits)
    return
  }
  add(fields, 'mobile_number', digits || raw.trim())
}

function add(fields: CardOcrProposal[], key: string, value: string | null | undefined) {
  if (!value?.trim()) return
  if (fields.some((f) => f.fieldKey === key)) return
  fields.push({ fieldKey: key, proposedValueText: value.trim().slice(0, 500) })
}
