<template>
  <div class="page-container">
    <PageHeader
      title="工作流运行详情"
      :subtitle="`运行ID: ${runId}`"
      :breadcrumbs="[
        { title: '工作流运行', path: '/workflow/runs' },
        { title: '运行详情' },
      ]"
    >
      <template #actions>
        <a-button
          v-if="runDetail?.status === 'failed'"
          type="primary"
          danger
          @click="handleResume"
          :loading="resuming"
        >
          <PlayCircleOutlined /> 从失败处恢复
        </a-button>
      </template>
    </PageHeader>

    <a-spin :spinning="loading">
      <a-row :gutter="16" v-if="runDetail">
        <!-- Summary Cards -->
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: statusBg[runDetail.status], color: statusColor[runDetail.status] }">
              <CheckCircleOutlined v-if="runDetail.status === 'success'" />
              <CloseCircleOutlined v-else-if="runDetail.status === 'failed'" />
              <SyncOutlined v-else-if="runDetail.status === 'running'" spin />
              <ClockCircleOutlined v-else />
            </div>
            <div class="stat-value">{{ statusLabel[runDetail.status] }}</div>
            <div class="stat-label">运行状态</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #e6f4ff; color: #1677ff">
              <FieldTimeOutlined />
            </div>
            <div class="stat-value">{{ formatDuration(runDetail.duration) }}</div>
            <div class="stat-label">总耗时</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #f6ffed; color: #52c41a">
              <ThunderboltOutlined />
            </div>
            <div class="stat-value">{{ formatNumber(runDetail.totalTokens) }}</div>
            <div class="stat-label">Token用量</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #fff7e6; color: #fa8c16">
              <DollarOutlined />
            </div>
            <div class="stat-value">${{ (runDetail.totalCost || 0).toFixed(4) }}</div>
            <div class="stat-label">总费用</div>
          </div>
        </a-col>
      </a-row>

      <a-row :gutter="16" style="margin-top: 16px" v-if="runDetail">
        <!-- Execution Timeline & DAG -->
        <a-col :span="16">
          <!-- DAG Topology View -->
          <a-card title="DAG 拓扑图" :bordered="false" class="page-card">
            <div class="dag-visualization">
              <svg :width="dagSvgWidth" height="300" :viewBox="`0 0 ${dagSvgWidth} 300`">
                <!-- Edges -->
                <line
                  v-for="(edge, idx) in dagEdges"
                  :key="'edge-' + idx"
                  :x1="getNodePos(edge.from).x"
                  :y1="getNodePos(edge.from).y"
                  :x2="getNodePos(edge.to).x"
                  :y2="getNodePos(edge.to).y"
                  :stroke="getEdgeColor(edge)"
                  stroke-width="2"
                  marker-end="url(#dag-arrow)"
                />
                <defs>
                  <marker id="dag-arrow" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
                    <polygon points="0 0, 10 3.5, 0 7" fill="#d9d9d9" />
                  </marker>
                </defs>
                <!-- Nodes -->
                <g
                  v-for="node in dagNodes"
                  :key="node.id"
                  :transform="`translate(${getNodePos(node.id).x}, ${getNodePos(node.id).y})`"
                  class="dag-node"
                  :class="{ 'dag-node-selected': selectedNode?.nodeId === node.id }"
                  @click="selectDagNode(node)"
                >
                  <rect
                    x="-65"
                    y="-28"
                    width="130"
                    height="56"
                    rx="10"
                    :fill="getStatusFill(node.status)"
                    :stroke="getStatusStroke(node.status)"
                    stroke-width="2"
                  />
                  <text x="0" y="-8" text-anchor="middle" fill="#666" font-size="10">
                    {{ nodeTypeLabel[node.nodeType] || node.nodeType }}
                  </text>
                  <text x="0" y="10" text-anchor="middle" fill="#333" font-size="12" font-weight="600">
                    {{ truncateText(node.nodeName, 12) }}
                  </text>
                  <text x="0" y="23" text-anchor="middle" fill="#999" font-size="9">
                    {{ formatDuration(node.duration) }}
                  </text>
                </g>
              </svg>
            </div>
          </a-card>

          <!-- Execution Timeline -->
          <a-card title="执行时间线" :bordered="false" class="page-card" style="margin-top: 16px">
            <div class="timeline-container">
              <div
                v-for="(nodeRun, idx) in runDetail.nodeRuns"
                :key="nodeRun.id"
                class="timeline-node"
                :class="{ 'is-selected': selectedNode?.id === nodeRun.id, 'is-error': nodeRun.status === 'failed' }"
                @click="selectedNode = nodeRun"
              >
                <div class="timeline-connector" v-if="idx > 0">
                  <div class="connector-line"></div>
                </div>
                <div class="timeline-dot" :style="{ background: statusColor[nodeRun.status] || '#d9d9d9' }">
                  <CheckCircleOutlined v-if="nodeRun.status === 'success'" />
                  <CloseCircleOutlined v-else-if="nodeRun.status === 'failed'" />
                  <SyncOutlined v-else-if="nodeRun.status === 'running'" spin />
                  <ClockCircleOutlined v-else />
                </div>
                <div class="timeline-content">
                  <div class="timeline-header">
                    <span class="timeline-title">{{ nodeRun.nodeName }}</span>
                    <a-tag size="small" :color="nodeRun.status === 'success' ? 'green' : nodeRun.status === 'failed' ? 'red' : 'blue'">
                      {{ nodeRun.nodeType }}
                    </a-tag>
                  </div>
                  <div class="timeline-meta">
                    <span><FieldTimeOutlined /> {{ formatDuration(nodeRun.duration) }}</span>
                    <span><ThunderboltOutlined /> {{ nodeRun.tokensUsed }} tokens</span>
                    <span><DollarOutlined /> ${{ (nodeRun.cost || 0).toFixed(4) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </a-card>

          <!-- State Machine -->
          <a-card title="状态快照" :bordered="false" class="page-card" style="margin-top: 16px">
            <a-timeline>
              <a-timeline-item
                v-for="(snap, idx) in runDetail.stateSnapshots"
                :key="idx"
                :color="idx === runDetail.stateSnapshots.length - 1 ? 'blue' : 'gray'"
              >
                <div>
                  <a-tag>{{ snap.state }}</a-tag>
                  <span style="color: #999; font-size: 12px; margin-left: 8px">{{ snap.timestamp }}</span>
                </div>
              </a-timeline-item>
            </a-timeline>
          </a-card>

          <!-- Full Execution Log -->
          <a-card title="完整执行日志" :bordered="false" class="page-card" style="margin-top: 16px">
            <div class="log-viewer">
              <div v-for="(nodeRun, idx) in runDetail.nodeRuns" :key="'log-' + idx" class="log-section">
                <div class="log-header" @click="toggleLog(idx)">
                  <RightOutlined :class="{ 'rotate-90': expandedLogs[idx] }" />
                  <span>{{ nodeRun.nodeName }}</span>
                  <a-tag size="small" :color="nodeRun.status === 'success' ? 'green' : 'red'">{{ nodeRun.status }}</a-tag>
                </div>
                <div v-if="expandedLogs[idx]" class="log-body">
                  <pre>{{ nodeRun.logs || '无日志' }}</pre>
                  <div v-if="nodeRun.error" class="log-error">
                    <CloseCircleOutlined /> {{ nodeRun.error }}
                  </div>
                </div>
              </div>
            </div>
          </a-card>
        </a-col>

        <!-- Node Detail Panel -->
        <a-col :span="8">
          <a-card title="节点详情" :bordered="false" class="page-card" v-if="selectedNode">
            <a-descriptions :column="1" bordered size="small">
              <a-descriptions-item label="节点名称">{{ selectedNode.nodeName }}</a-descriptions-item>
              <a-descriptions-item label="节点类型">{{ selectedNode.nodeType }}</a-descriptions-item>
              <a-descriptions-item label="状态">
                <a-badge :status="getStatusBadge(selectedNode.status)" :text="selectedNode.status" />
              </a-descriptions-item>
              <a-descriptions-item label="开始时间">{{ selectedNode.startedAt || '-' }}</a-descriptions-item>
              <a-descriptions-item label="结束时间">{{ selectedNode.finishedAt || '-' }}</a-descriptions-item>
              <a-descriptions-item label="耗时">{{ formatDuration(selectedNode.duration) }}</a-descriptions-item>
              <a-descriptions-item label="Token用量">{{ selectedNode.tokensUsed }}</a-descriptions-item>
              <a-descriptions-item label="费用">${{ (selectedNode.cost || 0).toFixed(4) }}</a-descriptions-item>
            </a-descriptions>

            <a-divider />

            <a-collapse>
              <a-collapse-panel key="input" header="输入数据">
                <div class="json-viewer">{{ formatJson(selectedNode.input) }}</div>
              </a-collapse-panel>
              <a-collapse-panel key="output" header="输出数据">
                <div class="json-viewer">{{ formatJson(selectedNode.output) }}</div>
              </a-collapse-panel>
              <a-collapse-panel key="logs" header="日志">
                <pre class="log-pre">{{ selectedNode.logs || '无日志' }}</pre>
              </a-collapse-panel>
              <a-collapse-panel v-if="selectedNode.error" key="error" header="错误详情">
                <a-alert type="error" :message="selectedNode.error" show-icon />
              </a-collapse-panel>
            </a-collapse>
          </a-card>

          <a-card title="节点详情" :bordered="false" class="page-card" v-else>
            <a-empty description="点击左侧时间线节点查看详情" />
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  ClockCircleOutlined,
  FieldTimeOutlined,
  ThunderboltOutlined,
  DollarOutlined,
  PlayCircleOutlined,
  RightOutlined,
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { workflowApi, type WorkflowRunDetail, type WorkflowNodeRun } from '@/api/workflow'

const route = useRoute()
const runId = route.params.runId as string

const loading = ref(false)
const resuming = ref(false)
const runDetail = ref<WorkflowRunDetail | null>(null)
const selectedNode = ref<WorkflowNodeRun | null>(null)
const expandedLogs = reactive<Record<number, boolean>>({})

const statusBg: Record<string, string> = {
  success: '#f6ffed',
  failed: '#fff2f0',
  running: '#e6f4ff',
  pending: '#fffbe6',
}

const statusColor: Record<string, string> = {
  success: '#52c41a',
  failed: '#ff4d4f',
  running: '#1677ff',
  pending: '#faad14',
}

const statusLabel: Record<string, string> = {
  success: '成功',
  failed: '失败',
  running: '运行中',
  pending: '等待中',
}

const nodeTypeLabel: Record<string, string> = {
  data_fetch: '数据提取',
  kpi_calc: 'KPI计算',
  ai_analysis: 'AI分析',
  report: '报表生成',
  output: '输出',
}

// DAG visualization
interface DagEdgeData { from: string; to: string }

const dagNodes = computed(() => runDetail.value?.nodeRuns || [])
const dagSvgWidth = computed(() => Math.max(800, dagNodes.value.length * 160))

const dagEdges = computed(() => {
  // Build edges from node dependencies (inferred from execution order or node metadata)
  const nodes = runDetail.value?.nodeRuns || []
  const edges: DagEdgeData[] = []
  for (let i = 1; i < nodes.length; i++) {
    // Simple heuristic: connect sequential nodes; in production, use actual DAG definition
    edges.push({ from: nodes[i - 1].nodeId, to: nodes[i].nodeId })
  }
  return edges
})

const dagNodePositions = computed(() => {
  const nodes = dagNodes.value
  const positions: Record<string, { x: number; y: number }> = {}
  const cols = Math.ceil(Math.sqrt(nodes.length))
  nodes.forEach((node, idx) => {
    const col = idx % cols
    const row = Math.floor(idx / cols)
    positions[node.nodeId] = {
      x: 100 + col * (dagSvgWidth.value - 200) / Math.max(cols - 1, 1),
      y: 80 + row * 140,
    }
  })
  return positions
})

function getNodePos(nodeId: string) {
  return dagNodePositions.value[nodeId] || { x: 400, y: 150 }
}

function getStatusFill(status: string) {
  const map: Record<string, string> = {
    success: '#f6ffed',
    failed: '#fff2f0',
    running: '#e6f4ff',
    pending: '#fffbe6',
  }
  return map[status] || '#fafafa'
}

function getStatusStroke(status: string) {
  return statusColor[status] || '#d9d9d9'
}

function getEdgeColor(edge: DagEdgeData) {
  const toNode = dagNodes.value.find((n) => n.nodeId === edge.to)
  if (toNode?.status === 'success') return '#52c41a'
  if (toNode?.status === 'failed') return '#ff4d4f'
  return '#d9d9d9'
}

function selectDagNode(node: WorkflowNodeRun) {
  selectedNode.value = node
}

function truncateText(str: string, len: number) {
  return str && str.length > len ? str.slice(0, len) + '...' : str
}

function getStatusBadge(status: string) {
  const map: Record<string, string> = {
    success: 'success',
    failed: 'error',
    running: 'processing',
    pending: 'warning',
  }
  return (map[status] || 'default') as 'success' | 'error' | 'processing' | 'warning' | 'default'
}

function formatDuration(ms: number): string {
  if (!ms) return '-'
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${Math.floor(ms / 60000)}m ${Math.round((ms % 60000) / 1000)}s`
}

function formatNumber(n: number): string {
  if (!n) return '0'
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`
  if (n >= 1000) return `${(n / 1000).toFixed(1)}K`
  return n.toString()
}

function formatJson(obj: unknown): string {
  try {
    return JSON.stringify(obj, null, 2)
  } catch {
    return String(obj)
  }
}

function toggleLog(idx: number) {
  expandedLogs[idx] = !expandedLogs[idx]
}

async function handleResume() {
  resuming.value = true
  try {
    await workflowApi.resumeRun(runId)
    message.success('已从失败处恢复运行')
    await loadRunDetail()
  } finally {
    resuming.value = false
  }
}

async function loadRunDetail() {
  loading.value = true
  try {
    const res = await workflowApi.getRunDetail(runId)
    runDetail.value = res.data
    if (res.data.nodeRuns?.length > 0) {
      selectedNode.value = res.data.nodeRuns[0]
    }
  } catch {} finally {
    loading.value = false
  }
}

// Auto-refresh when workflow is running
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null

function startAutoRefresh() {
  if (autoRefreshTimer) clearInterval(autoRefreshTimer)
  autoRefreshTimer = setInterval(async () => {
    if (runDetail.value?.status === 'running' || runDetail.value?.status === 'pending') {
      await loadRunDetail()
    } else {
      stopAutoRefresh()
    }
  }, 3000) // Refresh every 3 seconds when running
}

function stopAutoRefresh() {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
}

onMounted(async () => {
  await loadRunDetail()
  if (runDetail.value?.status === 'running' || runDetail.value?.status === 'pending') {
    startAutoRefresh()
  }
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style lang="scss" scoped>
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    margin: 0 auto 12px;
  }

  .stat-value { font-size: 24px; font-weight: 700; color: #1a1a1a; }
  .stat-label { font-size: 14px; color: #999; margin-top: 4px; }
}

.dag-visualization {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 12px;
}

.dag-node {
  cursor: pointer;
  transition: all 0.2s;

  &:hover rect {
    stroke-width: 3;
  }

  &.dag-node-selected rect {
    stroke-width: 3;
    stroke-dasharray: 5 3;
  }
}

.timeline-container {
  padding: 0 16px;
}

.timeline-node {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;

  &:hover { background: #f5f5f5; }
  &.is-selected { background: #e6f4ff; border: 1px solid #91caff; }
  &.is-error { border-left: 3px solid #ff4d4f; }
}

.timeline-connector {
  position: absolute;
  left: 24px;
  top: -8px;

  .connector-line {
    width: 2px;
    height: 16px;
    background: #d9d9d9;
  }
}

.timeline-dot {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  flex-shrink: 0;
}

.timeline-content {
  flex: 1;

  .timeline-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;

    .timeline-title { font-weight: 600; font-size: 14px; }
  }

  .timeline-meta {
    display: flex;
    gap: 16px;
    font-size: 12px;
    color: #999;

    span { display: flex; align-items: center; gap: 4px; }
  }
}

.log-viewer {
  max-height: 400px;
  overflow-y: auto;

  .log-section { border-bottom: 1px solid #f0f0f0; }

  .log-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px;
    cursor: pointer;

    &:hover { background: #f5f5f5; }
  }

  .log-body {
    padding: 8px 16px;
    background: #fafafa;

    pre {
      font-family: monospace;
      font-size: 12px;
      white-space: pre-wrap;
      margin: 0;
    }

    .log-error {
      margin-top: 8px;
      padding: 8px;
      background: #fff2f0;
      border-radius: 4px;
      color: #ff4d4f;
    }
  }
}

.log-pre {
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
  margin: 0;
  max-height: 300px;
  overflow-y: auto;
}

.json-viewer {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 6px;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
  max-height: 200px;
  overflow-y: auto;
}

.rotate-90 {
  transform: rotate(90deg);
}
</style>
