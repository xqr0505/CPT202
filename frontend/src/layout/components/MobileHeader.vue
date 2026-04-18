
<template>
  <header class="mobile-header">
    <div class="header-content">
      <img src="@/assets/images/ELicon.png" class="logo-icon" alt="Logo" />
      <span class="platform-name">ExpertLink</span>
      <div class="header-spacer"></div>
      <el-dropdown trigger="click" placement="bottom-end">
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
  </header>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { logout as clearAndRedirect } from '@/api/request'

const route = useRoute()
const userStore = useUserStore()

const displayName = computed(() => {
  const nickname = userStore.userInfo?.nickname?.trim()
  if (nickname) return nickname
  const username = userStore.userInfo?.username?.trim()
  return username || 'Guest'
})
const avatarSrc = computed(() => userStore.userInfo?.avatar?.trim() || '')
const userInitial = computed(() => displayName.value.charAt(0).toUpperCase())

const navigateToProfile = () => {
  window.location.href = '/customer/profile'
}
const handleLogout = () => {
  clearAndRedirect()
}
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
