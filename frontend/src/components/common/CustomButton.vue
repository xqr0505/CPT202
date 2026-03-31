<template>
  <el-button
    v-bind="$attrs"
    :class="['custom-button', `custom-button--${type}`]"
    :type="type === 'primary' ? 'primary' : undefined"
  >
    <slot />
  </el-button>
</template>

<script setup lang="ts">
interface Props {
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'default'
}

withDefaults(defineProps<Props>(), {
  type: 'default'
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss';

.custom-button {
  border-radius: var(--radius-md);
  font-weight: 600;
  transition: all var(--transition-base);
  height: auto;
  padding: 10px 20px;
  border-width: 1px;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px var(--color-shadow);
  }

  &:active {
    transform: translateY(0);
  }

  &--primary {
    background-color: var(--color-primary);
    border-color: var(--color-primary);
    color: var(--color-text-inverse);

    &:hover {
      background-color: var(--color-primary-hover);
      border-color: var(--color-primary-hover);
    }
  }

  &--default {
    background-color: var(--color-bg-surface);
    border-color: var(--color-border);
    color: var(--color-text-primary);

    &:hover {
      border-color: var(--color-primary);
      color: var(--color-primary);
    }
  }

  &--success {
    background-color: var(--color-success);
    border-color: var(--color-success);
    color: white;
  }

  &--warning {
    background-color: var(--color-warning);
    border-color: var(--color-warning);
    color: white;
  }

  &--danger {
    background-color: var(--color-danger);
    border-color: var(--color-danger);
    color: white;
  }

  &--info {
    background-color: var(--color-info);
    border-color: var(--color-info);
    color: white;
  }
}

/* Ensure Element Plus styles are overridden if needed */
:deep(.el-button--primary) {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-hover);
  --el-button-hover-border-color: var(--color-primary-hover);
}
</style>

