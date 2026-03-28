import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { logout as apiLogout } from '@/api/auth'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
}

export type UserRole = 'customer' | 'specialist' | 'admin' | null

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem('token') || null)
  const userInfo = ref<UserInfo | null>(null)
  const userRole = ref<UserRole>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isCustomer = computed(() => userRole.value === 'customer')
  const isSpecialist = computed(() => userRole.value === 'specialist')

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info: UserInfo, role: UserRole) => {
    userInfo.value = info
    userRole.value = role
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

  return {
    token,
    userInfo,
    userRole,
    isLoggedIn,
    isCustomer,
    isSpecialist,
    setToken,
    setUserInfo,
    logout
  }
})
