<template>
  <div class="page-container">
    <PageHeader title="用户管理" subtitle="管理系统用户">
      <template #actions>
        <a-button type="primary" @click="openCreateModal">
          <PlusOutlined /> 新建用户
        </a-button>
      </template>
    </PageHeader>

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="search">
        <a-form-item>
          <a-input v-model:value="searchParams.keyword" placeholder="搜索用户名/邮箱" allow-clear>
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
          <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 'active' ? 'success' : 'default'"
              :text="record.status === 'active' ? '启用' : '禁用'"
            />
          </template>
          <template v-if="column.key === 'roles'">
            <a-tag v-for="role in record.roles" :key="role" color="blue">{{ role }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <div class="table-actions">
              <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
              <a-button type="link" size="small" @click="openRoleModal(record)">分配角色</a-button>
              <ConfirmDelete @confirm="handleDelete(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </ConfirmDelete>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- User Form Modal -->
    <a-modal
      v-model:open="userModalVisible"
      :title="editingUser ? '编辑用户' : '新建用户'"
      :confirm-loading="confirmLoading"
      @ok="handleUserSubmit"
      width="600px"
    >
      <a-form ref="formRef" :model="formState" :rules="formRules" layout="vertical">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="formState.username" placeholder="请输入用户名" :disabled="!!editingUser" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formState.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="显示名称" name="displayName">
          <a-input v-model:value="formState.displayName" placeholder="请输入显示名称" />
        </a-form-item>
        <a-form-item v-if="!editingUser" label="密码" name="password">
          <a-input-password v-model:value="formState.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="formState.status">
            <a-select-option value="active">启用</a-select-option>
            <a-select-option value="disabled">禁用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Role Assignment Modal -->
    <a-modal
      v-model:open="roleModalVisible"
      title="分配角色"
      :confirm-loading="confirmLoading"
      @ok="handleRoleSubmit"
    >
      <p>为用户 <strong>{{ editingUser?.displayName }}</strong> 分配角色：</p>
      <a-checkbox-group v-model:value="selectedRoleIds" :options="roleOptions" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ConfirmDelete from '@/components/common/ConfirmDelete.vue'
import { useTable } from '@/composables/useTable'
import { userApi, type User, type UserForm } from '@/api/user'
import type { FormInstance } from 'ant-design-vue'

const formRef = ref<FormInstance>()
const userModalVisible = ref(false)
const roleModalVisible = ref(false)
const confirmLoading = ref(false)
const editingUser = ref<User | null>(null)
const selectedRoleIds = ref<string[]>([])

const roleOptions = [
  { label: '管理员', value: 'admin' },
  { label: '数据分析师', value: 'analyst' },
  { label: '报表查看者', value: 'viewer' },
  { label: '工作流编辑者', value: 'workflow_editor' },
]

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, search, resetSearch } =
  useTable<User>({
    fetchApi: (params) => userApi.list(params as { page: number; pageSize: number; keyword?: string }),
  })

const formState = reactive<UserForm>({
  username: '',
  email: '',
  displayName: '',
  password: '',
  status: 'active',
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email' as const, message: '请输入有效邮箱', trigger: 'blur' },
  ],
  displayName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '显示名称', dataIndex: 'displayName', key: 'displayName' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '租户', dataIndex: 'tenantName', key: 'tenantName' },
  { title: '角色', dataIndex: 'roles', key: 'roles' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' as const },
]

function openCreateModal() {
  editingUser.value = null
  Object.assign(formState, { username: '', email: '', displayName: '', password: '', status: 'active' })
  userModalVisible.value = true
}

function openEditModal(user: User) {
  editingUser.value = user
  Object.assign(formState, {
    username: user.username,
    email: user.email,
    displayName: user.displayName,
    status: user.status,
  })
  userModalVisible.value = true
}

function openRoleModal(user: User) {
  editingUser.value = user
  selectedRoleIds.value = [...user.roles]
  roleModalVisible.value = true
}

async function handleUserSubmit() {
  await formRef.value?.validateFields()
  confirmLoading.value = true
  try {
    if (editingUser.value) {
      await userApi.update(editingUser.value.id, formState)
      message.success('更新成功')
    } else {
      await userApi.create(formState)
      message.success('创建成功')
    }
    userModalVisible.value = false
    fetchData()
  } finally {
    confirmLoading.value = false
  }
}

async function handleRoleSubmit() {
  if (!editingUser.value) return
  confirmLoading.value = true
  try {
    await userApi.assignRoles(editingUser.value.id, selectedRoleIds.value)
    message.success('角色分配成功')
    roleModalVisible.value = false
    fetchData()
  } finally {
    confirmLoading.value = false
  }
}

async function handleDelete(id: string) {
  try {
    await userApi.remove(id)
    message.success('删除成功')
    fetchData()
  } catch {
    // Error handled by interceptor
  }
}
</script>

<style lang="scss" scoped>
.search-bar {
  margin-bottom: 16px;
}
</style>
