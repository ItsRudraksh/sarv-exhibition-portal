import { useMemo, useState } from 'react'
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import {
  getProductTypesForDepartments,
} from '../taxonomy'
import { validateSupplierProductTypes } from '../validation'
import {
  AppHeader,
  FixedFooter,
  PrimaryButton,
  SearchIcon,
} from '../../../components/ui'

export interface SupplierProductTypesScreenProps {
  readonly journey: InquiryJourney
}

export function SupplierProductTypesScreen({ journey }: SupplierProductTypesScreenProps) {
  const { draft, updateDraft, goBack, advance } = journey
  const [search, setSearch] = useState('')
  const [error, setError] = useState('')

  const available = useMemo(
    () => getProductTypesForDepartments(draft.departmentIds),
    [draft.departmentIds],
  )

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return available
    return available.filter((pt) => pt.name.toLowerCase().includes(term))
  }, [available, search])

  const toggle = (id: string) => {
    const ids = draft.productTypeIds.includes(id)
      ? draft.productTypeIds.filter((x) => x !== id)
      : [...draft.productTypeIds, id]
    updateDraft({ productTypeIds: ids })
    setError('')
  }

  const handleContinue = () => {
    const errs = validateSupplierProductTypes(draft.productTypeIds)
    if (errs.productTypes) {
      setError(errs.productTypes)
      return
    }
    advance()
  }

  // Prune invalid product types when departments change
  const validIds = new Set(available.map((p) => p.id))
  const effectiveSelected = draft.productTypeIds.filter((id) => validIds.has(id))

  return (
    <div className="inquiry-app">
      <AppHeader
        showBack
        onBack={goBack}
        stepLabel={copy.common.stepOf(2, 4)}
      />

      <main className="inquiry-main inquiry-main--with-header">
        <h1 className="screen-title" style={{ fontSize: '1.25rem' }}>
          {copy.supplier.productTypesTitle}
        </h1>
        <p className="screen-subtitle section-gap">
          {copy.supplier.productTypesSubtitle}
        </p>

        <p className="field-hint section-gap" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span aria-hidden>↻</span> {copy.supplier.savedSelections}
        </p>

        <div className="search-input-wrap">
          <SearchIcon />
          <input
            type="search"
            placeholder="Search product types"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search product types"
          />
        </div>

        <div className="checkbox-list" role="group" aria-label="Product types">
          {filtered.map((pt) => (
            <label key={pt.id} className="checkbox-item">
              <input
                type="checkbox"
                checked={effectiveSelected.includes(pt.id)}
                onChange={() => toggle(pt.id)}
              />
              <span>{pt.name}</span>
            </label>
          ))}
        </div>

        {error ? (
          <p className="field-error" role="alert" style={{ marginTop: 12 }}>
            {error}
          </p>
        ) : null}
      </main>

      <FixedFooter note={copy.supplier.savedSelections}>
        <PrimaryButton
          disabled={effectiveSelected.length === 0}
          onClick={handleContinue}
        >
          {copy.common.continue}
        </PrimaryButton>
      </FixedFooter>
    </div>
  )
}
