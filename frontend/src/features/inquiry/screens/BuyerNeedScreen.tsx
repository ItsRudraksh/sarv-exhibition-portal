import { useState } from 'react'
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import { PHARMACOPOEIAL_STANDARDS, searchProductAreas } from '../taxonomy'
import { validateBuyerNeed } from '../validation'
import type { PharmacopoeialStandard } from '../types'
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
  const [searchResults, setSearchResults] = useState<ReturnType<typeof searchProductAreas>>([])

  const updateBuyer = (field: keyof typeof draft.buyer, value: string) => {
    updateDraft({ buyer: { ...draft.buyer, [field]: value } })
    if (field === 'productAreaSearch') {
      setSearchResults(searchProductAreas(value))
    }
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

  const handleContinue = () => {
    const fieldErrors = validateBuyerNeed(draft.buyer)
    setErrors(fieldErrors)
    if (Object.keys(fieldErrors).length === 0) {
      advance()
    }
  }

  const selectArea = (name: string) => {
    updateDraft({
      buyer: {
        ...draft.buyer,
        productAreaSearch: name,
        requirement: draft.buyer.requirement || name,
      },
    })
    setSearchResults([])
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

        <div className="badge section-gap">
          {copy.buyer.contactSaved}
        </div>

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
                value={draft.buyer.productAreaSearch}
                onChange={(e) => updateBuyer('productAreaSearch', e.target.value)}
                placeholder="Search areas..."
              />
            </div>
            {searchResults.length > 0 ? (
              <ul
                style={{
                  listStyle: 'none',
                  margin: '8px 0 0',
                  padding: 0,
                  border: '1px solid var(--color-glass-border)',
                  borderRadius: 'var(--radius)',
                  background: 'var(--color-pure-surface)',
                }}
              >
                {searchResults.map((r) => (
                  <li key={r.id}>
                    <button
                      type="button"
                      style={{
                        width: '100%',
                        padding: 12,
                        border: 'none',
                        background: 'transparent',
                        textAlign: 'left',
                        cursor: 'pointer',
                        borderBottom: '1px solid var(--color-glass-border)',
                      }}
                      onClick={() => selectArea(r.name)}
                    >
                      {r.name}
                    </button>
                  </li>
                ))}
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
                <div style={{ fontSize: '0.875rem', color: 'var(--color-measured-slate)', marginTop: 4 }}>
                  {copy.buyer.specsHint}
                </div>
              </div>
              <span className={`accordion__chevron${specsOpen ? ' accordion__chevron--open' : ''}`}>
                ▼
              </span>
            </button>
            {specsOpen ? (
              <div className="accordion__content">
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                  <TextField
                    id="quantity"
                    label="Quantity"
                    value={draft.buyer.specifications.quantity}
                    onChange={(v) => updateSpec('quantity', v)}
                    placeholder="e.g. 500"
                  />
                  <TextField
                    id="packSize"
                    label="Pack size"
                    value={draft.buyer.specifications.packSize}
                    onChange={(v) => updateSpec('packSize', v)}
                    placeholder="e.g. 25 kg"
                  />
                </div>
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
