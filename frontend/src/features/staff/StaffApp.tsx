import { useState } from 'react'
import {
  clearStaffAuth,
  setStaffAuth,
  staffApi,
  type BuyerLead,
  type StaffMe,
  type SupplierReview,
} from './api'

type Tab = 'suppliers' | 'buyers' | 'exports'

export function StaffApp() {
  const [me, setMe] = useState<StaffMe | null>(null)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [tab, setTab] = useState<Tab>('suppliers')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [suppliers, setSuppliers] = useState<SupplierReview[]>([])
  const [buyers, setBuyers] = useState<BuyerLead[]>([])

  async function signIn(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    setStaffAuth(email.trim().toLowerCase(), password)
    try {
      const user = await staffApi.me()
      setMe(user)
      if (user.roles.includes('ADMIN') || user.roles.includes('SUPPLIER_REVIEWER')) {
        setTab('suppliers')
      } else if (user.roles.includes('MARKETING')) {
        setTab('buyers')
      } else {
        setTab('exports')
      }
      await refreshQueues(user)
    } catch (err) {
      clearStaffAuth()
      setError(err instanceof Error ? err.message : 'Sign-in failed.')
    } finally {
      setBusy(false)
    }
  }

  async function refreshQueues(user: StaffMe) {
    const canSuppliers = user.roles.includes('ADMIN') || user.roles.includes('SUPPLIER_REVIEWER')
    const canBuyers = user.roles.includes('ADMIN') || user.roles.includes('MARKETING')
    if (canSuppliers) {
      setSuppliers(await staffApi.suppliers())
    }
    if (canBuyers) {
      setBuyers(await staffApi.buyers())
    }
  }

  function signOut() {
    clearStaffAuth()
    setMe(null)
    setSuppliers([])
    setBuyers([])
  }

  if (!me) {
    return (
      <div className="staff-app">
        <header className="staff-header">
          <p className="staff-kicker">Internal</p>
          <h1>Staff review</h1>
          <p className="staff-lede">
            Separate from the visitor portal. HTTP Basic against app_users — not SSO.
          </p>
        </header>
        <form className="staff-card" onSubmit={(event) => void signIn(event)}>
          <label>
            Work email
            <input
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="username"
              placeholder="reviewer@sarv.local"
            />
          </label>
          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </label>
          {error ? <p className="staff-error">{error}</p> : null}
          <button type="submit" disabled={busy}>
            {busy ? 'Signing in…' : 'Sign in'}
          </button>
          <p className="staff-hint">
            Seeded emails: reviewer@sarv.local, marketing@sarv.local, admin@sarv.local. Use the
            staff password set on this server.
          </p>
        </form>
      </div>
    )
  }

  return (
    <div className="staff-app">
      <header className="staff-header">
        <p className="staff-kicker">Internal</p>
        <h1>Staff review</h1>
        <p className="staff-lede">
          {me.displayName} · {me.roles.join(', ')}
        </p>
        <button type="button" className="staff-text-btn" onClick={signOut}>
          Sign out
        </button>
      </header>
      <nav className="staff-tabs">
        {me.roles.includes('ADMIN') || me.roles.includes('SUPPLIER_REVIEWER') ? (
          <button type="button" className={tab === 'suppliers' ? 'is-active' : ''} onClick={() => setTab('suppliers')}>
            Suppliers
          </button>
        ) : null}
        {me.roles.includes('ADMIN') || me.roles.includes('MARKETING') ? (
          <button type="button" className={tab === 'buyers' ? 'is-active' : ''} onClick={() => setTab('buyers')}>
            Buyers
          </button>
        ) : null}
        {me.roles.includes('ADMIN') || me.roles.includes('EXPORTER') || me.roles.includes('MARKETING') ? (
          <button type="button" className={tab === 'exports' ? 'is-active' : ''} onClick={() => setTab('exports')}>
            Export
          </button>
        ) : null}
      </nav>
      {tab === 'suppliers' ? (
        <SupplierQueue rows={suppliers} onChange={() => void refreshQueues(me)} />
      ) : null}
      {tab === 'buyers' ? (
        <BuyerQueue rows={buyers} onChange={() => void refreshQueues(me)} />
      ) : null}
      {tab === 'exports' ? <ExportPanel /> : null}
    </div>
  )
}

function SupplierQueue({
  rows,
  onChange,
}: {
  rows: SupplierReview[]
  onChange: () => void
}) {
  const [error, setError] = useState<string | null>(null)
  const [notes, setNotes] = useState('')
  const [busyId, setBusyId] = useState<string | null>(null)

  async function decide(id: string, decision: string) {
    setBusyId(id)
    try {
      await staffApi.decide(id, decision, notes)
      setNotes('')
      setError(null)
      onChange()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Decision failed.')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <section className="staff-section">
      <p className="staff-lede">
        Add to production records the reviewer and enqueues a vendor outbox row. The worker writes a
        local stub file; it does not call an enterprise vendor API.
      </p>
      <label>
        Decision notes
        <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} />
      </label>
      {error ? <p className="staff-error">{error}</p> : null}
      <ul className="staff-list">
        {rows.length === 0 ? <li className="staff-empty">No submitted suppliers.</li> : null}
        {rows.map((row) => (
          <li key={row.id} className="staff-row">
            <div>
              <strong>{row.referenceCode}</strong>
              <span>
                {row.companyName || 'No company'} · {row.personName}
              </span>
              <span className="staff-meta">
                {row.reviewState} · production {row.productionState}
                {row.deliveryState ? ` · outbox ${row.deliveryState}` : ''}
              </span>
            </div>
            <div className="staff-actions">
              <button
                type="button"
                disabled={busyId === row.id || row.productionState !== 'NOT_REQUESTED'}
                onClick={() => void decide(row.id, 'APPROVE')}
              >
                Add to production
              </button>
              <button
                type="button"
                className="staff-secondary"
                disabled={busyId === row.id || row.reviewState === 'APPROVED'}
                onClick={() => void decide(row.id, 'REJECT')}
              >
                Reject
              </button>
              <button
                type="button"
                className="staff-secondary"
                disabled={busyId === row.id || row.reviewState === 'APPROVED' || row.reviewState === 'REJECTED'}
                onClick={() => void decide(row.id, 'REQUEST_INFORMATION')}
              >
                Request information
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}

function BuyerQueue({
  rows,
  onChange,
}: {
  rows: BuyerLead[]
  onChange: () => void
}) {
  const [error, setError] = useState<string | null>(null)
  const [notes, setNotes] = useState<Record<string, string>>(() =>
    Object.fromEntries(rows.map((row) => [row.id, row.marketingNotes ?? '']))
  )

  async function save(id: string) {
    try {
      await staffApi.saveBuyerNotes(id, notes[id] ?? '')
      setError(null)
      onChange()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save notes.')
    }
  }

  return (
    <section className="staff-section">
      <p className="staff-lede">
        Purchase leads. Submit enqueues a marketing-lead outbox row. The worker writes a local mailbox
        stub; CRM is not live.
      </p>
      {error ? <p className="staff-error">{error}</p> : null}
      <ul className="staff-list">
        {rows.length === 0 ? <li className="staff-empty">No submitted buyers.</li> : null}
        {rows.map((row) => (
          <li key={row.id} className="staff-row">
            <div>
              <strong>{row.referenceCode}</strong>
              <span>
                {row.personName} · {row.email}
              </span>
              <span className="staff-meta">
                {row.requirement}
                {row.deliveryState ? ` · outbox ${row.deliveryState}` : ''}
              </span>
            </div>
            <label>
              Internal notes
              <textarea
                value={notes[row.id] ?? row.marketingNotes ?? ''}
                onChange={(e) => setNotes((current) => ({ ...current, [row.id]: e.target.value }))}
                rows={2}
              />
            </label>
            <button type="button" className="staff-secondary" onClick={() => void save(row.id)}>
              Save notes
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}

function ExportPanel() {
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function run() {
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      const job = await staffApi.createExport()
      await staffApi.downloadExport(job.id)
      setMessage(`Ready until ${job.expiresAt ?? 'expiry'}. CSV is a controlled job, not a live table dump.`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export failed.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="staff-section">
      <p className="staff-lede">
        Creates an expiring purchase-lead spreadsheet (CSV in this POC). Excel workbook format comes later.
      </p>
      {error ? <p className="staff-error">{error}</p> : null}
      {message ? <p className="staff-ok">{message}</p> : null}
      <button type="button" disabled={busy} onClick={() => void run()}>
        {busy ? 'Generating…' : 'Export purchase leads'}
      </button>
    </section>
  )
}
