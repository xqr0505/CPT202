<template>
  <header class="app-navbar">
    <div class="navbar-inner">
      <button class="mobile-toggler" @click="isMobileMenuOpen = true" aria-label="Menu">
        <svg viewBox="0 0 24 24" width="24" height="24" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>
      </button>

      <div class="navbar-left" @click="goHome">
        <img src="@/assets/images/ELicon.png" class="logo-mark" alt="Logo" />
        <span class="system-name">ExpertLink</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        mode="horizontal"
        class="navbar-menu desktop-only"
        @select="handleMenuSelect"
        :ellipsis="false"
      >
        <el-menu-item v-for="item in currentMenus" :key="item.path" :index="item.path">
          {{ item.name }}
        </el-menu-item>
      </el-menu>

      <div class="navbar-right">
        <el-dropdown trigger="click" @command="handleDropdownCommand">
          <span class="user-trigger">
            <el-avatar :src="avatarSrc" :size="34">{{ userInitial }}</el-avatar>
            <span class="user-name desktop-only">{{ displayName }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">Profile</el-dropdown-item>
              <el-dropdown-item command="logout">Logout</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- Mobile Drawer -->
    <el-drawer
      v-model="isMobileMenuOpen"
      direction="ltr"
      size="240px"
      append-to-body
      :with-header="false"
    >
      <div class="drawer-header" @click="goHome">
        <img src="@/assets/images/ELicon.png" class="logo-mark" alt="Logo" />
        <span class="system-name-dark">ExpertLink</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="mobile-menu"
        @select="handleMobileMenuSelect"
      >
        <el-menu-item v-for="item in currentMenus" :key="item.path" :index="item.path">
          {{ item.name }}
        </el-menu-item>
      </el-menu>
    </el-drawer>
  </header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { USER_ROLES } from '@/constants/roles'

type DropdownCommand = 'profile' | 'logout'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isMobileMenuOpen = ref(false)

const ROLE_MENUS = {
  [USER_ROLES.CUSTOMER]: [
    { name: 'Find Specialists', path: '/customer/specialists' },
    { name: 'Dashboard', path: '/customer/dashboard' },
    { name: 'My Bookings', path: '/customer/bookings' }
  ],
  [USER_ROLES.SPECIALIST]: [
    { name: 'Dashboard', path: '/specialist/dashboard' },
    { name: 'Schedule Management', path: '/specialist/schedule' },
    { name: 'Appointment Approval', path: '/specialist/requests' }
  ],
  [USER_ROLES.ADMIN]: [
    { name: 'Specialist Management', path: '/admin/specialists' },
    { name: 'Category Settings', path: '/admin/categories' }
  ]
}

const currentMenus = computed(() => {
  // Use customer as default
  const role = userStore.userRole || USER_ROLES.CUSTOMER
  return ROLE_MENUS[role as keyof typeof ROLE_MENUS] || ROLE_MENUS[USER_ROLES.CUSTOMER]
})

const activeMenu = computed<string>(() => {
  const path: string = route.path
  const isMatch = currentMenus.value.some(m => path.startsWith(m.path))
  if (isMatch) {
    const matchedMenu = currentMenus.value.find(m => path.startsWith(m.path))
    return matchedMenu ? matchedMenu.path : (currentMenus.value[0] ? currentMenus.value[0].path : '/')
  }
  return currentMenus.value[0] ? currentMenus.value[0].path : '/'
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
  router.push(path)
}

const handleMobileMenuSelect = (path: string): void => {
  isMobileMenuOpen.value = false
  router.push(path)
}

const goHome = (): void => {
  isMobileMenuOpen.value = false
  const fallback = (currentMenus.value && currentMenus.value[0] && currentMenus.value[0].path) ? currentMenus.value[0].path : '/'
  router.push(fallback)
}

const handleDropdownCommand = async (command: DropdownCommand): Promise<void> => {
  if (command === 'profile') {
    await router.push('/customer/profile')
    return
  }

  await userStore.logout()
  await router.push('/auth/login')
}
</script>

<style scoped lang="scss">
.app-navbar {
  position: sticky;
  top: var(--space-4);
  z-index: 100;
  height: auto;
  padding: 0 var(--space-4);
  margin-bottom: var(--space-4);
  background: transparent;
}

.navbar-inner {
  max-width: var(--content-max-width);
  height: var(--navbar-height);
  margin: 0 auto;
  padding: 0 var(--space-4);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  background: var(--color-bg-overlay);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: 0 4px 16px var(--color-shadow);
  backdrop-filter: blur(8px);
  transition: all var(--transition-base);
}

.navbar-inner:hover {
  box-shadow: 0 6px 24px var(--color-shadow);
}

.mobile-toggler {
  display: none;
  background: transparent;
  border: none;
  color: var(--color-text-primary);
  cursor: pointer;
  padding: var(--space-2);
  margin-left: calc(-1 * var(--space-2));
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}

.mobile-toggler:hover {
  background: var(--color-bg-muted);
}

.navbar-left {
  min-width: 220px;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  color: var(--color-text-primary);
  cursor: pointer;
}

.logo-mark {
  width: 60px;
  height: 60px;
  object-fit: contain;
  transition: transform var(--transition-base);
}

.navbar-left:hover .logo-mark {
  transform: scale(1.08) rotate(-5deg);
}

.system-name {
  font-size: 19px;
  font-weight: 800;
  white-space: nowrap;
  letter-spacing: -0.5px;
}

.navbar-menu {
  flex: 1;
  min-width: 0;
  border-bottom: none !important;
  justify-content: center;
  background: transparent;
}

/* Style menu items explicitly to avoid linter warnings about unknown element-plus vars */
:deep(.navbar-menu .el-menu-item) {
  height: calc(var(--navbar-height) - var(--space-4));
  line-height: calc(var(--navbar-height) - var(--space-4));
  margin: var(--space-2) var(--space-1);
  border-radius: var(--radius-lg);
  color: var(--color-text-secondary);
  font-weight: 500;
  border-bottom: none !important;
  transition: all var(--transition-fast);
}

:deep(.navbar-menu .el-menu-item:hover) {
  background: var(--color-bg-muted);
  color: var(--color-text-primary);
  transform: translateY(-1px);
}

:deep(.navbar-menu .el-menu-item.is-active) {
  background: var(--color-primary-soft) !important;
  color: var(--color-text-inverse) !important;
  font-weight: 600;
  box-shadow: 0 2px 8px var(--color-shadow);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) var(--space-3) var(--space-1) var(--space-1);
  border-radius: var(--radius-xl);
  cursor: pointer;
  border: 1px solid transparent;
  transition: all var(--transition-fast);
}

.user-trigger:hover {
  background: var(--color-bg-muted);
  border-color: var(--color-border);
}

.user-name {
  font-weight: 500;
  color: var(--color-text-primary);
}

.drawer-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  margin-bottom: var(--space-2);
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
}

.system-name-dark {
  font-size: 19px;
  font-weight: 800;
  color: var(--color-text-secondary);
}

.mobile-menu {
  border-right: none;
}

/* Responsive */
@media (max-width: 900px) {
  .desktop-only {
    display: none !important;
  }

  .mobile-toggler {
    display: inline-block;
  }

  .navbar-left {
    flex: 1;
    min-width: auto;
  }

  .navbar-right {
    min-width: auto;
  }
}
</style>
