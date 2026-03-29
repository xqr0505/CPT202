<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <div class="footer-content">
        <img src="@/assets/images/ELicon.png" class="footer-logo" alt="Logo" />
        <span class="footer-brand">ExpertLink Consult System</span>
      </div>
      <div class="footer-copyright">
        © {{ currentYear }} All rights reserved.
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
const currentYear: number = new Date().getFullYear()
</script>

<style scoped lang="scss">
.app-footer {
  padding: var(--space-6) 0;
  background: transparent;
}

.footer-inner {
  max-width: var(--content-max-width);
  margin: 0 auto;
  padding: 0 var(--space-4);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}

.footer-content {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.footer-logo {
  width: 24px;
  height: 24px;
  object-fit: contain;
  opacity: 0.8;
  transition: opacity var(--transition-fast);
}

.footer-content:hover .footer-logo {
  opacity: 1;
}

.footer-brand {
  font-weight: 500;
  font-size: 15px;
  color: var(--color-text-secondary);
}

.footer-copyright {
  font-size: 13px;
  color: var(--color-text-tertiary);
}
</style>
