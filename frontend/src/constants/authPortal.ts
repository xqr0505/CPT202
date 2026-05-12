/**
 * Auth entry paths (customer / specialist / admin portals).
 */
export type AuthPortalMode = 'customer' | 'specialist' | 'admin'

export const AUTH_PORTAL_PATH: Record<AuthPortalMode, string> = {
  customer: '/auth',
  specialist: '/auth/specialist',
  admin: '/auth/admin'
}

export function loginPathForProtectedRouteRole(role: string | undefined): string {
  if (role === 'ADMIN') return AUTH_PORTAL_PATH.admin
  if (role === 'SPECIALIST') return AUTH_PORTAL_PATH.specialist
  return AUTH_PORTAL_PATH.customer
}

export function loginPathForStoredRole(role: string | null | undefined): string {
  const r = typeof role === 'string' ? role.toUpperCase() : ''
  if (r === 'ADMIN') return AUTH_PORTAL_PATH.admin
  if (r === 'SPECIALIST') return AUTH_PORTAL_PATH.specialist
  return AUTH_PORTAL_PATH.customer
}

export function loginPathFromForgotPasswordPortalQuery(portal: unknown): string {
  if (portal === 'admin') return AUTH_PORTAL_PATH.admin
  if (portal === 'specialist') return AUTH_PORTAL_PATH.specialist
  return AUTH_PORTAL_PATH.customer
}
