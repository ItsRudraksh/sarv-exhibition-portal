export interface Department {
  id: string
  name: string
}

export interface ProductType {
  id: string
  name: string
  departmentIds: string[]
}

/** Prototype taxonomy — replace with API-driven config in production. */
export const PROTOTYPE_DEPARTMENTS: Department[] = [
  { id: 'phytochemicals', name: 'Phytochemicals' },
  { id: 'oncology_apis', name: 'Oncology APIs' },
  { id: 'specialty_apis', name: 'Specialty APIs' },
  { id: 'advanced_intermediates', name: 'Advanced Intermediates' },
  { id: 'herbal_extracts', name: 'Herbal Extracts' },
  { id: 'research_development', name: 'Research & Development' },
]

export const PROTOTYPE_PRODUCT_TYPES: ProductType[] = [
  { id: 'api_bulk', name: 'Bulk APIs', departmentIds: ['specialty_apis', 'oncology_apis'] },
  { id: 'intermediates', name: 'Key Intermediates', departmentIds: ['advanced_intermediates', 'oncology_apis'] },
  { id: 'phyto_extracts', name: 'Phyto Extracts', departmentIds: ['phytochemicals', 'herbal_extracts'] },
  { id: 'oncology_finished', name: 'Oncology Products', departmentIds: ['oncology_apis'] },
  { id: 'herbal_ingredients', name: 'Herbal Ingredients', departmentIds: ['herbal_extracts', 'phytochemicals'] },
  { id: 'custom_synthesis', name: 'Custom Synthesis', departmentIds: ['research_development', 'advanced_intermediates'] },
  { id: 'antibiotics', name: 'Antibiotics', departmentIds: ['specialty_apis'] },
  { id: 'paclitaxel_docetaxel', name: 'Paclitaxel / Docetaxel Intermediates', departmentIds: ['oncology_apis', 'advanced_intermediates'] },
]

export const PHARMACOPOEIAL_STANDARDS = ['IP', 'USP', 'BP', 'EP'] as const

export function getDepartmentsByIds(ids: string[]): Department[] {
  return PROTOTYPE_DEPARTMENTS.filter((d) => ids.includes(d.id))
}

export function getProductTypesForDepartments(departmentIds: string[]): ProductType[] {
  if (departmentIds.length === 0) return []
  return PROTOTYPE_PRODUCT_TYPES.filter((pt) =>
    pt.departmentIds.some((id) => departmentIds.includes(id)),
  )
}

export function getProductTypesByIds(ids: string[]): ProductType[] {
  return PROTOTYPE_PRODUCT_TYPES.filter((pt) => ids.includes(pt.id))
}

export function searchProductAreas(query: string): ProductType[] {
  const term = query.trim().toLowerCase()
  if (!term) return []
  return PROTOTYPE_PRODUCT_TYPES.filter(
    (pt) =>
      pt.name.toLowerCase().includes(term) ||
      getDepartmentsByIds(pt.departmentIds).some((d) =>
        d.name.toLowerCase().includes(term),
      ),
  )
}
