/**
 * Public URL prefix for the packaged SPA.
 *
 * Vite {@code base} is `/` on the IP:port and on welcome.sarvbiolabs.com.
 * Set {@code VITE_BASE=/exhibit/} (build) and {@code SERVER_SERVLET_CONTEXT_PATH=/exhibit}
 * (runtime) together when a proxy serves https://sarvbiolabs.com/exhibit.
 */
export function publicBasePath(): string {
  const raw = import.meta.env.BASE_URL || '/'
  if (raw === '/') return ''
  return raw.endsWith('/') ? raw.slice(0, -1) : raw
}

/** Prefix a root-relative path (`/staff` → `/exhibit/staff` when the base is set). */
export function withPublicBase(path: string): string {
  const suffix = path.startsWith('/') ? path : `/${path}`
  return `${publicBasePath()}${suffix}`
}

/** Strip the public base so App.tsx still matches `/staff` and `/admin`. */
export function stripPublicBase(pathname: string): string {
  const base = publicBasePath()
  if (!base) return pathname || '/'
  if (pathname === base || pathname === `${base}/`) return '/'
  if (pathname.startsWith(`${base}/`)) {
    const rest = pathname.slice(base.length)
    return rest.startsWith('/') ? rest : `/${rest}`
  }
  return pathname || '/'
}
