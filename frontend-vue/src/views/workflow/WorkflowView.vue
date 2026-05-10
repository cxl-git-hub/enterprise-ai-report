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
        <a-form-item label="DAG节点定义">
          <template #tooltip><span>定义工作流的节点和依赖关系</span></template>

          <!-- Visual DAG Editor -->
          <div class="dag-editor">
            <div class="dag-toolbar">
              <a-space>
                <a-button size="small" type="primary" @click="addDagNode">
                  <PlusOutlined /> 添加节点
                </a-button>
                <a-button size="small" @click="addSampleDag">
                  <ThunderboltOutlined /> 加载示例
                </a-button>
                <a-button size="small" @click="toggleJsonEdit">
                  <CodeOutlined /> {{ showJsonEdit ? '可视化' : 'JSON' }}
                </a-button>
              </a-space>
            </div>

            <!-- Visual Node List -->
            <div v-if="!showJsonEdit" class="dag-nodes">
              <div v-if="dagNodes.length === 0" class="dag-empty">
                <a-empty description="暂无节点，点击上方按钮添加" :image-style="{ height: '40px' }" />
              </div>
              <div
                v-for="(node, idx) in dagNodes"
                :key="node.id"
                class="dag-node-card"
                :class="{ 'dag-node-error': nodeErrors[idx] }"
              >
                <div class="dag-node-header">
                  <a-tag :color="nodeTypeColors[node.type] || 'default'">
                    {{ nodeTypeLabels[node.type] || node.type }}
                  </a-tag>
                  <span class="dag-node-id">#{{ idx + 1 }}</span>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="removeDagNode(idx)"
                  >
                    <DeleteOutlined />
                  </a-button>
                </div>
                <a-row :gutter="8" style="margin-top: 8px">
                  <a-col :span="8">
                    <a-input
                      v-model:value="node.name"
                      size="small"
                      placeholder="节点名称"
                      @change="syncDagToJson"
                    />
                  </a-col>
                  <a-col :span="8">
                    <a-select
                      v-model:value="node.type"
                      size="small"
                      placeholder="节点类型"
                      style="width: 100%"
                      @change="syncDagToJson"
                    >
                      <a-select-option value="data_fetch">数据提取</a-select-option>
                      <a-select-option value="kpi_calc">KPI计算</a-select-option>
                      <a-select-option value="ai_analysis">AI分析</a-select-option>
                      <a-select-option value="report">报表生成</a-select-option>
                      <a-select-option value="output">输出</a-select-option>
                    </a-select>
                  </a-col>
                  <a-col :span="8">
                    <a-select
                      v-model:value="node.dependencies"
                      size="small"
                      mode="multiple"
                      placeholder="依赖节点"
                      style="width: 100%"
                      :options="dagNodes.filter((_, i) => i !== idx).map(n => ({ label: n.name || n.id, value: n.id }))"
                      @change="syncDagToJson"
                    />
                  </a-col>
                </a-row>
                <div v-if="nodeErrors[idx]" class="dag-node-error-msg">
                  {{ nodeErrors[idx] }}
                </div>
              </div>
            </div>

            <!-- JSON Fallback -->
            <div v-else class="code-editor">
              <a-textarea
                v-model:value="nodesJson"
                :rows="12"
                placeholder='[{"id": "node1", "name": "数据提取", "type": "data_fetch", "config": {}, "dependencies": []}]'
                style="font-family: monospace; font-size: 13px"
              />
            </div>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, ThunderboltOutlined, DeleteOutlined, CodeOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ConfirmDelete from '@/components/common/ConfirmDelete.vue'
import { useTable } from '@/composables/useTable'
import { useModal } from '@/composables/useModal'
import { workflowApi, type Workflow, type WorkflowForm, type WorkflowNode } from '@/api/workflow'
import type { FormInstance } from 'ant-design-vue'

const formRef = ref<FormInstance>()
const triggeringId = ref<string | null>(null)
const nodesJson = ref('')
const showJsonEdit = ref(false)

interface DagNodeItem {
  id: string
  name: string
  type: string
  dependencies: string[]
  config?: Record<string, unknown>
}

const dagNodes = ref<DagNodeItem[]>([])
const nodeErrors = ref<Record<number, string>>({})

const nodeTypeLabels: Record<string, string> = {
  data_fetch: '数据提取',
  kpi_calc: 'KPI计算',
  ai_analysis: 'AI分析',
  report: '报表生成',
  output: '输出',
}

const nodeTypeColors: Record<string, string> = {
  data_fetch: 'blue',
  kpi_calc: 'green',
  ai_analysis: 'purple',
  report: 'orange',
  output: 'cyan',
}

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
  dagNodes.value = []
  nodeErrors.value = {}
  showJsonEdit.value = false
  openModal()
}

async function openEditModal(record: Workflow) {
  openModal(record.id)
  const res = await workflowApi.detail(record.id)
  Object.assign(formState, res.data)
  const nodes = res.data.nodes || []
  nodesJson.value = JSON.stringify(nodes, null, 2)
  dagNodes.value = nodes.map((n: any) => ({
    id: n.id || `node_${Date.now()}`,
    name: n.name || '',
    type: n.type || 'data_fetch',
    dependencies: n.dependencies || [],
    config: n.config || {},
  }))
  showJsonEdit.value = false
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

function addDagNode() {
  const idx = dagNodes.value.length + 1
  const newNode: DagNodeItem = {
    id: `node_${Date.now()}`,
    name: `节点${idx}`,
    type: 'data_fetch',
    dependencies: [],
  }
  dagNodes.value.push(newNode)
  syncDagToJson()
}

function removeDagNode(idx: number) {
  const removedId = dagNodes.value[idx].id
  dagNodes.value.splice(idx, 1)
  // Remove references to deleted node
  dagNodes.value.forEach((node) => {
    node.dependencies = node.dependencies.filter((dep) => dep !== removedId)
  })
  syncDagToJson()
}

function syncDagToJson() {
  nodesJson.value = JSON.stringify(
    dagNodes.value.map((n) => ({
      id: n.id,
      name: n.name,
      type: n.type,
      config: n.config || {},
      dependencies: n.dependencies,
    })),
    null,
    2
  )
  // Validate
  validateDagNodes()
}

function validateDagNodes() {
  nodeErrors.value = {}
  const ids = new Set(dagNodes.value.map((n) => n.id))
  dagNodes.value.forEach((node, idx) => {
    if (!node.name?.trim()) {
      nodeErrors.value[idx] = '节点名称不能为空'
    }
    for (const dep of node.dependencies) {
      if (!ids.has(dep)) {
        nodeErrors.value[idx] = `依赖节点 ${dep} 不存在`
      }
    }
  })
}

function addSampleDag() {
  const now = Date.now()
  dagNodes.value = [
    { id: `node_${now}`, name: '数据提取', type: 'data_fetch', dependencies: [] },
    { id: `node_${now + 1}`, name: 'KPI计算', type: 'kpi_calc', dependencies: [`node_${now}`] },
    { id: `node_${now + 2}`, name: 'AI分析', type: 'ai_analysis', dependencies: [`node_${now + 1}`] },
    { id: `node_${now + 3}`, name: '报表生成', type: 'report', dependencies: [`node_${now + 2}`] },
    { id: `node_${now + 4}`, name: '输出', type: 'output', dependencies: [`node_${now + 3}`] },
  ]
  syncDagToJson()
}

function toggleJsonEdit() {
  if (showJsonEdit.value) {
    // Switching from JSON to visual - parse JSON
    try {
      const parsed = JSON.parse(nodesJson.value || '[]')
      dagNodes.value = parsed.map((n: any) => ({
        id: n.id || `node_${Date.now()}`,
        name: n.name || '',
        type: n.type || 'data_fetch',
        dependencies: n.dependencies || [],
        config: n.config || {},
      }))
    } catch {
      message.error('JSON格式错误，无法切换到可视化模式')
      return
    }
  }
  showJsonEdit.value = !showJsonEdit.value
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

.dag-editor {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.dag-toolbar {
  padding: 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.dag-nodes {
  padding: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.dag-empty {
  padding: 24px;
}

.dag-node-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 8px;
  transition: all 0.2s;

  &:hover {
    border-color: #91caff;
    box-shadow: 0 2px 8px rgba(22, 119, 255, 0.1);
  }

  &.dag-node-error {
    border-color: #ff4d4f;
  }
}

.dag-node-header {
  display: flex;
  align-items: center;
  gap: 8px;

  .dag-node-id {
    color: #999;
    font-size: 12px;
    flex: 1;
  }
}

.dag-node-error-msg {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 4px;
}
</style>
