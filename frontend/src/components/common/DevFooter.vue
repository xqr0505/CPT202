<script setup lang="ts">
import { ref, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, Rank, Expand, Fold, Sunny, Moon } from '@element-plus/icons-vue'
import { applyThemeMode, getCurrentThemeMode, THEME_MODE_EVENT_NAME } from '@/utils/theme'

const router = useRouter()

const isCollapsed = ref(false)
const isDark = ref(false)
const position = ref({ x: 0, y: 0 })
const isDragging = ref(false)
const dragStart = { x: 0, y: 0 }
const initialPosition = { x: 0, y: 0 }

const syncThemeState = () => {
  isDark.value = getCurrentThemeMode() === 'dark'
}

onMounted(() => {
  syncThemeState()
  window.addEventListener(THEME_MODE_EVENT_NAME, syncThemeState)
})

onBeforeUnmount(() => {
  window.removeEventListener(THEME_MODE_EVENT_NAME, syncThemeState)
})

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const onMouseDown = (e: MouseEvent) => {
  isDragging.value = true
  dragStart.x = e.clientX
  dragStart.y = e.clientY
  initialPosition.x = position.value.x
  initialPosition.y = position.value.y

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

const onMouseMove = (e: MouseEvent) => {
  if (!isDragging.value) return
  position.value.x = initialPosition.x + (e.clientX - dragStart.x)
  position.value.y = initialPosition.y + (e.clientY - dragStart.y)
}

const onMouseUp = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
}

const triggerManual = () => {
  if ((window as any).__triggerGlobalError) {
    (window as any).__triggerGlobalError('Manual trigger from debug footer')
  }
}

const handleCommand = (command: string) => {
  if (command === 'trigger') {
    triggerManual()
  } else if (command.startsWith('/')) {
    router.push(command)
  }
}

const toggleTheme = () => {
  isDark.value = !isDark.value
  applyThemeMode(isDark.value ? 'dark' : 'light')
}
</script>

<template>
  <footer
    class="dev-tools-footer"
    :class="{ collapsed: isCollapsed }"
    :style="{ transform: `translate(calc(-50% + ${position.x}px), ${position.y}px)` }"
    aria-hidden="false"
  >
    <div class="drag-handle" @mousedown="onMouseDown" title="Drag to move">
      <el-icon><Rank /></el-icon>
    </div>

    <div class="inner" v-show="!isCollapsed">
      <el-dropdown size="small" @command="handleCommand" trigger="click">
        <el-button type="danger" size="small" plain>
          Test Errors
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="/error/403">View 403 Forbidden</el-dropdown-item>
            <el-dropdown-item command="/error/404">View 404 Not Found</el-dropdown-item>
            <el-dropdown-item command="/error/500">View 500 Server Error</el-dropdown-item>
            <el-dropdown-item command="/error/global?code=Error&message=This+is+a+test+global+error">View Global Error</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <el-button type="primary" size="small" @click="router.push('/dev')">
        Components Demo
      </el-button>

      <el-button type="info" size="small" @click="router.push('/')">
        Home
      </el-button>

      <div class="theme-toggle" @click="toggleTheme" :title="isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode'">
        <el-icon>
          <Moon v-if="!isDark" />
          <Sunny v-else />
        </el-icon>
      </div>
    </div>

    <div class="toggle-btn" @click="toggleCollapse" :title="isCollapsed ? 'Expand' : 'Collapse'">
      <el-icon>
        <Expand v-if="isCollapsed" />
        <Fold v-else />
      </el-icon>
    </div>
  </footer>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss';

.dev-tools-footer {
  position: fixed;
  left: 50%;
  bottom: var(--space-4);
  background: var(--color-bg-overlay);
  backdrop-filter: blur(8px);
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-xl);
  z-index: 9999;
  border: 1px solid var(--color-border);
  box-shadow: 0 4px 16px var(--color-shadow);
  display: flex;
  align-items: center;
  gap: var(--space-3);
  transition: opacity var(--transition-base), padding 0.3s;

  &.collapsed {
    padding: var(--space-2);
  }

  &:hover {
    box-shadow: 0 8px 24px var(--color-shadow);
  }
}

.drag-handle {
  cursor: grab;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  color: var(--color-text-secondary);
  font-size: 16px;

  &:active {
    cursor: grabbing;
  }
}

.toggle-btn {
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  color: var(--color-text-secondary);
  font-size: 16px;
  transition: color 0.2s;

  &:hover {
    color: var(--color-primary);
  }
}

.theme-toggle {
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  color: var(--color-text-secondary);
  font-size: 16px;
  transition: color 0.2s;

  &:hover {
    color: var(--color-primary);
  }
}

.inner {
  display: flex;
  gap: var(--space-3);
  align-items: center;
}
</style>
