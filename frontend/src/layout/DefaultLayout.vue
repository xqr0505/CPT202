<template>
  <div class="default-layout">
    <MobileHeader />
    <AppNavbar />

    <main class="layout-main">
      <div class="content-shell">
        <router-view />
      </div>
    </main>
    <AiChatDrawer />

    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import MobileHeader from './components/MobileHeader.vue'
import AppNavbar from './components/AppNavbar.vue'
import AppFooter from './components/AppFooter.vue'
import AiChatDrawer from '@/components/ai/AiChatDrawer.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(async () => {
  if (!userStore.userInfo) {
    try {
      await userStore.fetchAndSetUserProfile()
    } catch (e) {
      console.error(e)
    }
  }
})
</script>

<style scoped lang="scss">
.default-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
}

.layout-main {
  flex: 1;
  padding: var(--space-6) var(--space-4);
}

@media (max-width: 900px) {
  .layout-main {
    padding-top: calc(56px + var(--space-6));
  }
}

.content-shell {
  max-width: var(--content-max-width);
  margin: 0 auto;
}
</style>
