<template>
  <div class="page-container">
    <PageHeader title="工作流定义" subtitle="管理工作流定义和DAG配置">
      <template #actions>
        <a-button type="primary" @click="openCreateModal">
          <PlusOutlined /> 新建工作流
        </a-button>
      </template>
    </PageHeader>

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="search">
        <a-form-item>
          <a-input v-model:value="searchParams.keyword" placeholder="搜索工作流名称" allow-clear>
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
          <template v-if="column.key === 'nodes'">
            <a-tag>{{ (record.nodes || []).length }} 个节点</a-tag>
          </template>
          <template v-if="column.key === 'schedule'">
            <a-tag v-if="record.schedule" color="blue">{{ record.schedule }}</a-tag>
            <span v-else style="color: #999">手动触发</span>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 'active' ? 'success' : 'default'"
              :text="record.status === 'active' ? '已启用' : '已禁用'"
            />
          </template>
          <template v-if="column.key === 'action'">
            <div class="table-actions">
              <a-button type="link" size="small" @click="handleTrigger(record)" :loading="triggeringId === record.id">
                触发
              </a-button>
              <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
              <ConfirmDelete @confirm="handleDelete(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </ConfirmDelete>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Form Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑工作流' : '新建工作流'"
      :confirm-loading="confirmLoading"
      @ok="handleSubmit"
      width="900px"
    >
      <a-form ref="formRef" :model="formState" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="工作流名称" name="name">
              <a-input v-model:value="formState.name" placeholder="请输入名称">
                <template #tooltip><span>工作流的显示名称</span></template>
              </a-input>
              <div class="field-hint">示例：每日KPI计算工作流</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="调度表达式" name="schedule">
              <a-input v-model:value="formState.schedule" placeholder="Cron表达式，留空为手动触发">
                <template #tooltip><span>标准Cron表达式，如 0 9 * * * 表示每天9点</span></template>
              </a-input>
              <div class="field-hint">示例：0 9 * * 1-5 (工作日9点)</div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="2" placeholder="请输入描述" />
        </a-form-item>
        <a-form-item label="DAG节点定义 (JSON)">
          <template #tooltip><span>定义工作流的节点和依赖关系</span></template>
          <a-button size="small" style="margin-bottom: 8px" @click="addSampleNode">
            <PlusOutlined /> 添加示例节点
          </a-button>
          <div class="code-editor">
            <a-textarea
              v-model:value="nodesJson"
              :rows="12"
              placeholder='[{"id": "node1", "name": "数据提取", "type": "extract", "config": {}, "dependencies": []}]'
              style="font-family: monospace; font-size: 13px"
            />
          </div>
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
import { workflowApi, type Workflow, type WorkflowForm, type WorkflowNode } from '@/api/workflow'
import type { FormInstance } from 'ant-design-vue'

const formRef = ref<FormInstance>()
const triggeringId = ref<string | null>(null)
const nodesJson = ref('')

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, search, resetSearch } =
  useTable<Workflow>({
    fetchApi: (params) => workflowApi.list(params as { page: number; pageSize: number; keyword?: string }),
  })

const { visible: modalVisible, confirmLoading, editingId, openModal, handleOk } = useModal()

const formState = reactive<WorkflowForm>({
  name: '',
  description: '',
  nodes: [],
  schedule: '',
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
}

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '节点', dataIndex: 'nodes', key: 'nodes', width: 120 },
  { title: '调度', dataIndex: 'schedule', key: 'schedule', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 180 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
]

function openCreateModal() {
  editingId.value = null
  Object.assign(formState, { name: '', description: '', nodes: [], schedule: '' })
  nodesJson.value = '[]'
  openModal()
}

async function openEditModal(record: Workflow) {
  openModal(record.id)
  const res = await workflowApi.detail(record.id)
  Object.assign(formState, res.data)
  nodesJson.value = JSON.stringify(res.data.nodes || [], null, 2)
}

function addSampleNode() {
  const sample: WorkflowNode = {
    id: `node_${Date.now()}`,
    name: '新节点',
    type: 'extract',
    config: {},
    dependencies: [],
  }
  try {
    const arr = JSON.parse(nodesJson.value || '[]')
    arr.push(sample)
    nodesJson.value = JSON.stringify(arr, null, 2)
  } catch {
    nodesJson.value = JSON.stringify([sample], null, 2)
  }
}

async function handleTrigger(record: Workflow) {
  triggeringId.value = record.id
  try {
    await workflowApi.trigger(record.id)
    message.success('工作流已触发')
  } finally {
    triggeringId.value = null
  }
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  try {
    formState.nodes = JSON.parse(nodesJson.value || '[]')
  } catch {
    message.error('DAG节点JSON格式错误')
    return
  }
  await handleOk(async () => {
    if (editingId.value) {
      await workflowApi.update(editingId.value, formState)
      message.success('更新成功')
    } else {
      await workflowApi.create(formState)
      message.success('创建成功')
    }
    fetchData()
  })
}

async function handleDelete(id: string) {
  try {
    await workflowApi.remove(id)
    message.success('删除成功')
    fetchData()
  } catch {}
}
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 16px; }
.field-hint { font-size: 12px; color: #999; margin-top: 4px; }
</style>
