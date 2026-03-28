<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <span>ExpertLink Consult System</span>
      <span>© {{ currentYear }}</span>
    </div>
  </footer>
</template>

<script setup lang="ts">
const currentYear: number = new Date().getFullYear()
</script>

<style scoped lang="scss">
.app-footer {
  height: var(--footer-height);
  border-top: 1px solid var(--color-border);
  background: var(--color-primary);
  color: var(--color-text-inverse);
}

.footer-inner {
  max-width: var(--content-max-width);
  height: 100%;
  margin: 0 auto;
  padding: 0 var(--space-4);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  color: inherit;
  font-size: 14px;
}
</style>
