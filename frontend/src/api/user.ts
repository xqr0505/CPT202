import request from './request'
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
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
  confirmationPassword: string
}

export interface AvatarUploadResponse {
  avatarUrl: string
}

export const applySavedThemePreference = (): void => {
  // Safe no-op until theme preference is centralized.
}

export const isStoredUserAccountDeactivated = (): boolean => {
  const storedProfile = localStorage.getItem('mock-user-profile')

  if (!storedProfile) {
    return false
  }

  try {
    const parsedProfile = JSON.parse(storedProfile) as Partial<AccountProfile>
    return (
      typeof parsedProfile.status === 'string' &&
      parsedProfile.status.trim().toUpperCase() === 'DEACTIVATED'
    )
  } catch {
    return false
  }
}

const silentAccountRequestConfig = {
  suppressErrorMessage: true
} as const

const sanitizeAvatarUrl = (value: unknown): string => {
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

export const getCurrentUserProfile = async (): Promise<AccountProfile> => {
  const response = await request.get<any, unknown>(
    `${getUserAccountApiPrefix()}/profile`,
    silentAccountRequestConfig as any
  )
  return toSafeAccountProfile(response)
}

export const updateCurrentUserProfile = async (
  payload: UpdateUserProfilePayload
): Promise<void> => {
  return request.put<any, void>(
    `${getUserAccountApiPrefix()}/profile`,
    payload,
    silentAccountRequestConfig as any
  )
}

export const uploadCurrentUserAvatar = async (
  file: File
): Promise<AvatarUploadResponse> => {
  const formData = new FormData()
  formData.append('file', file)

  const response = await request.post<any, unknown>(
    `${getUserAccountApiPrefix()}/avatar`,
    formData,
    {
      ...silentAccountRequestConfig,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    } as any
  )

  return {
    avatarUrl: sanitizeAvatarUrl((response as Partial<AvatarUploadResponse>)?.avatarUrl)
  }
}

export const changeCurrentUserPassword = async (
  payload: ChangePasswordPayload
): Promise<void> => {
  return request.post<any, void>(
    `${getUserAccountApiPrefix()}/change-password`,
    payload,
    silentAccountRequestConfig as any
  )
}

export const deactivateCurrentUserAccount = async (): Promise<void> => {
  return request.post<any, void>(
    `${getUserAccountApiPrefix()}/deactivate`,
    undefined,
    silentAccountRequestConfig as any
  )
}

// Kept intentionally stable because unrelated layouts/stores still rely on the mock shape.
export const fetchUserProfile = async (): Promise<UserProfile> => {
  let storedProfile: Partial<AccountProfile> | null = null

  try {
    const rawProfile = localStorage.getItem('mock-user-profile')
    storedProfile = rawProfile ? (JSON.parse(rawProfile) as Partial<AccountProfile>) : null
  } catch {
    storedProfile = null
  }

  return Promise.resolve({
    id:
      typeof storedProfile?.id === 'number' && Number.isFinite(storedProfile.id)
        ? storedProfile.id
        : 1,
    username: 'test_user',
    nickname: storedProfile?.fullName?.trim() || 'Test User',
    fullName: typeof storedProfile?.fullName === 'string' ? storedProfile.fullName.trim() : '',
    email: typeof storedProfile?.email === 'string' ? storedProfile.email.trim() : '',
    phoneNumber:
      typeof storedProfile?.phoneNumber === 'string' ? storedProfile.phoneNumber.trim() : '',
    avatar: sanitizeAvatarUrl(storedProfile?.avatarUrl),
    role: USER_ROLES.CUSTOMER
  })
}
