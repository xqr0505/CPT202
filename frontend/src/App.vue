<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import DevFooter from '@/components/common/DevFooter.vue'
import { logout } from '@/api/request'

const router = useRouter()
const isDev = Boolean(import.meta.env.DEV)
const globalError = ref<{ message: string | null; stack?: string | null } | null>(null)

function showError(message: string, stack?: string) {
  globalError.value = { message, stack }
}

function handleStorageEvent(event: StorageEvent) {
  if (event.key === 'logout-event' && event.newValue) {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    if (token) {
      logout()  
    }
  }
}

onMounted(() => {
  const onError = (event: ErrorEvent) => {
    try { event.preventDefault() } catch {}
    showError(event.message, (event.error && event.error.stack) || undefined)
  }
  const onRejection = (event: PromiseRejectionEvent) => {
    try { event.preventDefault() } catch {}
    const reason = (event.reason && (event.reason.message || String(event.reason))) || 'Unhandled promise rejection'
    showError(reason)
  }
  window.addEventListener('error', onError)
  window.addEventListener('unhandledrejection', onRejection)
  window.addEventListener('storage', handleStorageEvent)

  ;(window as any).__triggerGlobalError = (msg = 'Manual test error') => showError(msg)

  onBeforeUnmount(() => {
    window.removeEventListener('error', onError)
    window.removeEventListener('unhandledrejection', onRejection)
    window.removeEventListener('storage', handleStorageEvent)
  })
})

function goToErrorPage(code: string) {
  router.push(`/error/${code}`)
}

function dismiss() {
  globalError.value = null
}

function reloadApp() {
  window.location.reload()
}
</script>

<template>
  <router-view />
  <DevFooter v-if="isDev" />
</template>
