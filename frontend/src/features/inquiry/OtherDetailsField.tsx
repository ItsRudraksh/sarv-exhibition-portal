import { copy } from './copy'
import { TextField } from '../../components/ui'

export interface OtherDetailsFieldProps {
  readonly id: string
  readonly checked: boolean
  readonly label: string
  readonly value: string
  readonly onToggle: () => void
  readonly onDetailsChange: (value: string) => void
  readonly error?: string
  readonly disabled?: boolean
  readonly nested?: boolean
}

export function OtherDetailsField({
  id,
  checked,
  label,
  value,
  onToggle,
  onDetailsChange,
  error,
  disabled = false,
  nested = false,
}: OtherDetailsFieldProps) {
  return (
    <>
      <label className={`checkbox-item${disabled ? ' checkbox-item--parent' : ''}`}>
        <input
          type="checkbox"
          checked={checked}
          disabled={disabled}
          onChange={disabled ? undefined : onToggle}
          aria-label={disabled ? `${label} (selected)` : label}
        />
        <span>{label}</span>
      </label>
      {checked ? (
        <div className={`other-detail${nested ? ' other-detail--nested' : ''}`}>
          <TextField
            id={id}
            label={copy.common.otherDetailsLabel}
            value={value}
            onChange={onDetailsChange}
            multiline
            rows={3}
            required
            error={error}
            hint={copy.common.otherDetailsHint}
          />
        </div>
      ) : null}
    </>
  )
}
