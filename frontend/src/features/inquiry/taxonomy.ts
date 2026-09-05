export interface Department {
  id: string
  name: string
}

export interface ProductType {
  id: string
  name: string
  departmentIds: string[]
}

/**
 * Offline fallback — must match backend V7__business_taxonomy.sql (active rows).
 * Live taxonomy is loaded from GET /api/v1/taxonomy when the API is up.
 * Source of truth for labels/codes: specs/taxonomy/
 */
export const PROTOTYPE_DEPARTMENTS: Department[] = [
  { id: 'a1000000-0000-4000-8000-000000000001', name: 'Phytochemicals' },
  { id: 'a1000000-0000-4000-8000-000000000002', name: 'Oncology APIs' },
  { id: 'a1000000-0000-4000-8000-000000000003', name: 'Specialty APIs' },
  { id: 'a1000000-0000-4000-8000-000000000004', name: 'Advanced Intermediates' },
  { id: 'a1000000-0000-4000-8000-000000000005', name: 'Herbal Extracts' },
  { id: 'a1000000-0000-4000-8000-000000000006', name: 'Research & Development' },
]

export const PROTOTYPE_PRODUCT_TYPES: ProductType[] = [
  {
    id: 'a2000000-0000-4000-8000-000000000001',
    name: 'Bulk APIs',
    departmentIds: [
      'a1000000-0000-4000-8000-000000000003',
      'a1000000-0000-4000-8000-000000000002',
    ],
  },
  {
    id: 'a2000000-0000-4000-8000-000000000002',
    name: 'Key Intermediates',
    departmentIds: [
      'a1000000-0000-4000-8000-000000000004',
      'a1000000-0000-4000-8000-000000000002',
    ],
  },
  {
    id: 'a2000000-0000-4000-8000-000000000003',
    name: 'Phyto Extracts',
    departmentIds: [
      'a1000000-0000-4000-8000-000000000001',
      'a1000000-0000-4000-8000-000000000005',
    ],
  },
  {
    id: 'a2000000-0000-4000-8000-000000000004',
    name: 'Oncology Products',
    departmentIds: ['a1000000-0000-4000-8000-000000000002'],
  },
  {
    id: 'a2000000-0000-4000-8000-000000000005',
    name: 'Herbal Ingredients',
    departmentIds: [
      'a1000000-0000-4000-8000-000000000005',
      'a1000000-0000-4000-8000-000000000001',
    ],
  },
  {
    id: 'a2000000-0000-4000-8000-000000000006',
    name: 'Custom Synthesis',
    departmentIds: [
      'a1000000-0000-4000-8000-000000000006',
      'a1000000-0000-4000-8000-000000000004',
    ],
  },
  {
    id: 'a2000000-0000-4000-8000-000000000007',
    name: 'Antibiotics',
    departmentIds: ['a1000000-0000-4000-8000-000000000003'],
  },
  {
    id: 'a2000000-0000-4000-8000-000000000008',
    name: 'Paclitaxel / Docetaxel Intermediates',
    departmentIds: [
      'a1000000-0000-4000-8000-000000000002',
      'a1000000-0000-4000-8000-000000000004',
    ],
  },
]

export const PHARMACOPOEIAL_STANDARDS = ['IP', 'USP', 'BP', 'EP'] as const

let liveDepartments: Department[] | null = null
let liveProductTypes: ProductType[] | null = null

export function setLiveTaxonomy(departments: Department[], productTypes: ProductType[]): void {
  liveDepartments = departments
  liveProductTypes = productTypes
}

function departments(): Department[] {
  return liveDepartments ?? PROTOTYPE_DEPARTMENTS
}

function productTypes(): ProductType[] {
  return liveProductTypes ?? PROTOTYPE_PRODUCT_TYPES
}

export function listDepartments(): Department[] {
  return departments()
}

export function listProductTypes(): ProductType[] {
  return productTypes()
}

export function getDepartmentsByIds(ids: string[]): Department[] {
  return departments().filter((d) => ids.includes(d.id))
}

export function getProductTypesForDepartments(departmentIds: string[]): ProductType[] {
  if (departmentIds.length === 0) return []
  return productTypes().filter((pt) =>
    pt.departmentIds.some((id) => departmentIds.includes(id)),
  )
}

export function getProductTypesByIds(ids: string[]): ProductType[] {
  return productTypes().filter((pt) => ids.includes(pt.id))
}

export function searchProductAreas(query: string): ProductType[] {
  const term = query.trim().toLowerCase()
  if (!term) return []
  return productTypes().filter(
    (pt) =>
      pt.name.toLowerCase().includes(term) ||
      getDepartmentsByIds(pt.departmentIds).some((d) =>
        d.name.toLowerCase().includes(term),
      ),
  )
}
