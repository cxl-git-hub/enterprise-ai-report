<template>
  <div class="report-chart-builder">
    <div class="chart-toolbar">
      <a-space>
        <a-select v-model:value="chartType" style="width: 120px" size="small">
          <a-select-option value="line">折线图</a-select-option>
          <a-select-option value="bar">柱状图</a-select-option>
          <a-select-option value="pie">饼图</a-select-option>
          <a-select-option value="scatter">散点图</a-select-option>
          <a-select-option value="area">面积图</a-select-option>
          <a-select-option value="radar">雷达图</a-select-option>
        </a-select>
        <a-input v-model:value="chartTitle" placeholder="图表标题" size="small" style="width: 200px" />
        <a-button size="small" @click="refreshChart"><ReloadOutlined /></a-button>
        <a-button size="small" @click="exportChart"><DownloadOutlined /></a-button>
      </a-space>
    </div>
    <div ref="chartRef" class="chart-container" :style="{ height: height + 'px' }"></div>
    <div v-if="showData" class="chart-data-preview">
      <a-table :columns="dataColumns" :data-source="tableData" :pagination="false" size="small" :scroll="{ y: 200 }" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ReloadOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import * as echarts from 'echarts'

const props = withDefaults(defineProps<{
  data?: {
    type?: string
    title?: string
    categories?: string[]
    series?: Array<{ name: string; data: number[]; type?: string }>
  }
  height?: number
  showData?: boolean
}>(), {
  height: 400,
  showData: false,
})

const chartRef = ref<HTMLElement>()
const chartType = ref(props.data?.type || 'line')
const chartTitle = ref(props.data?.title || '')
let chartInstance: echarts.ECharts | null = null

const dataColumns = ref<Array<{ title: string; dataIndex: string; key: string }>>([])
const tableData = ref<Record<string, unknown>[]>([])

function renderChart() {
  if (!chartRef.value || !props.data) return

  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)

  const categories = props.data.categories || []
  const series = props.data.series || []

  let option: echarts.EChartsOption

  if (chartType.value === 'pie') {
    const pieData = series.length > 0
      ? categories.map((cat, i) => ({ name: cat, value: series[0]?.data[i] || 0 }))
      : []
    option = {
      title: chartTitle.value ? { text: chartTitle.value, left: 'center' } : undefined,
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['30%', '70%'],
        data: pieData,
        emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } },
      }],
    }
  } else if (chartType.value === 'radar') {
    option = {
      title: chartTitle.value ? { text: chartTitle.value, left: 'center' } : undefined,
      tooltip: {},
      radar: { indicator: categories.map(c => ({ name: c, max: 100 })) },
      series: [{
        type: 'radar',
        data: series.map(s => ({ value: s.data, name: s.name })),
      }],
    }
  } else {
    option = {
      title: chartTitle.value ? { text: chartTitle.value, left: 'center' } : undefined,
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
      xAxis: { type: 'category', data: categories },
      yAxis: { type: 'value' },
      series: series.map(s => ({
        name: s.name,
        type: (s.type || chartType.value) as any,
        data: s.data,
        smooth: chartType.value === 'line' || chartType.value === 'area',
        areaStyle: chartType.value === 'area' ? {} : undefined,
      })),
    }
  }

  chartInstance.setOption(option, true)

  // Build table data
  dataColumns.value = [
    { title: '类别', dataIndex: '_category', key: '_category' },
    ...series.map(s => ({ title: s.name, dataIndex: s.name, key: s.name })),
  ]
  tableData.value = categories.map((cat, i) => {
    const row: Record<string, unknown> = { _category: cat }
    series.forEach(s => { row[s.name] = s.data[i] })
    return row
  })
}

function refreshChart() {
  renderChart()
}

function exportChart() {
  if (!chartInstance) return
  const url = chartInstance.getDataURL({ type: 'png', pixelRatio: 2 })
  const link = document.createElement('a')
  link.href = url
  link.download = (chartTitle.value || 'chart') + '.png'
  link.click()
}

function handleResize() {
  chartInstance?.resize()
}

watch(() => props.data, () => { nextTick(renderChart) }, { deep: true })
watch(chartType, () => { renderChart() })

onMounted(() => {
  renderChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  chartInstance?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.report-chart-builder {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.chart-toolbar {
  padding: 8px 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
}

.chart-container {
  width: 100%;
}

.chart-data-preview {
  border-top: 1px solid #f0f0f0;
  padding: 8px;
}
</style>
