<template>
  <div class="ai-composer">
    <el-input
      :model-value="modelValue"
      :disabled="loading"
      type="textarea"
      :rows="AI_CHAT_TEXTAREA_ROWS"
      :maxlength="AI_CHAT_TEXTAREA_MAX_LENGTH"
      :placeholder="AI_CHAT_INPUT_PLACEHOLDER"
      resize="none"
      show-word-limit
      @update:model-value="handleInput"
      @focus="emit('focus')"
      @keydown.enter.exact.prevent="emit('submit')"
    />

    <div class="ai-composer__actions">
      <CustomButton type="primary" :loading="loading" @click="emit('submit')">
        {{ AI_CHAT_SEND_BUTTON_TEXT }}
      </CustomButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import CustomButton from '@/components/common/CustomButton.vue'
import {
  AI_CHAT_INPUT_PLACEHOLDER,
  AI_CHAT_SEND_BUTTON_TEXT,
  AI_CHAT_TEXTAREA_MAX_LENGTH,
  AI_CHAT_TEXTAREA_ROWS
} from '@/constants/ai'

interface Props {
  modelValue: string
  loading?: boolean
}

withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  focus: []
  submit: []
}>()

const handleInput = (value: string): void => {
  emit('update:modelValue', value)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.ai-composer {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--ai-chat-composer-padding);
  background: var(--ai-chat-composer-background);
  border: 1px solid var(--ai-chat-toolbar-border-color);
  border-radius: var(--radius-xl);
}

.ai-composer__actions {
  display: flex;
  justify-content: flex-end;
}

:deep(.el-textarea__inner) {
  min-height: 110px;
  padding: var(--space-3);
  background: transparent;
  border-color: var(--color-border);
  color: var(--color-text-primary);
  box-shadow: none;
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--color-primary);
}

:deep(.el-input__count) {
  color: var(--color-text-secondary);
  background: transparent;
}
</style>
