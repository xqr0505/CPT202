<template>
  <el-empty
    class="empty-placeholder"
    :description="description"
    :image="image"
    :image-size="imageSize"
  >
    <template #image v-if="$slots.image">
      <slot name="image" />
    </template>
    <template #description v-if="$slots.description">
      <slot name="description" />
    </template>
    <slot />
  </el-empty>
</template>

<script setup lang="ts">
import { useSlots } from 'vue'

interface Props {
  description?: string
  image?: string
  imageSize?: number
}

withDefaults(defineProps<Props>(), {
  description: 'No data',
  imageSize: 160
})

const $slots = useSlots()
</script>

<style scoped lang="scss">
@use '@/styles/variables';

.empty-placeholder {
  padding: var(--space-12);
  margin: var(--space-8) 0;
  background-color: var(--color-bg-surface);
  border-radius: var(--radius-lg);
  border: 1px dashed var(--color-border);
  box-shadow: 0 4px 12px var(--color-shadow);
  transition: all var(--transition-base);

  &:hover {
    box-shadow: 0 8px 24px var(--color-shadow);
    border-color: var(--color-border-strong);
  }

  :deep(.el-empty__description) {
    p {
      color: var(--color-text-secondary);
      font-size: 14px;
      font-weight: 500;
    }
  }

  :deep(.el-empty__image) {
    opacity: 0.9;
    filter: drop-shadow(0 8px 16px var(--color-shadow));
    transition: transform var(--transition-base);

    &:hover {
      transform: translateY(-4px);
    }
  }
}
</style>

