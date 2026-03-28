import service from './request'

export interface LogoutResponse {
  success?: boolean
}

/**
 * Request to log out.
 * Backend API: POST /auth/logout
 */
export const logout = async (): Promise<LogoutResponse> => {
  return service.post('/auth/logout')
}
