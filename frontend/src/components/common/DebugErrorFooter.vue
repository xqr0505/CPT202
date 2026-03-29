<script setup lang="ts">
const triggerManual = () => {
  if ((window as any).__triggerGlobalError) {
    (window as any).__triggerGlobalError('Manual trigger from debug footer')
  }
}
</script>

<template>
  <footer class="error-debug-footer" aria-hidden="false">
    <div class="inner">
      <span>View error pages:</span>
      <router-link to="/error/403" class="debug-link">403</router-link>
      <router-link to="/error/404" class="debug-link">404</router-link>
      <router-link to="/error/500" class="debug-link">500</router-link>
      <button class="manual-trigger" @click="triggerManual">Trigger Global Error</button>
    </div>
  </footer>
</template>

<style scoped lang="scss">
.error-debug-footer {
  position: fixed;
  left: var(--space-4);
  bottom: var(--space-4);
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  font-size: 14px;
  z-index: 1200;
  border: 1px solid var(--color-border);
  box-shadow: 0 2px 8px var(--color-shadow);
}

.inner {
  display: flex;
  gap: var(--space-2);
  align-items: center;
}

.debug-link {
  color: var(--color-primary);
  text-decoration: underline;

  &:hover {
    color: var(--color-primary-hover);
  }
}

.manual-trigger {
  background: var(--color-bg-muted);
  color: var(--color-primary);
  border: 1px solid var(--color-border);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 12px;
  transition: all var(--transition-fast);

  &:hover {
    background: var(--color-primary-soft);
    border-color: var(--color-primary);
  }
}
</style>

