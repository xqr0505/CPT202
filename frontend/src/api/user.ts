import { USER_ROLES, type UserRoleType } from '@/constants/roles'

const USER_PROFILE_STORAGE_KEY = 'mock-user-profile'
const USER_PASSWORD_STORAGE_KEY = 'mock-user-password'
const USER_THEME_STORAGE_KEY = 'mock-user-theme-preference'

export type AccountStatus = 'ACTIVE' | 'DEACTIVATED'
export type UserThemePreference = 'light' | 'dark'

const STYLE_OPTIONS: UserThemePreference[] = ['light', 'dark']
const DEFAULT_THEME_PREFERENCE: UserThemePreference = 'light'
const DEFAULT_PASSWORD = 'Password123'
const MIN_PASSWORD_LENGTH = 8

export interface UserProfile {
  id: number
  username: string
  nickname: string
  fullName: string
  email: string
  phoneNumber: string
  role: UserRoleType
  status: AccountStatus
  avatar?: string
}

export interface UpdateUserProfilePayload {
  fullName: string
  email: string
  phoneNumber: string
}

export type UserProfileField = keyof UpdateUserProfilePayload
export type UserProfileFieldErrors = Partial<Record<UserProfileField, string>>

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
  confirmationPassword: string
}

export type ChangePasswordField = keyof ChangePasswordPayload
export type ChangePasswordFieldErrors = Partial<Record<ChangePasswordField, string>>

export interface UserStyleSettings {
  savedPreference: UserThemePreference | null
  effectivePreference: UserThemePreference
  options: UserThemePreference[]
}

class UserProfileAuthError extends Error {
  constructor(message = 'Please log in to continue.') {
    super(message)
    this.name = 'UserProfileAuthError'
  }
}

export class UserProfileValidationError extends Error {
  fieldErrors: UserProfileFieldErrors

  constructor(
    fieldErrors: UserProfileFieldErrors,
    message = 'Please correct the highlighted fields and try again.'
  ) {
    super(message)
    this.name = 'UserProfileValidationError'
    this.fieldErrors = fieldErrors
  }
}

export class UserPasswordValidationError extends Error {
  fieldErrors: ChangePasswordFieldErrors

  constructor(
    fieldErrors: ChangePasswordFieldErrors,
    message = 'Please correct the highlighted fields and try again.'
  ) {
    super(message)
    this.name = 'UserPasswordValidationError'
    this.fieldErrors = fieldErrors
  }
}

export class UserAccountDeactivatedError extends Error {
  constructor(message = 'This account has been deactivated and is no longer accessible.') {
    super(message)
    this.name = 'UserAccountDeactivatedError'
  }
}

const DEFAULT_USER_PROFILE: UserProfile = {
  id: 1,
  username: 'emma.customer',
  nickname: 'Emma Chen',
  fullName: 'Emma Chen',
  email: 'emma.chen@example.com',
  phoneNumber: '+86 138 0013 8000',
  role: USER_ROLES.CUSTOMER,
  status: 'ACTIVE'
}

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const phonePattern = /^\+?[0-9][0-9()\-\s]{6,19}$/

const wait = async (delay = 250): Promise<void> => {
  await new Promise(resolve => window.setTimeout(resolve, delay))
}

const cloneProfile = (profile: UserProfile): UserProfile => ({
  ...profile
})

const normalizeProfilePayload = (
  payload: UpdateUserProfilePayload
): UpdateUserProfilePayload => ({
  fullName: payload.fullName.trim(),
  email: payload.email.trim(),
  phoneNumber: payload.phoneNumber.trim()
})

const hasValidationErrors = (fieldErrors: UserProfileFieldErrors): boolean => {
  return Object.values(fieldErrors).some(Boolean)
}

const hasPasswordValidationErrors = (fieldErrors: ChangePasswordFieldErrors): boolean => {
  return Object.values(fieldErrors).some(Boolean)
}

const ensureAuthenticated = (message: string): void => {
  if (!localStorage.getItem('token')) {
    throw new UserProfileAuthError(message)
  }
}

const ensureAccountActive = (
  message = 'This account has been deactivated and is no longer accessible.'
): void => {
  if (readStoredProfile().status === 'DEACTIVATED') {
    throw new UserAccountDeactivatedError(message)
  }
}

const readStoredPassword = (): string => {
  return localStorage.getItem(USER_PASSWORD_STORAGE_KEY) || DEFAULT_PASSWORD
}

const writeStoredPassword = (password: string): void => {
  localStorage.setItem(USER_PASSWORD_STORAGE_KEY, password)
}

const readStoredThemePreference = (): UserThemePreference | null => {
  const storedPreference = localStorage.getItem(USER_THEME_STORAGE_KEY)
  return storedPreference === 'dark' || storedPreference === 'light'
    ? storedPreference
    : null
}

const normalizePasswordPayload = (
  payload: ChangePasswordPayload
): ChangePasswordPayload => ({
  currentPassword: payload.currentPassword.trim(),
  newPassword: payload.newPassword.trim(),
  confirmationPassword: payload.confirmationPassword.trim()
})

const readStoredProfile = (): UserProfile => {
  const storedProfile = localStorage.getItem(USER_PROFILE_STORAGE_KEY)

  if (!storedProfile) {
    return cloneProfile(DEFAULT_USER_PROFILE)
  }

  try {
    const parsedProfile = JSON.parse(storedProfile) as Partial<UserProfile>
    return {
      ...DEFAULT_USER_PROFILE,
      ...parsedProfile
    }
  } catch (error) {
    console.warn('Failed to parse mock user profile, resetting to defaults.', error)
    localStorage.removeItem(USER_PROFILE_STORAGE_KEY)
    return cloneProfile(DEFAULT_USER_PROFILE)
  }
}

const writeStoredProfile = (profile: UserProfile): void => {
  localStorage.setItem(USER_PROFILE_STORAGE_KEY, JSON.stringify(profile))
}

export const isStoredUserAccountDeactivated = (): boolean => {
  return readStoredProfile().status === 'DEACTIVATED'
}

export const getSavedUserThemePreference = (): UserThemePreference | null => {
  return readStoredThemePreference()
}

export const applyThemePreference = (preference: UserThemePreference): void => {
  if (typeof document === 'undefined') {
    return
  }

  const html = document.documentElement
  html.setAttribute('data-theme', preference)

  if (preference === 'dark') {
    html.classList.add('dark')
  } else {
    html.classList.remove('dark')
  }
}

export const applySavedThemePreference = (): UserThemePreference => {
  const effectivePreference = readStoredThemePreference() || DEFAULT_THEME_PREFERENCE
  applyThemePreference(effectivePreference)
  return effectivePreference
}

export const isUserProfileAuthError = (error: unknown): error is Error => {
  return error instanceof Error && error.name === 'UserProfileAuthError'
}

export const isUserProfileValidationError = (
  error: unknown
): error is UserProfileValidationError => {
  return error instanceof Error && error.name === 'UserProfileValidationError'
}

export const isUserPasswordValidationError = (
  error: unknown
): error is UserPasswordValidationError => {
  return error instanceof Error && error.name === 'UserPasswordValidationError'
}

export const isUserAccountDeactivatedError = (
  error: unknown
): error is UserAccountDeactivatedError => {
  return error instanceof Error && error.name === 'UserAccountDeactivatedError'
}

export const validateUserProfilePayload = (
  payload: UpdateUserProfilePayload
): UserProfileFieldErrors => {
  const normalizedPayload = normalizeProfilePayload(payload)
  const fieldErrors: UserProfileFieldErrors = {}

  if (!normalizedPayload.fullName) {
    fieldErrors.fullName = 'Name is required.'
  }

  if (!normalizedPayload.email) {
    fieldErrors.email = 'Email is required.'
  } else if (!emailPattern.test(normalizedPayload.email)) {
    fieldErrors.email = 'Enter a valid email address.'
  }

  if (!normalizedPayload.phoneNumber) {
    fieldErrors.phoneNumber = 'Phone number is required.'
  } else if (!phonePattern.test(normalizedPayload.phoneNumber)) {
    fieldErrors.phoneNumber = 'Enter a valid phone number.'
  }

  return fieldErrors
}

export const validateChangePasswordPayload = (
  payload: ChangePasswordPayload
): ChangePasswordFieldErrors => {
  const normalizedPayload = normalizePasswordPayload(payload)
  const fieldErrors: ChangePasswordFieldErrors = {}

  if (!normalizedPayload.currentPassword) {
    fieldErrors.currentPassword = 'Current password is required.'
  }

  if (!normalizedPayload.newPassword) {
    fieldErrors.newPassword = 'New password is required.'
  } else if (normalizedPayload.newPassword.length < MIN_PASSWORD_LENGTH) {
    fieldErrors.newPassword = `New password must be at least ${MIN_PASSWORD_LENGTH} characters long.`
  }

  if (!normalizedPayload.confirmationPassword) {
    fieldErrors.confirmationPassword = 'Confirmation password is required.'
  } else if (
    normalizedPayload.newPassword &&
    normalizedPayload.confirmationPassword !== normalizedPayload.newPassword
  ) {
    fieldErrors.confirmationPassword = 'Confirmation password must match the new password.'
  }

  return fieldErrors
}

export const fetchUserProfile = async (): Promise<UserProfile> => {
  await wait()
  ensureAuthenticated('Please log in to view your profile.')
  ensureAccountActive()

  const storedProfile = readStoredProfile()
  writeStoredProfile(storedProfile)

  return cloneProfile(storedProfile)
}

export const updateUserProfile = async (
  payload: UpdateUserProfilePayload
): Promise<UserProfile> => {
  await wait(350)
  ensureAuthenticated('Please log in to update your profile.')
  ensureAccountActive()

  const normalizedPayload = normalizeProfilePayload(payload)
  const fieldErrors = validateUserProfilePayload(normalizedPayload)

  if (hasValidationErrors(fieldErrors)) {
    throw new UserProfileValidationError(fieldErrors)
  }

  const currentProfile = readStoredProfile()
  const nextProfile: UserProfile = {
    ...currentProfile,
    fullName: normalizedPayload.fullName,
    nickname: normalizedPayload.fullName || currentProfile.nickname,
    email: normalizedPayload.email,
    phoneNumber: normalizedPayload.phoneNumber
  }

  writeStoredProfile(nextProfile)

  return cloneProfile(nextProfile)
}

export const changeUserPassword = async (
  payload: ChangePasswordPayload
): Promise<void> => {
  await wait(350)
  ensureAuthenticated('Please log in to change your password.')
  ensureAccountActive()

  const normalizedPayload = normalizePasswordPayload(payload)
  const fieldErrors = validateChangePasswordPayload(normalizedPayload)

  if (normalizedPayload.currentPassword && normalizedPayload.currentPassword !== readStoredPassword()) {
    fieldErrors.currentPassword = 'Current password is incorrect.'
  }

  if (hasPasswordValidationErrors(fieldErrors)) {
    throw new UserPasswordValidationError(fieldErrors)
  }

  writeStoredPassword(normalizedPayload.newPassword)
}

export const fetchUserStyleSettings = async (): Promise<UserStyleSettings> => {
  await wait()
  ensureAuthenticated('Please log in to view page style settings.')
  ensureAccountActive()

  const savedPreference = readStoredThemePreference()
  const effectivePreference = savedPreference || DEFAULT_THEME_PREFERENCE
  applyThemePreference(effectivePreference)

  return {
    savedPreference,
    effectivePreference,
    options: [...STYLE_OPTIONS]
  }
}

export const updateUserStyleSettings = async (
  preference: UserThemePreference
): Promise<UserStyleSettings> => {
  await wait(250)
  ensureAuthenticated('Please log in to update page style settings.')
  ensureAccountActive()

  if (!STYLE_OPTIONS.includes(preference)) {
    throw new Error('Invalid page style preference.')
  }

  localStorage.setItem(USER_THEME_STORAGE_KEY, preference)
  applyThemePreference(preference)

  return {
    savedPreference: preference,
    effectivePreference: preference,
    options: [...STYLE_OPTIONS]
  }
}

export const deactivateUserAccount = async (): Promise<UserProfile> => {
  await wait(350)
  ensureAuthenticated('Please log in to manage your account settings.')

  const currentProfile = readStoredProfile()
  const nextProfile: UserProfile = {
    ...currentProfile,
    status: 'DEACTIVATED'
  }

  writeStoredProfile(nextProfile)

  return cloneProfile(nextProfile)
}
