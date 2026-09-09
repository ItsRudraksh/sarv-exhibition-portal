import { useEffect, useState } from 'react'
import { adminCopy as copy } from './copy'
import { staffApi, type PortalSupplierCandidate, type TradingSupplier } from '../staff/api'

const emptyOffline = {
  companyName: '',
  contactName: '',
  email: '',
  phone: '',
  websiteUrl: '',
  notes: '',
}

export function TradingCataloguePanel() {
  const [suppliers, setSuppliers] = useState<TradingSupplier[]>([])
  const [candidates, setCandidates] = useState<PortalSupplierCandidate[]>([])
  const [offline, setOffline] = useState(emptyOffline)
  const [productName, setProductName] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function refresh() {
    const [rows, portal] = await Promise.all([
      staffApi.listTradingSuppliers(),
      staffApi.portalCandidates(),
    ])
    setSuppliers(rows)
    setCandidates(portal)
  }

  useEffect(() => {
    let cancelled = false
    void Promise.all([staffApi.listTradingSuppliers(), staffApi.portalCandidates()])
      .then(([rows, portal]) => {
        if (cancelled) return
        setSuppliers(rows)
        setCandidates(portal)
      })
      .catch((err: unknown) => {
        if (cancelled) return
        setError(err instanceof Error ? err.message : 'Could not load trading catalogue.')
      })
    return () => {
      cancelled = true
    }
  }, [])

  async function createOffline(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      const created = await staffApi.createOfflineSupplier(offline)
      setOffline(emptyOffline)
      setMessage(`Added ${created.companyName}.`)
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not add supplier.')
    } finally {
      setBusy(false)
    }
  }

  async function linkPortal(inquiryId: string) {
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      const linked = await staffApi.linkPortalSupplier(inquiryId)
      setMessage(`Added ${linked.companyName}. Tag products to list them for buyers.`)
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not add portal supplier.')
    } finally {
      setBusy(false)
    }
  }

  async function addProduct(supplierId: string, name: string, listed: boolean) {
    const trimmed = name.trim()
    if (!trimmed) return
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      await staffApi.addTradingProduct(supplierId, { name: trimmed, listedForBuyers: listed })
      setProductName((current) => ({ ...current, [supplierId]: '' }))
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not add product.')
    } finally {
      setBusy(false)
    }
  }

  async function toggleListed(supplier: TradingSupplier, productId: string, listed: boolean) {
    const product = supplier.products.find((row) => row.id === productId)
    if (!product) return
    setBusy(true)
    setError(null)
    try {
      await staffApi.updateTradingProduct(supplier.id, productId, {
        name: product.name,
        listedForBuyers: listed,
      })
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not update product.')
    } finally {
      setBusy(false)
    }
  }

  async function deactivateSupplier(id: string, companyName: string) {
    if (!window.confirm(`Deactivate ${companyName}? Their products leave the buyer list.`)) return
    setBusy(true)
    setError(null)
    try {
      await staffApi.deactivateTradingSupplier(id)
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not deactivate supplier.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <section className="staff-section">
        <h2 className="staff-section-title">{copy.catalogueTitle}</h2>
        <p className="staff-lede">{copy.catalogueLede}</p>
        {error ? <p className="staff-error">{error}</p> : null}
        {message ? <p className="staff-ok">{message}</p> : null}
      </section>

      <form className="staff-card" onSubmit={(event) => void createOffline(event)}>
        <h2 className="staff-section-title">{copy.offlineTitle}</h2>
        <label>
          {copy.company}
          <input
            value={offline.companyName}
            onChange={(e) => setOffline((current) => ({ ...current, companyName: e.target.value }))}
            required
          />
        </label>
        <label>
          {copy.contact}
          <input
            value={offline.contactName}
            onChange={(e) => setOffline((current) => ({ ...current, contactName: e.target.value }))}
          />
        </label>
        <label>
          {copy.email}
          <input
            value={offline.email}
            onChange={(e) => setOffline((current) => ({ ...current, email: e.target.value }))}
          />
        </label>
        <label>
          {copy.phone}
          <input
            value={offline.phone}
            onChange={(e) => setOffline((current) => ({ ...current, phone: e.target.value }))}
          />
        </label>
        <label>
          {copy.website}
          <input
            value={offline.websiteUrl}
            onChange={(e) => setOffline((current) => ({ ...current, websiteUrl: e.target.value }))}
          />
        </label>
        <label>
          {copy.notes}
          <textarea
            value={offline.notes}
            onChange={(e) => setOffline((current) => ({ ...current, notes: e.target.value }))}
            rows={2}
          />
        </label>
        <button type="submit" disabled={busy}>
          {copy.addSupplier}
        </button>
      </form>

      <section className="staff-section">
        <h2 className="staff-section-title">{copy.portalTitle}</h2>
        <p className="staff-lede">{copy.portalLede}</p>
        <ul className="staff-list">
          {candidates.length === 0 ? <li className="staff-empty">{copy.noPortal}</li> : null}
          {candidates.map((row) => (
            <li key={row.inquiryId} className="staff-row">
              <div>
                <strong>{row.companyName || row.referenceCode}</strong>
                <span>
                  {row.referenceCode}
                  {row.personName ? ` · ${row.personName}` : ''}
                </span>
                <span className="staff-meta">{row.reviewState}</span>
              </div>
              <button
                type="button"
                className="staff-secondary"
                disabled={busy}
                onClick={() => void linkPortal(row.inquiryId)}
              >
                {copy.addFromPortal}
              </button>
            </li>
          ))}
        </ul>
      </section>

      <ul className="staff-list">
        {suppliers.length === 0 ? <li className="staff-empty">{copy.noSuppliers}</li> : null}
        {suppliers.map((supplier) => (
          <li key={supplier.id} className="staff-row">
            <div>
              <strong>{supplier.companyName}</strong>
              <span className="staff-meta">
                <span className={`staff-badge staff-badge-${supplier.status.toLowerCase()}`}>
                  {supplier.status}
                </span>
                {' · '}
                {supplier.sourceKind === 'PORTAL' ? copy.sourcePortal : copy.sourceOffline}
                {supplier.contactName ? ` · ${supplier.contactName}` : ''}
              </span>
            </div>
            <div className="staff-actions">
              <button
                type="button"
                className="staff-danger"
                disabled={busy || supplier.status === 'INACTIVE'}
                onClick={() => void deactivateSupplier(supplier.id, supplier.companyName)}
              >
                {copy.deactivate}
              </button>
            </div>
            <div>
              <span className="staff-hint">{copy.products}</span>
              {supplier.products.filter((row) => row.active).length === 0 ? (
                <p className="staff-empty">{copy.noProducts}</p>
              ) : (
                supplier.products
                  .filter((row) => row.active)
                  .map((product) => (
                    <label key={product.id} className="staff-check">
                      <input
                        type="checkbox"
                        checked={product.listedForBuyers}
                        disabled={busy}
                        onChange={(e) => void toggleListed(supplier, product.id, e.target.checked)}
                      />
                      <span>
                        {product.name}
                        <span className="staff-hint">
                          {' — '}
                          {product.listedForBuyers ? copy.listed : copy.notListed}
                        </span>
                      </span>
                    </label>
                  ))
              )}
              {supplier.suggestedProductNames.length > 0 ? (
                <p className="staff-hint" style={{ marginTop: 8 }}>
                  {copy.suggested}
                </p>
              ) : null}
              {supplier.suggestedProductNames.map((name) => (
                <button
                  key={name}
                  type="button"
                  className="staff-secondary"
                  disabled={busy}
                  onClick={() => void addProduct(supplier.id, name, false)}
                >
                  {copy.addSuggested}: {name}
                </button>
              ))}
              <label>
                {copy.productName}
                <input
                  value={productName[supplier.id] ?? ''}
                  onChange={(e) =>
                    setProductName((current) => ({ ...current, [supplier.id]: e.target.value }))
                  }
                />
              </label>
              <div className="staff-actions">
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => void addProduct(supplier.id, productName[supplier.id] ?? '', true)}
                >
                  {copy.addProduct}
                </button>
              </div>
            </div>
          </li>
        ))}
      </ul>
    </>
  )
}
