<template>
  <div class="page-container">
    <PageHeader title="仪表盘" subtitle="系统运行概览">
      <template #actions>
        <a-space>
          <a-switch v-model:checked="autoRefresh" checked-children="自动刷新" un-checked-children="手动" />
          <a-button @click="refreshAll" :loading="statsLoading || runsLoading">
            <ReloadOutlined /> 刷新
          </a-button>
        </a-space>
      </template>
    </PageHeader>

    <a-row :gutter="16" class="stat-row">
      <a-col :xs="12" :sm="12" :md="6" v-for="(item, idx) in statItems" :key="idx">
        <div class="stat-card" :style="{ '--accent': item.color, '--accent-bg': item.bg }">
          <a-spin :spinning="statsLoading">
            <div class="stat-icon" :style="{ background: item.bg, color: item.color }">
              <component :is="item.icon" />
            </div>
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-accent-bar" :style="{ background: item.color }"></div>
          </a-spin>
        </div>
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <a-col :xs="24" :md="16">
        <a-card title="最近工作流运行" :bordered="false" class="page-card">
          <a-spin :spinning="runsLoading">
            <a-table
              :columns="runColumns"
              :data-source="recentRuns"
              :pagination="false"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-badge
                    :status="getStatusBadge(record.status)"
                    :text="getStatusText(record.status)"
                  />
                </template>
                <template v-if="column.key === 'duration'">
                  {{ formatDuration(record.duration) }}
                </template>
                <template v-if="column.key === 'action'">
                  <router-link :to="`/workflow/run/${record.id}`">查看</router-link>
                </template>
              </template>
            </a-table>
          </a-spin>
        </a-card>
      </a-col>
      <a-col :xs="24" :md="8">
        <a-card title="运行状态分布" :bordered="false" class="page-card">
          <a-spin :spinning="chartLoading">
            <div ref="kpiChartRef" style="height: 300px"></div>
          </a-spin>
        </a-card>
      </a-col>
    </a-row>

    <!-- AI Usage Stats -->
    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :xs="24">
        <a-card title="AI用量统计" :bordered="false" class="page-card">
          <a-spin :spinning="aiStatsLoading">
            <a-row :gutter="16">
              <a-col :xs="12" :sm="12" :md="6">
                <a-statistic title="AI调用次数" :value="aiStats.totalCalls" :value-style="{ color: '#1677ff' }">
                  <template #prefix><ThunderboltOutlined /></template>
                </a-statistic>
              </a-col>
              <a-col :xs="12" :sm="12" :md="6">
                <a-statistic title="总Token消耗" :value="formatTokens(aiStats.totalTokens)" :value-style="{ color: '#52c41a' }" />
              </a-col>
              <a-col :xs="12" :sm="12" :md="6">
                <a-statistic title="总费用" :value="`$${aiStats.totalCost}`" :value-style="{ color: '#fa8c16' }" />
              </a-col>
              <a-col :xs="12" :sm="12" :md="6">
                <a-statistic title="成功率" :value="`${aiStats.successRate}%`" :value-style="{ color: aiStats.successRate >= 90 ? '#52c41a' : '#ff4d4f' }" />
              </a-col>
            </a-row>
          </a-spin>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed, markRaw, watch } from 'vue'
import {
  DatabaseOutlined,
  FundOutlined,
  ApartmentOutlined,
  FileTextOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { get } from '@/api/request'
import * as echarts from 'echarts'

const stats = reactive({
  datasourceCount: 0,
  kpiCount: 0,
  workflowCount: 0,
  reportCount: 0,
})

interface RecentRun {
  id: string
  workflowName: string
  status: string
  startedAt: string
  duration: number
}

const recentRuns = ref<RecentRun[]>([])
const kpiChartRef = ref<HTMLElement>()
const statsLoading = ref(true)
const runsLoading = ref(true)
const chartLoading = ref(true)
const aiStatsLoading = ref(true)
let chartInstance: echarts.ECharts | null = null

const aiStats = reactive({
  totalCalls: 0,
  totalTokens: 0,
  totalCost: 0,
  successRate: 0,
})

const statItems = computed(() => [
  { icon: markRaw(DatabaseOutlined), value: stats.datasourceCount, label: '数据源', bg: '#e6f4ff', color: '#1677ff' },
  { icon: markRaw(FundOutlined), value: stats.kpiCount, label: 'KPI指标', bg: '#f6ffed', color: '#52c41a' },
  { icon: markRaw(ApartmentOutlined), value: stats.workflowCount, label: '工作流', bg: '#fff7e6', color: '#fa8c16' },
  { icon: markRaw(FileTextOutlined), value: stats.reportCount, label: '报表输出', bg: '#fff1f0', color: '#ff4d4f' },
])

const runColumns = [
  { title: '工作流', dataIndex: 'workflowName', key: 'workflowName' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '开始时间', dataIndex: 'startedAt', key: 'startedAt' },
  { title: '耗时', dataIndex: 'duration', key: 'duration' },
  { title: '操作', key: 'action' },
]

function getStatusBadge(status: string) {
  const map: Record<string, string> = {
    success: 'success',
    failed: 'error',
    running: 'processing',
    pending: 'warning',
    cancelled: 'default',
    retrying: 'warning',
  }
  return (map[status] || 'default') as 'success' | 'error' | 'processing' | 'warning' | 'default'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    success: '成功',
    failed: '失败',
    running: '运行中',
    pending: '等待中',
    cancelled: '已取消',
    retrying: '重试中',
  }
  return map[status] || status
}

function formatDuration(ms: number): string {
  if (!ms) return '-'
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${Math.floor(ms / 60000)}m ${Math.round((ms % 60000) / 1000)}s`
}

function formatTokens(n: number): string {
  if (!n) return '0'
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`
  if (n >= 1000) return `${(n / 1000).toFixed(1)}K`
  return n.toString()
}

async function loadStats() {
  statsLoading.value = true
  try {
    const res = await get<{ data: Record<string, number> }>('/dashboard/stats')
    if (res?.data) {
      stats.datasourceCount = res.data.datasourceCount ?? 0
      stats.kpiCount = res.data.kpiCount ?? 0
      stats.workflowCount = res.data.workflowCount ?? 0
      stats.reportCount = res.data.reportCount ?? 0
    }
  } catch {
    // Show 0 on error
  } finally {
    statsLoading.value = false
  }
}

async function loadRecentRuns() {
  runsLoading.value = true
  try {
    const res = await get<{ data: RecentRun[] }>('/dashboard/recent-runs', { limit: 5 })
    recentRuns.value = res?.data ?? []
  } catch {
    recentRuns.value = []
  } finally {
    runsLoading.value = false
  }
}

async function loadKpiChart() {
  chartLoading.value = true
  try {
    const res = await get<{ data: Array<{ status: string; count: number }> }>('/dashboard/run-status-distribution')
    const statusData = res?.data ?? []
    const statusConfig: Record<string, { name: string; color: string }> = {
      success: { name: '成功', color: '#52c41a' },
      failed: { name: '失败', color: '#ff4d4f' },
      running: { name: '运行中', color: '#1677ff' },
      pending: { name: '等待中', color: '#faad14' },
    }
    const chartData = statusData.map((item) => {
      const cfg = statusConfig[item.status] || { name: item.status, color: '#999' }
      return { value: item.count, name: cfg.name, color: cfg.color }
    })
    if (chartData.length === 0) {
      chartData.push({ value: 0, name: '暂无数据', color: '#d9d9d9' })
    }
    initChart(chartData)
  } catch {
    initChart([{ value: 0, name: '暂无数据', color: '#d9d9d9' }])
  } finally {
    chartLoading.value = false
  }
}

async function loadAiStats() {
  aiStatsLoading.value = true
  try {
    const res = await get<{ data: { totalCalls: number; totalTokens: number; totalCost: number; successRate: number } }>('/dashboard/ai-stats')
    if (res?.data) {
      aiStats.totalCalls = res.data.totalCalls ?? 0
      aiStats.totalTokens = res.data.totalTokens ?? 0
      aiStats.totalCost = res.data.totalCost ?? 0
      aiStats.successRate = res.data.successRate ?? 0
    }
  } catch {
    // Show 0 on error
  } finally {
    aiStatsLoading.value = false
  }
}

function initChart(data: Array<{ value: number; name: string; color: string }>) {
  if (!kpiChartRef.value) return
  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(kpiChartRef.value)
  chartInstance.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        data: data.map((d) => ({
          value: d.value,
          name: d.name,
          itemStyle: { color: d.color },
        })),
      },
    ],
  })
}

function refreshAll() {
  loadStats()
  loadRecentRuns()
  loadKpiChart()
  loadAiStats()
}

const autoRefresh = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null

function handleResize() {
  chartInstance?.resize()
}

watch(autoRefresh, (val) => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (val) {
    refreshTimer = setInterval(refreshAll, 30000)
  }
})

onMounted(() => {
  refreshAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  chartInstance?.dispose()
  chartInstance = null
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.stat-row {
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  min-height: 120px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f0f0;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
    border-color: var(--accent, #1677ff);
  }

  .stat-accent-bar {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 3px;
    opacity: 0.6;
    transition: opacity 0.3s;
  }

  &:hover .stat-accent-bar {
    opacity: 1;
  }

  :deep(.ant-spin-nested-loading) {
    min-height: auto;
  }

  :deep(.ant-spin-container) {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    margin: 0 auto 12px;
    transition: transform 0.3s;
  }

  &:hover .stat-icon {
    transform: scale(1.1);
  }

  .stat-value {
    font-size: 28px;
    font-weight: 700;
    color: #1a1a1a;
    font-variant-numeric: tabular-nums;
  }

  .stat-label {
    font-size: 14px;
    color: #999;
    margin-top: 4px;
  }
}

// Mobile responsive
@media (max-width: 768px) {
  .stat-card {
    padding: 12px;
    min-height: 90px;

    .stat-icon {
      width: 36px;
      height: 36px;
      font-size: 18px;
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 20px;
    }

    .stat-label {
      font-size: 12px;
    }
  }
}
</style>
