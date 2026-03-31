<template>
  <div class="error-page">
    <div class="error-container">
      <h1 class="error-title">403</h1>
      <h2 class="error-subtitle">Access Denied</h2>
      <p class="error-desc">You do not have permission to view this page or perform this action.</p>
      <div class="error-actions">
        <CustomButton type="primary" @click="router.push('/')">Return Home</CustomButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import CustomButton from '@/components/common/CustomButton.vue'

const router = useRouter()
</script>

<style scoped lang="scss">
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: var(--color-bg-page);
  padding: var(--space-6);
}

.error-container {
  text-align: center;
  background-color: var(--color-bg-surface);
  padding: var(--space-10);
  border-radius: var(--radius-xl);
  box-shadow: 0 4px 20px var(--color-shadow);
  max-width: 500px;
  width: 100%;
}

.error-title {
  font-size: 72px;
  font-weight: 800;
  color: var(--color-danger);
  margin: 0;
  line-height: 1.2;
}

.error-subtitle {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: var(--space-4) 0 var(--space-2);
}

.error-desc {
  font-size: 16px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-bottom: var(--space-8);
}

.error-actions {
  display: flex;
  justify-content: center;
  gap: var(--space-4);
}
</style>
