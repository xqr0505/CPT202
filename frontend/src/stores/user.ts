import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { logout as apiLogout } from '@/api/auth'
import { getUser } from '@/api/request'
import { fetchUserProfile, type UserProfile } from '@/api/user'
import { USER_ROLES, type UserRoleType } from '@/constants/roles'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  fullName?: string
  email?: string
  phoneNumber?: string
  avatar?: string
}

export type UserRole = UserRoleType | null

interface StoredUser {
  userId?: number
  role?: string
  email?: string
  displayName?: string
}

const normalizeRole = (role?: string | null): UserRole => {
  if (!role) return null

  switch (role.toUpperCase()) {
    case 'CUSTOMER':
      return USER_ROLES.CUSTOMER
    case 'SPECIALIST':
      return USER_ROLES.SPECIALIST
    case 'ADMIN':
      return USER_ROLES.ADMIN
    default:
      return null
  }
}

const getStoredSession = (): { token: string | null; userInfo: UserInfo | null; role: UserRole } => {
  const storedUser = getUser() as StoredUser | null

  return {
    token: localStorage.getItem('token') || sessionStorage.getItem('token'),
    userInfo: storedUser
      ? {
          id: storedUser.userId || 0,
          username: storedUser.email || storedUser.displayName || 'mockuser',
          nickname: storedUser.displayName || storedUser.email || 'Mock User',
          email: storedUser.email
        }
      : null,
    role: normalizeRole(storedUser?.role)
  }
}

const mapUserProfileToUserInfo = (user: UserProfile): UserInfo => ({
  id: user.id,
  username: user.username,
  nickname: user.nickname || user.fullName || user.username,
  fullName: user.fullName,
  email: user.email,
  phoneNumber: user.phoneNumber,
  avatar: user.avatar
})

export const useUserStore = defineStore('user', () => {
  const storedSession = getStoredSession()

  const token = ref<string | null>(storedSession.token || 'fake-jwt-token-12345')
  const userInfo = ref<UserInfo | null>(
    storedSession.userInfo || {
      id: 1,
      username: 'mockuser',
      nickname: 'Mock Customer'
    }
  )
  const userRole = ref<UserRole>(storedSession.role || USER_ROLES.CUSTOMER)

  const isLoggedIn = computed(() => !!token.value)
  const isCustomer = computed(() => userRole.value === USER_ROLES.CUSTOMER)
  const isSpecialist = computed(() => userRole.value === USER_ROLES.SPECIALIST)
  const isAdmin = computed(() => userRole.value === USER_ROLES.ADMIN)

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info: UserInfo, role: UserRole) => {
    userInfo.value = info
    userRole.value = role
  }

  const syncUserProfile = (user: UserProfile) => {
    setUserInfo(mapUserProfileToUserInfo(user), user.role)
  }

  const logout = async () => {
    try {
      if (token.value) {
        await apiLogout()
      }
    } catch (error) {
      console.error('Logout error', error)
    } finally {
      token.value = null
      userInfo.value = null
      userRole.value = null
      localStorage.removeItem('token')
    }
  }

  const fetchAndSetUserProfile = async () => {
    try {
      const user = await fetchUserProfile()
      syncUserProfile(user)
      return user
    } catch (e) {
      console.error('Failed to fetch user profile', e)
      throw e
    }
  }

  return {
    token,
    userInfo,
    userRole,
    isLoggedIn,
    isCustomer,
    isSpecialist,
    isAdmin,
    setToken,
    setUserInfo,
    syncUserProfile,
    logout,
    fetchAndSetUserProfile
  }
})
