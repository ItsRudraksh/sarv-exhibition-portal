import { useMemo, useState } from 'react'
import type { InquiryJourney } from '../useInquiryJourney'
import { copy } from '../copy'
import { listDepartments, listProductTypes } from '../taxonomy'
import { validateSupplierDepartments } from '../validation'
import {
  AppHeader,
  FixedFooter,
  Notice,
  PrimaryButton,
  SearchIcon,
} from '../../../components/ui'

export interface SupplierDepartmentsScreenProps {
  readonly journey: InquiryJourney
}

export function SupplierDepartmentsScreen({ journey }: SupplierDepartmentsScreenProps) {
  const { draft, updateDraft, goBack, advance } = journey
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
    const errs = validateSupplierDepartments(draft.departmentIds)
    const msg = errs.departments
    if (msg) {
      setError(msg)
      return
    }
    advance()
  }

  return (
    <div className="inquiry-app">
      <AppHeader
        showBack
        onBack={goBack}
        stepLabel={copy.common.stepOf(1, 4)}
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

        <div className="search-input-wrap">
          <SearchIcon />
          <input
            type="search"
            placeholder="Search departments"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search departments"
          />
        </div>

        <div className="checkbox-list" role="group" aria-label="Departments">
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

      <FixedFooter>
        <PrimaryButton
          disabled={draft.departmentIds.length === 0}
          onClick={handleContinue}
        >
          {copy.common.continue}
        </PrimaryButton>
      </FixedFooter>
    </div>
  )
}
