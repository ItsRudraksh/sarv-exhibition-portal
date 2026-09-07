import { useEffect, useMemo, useState } from 'react'
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import { inquiryApi, type FinishedGood } from '../api'
import { PHARMACOPOEIAL_STANDARDS } from '../taxonomy'
import { validateBuyerNeed } from '../validation'
import type { BuyerFinishedGoodSelection, PharmacopoeialStandard } from '../types'
import {
  AppHeader,
  FixedFooter,
  PrimaryButton,
  SearchIcon,
  TextField,
} from '../../../components/ui'

export interface BuyerNeedScreenProps {
  readonly journey: InquiryJourney
}

export function BuyerNeedScreen({ journey }: BuyerNeedScreenProps) {
  const { draft, updateDraft, goBack, advance } = journey
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [specsOpen, setSpecsOpen] = useState(false)
  const [catalogue, setCatalogue] = useState<FinishedGood[]>([])
  const [catalogueLoaded, setCatalogueLoaded] = useState(false)
  const [search, setSearch] = useState(draft.buyer.productAreaSearch)

  useEffect(() => {
    let cancelled = false
    void inquiryApi
      .listFinishedGoods()
      .then((rows) => {
        if (!cancelled) {
          setCatalogue(rows)
          setCatalogueLoaded(true)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCatalogue([])
          setCatalogueLoaded(true)
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return catalogue.slice(0, 40)
    return catalogue.filter((g) => g.name.toLowerCase().includes(term)).slice(0, 40)
  }, [catalogue, search])

  const selectedIds = useMemo(
    () => new Set(draft.buyer.finishedGoods.map((g) => g.finishedGoodId)),
    [draft.buyer.finishedGoods],
  )

  const selected = useMemo(() => {
    return draft.buyer.finishedGoods
      .map((row) => {
        const good = catalogue.find((g) => g.id === row.finishedGoodId)
        return good ? { good, quantity: row.quantity } : null
      })
      .filter((row): row is { good: FinishedGood; quantity: string } => row !== null)
  }, [catalogue, draft.buyer.finishedGoods])

  const updateBuyer = (field: keyof typeof draft.buyer, value: string) => {
    updateDraft({ buyer: { ...draft.buyer, [field]: value } })
  }

  const updateSpec = (
    field: keyof typeof draft.buyer.specifications,
    value: string,
  ) => {
    updateDraft({
      buyer: {
        ...draft.buyer,
        specifications: { ...draft.buyer.specifications, [field]: value },
      },
    })
  }

  const setFinishedGoods = (rows: BuyerFinishedGoodSelection[]) => {
    const names = rows
      .map((row) => catalogue.find((g) => g.id === row.finishedGoodId)?.name)
      .filter((name): name is string => Boolean(name))
    const requirement =
      draft.buyer.requirement.trim() &&
      !draft.buyer.finishedGoods.some((row) =>
        draft.buyer.requirement.includes(
          catalogue.find((g) => g.id === row.finishedGoodId)?.name ?? '',
        ),
      )
        ? draft.buyer.requirement
        : names.join(', ')
    updateDraft({
      buyer: {
        ...draft.buyer,
        finishedGoods: rows,
        productAreaSearch: search,
        requirement: requirement || draft.buyer.requirement,
      },
    })
  }

  const toggleGood = (good: FinishedGood) => {
    const exists = draft.buyer.finishedGoods.some((row) => row.finishedGoodId === good.id)
    const rows = exists
      ? draft.buyer.finishedGoods.filter((row) => row.finishedGoodId !== good.id)
      : [...draft.buyer.finishedGoods, { finishedGoodId: good.id, quantity: '', name: good.name }]
    setFinishedGoods(rows)
  }

  const updateQuantity = (finishedGoodId: string, quantity: string) => {
    setFinishedGoods(
      draft.buyer.finishedGoods.map((row) =>
        row.finishedGoodId === finishedGoodId ? { ...row, quantity } : row,
      ),
    )
  }

  const handleContinue = () => {
    const fieldErrors = validateBuyerNeed(draft.buyer)
    setErrors(fieldErrors)
    if (Object.keys(fieldErrors).length === 0) {
      advance()
    }
  }

  return (
    <div className="inquiry-app">
      <AppHeader
        showBack
        onBack={goBack}
        stepLabel={copy.common.stepOf(1, 2)}
        progress={0.5}
      />

      <main className="inquiry-main inquiry-main--with-header">
        <h1 className="screen-title">{copy.buyer.needTitle}</h1>
        <p className="screen-subtitle section-gap">{copy.buyer.needSubtitle}</p>

        <div className="badge section-gap">{copy.buyer.contactSaved}</div>

        <div className="stack-gap section-gap">
          <TextField
            id="requirement"
            label={copy.buyer.requirementLabel}
            value={draft.buyer.requirement}
            onChange={(v) => updateBuyer('requirement', v)}
            multiline
            required
            error={errors.requirement}
            hint={copy.buyer.requirementHint}
          />

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 16,
              margin: '8px 0',
            }}
          >
            <div style={{ flex: 1, height: 1, background: 'var(--color-glass-border)' }} />
            <span className="step-label step-label--muted">OR</span>
            <div style={{ flex: 1, height: 1, background: 'var(--color-glass-border)' }} />
          </div>

          <div className="field">
            <label htmlFor="productAreaSearch">{copy.buyer.searchAreas}</label>
            <div className="search-input-wrap" style={{ marginBottom: 0 }}>
              <SearchIcon />
              <input
                id="productAreaSearch"
                type="search"
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value)
                  updateBuyer('productAreaSearch', e.target.value)
                }}
                placeholder={copy.buyer.searchPlaceholder}
              />
            </div>
            {catalogueLoaded && catalogue.length === 0 ? (
              <p className="field-hint" style={{ marginTop: 8 }}>
                {copy.buyer.catalogueEmpty}
              </p>
            ) : null}
            {selected.length > 0 ? (
              <div className="stack-gap" style={{ marginTop: 12 }}>
                <span className="field-hint">{copy.buyer.selectedGoods}</span>
                {selected.map(({ good, quantity }) => (
                  <div
                    key={good.id}
                    style={{
                      border: '2px solid var(--color-alpine-blue)',
                      borderRadius: 'var(--radius)',
                      background:
                        'color-mix(in srgb, var(--color-alpine-blue) 10%, var(--color-pure-surface))',
                      padding: 12,
                    }}
                  >
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        gap: 12,
                        alignItems: 'center',
                        marginBottom: 8,
                      }}
                    >
                      <strong>{good.name}</strong>
                      <button
                        type="button"
                        className="btn-text"
                        onClick={() => toggleGood(good)}
                      >
                        {copy.buyer.removeGood}
                      </button>
                    </div>
                    <TextField
                      id={`fg-qty-${good.id}`}
                      label={copy.buyer.quantityLabel}
                      value={quantity}
                      onChange={(v) => updateQuantity(good.id, v)}
                      placeholder={copy.buyer.quantityPlaceholder}
                      required
                      error={errors[`fgQty-${good.id}`]}
                    />
                  </div>
                ))}
              </div>
            ) : null}
            {filtered.length > 0 ? (
              <ul
                style={{
                  listStyle: 'none',
                  margin: '8px 0 0',
                  padding: 0,
                  border: '1px solid var(--color-glass-border)',
                  borderRadius: 'var(--radius)',
                  background: 'var(--color-pure-surface)',
                  maxHeight: 240,
                  overflowY: 'auto',
                }}
              >
                {filtered.map((r) => {
                  const checked = selectedIds.has(r.id)
                  return (
                    <li key={r.id}>
                      <button
                        type="button"
                        aria-pressed={checked}
                        style={{
                          width: '100%',
                          padding: '12px 12px 12px 10px',
                          border: 'none',
                          borderLeft: checked
                            ? '4px solid var(--color-alpine-blue)'
                            : '4px solid transparent',
                          background: checked
                            ? 'color-mix(in srgb, var(--color-alpine-blue) 18%, transparent)'
                            : 'transparent',
                          textAlign: 'left',
                          cursor: 'pointer',
                          borderBottom: '1px solid var(--color-glass-border)',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          gap: 12,
                          fontWeight: checked ? 600 : 400,
                          color: checked
                            ? 'var(--color-alpine-blue)'
                            : 'inherit',
                        }}
                        onClick={() => toggleGood(r)}
                      >
                        <span>{r.name}</span>
                        {checked ? (
                          <span
                            className="badge"
                            style={{
                              background: 'var(--color-alpine-blue)',
                              color: 'white',
                              flexShrink: 0,
                            }}
                          >
                            {copy.buyer.selectedBadge}
                          </span>
                        ) : null}
                      </button>
                    </li>
                  )
                })}
              </ul>
            ) : null}
          </div>

          <div className="accordion">
            <button
              type="button"
              className="accordion__trigger"
              aria-expanded={specsOpen}
              onClick={() => setSpecsOpen(!specsOpen)}
            >
              <div>
                <div style={{ fontWeight: 500 }}>{copy.buyer.specsTitle}</div>
                <div
                  style={{
                    fontSize: '0.875rem',
                    color: 'var(--color-measured-slate)',
                    marginTop: 4,
                  }}
                >
                  {copy.buyer.specsHint}
                </div>
              </div>
              <span
                className={`accordion__chevron${specsOpen ? ' accordion__chevron--open' : ''}`}
              >
                ▼
              </span>
            </button>
            {specsOpen ? (
              <div className="accordion__content">
                <TextField
                  id="packSize"
                  label="Pack size"
                  value={draft.buyer.specifications.packSize}
                  onChange={(v) => updateSpec('packSize', v)}
                  placeholder="e.g. 25 kg"
                />
                <div className="field">
                  <label htmlFor="standard">Standard</label>
                  <select
                    id="standard"
                    value={draft.buyer.specifications.standard}
                    onChange={(e) =>
                      updateSpec('standard', e.target.value as PharmacopoeialStandard | '')
                    }
                  >
                    <option value="">Select (optional)</option>
                    {PHARMACOPOEIAL_STANDARDS.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                </div>
                <TextField
                  id="neededByDate"
                  label="Needed-by date"
                  type="date"
                  value={draft.buyer.specifications.neededByDate}
                  onChange={(v) => updateSpec('neededByDate', v)}
                />
                <TextField
                  id="specNotes"
                  label="Notes"
                  value={draft.buyer.specifications.notes}
                  onChange={(v) => updateSpec('notes', v)}
                  multiline
                  rows={2}
                />
              </div>
            ) : null}
          </div>

          <p className="field-hint">{copy.buyer.teamFollowUp}</p>
        </div>
      </main>

      <FixedFooter note={copy.buyer.autoSave}>
        <PrimaryButton onClick={handleContinue}>{copy.buyer.review}</PrimaryButton>
      </FixedFooter>
    </div>
  )
}
