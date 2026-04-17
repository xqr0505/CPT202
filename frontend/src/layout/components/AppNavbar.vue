<template>
  <header class="app-sidebar">
    <div class="sidebar-inner desktop-only">
      <div class="sidebar-header" @click="goHome">
        <img src="@/assets/images/ELicon.png" class="logo-mark" alt="Logo" />
        <span class="system-name">ExpertLink</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        mode="vertical"
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item v-for="item in currentMenus" :key="item.path" :index="item.path">
          <span>{{ item.name }}</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <el-dropdown trigger="click" placement="top" class="user-dropdown">
          <span class="user-trigger" :class="{ 'is-active': route.path.includes('/profile') }">
            <el-avatar :src="avatarSrc" :size="36">{{ userInitial }}</el-avatar>
            <span class="user-name">{{ displayName }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="navigateToProfile">Profile</el-dropdown-item>
              <el-dropdown-item @click="handleLogout">Logout</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- Mobile Bottom Bar -->
    <div class="bottom-bar-inner mobile-only">
      <el-menu
        :default-active="activeMenu"
        mode="horizontal"
        class="bottom-menu"
        @select="handleMenuSelect"
        :ellipsis="false"
      >
        <el-menu-item v-for="item in currentMenus" :key="item.path" :index="item.path">
          <span class="menu-icon" v-html="item.icon"></span>
          <span class="menu-text" v-if="activeMenu === item.path">{{ item.name }}</span>
        </el-menu-item>
      </el-menu>

      <div class="mobile-user">
        <el-dropdown trigger="click" placement="top">
          <span class="user-trigger mobile-trigger" :class="{ 'is-active': route.path.includes('/profile') }">
            <el-avatar :src="avatarSrc" :size="30">{{ userInitial }}</el-avatar>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="navigateToProfile">Profile</el-dropdown-item>
              <el-dropdown-item @click="handleLogout">Logout</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  NavigationFailureType,
  isNavigationFailure,
  useRouter,
  useRoute
} from 'vue-router'
import { useUserStore } from '@/stores/user'
import { USER_ROLES } from '@/constants/roles'
import { useAiChatStore } from '@/stores/aiChat'
import { AI_NAV_MENU_KEY, AI_NAV_MENU_LABEL } from '@/constants/ai'
import { logout as clearAndRedirect } from '@/api/request'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const aiChatStore = useAiChatStore()

interface NavMenuItem {
  name: string
  path: string
  icon: string
}

const mapIcon = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="3 6 9 3 15 6 21 3 21 18 15 21 9 18 3 21"></polygon><line x1="9" y1="3" x2="9" y2="18"></line><line x1="15" y1="6" x2="15" y2="21"></line></svg>`
const dashIcon = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="9"></rect><rect x="14" y="3" width="7" height="5"></rect><rect x="14" y="12" width="7" height="9"></rect><rect x="3" y="16" width="7" height="5"></rect></svg>`
const bagIcon = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path><line x1="3" y1="6" x2="21" y2="6"></line><path d="M16 10a4 4 0 0 1-8 0"></path></svg>`
const chatIcon = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>`
const calendarIcon = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>`
const checkIcon = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5c-1.1 0-2 .9-2 2v2"></path><circle cx="8.5" cy="7" r="4"></circle><polyline points="17 11 19 13 23 9"></polyline></svg>`
const usersIcon = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>`
const listIcon = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" y1="6" x2="21" y2="6"></line><line x1="8" y1="12" x2="21" y2="12"></line><line x1="8" y1="18" x2="21" y2="18"></line><line x1="3" y1="6" x2="3.01" y2="6"></line><line x1="3" y1="12" x2="3.01" y2="12"></line><line x1="3" y1="18" x2="3.01" y2="18"></line></svg>`

const ROLE_MENUS: Record<string, NavMenuItem[]> = {
  [USER_ROLES.CUSTOMER]: [
    { name: 'Find Specialists', path: '/customer/specialists', icon: mapIcon },
    { name: 'Dashboard', path: '/customer/dashboard', icon: dashIcon },
    { name: 'My Bookings', path: '/customer/bookings', icon: bagIcon },
    { name: AI_NAV_MENU_LABEL, path: AI_NAV_MENU_KEY, icon: chatIcon }
  ],
  [USER_ROLES.SPECIALIST]: [
    { name: 'Schedule', path: '/specialist/schedule', icon: calendarIcon },
    { name: 'Approvals', path: '/specialist/booking-requests', icon: checkIcon }
  ],
  [USER_ROLES.ADMIN]: [
    { name: 'Specialists', path: '/admin/specialists', icon: usersIcon },
    { name: 'Categories', path: '/admin/categories', icon: listIcon }
  ]
}

const currentMenus = computed(() => {
  const role = userStore.userRole || USER_ROLES.CUSTOMER
  return (ROLE_MENUS[role as keyof typeof ROLE_MENUS] || ROLE_MENUS[USER_ROLES.CUSTOMER]) as NavMenuItem[]
})

const activeMenu = computed<string>(() => {
  if (aiChatStore.isDrawerOpen && userStore.userRole === USER_ROLES.CUSTOMER) {
    return AI_NAV_MENU_KEY
  }

  const path: string = route.path
  const matchedMenu = currentMenus.value.find(m => m.path !== AI_NAV_MENU_KEY && path.startsWith(m.path))
  if (matchedMenu) {
    return matchedMenu.path
  }
  return ''
})

const displayName = computed<string>(() => {
  const nickname = userStore.userInfo?.nickname?.trim()
  if (nickname) return nickname
  const username = userStore.userInfo?.username?.trim()
  return username || 'Guest'
})

const avatarSrc = computed<string>(() => userStore.userInfo?.avatar?.trim() || '')
const userInitial = computed<string>(() => displayName.value.charAt(0).toUpperCase())

const handleMenuSelect = (path: string): void => {
  if (path === AI_NAV_MENU_KEY) {
    aiChatStore.openDrawer()
    return
  }

  router.push(path)
}

const goHome = (): void => {
  const fallbackMenu = currentMenus.value.find(item => item.path !== AI_NAV_MENU_KEY)
  const fallback = fallbackMenu ? fallbackMenu.path : '/'
  router.push(fallback)
}

const navigateToProfile = async (): Promise<void> => {
  const failure = await router.push({ name: 'CustomerProfile' })

  if (failure && !isNavigationFailure(failure, NavigationFailureType.duplicated)) {
    console.error('Failed to navigate to profile', failure)
    await router.push('/customer/profile').catch(() => null)
  }
}

const handleLogout = (): void => {
  clearAndRedirect()
}
</script>

<style lang="scss">
/* Global layout adjustments for the fixed sidebar / bottom bar without modifying layout components */
@media (min-width: 901px) {
  .default-layout, .admin-layout, .specialist-layout {
    padding-left: var(--sidebar-width);
  }
}
@media (max-width: 900px) {
  .default-layout, .admin-layout, .specialist-layout {
    padding-bottom: max(var(--navbar-height), 64px);
  }
}
</style>

<style scoped lang="scss">
.app-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: var(--sidebar-width);
  height: 100vh;
  z-index: 100;
  background: var(--color-bg-surface);
  border-right: 1px solid var(--color-border);
  box-shadow: 2px 0 12px var(--color-shadow);
  display: flex;
  flex-direction: column;
}

.sidebar-inner {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: var(--space-4) var(--space-3);
  overflow-y: auto;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-1);
  margin-bottom: var(--space-6);
  cursor: pointer;
  color: var(--color-text-primary);
}

.logo-mark {
  width: 40px;
  height: 40px;
  object-fit: contain;
  transition: transform var(--transition-base);
}

.sidebar-header:hover .logo-mark {
  transform: scale(1.08) rotate(-5deg);
}

.system-name {
  font-size: 20px;
  font-weight: 800;
  white-space: nowrap;
  letter-spacing: -0.5px;
}

.sidebar-menu {
  flex: 1;
  border-right: none !important;
  background: transparent;
}

:deep(.sidebar-menu .el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin-bottom: var(--space-2);
  border-radius: var(--radius-lg);
  color: var(--color-text-secondary);
  font-weight: 500;
  display: flex;
  align-items: center;
  transition: all var(--transition-fast);
}

:deep(.sidebar-menu .el-menu-item:hover) {
  background: var(--color-bg-muted);
  color: var(--color-text-primary);
  transform: translateX(4px);
}

:deep(.sidebar-menu .el-menu-item.is-active) {
  background: var(--color-primary-soft) !important;
  color: var(--color-text-inverse) !important;
  font-weight: 600;
  box-shadow: 0 2px 8px var(--color-shadow);
}

/* desktop icons removed — mobile icons remain active in bottom bar */

.sidebar-footer {
  margin-top: auto;
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

.user-dropdown {
  width: 100%;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
  width: 100%;
}

.user-trigger:hover {
  background: var(--color-bg-muted);
}

.user-trigger.is-active {
  background: var(--color-primary-soft);
  color: var(--color-text-inverse);
}

.user-trigger.is-active .user-name {
  color: var(--color-text-inverse);
}

.user-name {
  font-weight: 500;
  color: var(--color-text-primary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-only {
  display: none;
}

/* Responsive: Mobile Bottom Bar */
@media (max-width: 900px) {
  .desktop-only {
    display: none !important;
  }

  .mobile-only {
    display: flex;
  }

  .app-sidebar {
    top: auto;
    bottom: 0;
    width: 100%;
    height: var(--navbar-height, 64px);
    border-right: none;
    border-top: 1px solid var(--color-border);
    box-shadow: 0 -4px 16px var(--color-shadow);
    background: var(--color-bg-surface);
  }

  .bottom-bar-inner {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 var(--space-2);
  }

  .bottom-menu {
    flex: 1;
    border-bottom: none !important;
    background: transparent;
    display: flex;
    justify-content: space-around;
    align-items: center;
    height: 100%;
  }

  :deep(.bottom-menu .el-menu-item) {
    height: 44px;
    line-height: normal;
    padding: 0 var(--space-4) !important;
    color: var(--color-text-secondary);
    font-weight: 600;
    border-bottom: none !important;
    border-radius: 22px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-2);
    margin: 0;
    transition: all 300ms ease;
  }

  :deep(.bottom-menu .el-menu-item.is-active) {
    color: var(--color-primary-active) !important;
    background: var(--color-primary-soft) !important;
    box-shadow: none;
  }

  .menu-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
  }

  .menu-icon :deep(svg) {
    width: 100%;
    height: 100%;
    stroke-width: 2.5;
  }

  .menu-text {
    font-size: 14px;
    white-space: nowrap;
    animation: fadeIn 300ms ease;
  }

  @keyframes fadeIn {
    from { opacity: 0; transform: translateX(-4px); width: 0; }
    to { opacity: 1; transform: translateX(0); width: auto; }
  }

  .mobile-user {
    display: flex;
    align-items: center;
    justify-content: center;
    padding-left: var(--space-2);
    margin-left: var(--space-1);
    border-left: 1px solid var(--color-border);
  }

  .mobile-trigger {
    padding: var(--space-1);
    border-radius: 50%;
  }
}
</style>
