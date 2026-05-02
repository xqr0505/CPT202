
<template>
  <header class="mobile-header">
    <div class="header-content">
      <img :src="logoSrc" class="logo-icon" alt="Logo" />
      <span class="platform-name">ExpertLink</span>
      <div class="header-spacer"></div>
      <el-dropdown trigger="click" placement="bottom-end">
        <span class="user-trigger mobile-trigger" :class="{ 'is-active': route.path.includes('/profile') }">
          <el-avatar :src="avatarSrc" :size="30">{{ userInitial }}</el-avatar>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-if="userStore.isLoggedIn" @click="navigateToProfile">Profile</el-dropdown-item>
            <el-dropdown-item v-if="userStore.isLoggedIn" @click="handleLogout">Logout</el-dropdown-item>
            <el-dropdown-item v-else @click="handleLogin">Login</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import lightLogo from '@/assets/images/ELicon.png'
import darkLogo from '@/assets/images/ELiconDark.png'
import { getCurrentThemeMode, THEME_MODE_EVENT_NAME, type ThemeMode } from '@/utils/theme'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isDarkTheme = ref(getCurrentThemeMode() === 'dark')
const logoSrc = computed(() => (isDarkTheme.value ? darkLogo : lightLogo))

const displayName = computed(() => {
  const nickname = userStore.userInfo?.nickname?.trim()
  if (nickname) return nickname
  const username = userStore.userInfo?.username?.trim()
  return username || 'Guest'
})
const avatarSrc = computed(() => userStore.userInfo?.avatar?.trim() || '')
const userInitial = computed(() => displayName.value.charAt(0).toUpperCase())

const navigateToProfile = () => {
  router.push({ name: 'CustomerProfile' }).catch(() => null)
}

const handleLogin = (): void => {
  router.push({ path: '/auth', query: { redirect: route.fullPath } }).catch(() => null)
}

const handleLogout = async (): Promise<void> => {
  try {
    await ElMessageBox.confirm(
      'After logging out, where do you want to go?',
      'Logout',
      {
        confirmButtonText: 'Go to Login',
        cancelButtonText: 'Browse as Guest',
        closeOnClickModal: false,
        closeOnPressEscape: false,
        showCancelButton: true,
        distinguishCancelAndClose: true,
        type: 'warning'
      }
    )

    await userStore.logout()
    await router.push('/auth')
  } catch (e: any) {
    if (e === 'cancel' || e?.toString?.() === 'cancel') {
      await userStore.logout()
      await router.push('/customer/search')
      return
    }

    // closed dialog -> do nothing
  }
}

function handleThemeChange(event: CustomEvent<ThemeMode>): void {
  isDarkTheme.value = event.detail === 'dark'
}

onMounted(() => {
  window.addEventListener(THEME_MODE_EVENT_NAME, handleThemeChange as EventListener)
})

onBeforeUnmount(() => {
  window.removeEventListener(THEME_MODE_EVENT_NAME, handleThemeChange as EventListener)
})
</script>

<style scoped lang="scss">
.mobile-header {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 56px;
  background: var(--color-bg-surface);
  border-bottom: 1px solid var(--color-border);
  box-shadow: 0 2px 8px var(--color-shadow);
  z-index: 101;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  height: 100%;
  padding: 0 var(--space-4);
  gap: var(--space-3);
}

.header-spacer {
  flex: 1 1 auto;
}

.mobile-trigger {
  margin-left: auto;
  padding: var(--space-1);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.logo-icon {
  width: 36px;
  height: 36px;
  object-fit: contain;
  flex-shrink: 0;
}

.platform-name {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: var(--color-text-primary);
  white-space: nowrap;
}

/* Mobile: Show header on screens <= 900px */
@media (max-width: 900px) {
  .mobile-header {
    display: block;
  }
}
</style>
