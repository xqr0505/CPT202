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
  font-weight: 500;
  transition: all var(--transition-base);
  height: auto;
  padding: 8px 20px;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  outline: none;
  font-size: 14px;
  letter-spacing: 0.01em;

  &:hover {
    box-shadow: 0 4px 12px var(--color-shadow);
  }

  &:active {
    transform: translateY(0);
    opacity: 0.85;
  }

  &--default {
    background-color: var(--color-btn-bg-default);
    border-color: var(--color-btn-border-default);
    color: var(--color-btn-text-default);

    &:hover {
      border-color: var(--color-btn-border-default-hover);
      color: var(--color-btn-text-default-hover);
      background-color: var(--color-btn-bg-default-hover);
      box-shadow: 0 4px 12px var(--color-btn-shadow-default, var(--color-shadow));
    }
  }

  &--primary {
    background-color: var(--color-btn-bg-primary);
    border-color: var(--color-btn-border-primary);
    color: var(--color-btn-text-primary);

    &:hover {
      background-color: var(--color-btn-bg-primary-hover);
      color: var(--color-btn-text-primary-hover);
      border-color: var(--color-btn-border-primary-hover);
      box-shadow: 0 4px 15px var(--color-btn-shadow-primary);
    }
  }

  &--success {
    background-color: var(--color-btn-bg-success);
    border-color: var(--color-btn-border-success);
    color: var(--color-btn-text-success);

    &:hover {
      background-color: var(--color-btn-bg-success-hover);
      color: var(--color-btn-text-success-hover);
      box-shadow: 0 4px 12px var(--color-btn-shadow-success);
    }
  }

  &--warning {
    background-color: var(--color-btn-bg-warning);
    border-color: var(--color-btn-border-warning);
    color: var(--color-btn-text-warning);

    &:hover {
      background-color: var(--color-btn-bg-warning-hover);
      color: var(--color-btn-text-warning-hover);
      box-shadow: 0 4px 12px var(--color-btn-shadow-warning);
    }
  }

  &--danger {
    background-color: var(--color-btn-bg-danger);
    border-color: var(--color-btn-border-danger);
    color: var(--color-btn-text-danger);

    &:hover {
      background-color: var(--color-btn-bg-danger-hover);
      color: var(--color-btn-text-danger-hover);
      box-shadow: 0 4px 12px var(--color-btn-shadow-danger);
    }
  }

  &--info {
    background-color: var(--color-btn-bg-info);
    border-color: var(--color-btn-border-info);
    color: var(--color-btn-text-info);

    &:hover {
      background-color: var(--color-btn-bg-info-hover);
      color: var(--color-btn-text-info-hover);
      box-shadow: 0 4px 12px var(--color-btn-shadow-info);
    }
  }
}

:deep(.el-button) {
  border: inherit;
  background: inherit;
  color: inherit;
  padding: inherit;
  height: inherit;
  font-family: inherit;

  & [class*='el-icon'] + span {
    margin-left: 8px;
  }
}
</style>
