<template>
  <div class="view-all-wrapper">
    <router-link to="/customer/bookings" class="view-all-link">
      View All
    </router-link>
  </div>
</template>

<script setup lang="ts">

</script>

<style scoped lang="scss">
.view-all-wrapper {
  text-align: center;
  margin-top: var(--space-4);
}

.view-all-link {
  font-size: 16px;
  color: var(--color-primary);
  text-decoration: underline;
  cursor: pointer;

  &:hover {
    color: var(--color-primary-dark);
  }
}
</style>

