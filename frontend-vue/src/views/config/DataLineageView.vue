<template>
  <div class="page-container">
    <PageHeader title="数据血缘" subtitle="查看数据从源头到报表的完整链路" />

    <a-row :gutter="16">
      <!-- Controls -->
      <a-col :xs="24" :md="6">
        <a-card title="数据源" :bordered="false" class="page-card" size="small">
          <a-form layout="vertical">
            <a-form-item label="查看模式">
              <a-radio-group v-model:value="viewMode" button-style="solid" size="small">
                <a-radio-button value="graph">关系图</a-radio-button>
                <a-radio-button value="impact">影响分析</a-radio-button>
              </a-radio-group>
            </a-form-item>

            <a-form-item label="选择数据集">
              <a-select
                v-model:value="selectedDatasetId"
                placeholder="选择数据集"
                style="width: 100%"
                allow-clear
                @change="handleDatasetChange"
              >
                <a-select-option v-for="ds in datasets" :key="ds.id" :value="ds.id">
                  {{ ds.name }}
                </a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item label="层级筛选">
              <a-checkbox-group v-model:value="visibleLayers">
                <a-checkbox value="datasource">数据源</a-checkbox>
                <a-checkbox value="dataset">数据集</a-checkbox>
                <a-checkbox value="schema">Schema</a-checkbox>
                <a-checkbox value="kpi">KPI</a-checkbox>
                <a-checkbox value="workflow">工作流</a-checkbox>
                <a-checkbox value="report">报表</a-checkbox>
              </a-checkbox-group>
            </a-form-item>
          </a-form>

          <!-- Legend -->
          <div class="legend">
            <div class="legend-title">图例</div>
            <div v-for="(item, key) in layerColors" :key="key" class="legend-item">
              <span class="legend-dot" :style="{ background: item.color }"></span>
              {{ item.label }}
            </div>
          </div>
        </a-card>

        <!-- Impact Analysis Panel -->
        <a-card v-if="impactAnalysis" title="影响分析" :bordered="false" class="page-card" size="small" style="margin-top: 16px">
          <a-alert :type="impactAnalysis.level === 'high' ? 'error' : impactAnalysis.level === 'medium' ? 'warning' : 'info'" show-icon style="margin-bottom: 12px">
            <template #message>
              影响范围：{{ impactAnalysis.affectedCount }} 个对象
            </template>
          </a-alert>
          <div v-for="(item, idx) in impactAnalysis.items" :key="idx" class="impact-item">
            <a-tag :color="layerColors[item.type]?.color || 'default'">{{ item.type }}</a-tag>
            <span>{{ item.name }}</span>
          </div>
        </a-card>
      </a-col>

      <!-- Graph Area -->
      <a-col :xs="24" :md="18">
        <a-card :bordered="false" class="page-card">
          <template #title>
            <a-space>
              <span>{{ viewMode === 'graph' ? '血缘关系图' : '影响分析图' }}</span>
              <a-tag v-if="graphNodes.length">{{ graphNodes.length }} 个节点</a-tag>
              <a-tag v-if="graphEdges.length">{{ graphEdges.length }} 条关系</a-tag>
            </a-space>
          </template>
          <template #extra>
            <a-space>
              <a-button size="small" @click="resetZoom"><CompressOutlined /> 重置视图</a-button>
              <a-button size="small" @click="exportGraph"><DownloadOutlined /> 导出</a-button>
            </a-space>
          </template>
          <div ref="graphContainer" class="graph-container"></div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { CompressOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { get } from '@/api/request'
import { datasetApi } from '@/api/dataset'
import * as echarts from 'echarts'

const graphContainer = ref<HTMLElement>()
const viewMode = ref<'graph' | 'impact'>('graph')
const selectedDatasetId = ref<string>('')
const visibleLayers = ref(['datasource', 'dataset', 'schema', 'kpi', 'workflow', 'report'])
const datasets = ref<Array<{ id: string; name: string }>>([])

interface GraphNode {
  id: string
  name: string
  type: string
  x?: number
  y?: number
}

interface GraphEdge {
  source: string
  target: string
  type: string
}

const graphNodes = ref<GraphNode[]>([])
const graphEdges = ref<GraphEdge[]>([])
const impactAnalysis = ref<{ level: string; affectedCount: number; items: Array<{ type: string; name: string }> } | null>(null)

let chartInstance: echarts.ECharts | null = null

const layerColors: Record<string, { label: string; color: string }> = {
  datasource: { label: '数据源', color: '#1677ff' },
  dataset: { label: '数据集', color: '#52c41a' },
  schema: { label: 'Schema', color: '#722ed1' },
  kpi: { label: 'KPI', color: '#fa8c16' },
  workflow: { label: '工作流', color: '#13c2c2' },
  report: { label: '报表', color: '#eb2f96' },
}

async function loadGraph() {
  try {
    const res = await get<{ data: { nodes: any[]; edges: any[] } }>('/config/dependency-graph')
    if (res?.data) {
      graphNodes.value = res.data.nodes || []
      graphEdges.value = res.data.edges || []
      await nextTick()
      renderGraph()
    }
  } catch {
    // Use sample data for demo
    graphNodes.value = [
      { id: 'ds_1', name: 'MySQL主库', type: 'datasource' },
      { id: 'ds_2', name: 'ClickHouse', type: 'datasource' },
      { id: 'dataset_1', name: '订单数据集', type: 'dataset' },
      { id: 'dataset_2', name: '用户数据集', type: 'dataset' },
      { id: 'schema_1', name: 'orders_schema', type: 'schema' },
      { id: 'schema_2', name: 'users_schema', type: 'schema' },
      { id: 'kpi_1', name: 'GMV', type: 'kpi' },
      { id: 'kpi_2', name: 'DAU', type: 'kpi' },
      { id: 'kpi_3', name: '转化率', type: 'kpi' },
      { id: 'wf_1', name: '每日KPI计算', type: 'workflow' },
      { id: 'wf_2', name: '月度报表生成', type: 'workflow' },
      { id: 'rpt_1', name: '月度经营报告', type: 'report' },
      { id: 'rpt_2', name: '实时看板', type: 'report' },
    ]
    graphEdges.value = [
      { source: 'ds_1', target: 'dataset_1', type: 'reference' },
      { source: 'ds_1', target: 'dataset_2', type: 'reference' },
      { source: 'ds_2', target: 'dataset_1', type: 'reference' },
      { source: 'dataset_1', target: 'schema_1', type: 'reference' },
      { source: 'dataset_2', target: 'schema_2', type: 'reference' },
      { source: 'schema_1', target: 'kpi_1', type: 'reference' },
      { source: 'schema_1', target: 'kpi_3', type: 'reference' },
      { source: 'schema_2', target: 'kpi_2', type: 'reference' },
      { source: 'kpi_1', target: 'wf_1', type: 'reference' },
      { source: 'kpi_2', target: 'wf_1', type: 'reference' },
      { source: 'kpi_3', target: 'wf_1', type: 'reference' },
      { source: 'wf_1', target: 'wf_2', type: 'reference' },
      { source: 'wf_2', target: 'rpt_1', type: 'reference' },
      { source: 'wf_1', target: 'rpt_2', type: 'reference' },
    ]
    await nextTick()
    renderGraph()
  }
}

function renderGraph() {
  if (!graphContainer.value) return
  if (chartInstance) chartInstance.dispose()

  chartInstance = echarts.init(graphContainer.value)

  const filteredNodes = graphNodes.value.filter(n => visibleLayers.value.includes(n.type))
  const filteredNodeIds = new Set(filteredNodes.map(n => n.id))
  const filteredEdges = graphEdges.value.filter(e => filteredNodeIds.has(e.source) && filteredNodeIds.has(e.target))

  const option: echarts.EChartsOption = {
    tooltip: {
      formatter: (params: any) => {
        if (params.dataType === 'node') {
          return `<strong>${params.data.name}</strong><br/>类型: ${layerColors[params.data.type]?.label || params.data.type}`
        }
        return `${params.data.source} → ${params.data.target}`
      },
    },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      label: { show: true, fontSize: 12, position: 'right' },
      force: { repulsion: 300, edgeLength: [100, 200], gravity: 0.1 },
      data: filteredNodes.map(n => ({
        id: n.id,
        name: n.name,
        type: n.type,
        symbolSize: n.type === 'datasource' ? 50 : n.type === 'report' ? 45 : 35,
        itemStyle: { color: layerColors[n.type]?.color || '#999' },
        category: n.type,
      })),
      links: filteredEdges.map(e => ({
        source: e.source,
        target: e.target,
        lineStyle: { color: '#ccc', width: 2, curveness: 0.1 },
      })),
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 4 },
      },
    }],
  }

  chartInstance.setOption(option)

  // Click handler for impact analysis
  chartInstance.on('click', (params: any) => {
    if (params.dataType === 'node') {
      performImpactAnalysis(params.data.id, params.data.type)
    }
  })
}

function performImpactAnalysis(nodeId: string, nodeType: string) {
  // Find all downstream nodes
  const affected: Array<{ type: string; name: string }> = []
  const visited = new Set<string>()
  const queue = [nodeId]

  while (queue.length > 0) {
    const current = queue.shift()!
    if (visited.has(current)) continue
    visited.add(current)

    const edges = graphEdges.value.filter(e => e.source === current)
    for (const edge of edges) {
      const targetNode = graphNodes.value.find(n => n.id === edge.target)
      if (targetNode && !visited.has(edge.target)) {
        affected.push({ type: targetNode.type, name: targetNode.name })
        queue.push(edge.target)
      }
    }
  }

  const level = affected.length >= 5 ? 'high' : affected.length >= 2 ? 'medium' : 'low'

  impactAnalysis.value = { level, affectedCount: affected.length, items: affected }
}

function resetZoom() {
  chartInstance?.dispatchAction({ type: 'restore' })
}

function exportGraph() {
  if (!chartInstance) return
  const url = chartInstance.getDataURL({ type: 'png', pixelRatio: 2 })
  const link = document.createElement('a')
  link.href = url
  link.download = 'data_lineage_' + new Date().toISOString().slice(0, 10) + '.png'
  link.click()
  message.success('图表已导出')
}

async function handleDatasetChange(dsId: string) {
  if (!dsId) {
    impactAnalysis.value = null
    return
  }
  performImpactAnalysis(`dataset_${dsId}`, 'dataset')
}

function handleResize() {
  chartInstance?.resize()
}

watch(visibleLayers, () => { renderGraph() }, { deep: true })

onMounted(async () => {
  loadGraph()
  window.addEventListener('resize', handleResize)
  try {
    const res = await datasetApi.list({ page: 1, size: 100 })
    datasets.value = res.data.items
  } catch {}
})

onUnmounted(() => {
  chartInstance?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.graph-container {
  height: 600px;
  width: 100%;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.legend {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;

  .legend-title {
    font-weight: 600;
    font-size: 13px;
    margin-bottom: 8px;
  }

  .legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    margin-bottom: 4px;
    color: #666;
  }

  .legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
  }
}

.impact-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
}
</style>
