<script setup lang="ts"></script>

<template>
  <main class="app-shell">
    <section class="card hero-card">
      <p class="eyebrow">ExpertLink Consult System</p>
      <h1>Frontend foundation is ready</h1>
      <p>
        The project now includes a structured `src/` layout and light/dark design tokens.
      </p>
    </section>
  </main>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: var(--space-8);
}

.hero-card {
  width: min(100%, 720px);
  padding: var(--space-8);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
}

.eyebrow {
  margin: 0 0 var(--space-3);
  color: var(--color-primary);
  font-size: 0.875rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0 0 var(--space-4);
  font-size: clamp(2rem, 4vw, 3rem);
}

p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}
</style>
