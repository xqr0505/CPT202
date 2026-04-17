<template>
  <section class="filter-bar card">
    <div class="filter-grid">
      <label class="field">
        <span class="field-label">Expert name</span>
        <el-input
          :model-value="modelValue.keyword"
          placeholder="Search by specialist name"
          clearable
          @update:model-value="updateField('keyword', $event)"
          @keyup.enter="emit('search')"
        />
      </label>

      <label class="field">
        <span class="field-label">Category</span>
        <el-select
          :model-value="modelValue.categoryId"
          placeholder="All categories"
          clearable
          filterable
          @update:model-value="updateField('categoryId', normalizeCategoryId($event))"
        >
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
      </label>

      <label class="field">
        <span class="field-label">Available on</span>
        <el-date-picker
          :model-value="modelValue.date || ''"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="Choose a date"
          @update:model-value="updateField('date', $event ?? '')"
        />
      </label>

      <label class="field">
        <span class="field-label">Sort by</span>
        <el-select
          :model-value="modelValue.sortBy"
          @update:model-value="updateField('sortBy', $event)"
        >
          <el-option
            v-for="option in sortOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </label>
    </div>

    <div class="actions">
      <CustomButton type="primary" @click="emit('search')">Search</CustomButton>
      <CustomButton @click="handleReset">Reset</CustomButton>
    </div>
  </section>
</template>

<script setup lang="ts">
import CustomButton from '@/components/common/CustomButton.vue'
import {
  SPECIALIST_SORT_OPTIONS,
  type SpecialistCategory,
  type SpecialistSearchForm,
  type SpecialistSortOption,
} from '@/types/specialist'

const props = defineProps<{
  modelValue: SpecialistSearchForm
  categories: SpecialistCategory[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: SpecialistSearchForm]
  search: []
  reset: []
}>()

const sortOptions: Array<{ label: string; value: SpecialistSortOption }> = [
  { label: 'Recommended', value: SPECIALIST_SORT_OPTIONS.RECOMMENDED },
  { label: 'Level: High to Low', value: SPECIALIST_SORT_OPTIONS.LEVEL_DESC },
  { label: 'Fee: Low to High', value: SPECIALIST_SORT_OPTIONS.FEE_ASC },
  { label: 'Fee: High to Low', value: SPECIALIST_SORT_OPTIONS.FEE_DESC },
]

const normalizeCategoryId = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') {
    return null
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const updateField = <K extends keyof SpecialistSearchForm>(
  field: K,
  value: SpecialistSearchForm[K],
) => {
  emit('update:modelValue', {
    ...props.modelValue,
    [field]: value,
  })
}

const handleReset = () => {
  emit('reset')
}
</script>

<style scoped lang="scss">
.filter-bar {
  padding: var(--space-6);
  display: grid;
  gap: var(--space-5);
}

.filter-grid {
  display: grid;
  gap: var(--space-4);
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.field {
  display: grid;
  gap: var(--space-2);
}

.field-label {
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
}

@media (max-width: 1080px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .actions {
    justify-content: stretch;
  }

  .actions :deep(.el-button) {
    flex: 1;
  }
}
</style>
