<template>
  <div class="image-uploader">
    <div class="preview-shell" :class="{ uploading: loading }">
      <img v-if="previewUrl" :src="previewUrl" alt="Uploaded preview" class="preview-image" />
      <div v-else class="empty-state">
        <el-icon class="empty-icon"><Plus /></el-icon>
        <span>Specialist portrait preview</span>
        <small>Upload a clean JPG or PNG image to present this profile.</small>
      </div>
      <div v-if="loading" class="loading-mask">
        <el-icon class="loading-icon is-loading"><Loading /></el-icon>
        <span>Uploading...</span>
      </div>
    </div>

    <el-upload
      class="uploader-control"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :http-request="handleUpload"
      accept=".jpg,.jpeg,.png,image/jpeg,image/png"
    >
      <template #trigger>
        <el-button size="small">Choose Image</el-button>
      </template>
    </el-upload>

    <div class="uploader-actions">
      <el-button v-if="previewUrl" size="small" text type="danger" @click="clearImage">Remove</el-button>
    </div>

    <p class="uploader-hint">JPG/PNG only, up to 2MB.</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, type UploadProps, type UploadRequestOptions } from 'element-plus'
import { Loading, Plus } from '@element-plus/icons-vue'
import { uploadImage } from '@/api/upload'

const props = defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const loading = ref(false)

const previewUrl = computed(() => props.modelValue?.trim() || '')

const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  const isSupportedType = ['image/jpeg', 'image/png'].includes(rawFile.type)
  if (!isSupportedType) {
    ElMessage.error('Only JPG and PNG images are supported')
    return false
  }

  const isValidSize = rawFile.size <= 2 * 1024 * 1024
  if (!isValidSize) {
    ElMessage.error('Image size must not exceed 2MB')
    return false
  }

  return true
}

async function handleUpload(options: UploadRequestOptions) {
  loading.value = true
  try {
    const response = await uploadImage(options.file as File)
    emit('update:modelValue', response.url)
    ElMessage.success('Image uploaded successfully')
    options.onSuccess?.(response)
  } catch (error) {
    console.error('Failed to upload image:', error)
    options.onError?.(error as never)
  } finally {
    loading.value = false
  }
}

function clearImage() {
  emit('update:modelValue', '')
}
</script>

<style scoped>
.image-uploader {
  display: grid;
  gap: 12px;
}

.preview-shell {
  position: relative;
  display: grid;
  place-items: center;
  width: 100%;
  min-height: 200px;
  overflow: hidden;
  border: 1px dashed var(--color-border);
  border-radius: 18px;
  background: linear-gradient(180deg, var(--color-bg-surface) 0%, var(--color-bg-muted) 100%);
  transition: border-color 0.18s ease, transform 0.18s ease;
}

.preview-shell.uploading {
  pointer-events: none;
}

.preview-image {
  width: 100%;
  height: 240px;
  object-fit: cover;
  display: block;
}

.empty-state {
  display: grid;
  gap: 10px;
  justify-items: center;
  color: var(--color-text-secondary);
  font-weight: 600;
  text-align: center;
  padding: 24px;
}

.empty-icon {
  font-size: 28px;
  color: var(--color-primary);
}

.empty-state small {
  max-width: 220px;
  font-size: 12px;
  line-height: 1.6;
  font-weight: 500;
  color: var(--color-text-tertiary);
}

.loading-mask {
  position: absolute;
  inset: 0;
  display: grid;
  gap: 10px;
  align-content: center;
  justify-items: center;
  color: #fff;
  background: rgba(var(--color-shadow-rgb), 0.48);
  backdrop-filter: blur(2px);
}

.loading-icon {
  font-size: 22px;
}

.uploader-actions {
  display: flex;
  gap: 10px;
}

.uploader-hint {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
</style>
