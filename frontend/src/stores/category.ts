import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCategoryList } from '@/api/adminCategory'

export interface CategoryItem {
  id: number
  categoryName: string
  createTime?: string
}

export const useCategoryStore = defineStore('category', () => {
  const categories = ref<CategoryItem[]>([])
  const loading = ref(false)

  const categoryOptions = computed(() =>
    categories.value.map(item => ({
      label: item.categoryName,
      value: item.id
    }))
  )

  const fetchCategories = async () => {
    loading.value = true
    try {
      const data = await getCategoryList()
      categories.value = Array.isArray(data)
        ? [...data].sort((a, b) => a.categoryName.localeCompare(b.categoryName, 'en', { sensitivity: 'base' }))
        : []
      return categories.value
    } finally {
      loading.value = false
    }
  }

  return {
    categories,
    loading,
    categoryOptions,
    fetchCategories
  }
})
