import { useEffect, useState } from 'react'
import { adminCopy as copy } from './copy'
import { TradingCataloguePanel } from './TradingCataloguePanel'
import {
  clearStaffAuth,
  setStaffAuth,
  staffApi,
  staffAuthHeader,
  type StaffAccount,
  type StaffMe,
  type StaffRole,
} from '../staff/api'

const STATUSES = ['ACTIVE', 'INACTIVE', 'SUSPENDED'] as const

type AdminTab = 'staff' | 'catalogue'

const emptyForm = {
  email: '',
  displayName: '',
  password: '',
  roles: [] as string[],
  status: 'ACTIVE',
}

export function AdminApp() {
  const [me, setMe] = useState<StaffMe | null>(null)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [tab, setTab] = useState<AdminTab>('catalogue')
  const [restoring, setRestoring] = useState(() => Boolean(staffAuthHeader()))

  useEffect(() => {
    if (!staffAuthHeader()) {
      return
    }
    void staffApi
      .me()
      .then(setMe)
      .catch(() => {
        clearStaffAuth()
        setMe(null)
      })
      .finally(() => setRestoring(false))
  }, [])

  async function signIn(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    setStaffAuth(email.trim().toLowerCase(), password)
    try {
      setMe(await staffApi.me())
    } catch (err) {
      clearStaffAuth()
      setError(err instanceof Error ? err.message : 'Sign-in failed.')
    } finally {
      setBusy(false)
    }
  }

  function signOut() {
    clearStaffAuth()
    setMe(null)
  }

  if (restoring) {
    return (
      <div className="staff-app">
        <header className="staff-header">
          <p className="staff-kicker">{copy.kicker}</p>
          <h1>{copy.title}</h1>
        </header>
      </div>
    )
  }

  if (!me) {
    return (
      <div className="staff-app">
        <header className="staff-header">
          <p className="staff-kicker">{copy.kicker}</p>
          <h1>{copy.title}</h1>
          <p className="staff-lede">{copy.lede}</p>
        </header>
        <form className="staff-card" onSubmit={(event) => void signIn(event)}>
          <label>
            Work email
            <input
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="username"
              placeholder="admin@sarv.local"
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
          <p className="staff-hint">{copy.signInHint}</p>
        </form>
      </div>
    )
  }

  if (!me.roles.includes('ADMIN')) {
    return (
      <div className="staff-app">
        <header className="staff-header">
          <p className="staff-kicker">{copy.kicker}</p>
          <h1>{copy.title}</h1>
          <p className="staff-lede">
            {me.displayName} · {me.roles.join(', ')}
          </p>
          <button type="button" className="staff-text-btn" onClick={signOut}>
            Sign out
          </button>
        </header>
        <section className="staff-section">
          <p className="staff-lede">{copy.forbidden}</p>
          <a className="staff-link-btn" href="/staff">
            {copy.openStaff}
          </a>
        </section>
      </div>
    )
  }

  return (
    <div className="staff-app">
      <header className="staff-header">
        <p className="staff-kicker">{copy.kicker}</p>
        <h1>{copy.title}</h1>
        <p className="staff-lede">
          {me.displayName} · {me.roles.join(', ')}
        </p>
        <div className="staff-header-links">
          <a className="staff-text-btn" href="/staff">
            {copy.openStaff}
          </a>
          <button type="button" className="staff-text-btn" onClick={signOut}>
            Sign out
          </button>
        </div>
      </header>
      <nav className="staff-tabs">
        <button type="button" className={tab === 'catalogue' ? 'is-active' : ''} onClick={() => setTab('catalogue')}>
          {copy.tabCatalogue}
        </button>
        <button type="button" className={tab === 'staff' ? 'is-active' : ''} onClick={() => setTab('staff')}>
          {copy.tabStaff}
        </button>
      </nav>
      {tab === 'catalogue' ? <TradingCataloguePanel /> : <StaffAccountsPanel me={me} />}
    </div>
  )
}

function StaffAccountsPanel({ me }: { me: StaffMe }) {
  const [rows, setRows] = useState<StaffAccount[]>([])
  const [roles, setRoles] = useState<StaffRole[]>([])
  const [form, setForm] = useState(emptyForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function refresh() {
    const [users, catalog] = await Promise.all([staffApi.listUsers(), staffApi.listRoles()])
    setRows(users)
    setRoles(catalog)
  }

  useEffect(() => {
    let cancelled = false
    void Promise.all([staffApi.listUsers(), staffApi.listRoles()])
      .then(([users, catalog]) => {
        if (cancelled) return
        setRows(users)
        setRoles(catalog)
      })
      .catch((err: unknown) => {
        if (cancelled) return
        setError(err instanceof Error ? err.message : 'Could not load staff accounts.')
      })
    return () => {
      cancelled = true
    }
  }, [])

  function resetForm() {
    setEditingId(null)
    setForm(emptyForm)
  }

  function startEdit(row: StaffAccount) {
    setEditingId(row.id)
    setForm({
      email: row.email,
      displayName: row.displayName,
      password: '',
      roles: [...row.roles],
      status: row.status,
    })
    setError(null)
    setMessage(null)
  }

  function toggleRole(code: string) {
    setForm((current) => ({
      ...current,
      roles: current.roles.includes(code)
        ? current.roles.filter((role) => role !== code)
        : [...current.roles, code],
    }))
  }

  async function save(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    setMessage(null)
    const body = {
      email: form.email.trim().toLowerCase(),
      displayName: form.displayName.trim(),
      roles: form.roles,
      status: form.status,
      password: form.password.length > 0 ? form.password : null,
    }
    try {
      if (editingId) {
        const updated = await staffApi.updateUser(editingId, body)
        if (editingId === me.id) {
          setStaffAuth(updated.email, form.password.length > 0 ? form.password : recoverPassword())
        }
        setMessage(`Updated ${updated.email}.`)
      } else {
        if (!body.password) {
          throw new Error('Password must be between 8 and 128 characters.')
        }
        const created = await staffApi.createUser({ ...body, password: body.password })
        setMessage(`Created ${created.email}.`)
      }
      resetForm()
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save staff account.')
    } finally {
      setBusy(false)
    }
  }

  function recoverPassword(): string {
    const header = staffAuthHeader()
    if (!header) return ''
    try {
      const decoded = atob(header)
      const idx = decoded.indexOf(':')
      return idx >= 0 ? decoded.slice(idx + 1) : ''
    } catch {
      return ''
    }
  }

  async function deactivate(row: StaffAccount) {
    if (!window.confirm(copy.deactivateConfirm(row.email))) return
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      await staffApi.deactivateUser(row.id)
      if (editingId === row.id) resetForm()
      setMessage(`Deactivated ${row.email}.`)
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not deactivate staff account.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <section className="staff-section">
        <h2 className="staff-section-title">{copy.accountsTitle}</h2>
        <p className="staff-lede">{copy.accountsLede}</p>
        {error ? <p className="staff-error">{error}</p> : null}
        {message ? <p className="staff-ok">{message}</p> : null}
        <ul className="staff-list">
          {rows.length === 0 ? <li className="staff-empty">{copy.empty}</li> : null}
          {rows.map((row) => (
            <li key={row.id} className="staff-row">
              <div>
                <strong>{row.displayName}</strong>
                <span>{row.email}</span>
                <span className="staff-meta">
                  <span className={`staff-badge staff-badge-${row.status.toLowerCase()}`}>{row.status}</span>
                  {' · '}
                  {row.roles.join(', ') || 'No roles'}
                </span>
                {row.id === me.id ? <span className="staff-hint">{copy.selfNote}</span> : null}
              </div>
              <div className="staff-actions">
                <button type="button" className="staff-secondary" onClick={() => startEdit(row)}>
                  {copy.edit}
                </button>
                <button
                  type="button"
                  className="staff-danger"
                  disabled={busy || row.id === me.id || row.status === 'INACTIVE'}
                  onClick={() => void deactivate(row)}
                >
                  {copy.deactivate}
                </button>
              </div>
            </li>
          ))}
        </ul>
      </section>

      <form className="staff-card" onSubmit={(event) => void save(event)}>
        <h2 className="staff-section-title">{editingId ? copy.editTitle : copy.createTitle}</h2>
        <label>
          {copy.email}
          <input
            value={form.email}
            onChange={(e) => setForm((current) => ({ ...current, email: e.target.value }))}
            autoComplete="off"
            required
          />
        </label>
        <label>
          {copy.displayName}
          <input
            value={form.displayName}
            onChange={(e) => setForm((current) => ({ ...current, displayName: e.target.value }))}
            required
          />
        </label>
        <label>
          {copy.password}
          <input
            type="password"
            value={form.password}
            onChange={(e) => setForm((current) => ({ ...current, password: e.target.value }))}
            autoComplete="new-password"
            required={!editingId}
            minLength={editingId ? undefined : 8}
          />
        </label>
        <p className="staff-hint">{editingId ? copy.passwordEditHint : copy.passwordCreateHint}</p>
        <fieldset className="staff-fieldset">
          <legend>{copy.roles}</legend>
          {roles.map((role) => (
            <label key={role.code} className="staff-check">
              <input
                type="checkbox"
                checked={form.roles.includes(role.code)}
                onChange={() => toggleRole(role.code)}
              />
              <span>
                {role.name}
                {role.description ? <span className="staff-hint"> — {role.description}</span> : null}
              </span>
            </label>
          ))}
        </fieldset>
        <label>
          {copy.status}
          <select
            value={form.status}
            onChange={(e) => setForm((current) => ({ ...current, status: e.target.value }))}
          >
            {STATUSES.map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </select>
        </label>
        <div className="staff-actions">
          <button type="submit" disabled={busy}>
            {busy ? 'Saving…' : editingId ? copy.save : copy.create}
          </button>
          {editingId ? (
            <button type="button" className="staff-secondary" onClick={resetForm}>
              {copy.cancel}
            </button>
          ) : null}
        </div>
        <p className="staff-hint">{copy.prototypeNote}</p>
      </form>
    </>
  )
}
