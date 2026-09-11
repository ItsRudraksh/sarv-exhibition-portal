import { useMemo, useState } from 'react'
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import { groupProductTypesByDepartments } from '../taxonomy'
import { validateSupplierProductTypes } from '../validation'
import { OtherDetailsField } from '../OtherDetailsField'
import { ReviewEditFooter } from '../ReviewEditFooter'
import {
  AppHeader,
  FixedFooter,
  PrimaryButton,
  SearchIcon,
  TextField,
} from '../../../components/ui'

export interface SupplierProductTypesScreenProps {
  readonly journey: InquiryJourney
}

export function SupplierProductTypesScreen({ journey }: SupplierProductTypesScreenProps) {
  const { draft, updateDraft, goBack, advance, editing, finishEdit, cancelEdit } = journey
  const [search, setSearch] = useState('')
  const [error, setError] = useState('')

  const grouped = useMemo(
    () => groupProductTypesByDepartments(draft.departmentIds),
    [draft.departmentIds],
  )

  const filteredGroups = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return grouped
    return grouped
      .map((group) => ({
        ...group,
        types: group.types.filter((pt) => pt.name.toLowerCase().includes(term)),
      }))
      .filter(
        (group) =>
          group.types.length > 0 || group.department.name.toLowerCase().includes(term),
      )
  }, [grouped, search])

  const availableIds = useMemo(
    () => grouped.flatMap((group) => group.types.map((pt) => pt.id)),
    [grouped],
  )

  const toggle = (id: string) => {
    const ids = draft.productTypeIds.includes(id)
      ? draft.productTypeIds.filter((x) => x !== id)
      : [...draft.productTypeIds, id]
    updateDraft({ productTypeIds: ids })
    setError('')
  }

  const handleContinue = () => {
    const fieldErrors = validateSupplierProductTypes(
      draft.productTypeIds,
      draft.supplier.otherProductType,
      draft.supplier.otherProductTypeDetail,
      draft.supplier.capabilityNotes,
    )
    if (fieldErrors.productTypes || fieldErrors.otherProductType) {
      setError(fieldErrors.productTypes || fieldErrors.otherProductType)
      return
    }
    if (editing) {
      finishEdit()
      return
    }
    advance()
  }

  const validIds = new Set(availableIds)
  const effectiveSelected = draft.productTypeIds.filter((id) => validIds.has(id))
  const searchTerm = search.trim().toLowerCase()
  const showOtherCategory =
    draft.supplier.otherCategory &&
    (!searchTerm || copy.supplier.otherCategory.toLowerCase().includes(searchTerm))
  const showOtherType =
    !searchTerm || copy.supplier.otherProductType.toLowerCase().includes(searchTerm)
  const canContinue =
    effectiveSelected.length > 0 ||
    (draft.supplier.otherProductType &&
      draft.supplier.otherProductTypeDetail.trim().length > 0) ||
    draft.supplier.capabilityNotes.trim().length > 0

  const hasCategoryRows =
    filteredGroups.length > 0 || showOtherCategory || showOtherType

  return (
    <div className="inquiry-app">
      <AppHeader
        showBack
        onBack={goBack}
        stepLabel={editing ? copy.common.edit : copy.common.stepOf(2, 4)}
      />

      <main className="inquiry-main inquiry-main--with-header">
        <p className="step-label step-label--muted section-gap">
          {copy.supplier.productTypesStep}
        </p>
        <h1 className="screen-title" style={{ fontSize: '1.25rem' }}>
          {copy.supplier.productTypesTitle}
        </h1>
        <p className="screen-subtitle section-gap">
          {copy.supplier.productTypesSubtitle}
        </p>

        <TextField
          id="capabilityNotes"
          label={copy.supplier.capabilityLabel}
          value={draft.supplier.capabilityNotes}
          onChange={(v) => {
            updateDraft({ supplier: { ...draft.supplier, capabilityNotes: v } })
            setError('')
          }}
          multiline
          hint={copy.supplier.capabilityHint}
        />

        {grouped.length > 0 || draft.supplier.otherCategory ? (
          <div className="search-input-wrap" style={{ marginTop: 16 }}>
            <SearchIcon />
            <input
              type="search"
              placeholder="Search product types"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Search product types"
            />
          </div>
        ) : null}

        {hasCategoryRows ? (
          <div className="checkbox-list" role="group" aria-label={copy.supplier.selectedCategories}>
            {filteredGroups.map((group) => (
              <div key={group.department.id}>
                <label className="checkbox-item checkbox-item--parent">
                  <input
                    type="checkbox"
                    checked
                    disabled
                    aria-label={`${group.department.name} (selected)`}
                  />
                  <span>{group.department.name}</span>
                </label>
                {group.types.map((pt) => (
                  <label key={pt.id} className="checkbox-item checkbox-item--child">
                    <input
                      type="checkbox"
                      checked={effectiveSelected.includes(pt.id)}
                      onChange={() => toggle(pt.id)}
                    />
                    <span>{pt.name}</span>
                  </label>
                ))}
              </div>
            ))}
            {showOtherCategory ? (
              <>
                <label className="checkbox-item checkbox-item--parent">
                  <input
                    type="checkbox"
                    checked
                    disabled
                    aria-label={`${copy.supplier.otherCategory} (selected)`}
                  />
                  <span>{copy.supplier.otherCategory}</span>
                </label>
                {draft.supplier.otherCategoryDetail.trim() ? (
                  <p className="other-detail other-detail--nested field-hint" style={{ margin: 0 }}>
                    {draft.supplier.otherCategoryDetail.trim()}
                  </p>
                ) : null}
              </>
            ) : null}
            {showOtherType ? (
              <OtherDetailsField
                id="otherProductTypeDetail"
                nested
                checked={draft.supplier.otherProductType}
                label={copy.supplier.otherProductType}
                value={draft.supplier.otherProductTypeDetail}
                onToggle={() => {
                  const next = !draft.supplier.otherProductType
                  updateDraft({
                    supplier: {
                      ...draft.supplier,
                      otherProductType: next,
                      otherProductTypeDetail: next ? draft.supplier.otherProductTypeDetail : '',
                    },
                  })
                }}
                onDetailsChange={(v) =>
                  updateDraft({
                    supplier: { ...draft.supplier, otherProductTypeDetail: v },
                  })
                }
                error={
                  error &&
                  draft.supplier.otherProductType &&
                  !draft.supplier.otherProductTypeDetail.trim()
                    ? error
                    : undefined
                }
              />
            ) : null}
          </div>
        ) : null}

        {error ? (
          <p className="field-error" role="alert" style={{ marginTop: 12 }}>
            {error}
          </p>
        ) : null}
      </main>

      {editing ? (
        <ReviewEditFooter
          canSave={canContinue}
          onCancel={cancelEdit}
          onSave={handleContinue}
        />
      ) : (
        <FixedFooter note={copy.supplier.savedSelections}>
          <PrimaryButton disabled={!canContinue} onClick={handleContinue}>
            {copy.common.continue}
          </PrimaryButton>
        </FixedFooter>
      )}
    </div>
  )
}
