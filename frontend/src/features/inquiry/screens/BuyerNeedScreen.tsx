import { useEffect, useMemo, useState } from 'react'
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import { inquiryApi, type BuyerProduct } from '../api'
import { PHARMACOPOEIAL_STANDARDS } from '../taxonomy'
import { validateBuyerNeed } from '../validation'
import { OtherDetailsField } from '../OtherDetailsField'
import type { BuyerFinishedGoodSelection, BuyerTradingProductSelection, PharmacopoeialStandard } from '../types'
import { InquiryAttachments } from '../InquiryAttachments'
import { ReviewEditFooter } from '../ReviewEditFooter'
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
  const {
    draft,
    updateDraft,
    goBack,
    advance,
    uploadAttachments,
    removeAttachment,
    apiAvailable,
    editing,
    finishEdit,
    cancelEdit,
  } = journey
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [specsOpen, setSpecsOpen] = useState(false)
  const [catalogue, setCatalogue] = useState<BuyerProduct[]>([])
  const [catalogueLoaded, setCatalogueLoaded] = useState(false)
  const [search, setSearch] = useState(draft.buyer.productAreaSearch)
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    let cancelled = false
    void inquiryApi
      .listBuyerProducts()
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

  const selectedIds = useMemo(() => {
    const ids = new Set<string>()
    for (const row of draft.buyer.finishedGoods) {
      ids.add(`PHARMA_ERP:${row.finishedGoodId}`)
    }
    for (const row of draft.buyer.tradingProducts) {
      ids.add(`TRADING:${row.tradingProductId}`)
    }
    return ids
  }, [draft.buyer.finishedGoods, draft.buyer.tradingProducts])

  const selected = useMemo(() => {
    const rows: { good: BuyerProduct; quantity: string; qtyKey: string }[] = []
    for (const row of draft.buyer.finishedGoods) {
      const good = catalogue.find((g) => g.id === row.finishedGoodId && g.sourceKind === 'PHARMA_ERP')
      if (good) {
        rows.push({ good, quantity: row.quantity, qtyKey: `qty-PHARMA_ERP-${good.id}` })
      }
    }
    for (const row of draft.buyer.tradingProducts) {
      const good = catalogue.find((g) => g.id === row.tradingProductId && g.sourceKind !== 'PHARMA_ERP')
      if (good) {
        rows.push({ good, quantity: row.quantity, qtyKey: `qty-TRADING-${good.id}` })
      }
    }
    return rows
  }, [catalogue, draft.buyer.finishedGoods, draft.buyer.tradingProducts])

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

  const setCatalogueSelections = (
    finishedGoods: BuyerFinishedGoodSelection[],
    tradingProducts: BuyerTradingProductSelection[],
  ) => {
    const names = [
      ...finishedGoods.map((row) => catalogue.find((g) => g.id === row.finishedGoodId)?.name),
      ...tradingProducts.map((row) => catalogue.find((g) => g.id === row.tradingProductId)?.name),
    ].filter((name): name is string => Boolean(name))
    const requirement =
      draft.buyer.requirement.trim() &&
      ![...draft.buyer.finishedGoods, ...draft.buyer.tradingProducts].some((row) => {
        const name =
          'finishedGoodId' in row
            ? catalogue.find((g) => g.id === row.finishedGoodId)?.name
            : catalogue.find((g) => g.id === row.tradingProductId)?.name
        return name ? draft.buyer.requirement.includes(name) : false
      })
        ? draft.buyer.requirement
        : names.join(', ')
    updateDraft({
      buyer: {
        ...draft.buyer,
        finishedGoods,
        tradingProducts,
        productAreaSearch: search,
        requirement: requirement || draft.buyer.requirement,
      },
    })
  }

  const isPharma = (good: BuyerProduct) => good.sourceKind === 'PHARMA_ERP'

  const toggleGood = (good: BuyerProduct) => {
    if (isPharma(good)) {
      const exists = draft.buyer.finishedGoods.some((row) => row.finishedGoodId === good.id)
      const finishedGoods = exists
        ? draft.buyer.finishedGoods.filter((row) => row.finishedGoodId !== good.id)
        : [...draft.buyer.finishedGoods, { finishedGoodId: good.id, quantity: '', name: good.name }]
      setCatalogueSelections(finishedGoods, draft.buyer.tradingProducts)
      return
    }
    const exists = draft.buyer.tradingProducts.some((row) => row.tradingProductId === good.id)
    const tradingProducts = exists
      ? draft.buyer.tradingProducts.filter((row) => row.tradingProductId !== good.id)
      : [...draft.buyer.tradingProducts, { tradingProductId: good.id, quantity: '', name: good.name }]
    setCatalogueSelections(draft.buyer.finishedGoods, tradingProducts)
  }

  const updateQuantity = (good: BuyerProduct, quantity: string) => {
    if (isPharma(good)) {
      setCatalogueSelections(
        draft.buyer.finishedGoods.map((row) =>
          row.finishedGoodId === good.id ? { ...row, quantity } : row,
        ),
        draft.buyer.tradingProducts,
      )
      return
    }
    setCatalogueSelections(
      draft.buyer.finishedGoods,
      draft.buyer.tradingProducts.map((row) =>
        row.tradingProductId === good.id ? { ...row, quantity } : row,
      ),
    )
  }

  const handleContinue = () => {
    const nextBuyer = { ...draft.buyer }
    if (
      !nextBuyer.requirement.trim() &&
      nextBuyer.otherProduct &&
      nextBuyer.otherProductDetail.trim()
    ) {
      nextBuyer.requirement = nextBuyer.otherProductDetail.trim()
    }
    const fieldErrors = validateBuyerNeed(nextBuyer)
    setErrors(fieldErrors)
    if (Object.keys(fieldErrors).length === 0) {
      updateDraft({ buyer: nextBuyer })
      if (editing) {
        finishEdit()
        return
      }
      advance()
    }
  }

  return (
    <div className="inquiry-app">
      <AppHeader
        showBack
        onBack={goBack}
        stepLabel={editing ? copy.common.edit : copy.common.stepOf(1, 2)}
        progress={editing ? undefined : 0.5}
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
            required={!draft.buyer.otherProduct}
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
                {selected.map(({ good, quantity, qtyKey }) => (
                  <div
                    key={`${good.sourceKind}-${good.id}`}
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
                      id={qtyKey}
                      label={copy.buyer.quantityLabel}
                      value={quantity}
                      onChange={(v) => updateQuantity(good, v)}
                      placeholder={copy.buyer.quantityPlaceholder}
                      required
                      error={errors[qtyKey]}
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
                  const checked = selectedIds.has(
                    r.sourceKind === 'PHARMA_ERP' ? `PHARMA_ERP:${r.id}` : `TRADING:${r.id}`,
                  )
                  return (
                    <li key={`${r.sourceKind}-${r.id}`}>
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
            <div className="checkbox-list" style={{ marginTop: filtered.length > 0 ? 12 : 8 }}>
              <OtherDetailsField
                id="otherProductDetail"
                checked={draft.buyer.otherProduct}
                label={copy.buyer.otherProduct}
                value={draft.buyer.otherProductDetail}
                onToggle={() => {
                  const next = !draft.buyer.otherProduct
                  updateDraft({
                    buyer: {
                      ...draft.buyer,
                      otherProduct: next,
                      otherProductDetail: next ? draft.buyer.otherProductDetail : '',
                    },
                  })
                }}
                onDetailsChange={(v) =>
                  updateDraft({
                    buyer: { ...draft.buyer, otherProductDetail: v },
                  })
                }
                error={errors.otherProduct}
              />
            </div>
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

          <InquiryAttachments
            title={copy.buyer.attachmentsTitle}
            hint={copy.buyer.attachmentsHint}
            files={draft.buyer.attachments}
            uploading={uploading}
            error={errors.attachments}
            apiAvailable={apiAvailable}
            onAdd={(list) => {
              setUploading(true)
              void uploadAttachments(list)
                .then(() => {
                  setErrors((e) => {
                    const next = { ...e }
                    delete next.attachments
                    return next
                  })
                })
                .catch((caught: unknown) => {
                  setErrors((e) => ({
                    ...e,
                    attachments:
                      caught instanceof Error ? caught.message : copy.cardCapture.processingFailed,
                  }))
                })
                .finally(() => setUploading(false))
            }}
            onRemove={(file) => void removeAttachment(file)}
          />

          <p className="field-hint">{copy.buyer.teamFollowUp}</p>
        </div>
      </main>

      {editing ? (
        <ReviewEditFooter
          canSave={Object.keys(validateBuyerNeed(draft.buyer)).length === 0}
          onCancel={cancelEdit}
          onSave={handleContinue}
        />
      ) : (
        <FixedFooter note={copy.buyer.autoSave}>
          <PrimaryButton onClick={handleContinue}>{copy.buyer.review}</PrimaryButton>
        </FixedFooter>
      )}
    </div>
  )
}
