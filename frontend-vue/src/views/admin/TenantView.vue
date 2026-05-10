<template>
  <div class="page-container">
    <PageHeader title="租户管理" subtitle="管理系统租户">
      <template #actions>
        <a-button type="primary" @click="openModal()">
          <PlusOutlined /> 新建租户
        </a-button>
      </template>
    </PageHeader>

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="search">
        <a-form-item>
          <a-input
            v-model:value="searchParams.keyword"
            placeholder="搜索租户名称/编码"
            allow-clear
            @pressEnter="search"
          >
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
              :status="record.status === 1 || record.status === 'active' ? 'success' : 'default'"
              :text="record.status === 1 || record.status === 'active' ? '启用' : '禁用'"
            />
          </template>
          <template v-if="column.key === 'action'">
            <div class="table-actions">
              <a-button type="link" size="small" @click="openModal(record.id)">编辑</a-button>
              <ConfirmDelete @confirm="handleDelete(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </ConfirmDelete>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Create/Edit Modal -->
    <a-modal
      v-model:open="visible"
      :title="editingId ? '编辑租户' : '新建租户'"
      :confirm-loading="confirmLoading"
      @ok="handleSubmit"
      @cancel="closeModal"
      width="600px"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        layout="vertical"
        :label-col="{ span: 24 }"
        :wrapper-col="{ span: 24 }"
      >
        <a-form-item label="租户名称" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入租户名称">
            <template #tooltip>
              <span>租户的显示名称，如公司名称</span>
            </template>
          </a-input>
          <div class="field-hint">示例：北京科技有限公司</div>
        </a-form-item>
        <a-form-item label="租户编码" name="code">
          <a-input v-model:value="formState.code" placeholder="请输入唯一编码" :disabled="!!editingId">
            <template #tooltip>
              <span>唯一标识码，创建后不可修改</span>
            </template>
          </a-input>
          <div class="field-hint">示例：beijing-tech-001</div>
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="3" placeholder="请输入租户描述" />
        </a-form-item>
        <a-form-item label="最大用户数" name="maxUsers">
          <a-input-number v-model:value="formState.maxUsers" :min="1" :max="10000" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="formState.status">
            <a-select-option value="active">启用</a-select-option>
            <a-select-option value="disabled">禁用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
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
import { useModal } from '@/composables/useModal'
import { tenantApi, type Tenant, type TenantForm } from '@/api/tenant'
import type { FormInstance } from 'ant-design-vue'

const formRef = ref<FormInstance>()

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, search, resetSearch } =
  useTable<Tenant>({
    fetchApi: (params) => tenantApi.list(params as { page: number; pageSize: number; keyword?: string }),
  })

const { visible, confirmLoading, editingId, openModal, closeModal, handleOk } = useModal()

const formState = reactive<TenantForm>({
  name: '',
  code: '',
  description: '',
  maxUsers: 100,
  status: 'active',
})

const formRules = {
  name: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入租户编码', trigger: 'blur' },
    { pattern: /^[a-z0-9-]+$/, message: '编码只能包含小写字母、数字和横线', trigger: 'blur' },
  ],
}

const columns = [
  { title: '租户名称', dataIndex: 'tenantName', key: 'tenantName' },
  { title: '编码', dataIndex: 'tenantCode', key: 'tenantCode' },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '最大用户数', dataIndex: 'maxUsers', key: 'maxUsers', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
]

async function openEditModal(id?: string) {
  openModal(id)
  if (id) {
    const res = await tenantApi.detail(id)
    const data = res.data
    Object.assign(formState, {
      name: data.tenantName || data.name,
      code: data.tenantCode || data.code,
      description: data.description,
      maxUsers: data.maxUsers,
      status: data.status === 1 || data.status === 'active' ? 'active' : 'disabled',
    })
  } else {
    Object.assign(formState, { name: '', code: '', description: '', maxUsers: 100, status: 'active' })
  }
}

// Override openModal to use our custom version
const _openModal = openModal

async function handleSubmit() {
  await formRef.value?.validateFields()
  await handleOk(async () => {
    if (editingId.value) {
      await tenantApi.update(editingId.value, formState)
      message.success('更新成功')
    } else {
      await tenantApi.create(formState)
      message.success('创建成功')
    }
    fetchData()
  })
}

async function handleDelete(id: string) {
  try {
    await tenantApi.remove(id)
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
.field-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
