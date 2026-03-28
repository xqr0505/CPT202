<template>
  <header class="app-navbar">
    <div class="navbar-inner">
      <button class="mobile-toggler" @click="isMobileMenuOpen = true" aria-label="Menu">
        <svg viewBox="0 0 24 24" width="24" height="24" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>
      </button>

      <div class="navbar-left" @click="goHome">
        <div class="logo-mark" aria-hidden="true">EL</div>
        <span class="system-name">ExpertLink</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        mode="horizontal"
        class="navbar-menu desktop-only"
        @select="handleMenuSelect"
        :ellipsis="false"
      >
        <el-menu-item index="/customer/specialists">Find Specialists</el-menu-item>
        <el-menu-item index="/customer/dashboard">Dashboard</el-menu-item>
        <el-menu-item index="/customer/bookings">My Bookings</el-menu-item>
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
        <div class="logo-mark" aria-hidden="true">EL</div>
        <span class="system-name-dark">ExpertLink</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="mobile-menu"
        @select="handleMobileMenuSelect"
      >
        <el-menu-item index="/customer/specialists">Find Specialists</el-menu-item>
        <el-menu-item index="/customer/dashboard">Dashboard</el-menu-item>
        <el-menu-item index="/customer/bookings">My Bookings</el-menu-item>
      </el-menu>
    </el-drawer>
  </header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

type NavbarMenuPath = '/customer/specialists' | '/customer/dashboard' | '/customer/bookings'
type DropdownCommand = 'profile' | 'logout'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isMobileMenuOpen = ref(false)

const activeMenu = computed<NavbarMenuPath>(() => {
  const path: string = route.path
  if (path.startsWith('/customer/dashboard')) return '/customer/dashboard'
  if (path.startsWith('/customer/bookings')) return '/customer/bookings'
  return '/customer/specialists'
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
  router.push('/customer/specialists')
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
  top: 0;
  z-index: 100;
  height: var(--navbar-height);
  background: var(--color-primary);
  border-bottom: none;
}

.navbar-inner {
  max-width: var(--content-max-width);
  height: 100%;
  margin: 0 auto;
  padding: 0 var(--space-4);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.mobile-toggler {
  display: none;
  background: transparent;
  border: none;
  color: var(--color-text-inverse);
  cursor: pointer;
  padding: 8px;
  margin-left: -8px;
}

.navbar-left {
  min-width: 220px;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  color: var(--color-text-inverse);
  cursor: pointer;
}

.logo-mark {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-surface);
  color: var(--color-primary);
  font-weight: 700;
}

.system-name {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.navbar-menu {
  flex: 1;
  min-width: 0;
  border-bottom: none !important;
  justify-content: center;
  background: transparent;

  /* Override Element Plus menu colors to match primary theme */
  --el-menu-bg-color: transparent;
  --el-menu-text-color: rgba(255, 255, 255, 0.7);
  --el-menu-hover-text-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.1);
  --el-menu-active-color: #ffffff;
}

/* Ensure submenus inside el-menu override the menu bottom border highlight correctly */
:deep(.el-menu-item.is-active) {
  border-bottom-color: #ffffff !important;
}

.navbar-right {
  min-width: 220px;
  display: flex;
  justify-content: flex-end;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-inverse);
  cursor: pointer;
}

.user-name {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  margin-bottom: var(--space-2);
  cursor: pointer;
}

.system-name-dark {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-primary);
}

.mobile-menu {
  border-right: none;
}

/* Responsive adjustments */
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
