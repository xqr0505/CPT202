<template>
  <div class="admin-category-page">
    <h2>Category Management</h2>

    <el-button
      type="primary"
      class="create-button"
      @click="openCreateDialog"
    >
      New Category
    </el-button>

    <div class="pagination-wrapper">
      <div class="pagination-summary">
        Total {{ totalItems }} categories, Page {{ totalPages === 0 ? 0 : pagination.pageNo }} / {{ totalPages }}
      </div>
      <div class="pagination-actions">
        <el-select v-model="pagination.pageSize" class="page-size-select">
          <el-option :value="10" label="10 / page" />
          <el-option :value="20" label="20 / page" />
          <el-option :value="50" label="50 / page" />
          <el-option :value="100" label="100 / page" />
        </el-select>
        <el-button :disabled="pagination.pageNo <= 1" @click="pagination.pageNo -= 1">
          Previous
        </el-button>
        <el-button :disabled="pagination.pageNo >= totalPages" type="primary" @click="pagination.pageNo += 1">
          Next
        </el-button>
      </div>
    </div>

    <el-table :data="tableData" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="categoryName" label="Category Name" />
      <el-table-column prop="createTime" label="Create Time" />

      <el-table-column label="Actions">
        <template #default="scope">
          <el-button size="small" @click="openEditDialog(scope.row)">Edit</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">
            Delete
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
      <el-form>
        <el-form-item label="Category Name">
          <el-input
            v-model="form.categoryName"
            placeholder="Please enter category name"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="handleSave">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCategory,
  deleteCategory,
  updateCategory
} from '@/api/adminCategory'
import { useCategoryStore, type CategoryItem } from '@/stores/category'

const categoryStore = useCategoryStore()
const pagination = reactive({
  pageNo: 1,
  pageSize: 10
})
const totalItems = computed(() => categoryStore.categories.length)
const totalPages = computed(() =>
  totalItems.value === 0 ? 0 : Math.ceil(totalItems.value / pagination.pageSize)
)
const tableData = computed(() => {
  const start = (pagination.pageNo - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return categoryStore.categories.slice(start, end)
})

const dialogVisible = ref(false)
const isEditMode = ref(false)
const currentCategoryId = ref<number | null>(null)

const form = reactive({
  categoryName: ''
})

const dialogTitle = computed(() => (isEditMode.value ? 'Edit Category' : 'New Category'))

async function fetchCategoryList() {
  try {
    await categoryStore.fetchCategories()
    if (totalPages.value > 0 && pagination.pageNo > totalPages.value) {
      pagination.pageNo = totalPages.value
    }
  } catch (error) {
    console.error('Failed to fetch category list:', error)
  }
}

function openCreateDialog() {
  isEditMode.value = false
  currentCategoryId.value = null
  form.categoryName = ''
  dialogVisible.value = true
}

function openEditDialog(row: CategoryItem) {
  isEditMode.value = true
  currentCategoryId.value = row.id
  form.categoryName = row.categoryName
  dialogVisible.value = true
}

async function handleSave() {
  const name = form.categoryName.trim()

  if (!name) {
    ElMessage.warning('Category name cannot be empty')
    return
  }

  try {
    if (isEditMode.value && currentCategoryId.value !== null) {
      await updateCategory(currentCategoryId.value, {
        categoryName: name
      })
      ElMessage.success('Category updated successfully')
    } else {
      await createCategory({
        categoryName: name
      })
      ElMessage.success('Category created successfully')
    }

    dialogVisible.value = false
    isEditMode.value = false
    currentCategoryId.value = null
    form.categoryName = ''
    await fetchCategoryList()
  } catch (error) {
    console.error('Failed to save category:', error)
  }
}

async function handleDelete(row: CategoryItem) {
  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete category "${row.categoryName}"?`,
      'Delete Category',
      {
        type: 'warning',
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        customClass: 'admin-confirm-dialog',
        confirmButtonClass: 'admin-confirm-primary',
        cancelButtonClass: 'admin-confirm-default'
      }
    )
  } catch {
    return
  }

  try {
    await deleteCategory(row.id)
    ElMessage.success('Category deleted successfully')
    await fetchCategoryList()
  } catch (error) {
    console.error('Failed to delete category:', error)
  }
}

onMounted(() => {
  void fetchCategoryList()
})

watch(
  () => pagination.pageSize,
  () => {
    pagination.pageNo = 1
  }
)
</script>

<style scoped>
.admin-category-page {
  padding: 20px;
  --el-color-primary: var(--color-primary);
  --el-color-primary-light-3: color-mix(in srgb, var(--color-primary) 70%, white);
  --el-color-primary-light-5: color-mix(in srgb, var(--color-primary) 50%, white);
  --el-color-primary-light-7: color-mix(in srgb, var(--color-primary) 30%, white);
  --el-color-primary-dark-2: color-mix(in srgb, var(--color-primary) 80%, black);
}

.create-button {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin: 0 0 16px;
  padding: 12px 16px;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
}

.pagination-summary {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.pagination-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-size-select {
  width: 120px;
}

@media (max-width: 768px) {
  .pagination-wrapper {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-actions {
    justify-content: space-between;
    flex-wrap: wrap;
  }
}
</style>

<style>
.admin-confirm-dialog .el-message-box__btns .admin-confirm-primary {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-text-inverse);
}

.admin-confirm-dialog .el-message-box__btns .admin-confirm-primary:hover {
  background-color: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.admin-confirm-dialog .el-message-box__btns .admin-confirm-default {
  border-color: var(--color-border);
  color: var(--color-text-primary);
  background: var(--color-bg-surface);
}

.admin-confirm-dialog .el-message-box__btns .admin-confirm-default:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.admin-confirm-dialog .el-message-box__headerbtn:focus-visible {
  outline: 2px solid rgba(var(--color-primary-rgb), 0.35);
  border-radius: 6px;
}

.admin-confirm-dialog .el-message-box__headerbtn:hover .el-message-box__close,
.admin-confirm-dialog .el-message-box__headerbtn:focus-visible .el-message-box__close {
  color: var(--color-primary);
}
</style>
