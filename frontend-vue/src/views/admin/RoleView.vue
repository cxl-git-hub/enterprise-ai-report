<template>
  <div class="page-container">
    <PageHeader title="角色管理" subtitle="管理系统角色和权限">
      <template #actions>
        <a-button type="primary" @click="openCreateModal">
          <PlusOutlined /> 新建角色
        </a-button>
      </template>
    </PageHeader>

    <a-card :bordered="false" class="page-card">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'permissions'">
            <a-tag v-for="perm in (record.permissions || []).slice(0, 3)" :key="perm" color="blue" style="margin-bottom: 4px">
              {{ perm }}
            </a-tag>
            <a-tag v-if="(record.permissions || []).length > 3" color="default">
              +{{ (record.permissions || []).length - 3 }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <div class="table-actions">
              <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
              <a-button type="link" size="small" @click="openPermModal(record)">权限</a-button>
              <ConfirmDelete @confirm="handleDelete(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </ConfirmDelete>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Role Form Modal -->
    <a-modal
      v-model:open="roleModalVisible"
      :title="editingRole ? '编辑角色' : '新建角色'"
      :confirm-loading="confirmLoading"
      @ok="handleRoleSubmit"
      width="600px"
    >
      <a-form ref="formRef" :model="formState" :rules="formRules" layout="vertical">
        <a-form-item label="角色名称" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入角色名称">
            <template #tooltip><span>角色的显示名称</span></template>
          </a-input>
          <div class="field-hint">示例：数据分析师</div>
        </a-form-item>
        <a-form-item label="角色编码" name="code">
          <a-input v-model:value="formState.code" placeholder="请输入角色编码" :disabled="!!editingRole" />
          <div class="field-hint">示例：analyst</div>
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="3" placeholder="请输入角色描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Permission Modal -->
    <a-modal
      v-model:open="permModalVisible"
      title="权限配置"
      :confirm-loading="confirmLoading"
      @ok="handlePermSubmit"
      width="700px"
    >
      <p>角色：<strong>{{ editingRole?.name }}</strong></p>
      <a-tree
        v-model:checkedKeys="checkedPermissions"
        :tree-data="permissionTree"
        checkable
        :default-expand-all="true"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ConfirmDelete from '@/components/common/ConfirmDelete.vue'
import { useTable } from '@/composables/useTable'
import { get, post, put, del } from '@/api/request'
import type { FormInstance } from 'ant-design-vue'

interface Role {
  id: string
  name: string
  code: string
  description: string
  permissions: string[]
}

const formRef = ref<FormInstance>()
const roleModalVisible = ref(false)
const permModalVisible = ref(false)
const confirmLoading = ref(false)
const editingRole = ref<Role | null>(null)
const checkedPermissions = ref<string[]>([])

const { loading, dataSource, pagination, fetchData, handleTableChange } = useTable<Role>({
  fetchApi: (params) => get('/roles', params) as Promise<{ data: { items: Role[]; total: number } }>,
})

const formState = reactive({ name: '', code: '', description: '' })
const formRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

const columns = [
  { title: '角色名称', dataIndex: 'name', key: 'name' },
  { title: '编码', dataIndex: 'code', key: 'code' },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '权限', dataIndex: 'permissions', key: 'permissions', width: 300 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
]

const permissionTree = [
  {
    title: '数据管理',
    key: 'data',
    children: [
      { title: '数据源读取', key: 'datasource:read' },
      { title: '数据源编辑', key: 'datasource:write' },
      { title: '数据集读取', key: 'dataset:read' },
      { title: '数据集编辑', key: 'dataset:write' },
    ],
  },
  {
    title: '配置中心',
    key: 'config',
    children: [
      { title: 'Schema管理', key: 'schema:read' },
      { title: 'Schema编辑', key: 'schema:write' },
      { title: 'KPI管理', key: 'kpi:read' },
      { title: 'KPI编辑', key: 'kpi:write' },
      { title: '提示词管理', key: 'prompt:read' },
      { title: '提示词编辑', key: 'prompt:write' },
      { title: '报表模板管理', key: 'report-template:read' },
      { title: '报表模板编辑', key: 'report-template:write' },
      { title: '配置管理', key: 'config:manage' },
    ],
  },
  {
    title: '工作流',
    key: 'workflow',
    children: [
      { title: '工作流读取', key: 'workflow:read' },
      { title: '工作流编辑', key: 'workflow:write' },
      { title: '工作流触发', key: 'workflow:trigger' },
    ],
  },
  {
    title: 'AI能力',
    key: 'ai',
    children: [
      { title: 'NL2SQL', key: 'ai:nl2sql' },
      { title: 'AI分析', key: 'ai:analysis' },
      { title: 'AI追踪', key: 'ai:trace' },
    ],
  },
  {
    title: '报表输出',
    key: 'report',
    children: [
      { title: '报表查看', key: 'report:read' },
      { title: '报表下载', key: 'report:download' },
    ],
  },
  {
    title: '系统管理',
    key: 'admin',
    children: [
      { title: '租户管理', key: 'tenant:manage' },
      { title: '用户管理', key: 'user:manage' },
      { title: '角色管理', key: 'role:manage' },
      { title: '审计日志', key: 'audit:read' },
    ],
  },
]

function openCreateModal() {
  editingRole.value = null
  Object.assign(formState, { name: '', code: '', description: '' })
  roleModalVisible.value = true
}

function openEditModal(role: Role) {
  editingRole.value = role
  Object.assign(formState, { name: role.name, code: role.code, description: role.description })
  roleModalVisible.value = true
}

function openPermModal(role: Role) {
  editingRole.value = role
  checkedPermissions.value = [...(role.permissions || [])]
  permModalVisible.value = true
}

async function handleRoleSubmit() {
  await formRef.value?.validateFields()
  confirmLoading.value = true
  try {
    if (editingRole.value) {
      await put(`/roles/${editingRole.value.id}`, formState)
      message.success('更新成功')
    } else {
      await post('/roles', formState)
      message.success('创建成功')
    }
    roleModalVisible.value = false
    fetchData()
  } finally {
    confirmLoading.value = false
  }
}

async function handlePermSubmit() {
  if (!editingRole.value) return
  confirmLoading.value = true
  try {
    await post(`/roles/${editingRole.value.id}/permissions`, { permissions: checkedPermissions.value })
    message.success('权限更新成功')
    permModalVisible.value = false
    fetchData()
  } finally {
    confirmLoading.value = false
  }
}

async function handleDelete(id: string) {
  try {
    await del(`/roles/${id}`)
    message.success('删除成功')
    fetchData()
  } catch {}
}
</script>

<style lang="scss" scoped>
.field-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
