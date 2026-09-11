/**
 * Sell categories: listed checkboxes and/or Other details and/or capability notes.
 */
import { useMemo, useState } from 'react'
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import { listDepartments, listProductTypes } from '../taxonomy'
import { validateSupplierDepartments } from '../validation'
import { OtherDetailsField } from '../OtherDetailsField'
import { ReviewEditFooter } from '../ReviewEditFooter'
import {
  AppHeader,
  FixedFooter,
  Notice,
  PrimaryButton,
  SearchIcon,
  TextField,
} from '../../../components/ui'

export interface SupplierDepartmentsScreenProps {
  readonly journey: InquiryJourney
}

export function SupplierDepartmentsScreen({ journey }: SupplierDepartmentsScreenProps) {
  const { draft, updateDraft, goBack, advance, editing, finishEdit, cancelEdit, goToStep } =
    journey
  const [search, setSearch] = useState('')
  const [error, setError] = useState('')

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return listDepartments()
    return listDepartments().filter((d) =>
      d.name.toLowerCase().includes(term),
    )
  }, [search])

  const toggle = (id: string) => {
    const ids = draft.departmentIds.includes(id)
      ? draft.departmentIds.filter((x) => x !== id)
      : [...draft.departmentIds, id]
    updateDraft({
      departmentIds: ids,
      productTypeIds: draft.productTypeIds.filter((ptId) => {
        const pt = listProductTypes().find((p) => p.id === ptId)
        return pt?.departmentIds.some((d) => ids.includes(d))
      }),
    })
    setError('')
  }

  const handleContinue = () => {
    const errs = validateSupplierDepartments(
      draft.departmentIds,
      draft.supplier.otherCategory,
      draft.supplier.otherCategoryDetail,
      draft.supplier.capabilityNotes,
    )
    const msg = errs.departments || errs.otherCategory
    if (msg) {
      setError(msg)
      return
    }
    if (editing) {
      finishEdit()
      return
    }
    advance()
  }

  const canContinue =
    draft.departmentIds.length > 0 ||
    (draft.supplier.otherCategory && draft.supplier.otherCategoryDetail.trim().length > 0) ||
    draft.supplier.capabilityNotes.trim().length > 0

  return (
    <div className="inquiry-app">
      <AppHeader
        showBack
        onBack={goBack}
        stepLabel={editing ? copy.common.edit : copy.common.stepOf(1, 4)}
      />

      <main className="inquiry-main inquiry-main--with-header">
        <p className="step-label step-label--muted section-gap">
          {copy.supplier.intake}
        </p>
        <h1 className="screen-title" style={{ fontSize: '1.25rem' }}>
          {copy.supplier.departmentsTitle}
        </h1>
        <p className="screen-subtitle section-gap">
          {copy.supplier.departmentsSubtitle}
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

        <div className="search-input-wrap" style={{ marginTop: 16 }}>
          <SearchIcon />
          <input
            type="search"
            placeholder="Search categories"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search categories"
          />
        </div>

        <div className="checkbox-list" role="group" aria-label="Categories">
          {filtered.map((dept) => (
            <label key={dept.id} className="checkbox-item">
              <input
                type="checkbox"
                checked={draft.departmentIds.includes(dept.id)}
                onChange={() => toggle(dept.id)}
              />
              <span>{dept.name}</span>
            </label>
          ))}
          <OtherDetailsField
            id="otherCategoryDetail"
            checked={draft.supplier.otherCategory}
            label={copy.supplier.otherCategory}
            value={draft.supplier.otherCategoryDetail}
            onToggle={() => {
              const next = !draft.supplier.otherCategory
              updateDraft({
                supplier: {
                  ...draft.supplier,
                  otherCategory: next,
                  otherCategoryDetail: next ? draft.supplier.otherCategoryDetail : '',
                },
              })
            }}
            onDetailsChange={(v) =>
              updateDraft({
                supplier: { ...draft.supplier, otherCategoryDetail: v },
              })
            }
            error={
              error && draft.supplier.otherCategory && !draft.supplier.otherCategoryDetail.trim()
                ? error
                : undefined
            }
          />
        </div>

        {error ? (
          <p className="field-error" role="alert" style={{ marginTop: 12 }}>
            {error}
          </p>
        ) : null}

        <Notice icon={null}>
          <p>{copy.supplier.departmentsNote}</p>
        </Notice>
      </main>

      {editing ? (
        <ReviewEditFooter
          canSave={canContinue}
          onCancel={cancelEdit}
          onSave={handleContinue}
          extra={
            <button
              type="button"
              className="btn-text"
              onClick={() => {
                const errs = validateSupplierDepartments(
                  draft.departmentIds,
                  draft.supplier.otherCategory,
                  draft.supplier.otherCategoryDetail,
                  draft.supplier.capabilityNotes,
                )
                if (errs.departments) {
                  setError(errs.departments)
                  return
                }
                goToStep('supplier-product-types')
              }}
            >
              {copy.common.editProductTypes}
            </button>
          }
        />
      ) : (
        <FixedFooter>
          <PrimaryButton disabled={!canContinue} onClick={handleContinue}>
            {copy.common.continue}
          </PrimaryButton>
        </FixedFooter>
      )}
    </div>
  )
}
