<template>
  <div style="padding: 20px">
    <h2>Category Management</h2>

    <el-button
      type="primary"
      style="margin-bottom: 20px"
      @click="openCreateDialog"
    >
      New Category
    </el-button>

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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCategory,
  deleteCategory,
  updateCategory
} from '@/api/adminCategory'
import { useCategoryStore, type CategoryItem } from '@/stores/category'

const categoryStore = useCategoryStore()
const tableData = computed(() => categoryStore.categories)

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
        cancelButtonText: 'Cancel'
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
</script>
