import request, { getUser } from './request'
import { USER_ROLES, type UserRoleType } from '@/constants/roles'

export interface UserProfile {
  id: number
  username: string
  nickname?: string
  fullName?: string
  email?: string
  phoneNumber?: string
  avatar?: string
  role: UserRoleType
}

export interface AccountProfile {
  id: number
  fullName: string
  email: string
  phoneNumber: string
  avatarUrl: string
  status: string
}

export interface UpdateUserProfilePayload {
  fullName: string
  email: string
  phoneNumber: string
  currentPassword?: string
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
  confirmationPassword: string
}

export interface AvatarUploadResponse {
  avatarUrl: string
}

export interface DeactivateCurrentUserAccountPayload {
  currentPassword: string
}

export interface SecurityActivityItem {
  id: number
  eventType: string
  summary: string
  createdAt: string
}

interface StoredSessionUser {
  userId?: number
  role?: string
  email?: string
  displayName?: string
}

const normalizeUserRole = (value?: string | null): UserRoleType => {
  switch ((value || '').trim().toUpperCase()) {
    case 'CUSTOMER':
      return USER_ROLES.CUSTOMER
    case 'SPECIALIST':
      return USER_ROLES.SPECIALIST
    case 'ADMIN':
      return USER_ROLES.ADMIN
    default:
      throw new Error('Authenticated user role is unavailable.')
  }
}

const silentAccountRequestConfig = {
  suppressErrorMessage: true
} as const

const sanitizeAvatarUrl = (value: unknown): string => {
  return typeof value === 'string' ? value.trim() : ''
}

const sanitizeSecurityActivityText = (value: unknown): string => {
  return typeof value === 'string' ? value.trim() : ''
}

const getUserAccountApiPrefix = (): string => {
  const baseUrl = String(request.defaults.baseURL ?? '')
  return /\/api\/?$/.test(baseUrl) ? '/user' : '/api/user'
}

const toSafeAccountProfile = (payload: unknown): AccountProfile => {
  if (!payload || typeof payload !== 'object') {
    return {
      id: 0,
      fullName: '',
      email: '',
      phoneNumber: '',
      avatarUrl: '',
      status: 'ACTIVE'
    }
  }

  const profile = payload as Partial<AccountProfile>

  return {
    id: typeof profile.id === 'number' && Number.isFinite(profile.id) ? profile.id : 0,
    fullName: typeof profile.fullName === 'string' ? profile.fullName.trim() : '',
    email: typeof profile.email === 'string' ? profile.email.trim() : '',
    phoneNumber: typeof profile.phoneNumber === 'string' ? profile.phoneNumber.trim() : '',
    avatarUrl: sanitizeAvatarUrl(profile.avatarUrl),
    status:
      typeof profile.status === 'string' && profile.status.trim()
        ? profile.status.trim().toUpperCase()
        : 'ACTIVE'
  }
}

const toSafeSecurityActivityItem = (payload: unknown): SecurityActivityItem => {
  if (!payload || typeof payload !== 'object') {
    return {
      id: 0,
      eventType: '',
      summary: '',
      createdAt: ''
    }
  }

  const item = payload as Partial<SecurityActivityItem>

  return {
    id: typeof item.id === 'number' && Number.isFinite(item.id) ? item.id : 0,
    eventType: sanitizeSecurityActivityText(item.eventType),
    summary: sanitizeSecurityActivityText(item.summary),
    createdAt: sanitizeSecurityActivityText(item.createdAt)
  }
}

export const getCurrentUserProfile = async (): Promise<AccountProfile> => {
  const response = await request.get<unknown, unknown>(
    `${getUserAccountApiPrefix()}/profile`,
    silentAccountRequestConfig as unknown as Record<string, unknown>
  )
  return toSafeAccountProfile(response)
}

export const getCurrentUserSecurityActivity = async (): Promise<SecurityActivityItem[]> => {
  const response = await request.get<unknown, unknown>(
    `${getUserAccountApiPrefix()}/security-activity`,
    silentAccountRequestConfig as unknown as Record<string, unknown>
  )

  return Array.isArray(response) ? response.map(toSafeSecurityActivityItem) : []
}

export const updateCurrentUserProfile = async (
  payload: UpdateUserProfilePayload
): Promise<void> => {
  return request.put<unknown, void>(
    `${getUserAccountApiPrefix()}/profile`,
    payload,
    silentAccountRequestConfig as unknown as Record<string, unknown>
  )
}

export const uploadCurrentUserAvatar = async (
  file: File
): Promise<AvatarUploadResponse> => {
  const formData = new FormData()
  formData.append('file', file)

  const response = await request.post<unknown, unknown>(
    `${getUserAccountApiPrefix()}/avatar`,
    formData,
    {
      ...silentAccountRequestConfig,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    } as unknown as Record<string, unknown>
  )

  return {
    avatarUrl: sanitizeAvatarUrl((response as Partial<AvatarUploadResponse>)?.avatarUrl)
  }
}

export const changeCurrentUserPassword = async (
  payload: ChangePasswordPayload
): Promise<void> => {
  return request.post<unknown, void>(
    `${getUserAccountApiPrefix()}/change-password`,
    payload,
    silentAccountRequestConfig as unknown as Record<string, unknown>
  )
}

export const deactivateCurrentUserAccount = async (
  payload: DeactivateCurrentUserAccountPayload
): Promise<void> => {
  return request.post<unknown, void>(
    `${getUserAccountApiPrefix()}/deactivate`,
    payload,
    silentAccountRequestConfig as unknown as Record<string, unknown>
  )
}

export const fetchUserProfile = async (): Promise<UserProfile> => {
  const accountProfile = await getCurrentUserProfile()
  const storedUser = getUser() as StoredSessionUser | null
  const role = normalizeUserRole(storedUser?.role)
  const sessionEmail = storedUser?.email?.trim() || ''
  const sessionDisplayName = storedUser?.displayName?.trim() || ''
  const email = accountProfile.email || sessionEmail
  const username = sessionEmail || email || `user-${accountProfile.id}`

  return {
    id: accountProfile.id,
    username,
    nickname: accountProfile.fullName || sessionDisplayName || email || username,
    fullName: accountProfile.fullName,
    email,
    phoneNumber: accountProfile.phoneNumber,
    avatar: sanitizeAvatarUrl(accountProfile.avatarUrl),
    role
  }
}
