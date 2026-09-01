import type { ReactNode } from 'react'
import { Logo } from './Logo'

export interface AppHeaderProps {
  readonly showBack?: boolean
  readonly onBack?: () => void
  readonly stepLabel?: string
  readonly subLabel?: string
  readonly progress?: number
}

export function AppHeader({
  showBack = false,
  onBack,
  stepLabel,
  subLabel,
  progress,
}: AppHeaderProps) {
  return (
    <header className="app-header">
      <div className="app-header__bar">
        <div className="app-header__side">
          {showBack && onBack ? (
            <button
              type="button"
              className="btn-icon"
              aria-label="Go back"
              onClick={onBack}
            >
              <ArrowBackIcon />
            </button>
          ) : null}
        </div>
        <div className="app-header__brand">
          <Logo variant="header" />
        </div>
        <div className="app-header__side app-header__side--end">
          {stepLabel ? (
            <span className="step-label" style={{ fontSize: '0.625rem' }}>
              {stepLabel}
            </span>
          ) : null}
        </div>
      </div>
      {subLabel ? (
        <div className="app-header__sub">
          <span className="step-label">{subLabel}</span>
        </div>
      ) : null}
      {progress !== undefined ? (
        <div className="progress-bar">
          <div
            className="progress-bar__fill"
            style={{ width: `${progress * 100}%` }}
          />
        </div>
      ) : null}
    </header>
  )
}

export interface FixedFooterProps {
  readonly children: ReactNode
  readonly note?: string
}

export function FixedFooter({ children, note }: FixedFooterProps) {
  return (
    <div className="fixed-footer">
      <div className="fixed-footer__inner">
        {note ? <p className="fixed-footer__note">{note}</p> : null}
        {children}
      </div>
    </div>
  )
}

export interface PrimaryButtonProps {
  readonly children: ReactNode
  readonly onClick?: () => void
  readonly disabled?: boolean
  readonly type?: 'button' | 'submit'
  readonly ariaLabel?: string
}

export function PrimaryButton({
  children,
  onClick,
  disabled,
  type = 'button',
  ariaLabel,
}: PrimaryButtonProps) {
  return (
    <button
      type={type}
      className="btn btn-primary"
      onClick={onClick}
      disabled={disabled}
      aria-label={ariaLabel}
    >
      {children}
    </button>
  )
}

export interface TextFieldProps {
  readonly id: string
  readonly label: string
  readonly value: string
  readonly onChange: (value: string) => void
  readonly type?: string
  readonly placeholder?: string
  readonly required?: boolean
  readonly error?: string
  readonly hint?: string
  readonly multiline?: boolean
  readonly rows?: number
}

export function TextField({
  id,
  label,
  value,
  onChange,
  type = 'text',
  placeholder,
  required,
  error,
  hint,
  multiline,
  rows = 4,
}: TextFieldProps) {
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined

  return (
    <div className="field">
      <label htmlFor={id}>
        {label}
        {required ? ' *' : ''}
      </label>
      {multiline ? (
        <textarea
          id={id}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          required={required}
          aria-invalid={!!error}
          aria-describedby={describedBy}
          rows={rows}
        />
      ) : (
        <input
          id={id}
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          required={required}
          aria-invalid={!!error}
          aria-describedby={describedBy}
        />
      )}
      {error ? (
        <p id={`${id}-error`} className="field-error" role="alert">
          {error}
        </p>
      ) : null}
      {hint && !error ? (
        <p id={`${id}-hint`} className="field-hint">
          {hint}
        </p>
      ) : null}
    </div>
  )
}

export interface NoticeProps {
  readonly children: ReactNode
  readonly icon?: ReactNode
}

export function Notice({ children, icon }: NoticeProps) {
  return (
    <div className="notice">
      {icon}
      <div>{children}</div>
    </div>
  )
}

export interface SummaryCardProps {
  readonly title: string
  readonly rows: { label: string; value: string }[]
  readonly onEdit?: () => void
}

export function SummaryCard({ title, rows, onEdit }: SummaryCardProps) {
  return (
    <section className="section-gap">
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 8,
        }}
      >
        <h2 style={{ margin: 0, fontSize: '1rem', fontWeight: 600 }}>{title}</h2>
        {onEdit ? (
          <button type="button" className="btn-text" onClick={onEdit}>
            Edit
          </button>
        ) : null}
      </div>
      <div className="card">
        {rows.map((row) => (
          <div key={row.label} className="card-row">
            <div style={{ flex: 1 }}>
              <p className="card-row-label">{row.label}</p>
              <p className="card-row-value">{row.value}</p>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

function ArrowBackIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M19 12H5M5 12L12 19M5 12L12 5"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

export function SearchIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden>
      <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="2" />
      <path d="M20 20L16.5 16.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}

export function ChevronRightIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d="M9 6L15 12L9 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function CheckIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d="M20 6L9 17L4 12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function StorefrontIcon() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d="M3 9L5 3H19L21 9M3 9V20H21V9M3 9H21" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
      <path d="M9 20V14H15V20" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
    </svg>
  )
}

export function ShoppingBagIcon() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d="M6 6H18L20 20H4L6 6Z" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
      <path d="M9 6V4C9 2.9 9.9 2 11 2H13C14.1 2 15 2.9 15 4V6" stroke="currentColor" strokeWidth="2" />
    </svg>
  )
}

export { Logo }
