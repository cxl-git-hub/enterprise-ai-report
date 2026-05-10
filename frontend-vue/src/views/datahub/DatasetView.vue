<template>
  <div class="page-container">
    <PageHeader title="数据集管理" subtitle="管理数据集和列信息">
      <template #actions>
        <a-button type="primary" @click="openCreateModal">
          <PlusOutlined /> 新建数据集
        </a-button>
      </template>
    </PageHeader>

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="search">
        <a-form-item>
          <a-input v-model:value="searchParams.keyword" placeholder="搜索数据集名称" allow-clear>
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
        :expanded-row-keys="expandedRowKeys"
        @expand="handleExpand"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'rowCount'">
            <a-tag>{{ (record.rowCount || 0).toLocaleString() }} 行</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 'active' ? 'success' : 'default'"
              :text="record.status === 'active' ? '已激活' : '草稿'"
            />
          </template>
          <template v-if="column.key === 'action'">
            <div class="table-actions">
              <a-button type="link" size="small" @click="handlePreviewData(record)">预览</a-button>
              <a-button type="link" size="small" @click="handleRefreshColumns(record.id)">刷新列</a-button>
              <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
              <ConfirmDelete @confirm="handleDelete(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </ConfirmDelete>
            </div>
          </template>
        </template>
        <template #expandedRowRender="{ record }">
          <a-table
            :columns="columnColumns"
            :data-source="record.columns || []"
            :pagination="false"
            size="small"
            row-key="columnName"
          >
            <template #bodyCell="{ column: col, record: colRecord }">
              <template v-if="col.key === 'isPrimaryKey'">
                <a-tag v-if="colRecord.isPrimaryKey" color="gold">PK</a-tag>
              </template>
              <template v-if="col.key === 'isNullable'">
                <a-tag :color="colRecord.isNullable ? 'default' : 'red'">
                  {{ colRecord.isNullable ? '可空' : '非空' }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </template>
      </a-table>
    </a-card>

    <!-- Form Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑数据集' : '新建数据集'"
      :confirm-loading="confirmLoading"
      @ok="handleSubmit"
      width="600px"
    >
      <a-form ref="formRef" :model="formState" :rules="formRules" layout="vertical">
        <a-form-item label="数据集名称" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入名称">
            <template #tooltip><span>数据集的显示名称</span></template>
          </a-input>
          <div class="field-hint">示例：订单明细数据集</div>
        </a-form-item>
        <a-form-item label="数据源" name="datasourceId">
          <a-select v-model:value="formState.datasourceId" placeholder="请选择数据源">
            <a-select-option v-for="ds in datasourceOptions" :key="ds.id" :value="ds.id">
              {{ ds.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="表名" name="tableName">
          <a-input v-model:value="formState.tableName" placeholder="请输入表名">
            <template #tooltip><span>数据库中的实际表名</span></template>
          </a-input>
          <div class="field-hint">示例：orders</div>
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="3" placeholder="请输入描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Data Preview Modal -->
    <a-modal
      v-model:open="previewVisible"
      :title="`数据预览: ${previewDatasetName}`"
      :footer="null"
      width="900px"
    >
      <a-spin :spinning="previewLoading">
        <a-table
          v-if="previewData.columns.length > 0"
          :columns="previewData.columns.map(c => ({ title: c, dataIndex: c, key: c, ellipsis: true }))"
          :data-source="previewData.rows"
          :pagination="{ pageSize: 20 }"
          size="small"
          :scroll="{ x: true }"
        />
        <a-empty v-else description="暂无数据" />
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ConfirmDelete from '@/components/common/ConfirmDelete.vue'
import { useTable } from '@/composables/useTable'
import { useModal } from '@/composables/useModal'
import { datasetApi, type Dataset, type DatasetForm } from '@/api/dataset'
import { datasourceApi, type DataSource } from '@/api/datasource'
import { get } from '@/api/request'
import type { FormInstance } from 'ant-design-vue'

const formRef = ref<FormInstance>()
const expandedRowKeys = ref<string[]>([])
const datasourceOptions = ref<DataSource[]>([])
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewData = ref<{ columns: string[]; rows: Record<string, unknown>[] }>({ columns: [], rows: [] })
const previewDatasetName = ref('')

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, search, resetSearch } =
  useTable<Dataset>({
    fetchApi: (params) => datasetApi.list(params as { page: number; pageSize: number; keyword?: string }),
  })

const { visible: modalVisible, confirmLoading, editingId, openModal, closeModal, handleOk } = useModal()

const formState = reactive<DatasetForm>({
  name: '',
  datasourceId: '',
  tableName: '',
  description: '',
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
}

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '数据源', dataIndex: 'datasourceName', key: 'datasourceName' },
  { title: '表名', dataIndex: 'tableName', key: 'tableName' },
  { title: '数据量', dataIndex: 'rowCount', key: 'rowCount', width: 120 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 280, fixed: 'right' as const },
]

const columnColumns = [
  { title: '列名', dataIndex: 'columnName', key: 'columnName' },
  { title: '类型', dataIndex: 'columnType', key: 'columnType' },
  { title: '可空', dataIndex: 'isNullable', key: 'isNullable', width: 80 },
  { title: '主键', dataIndex: 'isPrimaryKey', key: 'isPrimaryKey', width: 80 },
  { title: '描述', dataIndex: 'description', key: 'description' },
]

function openCreateModal() {
  editingId.value = null
  Object.assign(formState, { name: '', datasourceId: '', tableName: '', description: '' })
  openModal()
}

async function openEditModal(record: Dataset) {
  openModal(record.id)
  const res = await datasetApi.detail(record.id)
  Object.assign(formState, res.data)
}

async function handleExpand(expanded: boolean, record: Dataset) {
  if (expanded) {
    expandedRowKeys.value = [record.id]
    if (!record.columns || record.columns.length === 0) {
      try {
        const res = await datasetApi.getColumns(record.id)
        record.columns = res.data
      } catch {}
    }
  } else {
    expandedRowKeys.value = []
  }
}

async function handleRefreshColumns(id: string) {
  try {
    await datasetApi.refreshColumns(id)
    message.success('列信息已刷新')
    fetchData()
  } catch {}
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  await handleOk(async () => {
    if (editingId.value) {
      await datasetApi.update(editingId.value, formState)
      message.success('更新成功')
    } else {
      await datasetApi.create(formState)
      message.success('创建成功')
    }
    fetchData()
  })
}

async function handleDelete(id: string) {
  try {
    await datasetApi.remove(id)
    message.success('删除成功')
    fetchData()
  } catch {}
}

async function handlePreviewData(record: Dataset) {
  previewDatasetName.value = record.name
  previewVisible.value = true
  previewLoading.value = true
  try {
    const res = await get<{ data: { columns: string[]; rows: Record<string, unknown>[] } }>(`/datasets/${record.id}/preview`, { limit: 100 })
    previewData.value = res?.data || { columns: [], rows: [] }
  } catch {
    previewData.value = { columns: [], rows: [] }
  } finally {
    previewLoading.value = false
  }
}

onMounted(async () => {
  try {
    const res = await datasourceApi.list({ page: 1, pageSize: 100 })
    datasourceOptions.value = res.data.items
  } catch {}
})
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 16px; }
.field-hint { font-size: 12px; color: #999; margin-top: 4px; }
</style>
