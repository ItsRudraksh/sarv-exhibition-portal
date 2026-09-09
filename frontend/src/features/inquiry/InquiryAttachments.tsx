import { useRef } from 'react'
import { copy } from './copy'
import type { CardFileMeta } from './types'
import { ATTACHMENT_MAX_COUNT } from './types'

export interface InquiryAttachmentsProps {
  readonly title: string
  readonly hint: string
  readonly files: CardFileMeta[]
  readonly uploading: boolean
  readonly error?: string
  readonly apiAvailable: boolean
  readonly onAdd: (files: FileList) => void
  readonly onRemove: (file: CardFileMeta) => void
}

export function InquiryAttachments({
  title,
  hint,
  files,
  uploading,
  error,
  apiAvailable,
  onAdd,
  onRemove,
}: InquiryAttachmentsProps) {
  const fileRef = useRef<HTMLInputElement>(null)
  const atLimit = files.length >= ATTACHMENT_MAX_COUNT

  return (
    <div className="card" style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
        <div>
          <p style={{ margin: 0, fontWeight: 600, fontSize: '0.875rem' }}>{title}</p>
          <p className="step-label step-label--muted" style={{ marginTop: 4 }}>
            {hint}
          </p>
        </div>
        <button
          type="button"
          className="btn btn-secondary"
          style={{ width: 'auto', minHeight: 36, fontSize: '0.875rem' }}
          disabled={uploading || atLimit}
          onClick={() => fileRef.current?.click()}
        >
          {uploading ? copy.cardCapture.uploading : copy.supplier.attachmentsAdd}
        </button>
      </div>
      <input
        ref={fileRef}
        type="file"
        accept=".pdf,image/jpeg,image/png,image/webp"
        multiple
        className="sr-only"
        onChange={(e) => {
          if (e.target.files && e.target.files.length > 0) {
            onAdd(e.target.files)
          }
          e.target.value = ''
        }}
      />
      {files.length > 0 ? (
        <ul style={{ listStyle: 'none', margin: '12px 0 0', padding: 0 }} className="stack-gap">
          {files.map((file) => (
            <li
              key={file.assetId ?? `${file.name}-${file.size}`}
              style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center' }}
            >
              <p style={{ margin: 0, fontSize: '0.875rem' }}>
                {file.name} — {file.assetId || apiAvailable ? copy.common.uploaded : copy.common.localFileOnly}
              </p>
              <button type="button" className="btn-text" onClick={() => onRemove(file)}>
                {copy.supplier.attachmentsRemove}
              </button>
            </li>
          ))}
        </ul>
      ) : null}
      {error ? (
        <p className="field-error" role="alert" style={{ marginTop: 8 }}>
          {error}
        </p>
      ) : null}
    </div>
  )
}
