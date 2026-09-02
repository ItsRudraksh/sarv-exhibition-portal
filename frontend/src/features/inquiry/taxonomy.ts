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
 * POC seed IDs — must match backend V2__poc_seed.sql.
 * Live taxonomy is loaded from GET /api/v1/taxonomy when the API is up.
 */
export const PROTOTYPE_DEPARTMENTS: Department[] = [
  { id: '10000000-0000-4000-8000-000000000001', name: 'Phytochemicals' },
  { id: '10000000-0000-4000-8000-000000000002', name: 'Oncology APIs' },
  { id: '10000000-0000-4000-8000-000000000003', name: 'Specialty APIs' },
  { id: '10000000-0000-4000-8000-000000000004', name: 'Advanced Intermediates' },
  { id: '10000000-0000-4000-8000-000000000005', name: 'Herbal Extracts' },
  { id: '10000000-0000-4000-8000-000000000006', name: 'Research & Development' },
]

export const PROTOTYPE_PRODUCT_TYPES: ProductType[] = [
  {
    id: '20000000-0000-4000-8000-000000000001',
    name: 'Bulk APIs',
    departmentIds: [
      '10000000-0000-4000-8000-000000000003',
      '10000000-0000-4000-8000-000000000002',
    ],
  },
  {
    id: '20000000-0000-4000-8000-000000000002',
    name: 'Key Intermediates',
    departmentIds: [
      '10000000-0000-4000-8000-000000000004',
      '10000000-0000-4000-8000-000000000002',
    ],
  },
  {
    id: '20000000-0000-4000-8000-000000000003',
    name: 'Phyto Extracts',
    departmentIds: [
      '10000000-0000-4000-8000-000000000001',
      '10000000-0000-4000-8000-000000000005',
    ],
  },
  {
    id: '20000000-0000-4000-8000-000000000004',
    name: 'Oncology Products',
    departmentIds: ['10000000-0000-4000-8000-000000000002'],
  },
  {
    id: '20000000-0000-4000-8000-000000000005',
    name: 'Herbal Ingredients',
    departmentIds: [
      '10000000-0000-4000-8000-000000000005',
      '10000000-0000-4000-8000-000000000001',
    ],
  },
  {
    id: '20000000-0000-4000-8000-000000000006',
    name: 'Custom Synthesis',
    departmentIds: [
      '10000000-0000-4000-8000-000000000006',
      '10000000-0000-4000-8000-000000000004',
    ],
  },
  {
    id: '20000000-0000-4000-8000-000000000007',
    name: 'Antibiotics',
    departmentIds: ['10000000-0000-4000-8000-000000000003'],
  },
  {
    id: '20000000-0000-4000-8000-000000000008',
    name: 'Paclitaxel / Docetaxel Intermediates',
    departmentIds: [
      '10000000-0000-4000-8000-000000000002',
      '10000000-0000-4000-8000-000000000004',
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
