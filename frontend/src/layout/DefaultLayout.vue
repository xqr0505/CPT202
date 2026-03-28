<template>
  <div class="default-layout">
    <AppNavbar />

    <main class="layout-main">
      <div class="content-shell">
        <router-view />
      </div>
    </main>

    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import AppNavbar from './components/AppNavbar.vue'
import AppFooter from './components/AppFooter.vue'
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

.content-shell {
  max-width: var(--content-max-width);
  margin: 0 auto;
}
</style>
