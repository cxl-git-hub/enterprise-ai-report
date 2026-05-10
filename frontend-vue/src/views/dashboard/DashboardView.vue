<template>
  <div class="page-container">
    <PageHeader title="仪表盘" subtitle="系统运行概览" />

    <a-row :gutter="16" class="stat-row">
      <a-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #e6f4ff; color: #1677ff">
            <DatabaseOutlined />
          </div>
          <div class="stat-value">{{ stats.datasourceCount }}</div>
          <div class="stat-label">数据源</div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #f6ffed; color: #52c41a">
            <FundOutlined />
          </div>
          <div class="stat-value">{{ stats.kpiCount }}</div>
          <div class="stat-label">KPI指标</div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #fff7e6; color: #fa8c16">
            <ApartmentOutlined />
          </div>
          <div class="stat-value">{{ stats.workflowCount }}</div>
          <div class="stat-label">工作流</div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #fff1f0; color: #ff4d4f">
            <FileTextOutlined />
          </div>
          <div class="stat-value">{{ stats.reportCount }}</div>
          <div class="stat-label">报表输出</div>
        </div>
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <a-col :span="16">
        <a-card title="最近工作流运行" :bordered="false" class="page-card">
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
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="KPI执行概览" :bordered="false" class="page-card">
          <div ref="kpiChartRef" style="height: 300px"></div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  DatabaseOutlined,
  FundOutlined,
  ApartmentOutlined,
  FileTextOutlined,
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
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
  }
  return (map[status] || 'default') as 'success' | 'error' | 'processing' | 'warning' | 'default'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    success: '成功',
    failed: '失败',
    running: '运行中',
    pending: '等待中',
  }
  return map[status] || status
}

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${Math.floor(ms / 60000)}m ${Math.round((ms % 60000) / 1000)}s`
}

function initChart() {
  if (!kpiChartRef.value) return
  const chart = echarts.init(kpiChartRef.value)
  chart.setOption({
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
        data: [
          { value: 735, name: '成功', itemStyle: { color: '#52c41a' } },
          { value: 58, name: '失败', itemStyle: { color: '#ff4d4f' } },
          { value: 48, name: '运行中', itemStyle: { color: '#1677ff' } },
        ],
      },
    ],
  })
}

onMounted(() => {
  // Simulated data - in production, fetch from API
  stats.datasourceCount = 12
  stats.kpiCount = 45
  stats.workflowCount = 8
  stats.reportCount = 156

  recentRuns.value = [
    { id: '1', workflowName: '日报生成', status: 'success', startedAt: '2024-03-10 09:00', duration: 45000 },
    { id: '2', workflowName: '周报汇总', status: 'running', startedAt: '2024-03-10 08:30', duration: 120000 },
    { id: '3', workflowName: 'KPI计算', status: 'failed', startedAt: '2024-03-10 08:00', duration: 30000 },
    { id: '4', workflowName: '数据同步', status: 'success', startedAt: '2024-03-10 07:00', duration: 180000 },
    { id: '5', workflowName: '月度报表', status: 'pending', startedAt: '2024-03-10 06:00', duration: 0 },
  ]

  initChart()
})
</script>

<style lang="scss" scoped>
.stat-row {
  margin-bottom: 24px;
}

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

  .stat-value {
    font-size: 28px;
    font-weight: 700;
    color: #1a1a1a;
  }

  .stat-label {
    font-size: 14px;
    color: #999;
    margin-top: 4px;
  }
}
</style>
