import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { logout as apiLogout } from '@/api/auth'
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
  const token = ref<string | null>(localStorage.getItem('token') || null)
  const userInfo = ref<UserInfo | null>(null)
  const userRole = ref<UserRole>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isCustomer = computed(() => userRole.value === USER_ROLES.CUSTOMER)
  const isSpecialist = computed(() => userRole.value === USER_ROLES.SPECIALIST)

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
    setToken,
    setUserInfo,
    syncUserProfile,
    logout,
    fetchAndSetUserProfile
  }
})
