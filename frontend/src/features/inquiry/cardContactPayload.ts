/**
 * Parse contact payloads from card QR text (vCard / MECARD / mailto / tel / email).
 * Mirrors backend LocalCardScanEngine — no cloud provider.
 */

export interface CardOcrProposal {
  fieldKey: string
  proposedValueText: string
}

const EMAIL = /[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i

export function parseContactPayload(payload: string): CardOcrProposal[] {
  const fields: CardOcrProposal[] = []
  if (!payload?.trim()) return fields
  const text = payload.trim()
  const upper = text.toUpperCase()
  if (upper.startsWith('BEGIN:VCARD')) {
    parseVcard(text, fields)
  } else if (upper.startsWith('MECARD:')) {
    parseMecard(text, fields)
  } else if (/^mailto:/i.test(text)) {
    add(fields, 'work_email', text.slice(7).trim())
  } else if (/^tel:/i.test(text)) {
    addPhone(fields, text.slice(4).trim())
  } else {
    const email = text.match(EMAIL)
    if (email) add(fields, 'work_email', email[0])
  }
  return fields
}

function parseVcard(payload: string, fields: CardOcrProposal[]) {
  let fullName: string | null = null
  let org: string | null = null
  let title: string | null = null
  let email: string | null = null
  let tel: string | null = null
  let city: string | null = null
  for (const rawLine of payload.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || /^(BEGIN|END|VERSION):/i.test(line)) continue
    const colon = line.indexOf(':')
    if (colon <= 0) continue
    const key = line.slice(0, colon).split(';', 1)[0].toUpperCase()
    const value = line.slice(colon + 1).trim()
    switch (key) {
      case 'FN':
        fullName = value
        break
      case 'N':
        if (!fullName) {
          const parts = value.split(';', -1)
          fullName = `${(parts[1] ?? '').trim()} ${(parts[0] ?? '').trim()}`.trim()
        }
        break
      case 'ORG':
        org = value.split(';', 1)[0].trim()
        break
      case 'TITLE':
        title = value
        break
      case 'EMAIL':
        if (!email) email = value
        break
      case 'TEL':
        if (!tel) tel = value
        break
      case 'ADR': {
        if (!city) {
          const parts = value.split(';', -1)
          city = (parts[3] || parts[6] || '').trim() || null
        }
        break
      }
      default:
        break
    }
  }
  add(fields, 'full_name', fullName)
  add(fields, 'work_email', email)
  addPhone(fields, tel)
  add(fields, 'company_name', org)
  add(fields, 'job_title', title)
  add(fields, 'location_from_card', city)
}

function parseMecard(payload: string, fields: CardOcrProposal[]) {
  const body = payload.slice('MECARD:'.length)
  const name = mecardField(body, 'N:')
  add(fields, 'full_name', name ? name.replace(/,/g, ' ').trim() : null)
  add(fields, 'work_email', mecardField(body, 'EMAIL:'))
  addPhone(fields, mecardField(body, 'TEL:'))
  add(fields, 'company_name', mecardField(body, 'ORG:'))
  add(fields, 'location_from_card', mecardField(body, 'NOTE:'))
}

function mecardField(body: string, prefix: string): string | null {
  const upper = body.toUpperCase()
  const idx = upper.indexOf(prefix.toUpperCase())
  if (idx < 0) return null
  const start = idx + prefix.length
  const end = body.indexOf(';', start)
  const value = body.slice(start, end < 0 ? body.length : end).trim()
  return value || null
}

function addPhone(fields: CardOcrProposal[], raw: string | null | undefined) {
  if (!raw?.trim()) return
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
