<template>
  <section class="page-card auth-page">
    <p class="page-tag">Auth</p>
    <h1>Login</h1>
    <el-alert
      v-if="deactivatedMessage"
      class="login-alert"
      type="error"
      :closable="false"
      show-icon
      :title="deactivatedMessage"
    />
    <p class="page-text">This is the login page placeholder.</p>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

// Add component name to satisfy lint rule
defineOptions({ name: 'AuthLogin' })

const route = useRoute()

const deactivatedMessage = computed(() => {
  return route.query.reason === 'deactivated'
    ? 'This account has been deactivated. You cannot log in or access customer functions.'
    : ''
})
</script>

<style scoped lang="scss">
.login-alert {
  margin: var(--space-4) 0;
}
</style>
