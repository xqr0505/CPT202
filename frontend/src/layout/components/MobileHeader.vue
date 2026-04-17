<template>
  <header class="mobile-header">
    <div class="header-content">
      <img src="@/assets/images/ELicon.png" class="logo-icon" alt="Logo" />
      <span class="platform-name">ExpertLink</span>
    </div>
  </header>
</template>

<script setup lang="ts">
// Mobile header component for displaying platform branding
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
