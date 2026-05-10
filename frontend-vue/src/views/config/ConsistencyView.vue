<template>
  <div class="page-container">
    <PageHeader title="配置一致性" subtitle="检查配置依赖关系和一致性">
      <template #actions>
        <a-space>
          <a-button type="primary" @click="handleValidate" :loading="validating">
            <CheckCircleOutlined /> 一致性校验
          </a-button>
          <a-button @click="handleCreateSnapshot">
            <CameraOutlined /> 创建快照
          </a-button>
        </a-space>
      </template>
    </PageHeader>

    <a-row :gutter="16">
      <!-- Dependency Graph -->
      <a-col :span="16">
        <a-card title="依赖关系图" :bordered="false" class="page-card">
          <div class="dependency-graph" ref="graphRef">
            <div v-if="graphLoading" class="graph-loading">
              <a-spin size="large" />
            </div>
            <div v-else class="graph-container">
              <!-- SVG-based dependency graph -->
              <svg width="100%" height="450" viewBox="0 0 800 450">
                <!-- Edges -->
                <line
                  v-for="(edge, idx) in graphEdges"
                  :key="'edge-' + idx"
                  :x1="getNodePos(edge.from).x"
                  :y1="getNodePos(edge.from).y"
                  :x2="getNodePos(edge.to).x"
                  :y2="getNodePos(edge.to).y"
                  stroke="#d9d9d9"
                  stroke-width="2"
                  marker-end="url(#arrowhead)"
                />
                <defs>
                  <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
                    <polygon points="0 0, 10 3.5, 0 7" fill="#d9d9d9" />
                  </marker>
                </defs>
                <!-- Nodes -->
                <g
                  v-for="node in graphNodes"
                  :key="node.id"
                  :transform="`translate(${getNodePos(node.id).x}, ${getNodePos(node.id).y})`"
                  class="graph-node"
                  @click="handleNodeClick(node)"
                >
                  <rect
                    x="-60"
                    y="-25"
                    width="120"
                    height="50"
                    rx="8"
                    :fill="selectedNode?.id === node.id ? '#e6f4ff' : '#fff'"
                    :stroke="selectedNode?.id === node.id ? '#1677ff' : '#d9d9d9'"
                    stroke-width="2"
                  />
                  <text x="0" y="-5" text-anchor="middle" fill="#999" font-size="10">
                    {{ nodeTypeLabel[node.type] || node.type }}
                  </text>
                  <text x="0" y="12" text-anchor="middle" fill="#333" font-size="13" font-weight="600">
                    {{ truncate(node.name, 10) }}
                  </text>
                </g>
              </svg>
            </div>
          </div>

          <!-- Selected Node Detail -->
          <a-collapse v-if="selectedNode" style="margin-top: 16px">
            <a-collapse-panel :key="selectedNode.id" :header="`${selectedNode.name} 详情`">
              <a-descriptions :column="2" bordered size="small">
                <a-descriptions-item label="名称">{{ selectedNode.name }}</a-descriptions-item>
                <a-descriptions-item label="类型">{{ nodeTypeLabel[selectedNode.type] || selectedNode.type }}</a-descriptions-item>
                <a-descriptions-item label="ID">{{ selectedNode.id }}</a-descriptions-item>
                <a-descriptions-item label="依赖数">{{ selectedNode.dependencies?.length || 0 }}</a-descriptions-item>
              </a-descriptions>
            </a-collapse-panel>
          </a-collapse>
        </a-card>
      </a-col>

      <!-- Validation & Snapshots -->
      <a-col :span="8">
        <!-- Validation Results -->
        <a-card title="校验结果" :bordered="false" class="page-card">
          <div v-if="validationResult">
            <a-alert
              :type="validationResult.valid ? 'success' : 'error'"
              :message="validationResult.valid ? '配置一致性校验通过' : '发现一致性问题'"
              show-icon
              style="margin-bottom: 16px"
            />
            <div v-if="validationResult.errors?.length" class="validation-list">
              <h4 style="color: #ff4d4f; margin-bottom: 8px">
                <CloseCircleOutlined /> 错误 ({{ validationResult.errors.length }})
              </h4>
              <div v-for="(err, idx) in validationResult.errors" :key="'err-' + idx" class="validation-item error">
                <a-tag color="red">{{ err.type }}</a-tag>
                <span>{{ err.message }}</span>
                <span class="ref-info">{{ err.refName }}</span>
              </div>
            </div>
            <div v-if="validationResult.warnings?.length" class="validation-list" style="margin-top: 12px">
              <h4 style="color: #faad14; margin-bottom: 8px">
                <WarningOutlined /> 警告 ({{ validationResult.warnings.length }})
              </h4>
              <div v-for="(warn, idx) in validationResult.warnings" :key="'warn-' + idx" class="validation-item warning">
                <a-tag color="orange">{{ warn.type }}</a-tag>
                <span>{{ warn.message }}</span>
                <span class="ref-info">{{ warn.refName }}</span>
              </div>
            </div>
          </div>
          <a-empty v-else description="点击上方「一致性校验」按钮开始检查" />
        </a-card>

        <!-- Snapshots -->
        <a-card title="配置快照" :bordered="false" class="page-card">
          <a-list :data-source="snapshots" :loading="snapshotLoading" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <span>{{ item.name }}</span>
                    <a-tag style="margin-left: 8px" size="small">{{ item.itemCount }} 项配置</a-tag>
                  </template>
                  <template #description>
                    <span>{{ item.createdAt }} · {{ item.createdBy }}</span>
                  </template>
                </a-list-item-meta>
                <template #actions>
                  <a-popconfirm title="确认恢复此快照？将覆盖当前配置。" @confirm="handleRestore(item.id)">
                    <a-button type="link" size="small">恢复</a-button>
                  </a-popconfirm>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>

    <!-- Snapshot Modal -->
    <a-modal
      v-model:open="snapshotModalVisible"
      title="创建配置快照"
      :confirm-loading="snapshotCreating"
      @ok="handleSnapshotSubmit"
    >
      <a-form layout="vertical">
        <a-form-item label="快照名称" required>
          <a-input v-model:value="snapshotName" placeholder="请输入快照名称" />
          <div class="field-hint">示例：v2.1.0发布前备份</div>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="snapshotDesc" :rows="3" placeholder="请输入描述" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  CameraOutlined,
  CloseCircleOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { configApi, type ValidationResult, type Snapshot, type DependencyGraph, type DependencyNode } from '@/api/config'

const graphRef = ref<HTMLElement>()
const graphLoading = ref(false)
const validating = ref(false)
const snapshotLoading = ref(false)
const snapshotCreating = ref(false)
const snapshotModalVisible = ref(false)
const snapshotName = ref('')
const snapshotDesc = ref('')

const validationResult = ref<ValidationResult | null>(null)
const snapshots = ref<Snapshot[]>([])
const graphData = ref<DependencyGraph | null>(null)
const selectedNode = ref<DependencyNode | null>(null)

const nodeTypeLabel: Record<string, string> = {
  schema: 'Schema',
  kpi: 'KPI',
  workflow: '工作流',
  prompt: '提示词',
  report_template: '报表模板',
  datasource: '数据源',
  dataset: '数据集',
}

// Layout nodes in layers
const nodePositions = computed(() => {
  if (!graphData.value) return {}
  const layers: Record<string, string[]> = {
    datasource: [],
    dataset: [],
    schema: [],
    kpi: [],
    prompt: [],
    workflow: [],
    report_template: [],
  }
  graphData.value.nodes.forEach((node) => {
    if (layers[node.type]) layers[node.type].push(node.id)
  })

  const positions: Record<string, { x: number; y: number }> = {}
  const layerKeys = Object.keys(layers)
  const layerWidth = 800
  const layerHeight = 400

  layerKeys.forEach((type, col) => {
    const nodes = layers[type]
    const x = 80 + (col * (layerWidth - 160)) / Math.max(layerKeys.length - 1, 1)
    nodes.forEach((nodeId, row) => {
      const y = 60 + (row * (layerHeight - 120)) / Math.max(nodes.length - 1, 1)
      positions[nodeId] = { x, y: nodes.length === 1 ? 225 : y }
    })
  })
  return positions
})

const graphNodes = computed(() => graphData.value?.nodes || [])
const graphEdges = computed(() => graphData.value?.edges || [])

function getNodePos(id: string) {
  return nodePositions.value[id] || { x: 400, y: 225 }
}

function truncate(str: string, len: number) {
  return str && str.length > len ? str.slice(0, len) + '...' : str
}

function handleNodeClick(node: DependencyNode) {
  selectedNode.value = node
}

async function loadGraph() {
  graphLoading.value = true
  try {
    const res = await configApi.getDependencyGraph()
    graphData.value = res.data
  } catch {} finally {
    graphLoading.value = false
  }
}

async function handleValidate() {
  validating.value = true
  try {
    const res = await configApi.validate()
    validationResult.value = res.data
    if (res.data.valid) {
      message.success('配置一致性校验通过')
    } else {
      message.warning(`发现 ${res.data.errors?.length || 0} 个错误, ${res.data.warnings?.length || 0} 个警告`)
    }
  } finally {
    validating.value = false
  }
}

function handleCreateSnapshot() {
  snapshotName.value = ''
  snapshotDesc.value = ''
  snapshotModalVisible.value = true
}

async function handleSnapshotSubmit() {
  if (!snapshotName.value) {
    message.error('请输入快照名称')
    return
  }
  snapshotCreating.value = true
  try {
    await configApi.createSnapshot({ name: snapshotName.value, description: snapshotDesc.value })
    message.success('快照创建成功')
    snapshotModalVisible.value = false
    loadSnapshots()
  } finally {
    snapshotCreating.value = false
  }
}

async function handleRestore(snapshotId: string) {
  try {
    await configApi.restoreSnapshot(snapshotId)
    message.success('快照恢复成功')
    loadGraph()
    loadSnapshots()
  } catch {}
}

async function loadSnapshots() {
  snapshotLoading.value = true
  try {
    const res = await configApi.listSnapshots({ page: 1, pageSize: 20 })
    snapshots.value = res.data.items
  } catch {} finally {
    snapshotLoading.value = false
  }
}

onMounted(() => {
  loadGraph()
  loadSnapshots()
})
</script>

<style lang="scss" scoped>
.dependency-graph {
  min-height: 450px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.graph-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 450px;
}

.graph-container {
  width: 100%;
  height: 450px;
}

.graph-node {
  cursor: pointer;
  transition: all 0.2s;

  &:hover rect {
    stroke: #1677ff;
    fill: #e6f4ff;
  }
}

.validation-list {
  .validation-item {
    padding: 8px 12px;
    margin-bottom: 8px;
    border-radius: 6px;
    font-size: 13px;

    &.error {
      background: #fff2f0;
      border: 1px solid #ffccc7;
    }

    &.warning {
      background: #fffbe6;
      border: 1px solid #ffe58f;
    }

    .ref-info {
      display: block;
      font-size: 12px;
      color: #999;
      margin-top: 2px;
    }
  }
}

.field-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
