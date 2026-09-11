import type { ReactNode } from 'react'
import { copy } from './copy'
import { FixedFooter, PrimaryButton } from '../../components/ui'

export interface ReviewEditFooterProps {
  readonly canSave: boolean
  readonly onCancel: () => void
  readonly onSave: () => void
  readonly extra?: ReactNode
}

export function ReviewEditFooter({ canSave, onCancel, onSave, extra }: ReviewEditFooterProps) {
  return (
    <FixedFooter note={copy.common.savedAutomatically}>
      <div className="fixed-footer__actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel}>
          {copy.common.cancel}
        </button>
        <PrimaryButton disabled={!canSave} onClick={onSave}>
          {copy.common.saveChanges}
        </PrimaryButton>
      </div>
      {extra ? <div className="fixed-footer__extra">{extra}</div> : null}
    </FixedFooter>
  )
}
