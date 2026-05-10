<template>
  <div class="page-container">
    <PageHeader title="AI分析" subtitle="AI驱动的数据分析" />

    <a-row :gutter="16">
      <a-col :xs="24" :md="8">
        <a-card title="分析配置" :bordered="false" class="page-card">
          <a-form layout="vertical">
            <a-form-item label="选择数据集">
              <a-select v-model:value="selectedDatasetId" placeholder="请选择数据集">
                <a-select-option v-for="ds in datasets" :key="ds.id" :value="ds.id">
                  {{ ds.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="分析类型">
              <a-select v-model:value="analysisType" placeholder="请选择分析类型">
                <a-select-option value="trend">趋势分析</a-select-option>
                <a-select-option value="anomaly">异常检测</a-select-option>
                <a-select-option value="correlation">相关性分析</a-select-option>
                <a-select-option value="summary">数据摘要</a-select-option>
                <a-select-option value="forecast">预测分析</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="分析说明">
              <a-textarea
                v-model:value="analysisPrompt"
                :rows="4"
                placeholder="描述你想要分析的内容，例如：分析最近3个月的销售趋势"
              />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" block @click="handleAnalyze" :loading="analyzing">
                <ThunderboltOutlined /> 开始分析
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <a-col :xs="24" :md="16">
        <a-card title="分析结果" :bordered="false" class="page-card">
          <div v-if="analyzing" class="analyzing-state">
            <a-spin size="large" />
            <p>AI正在分析数据，请稍候...</p>
          </div>
          <div v-else-if="analysisResult">
            <!-- Narrative -->
            <div class="analysis-narrative">
              <h3>分析摘要</h3>
              <div class="narrative-content" v-html="analysisResult.narrative"></div>
            </div>

            <!-- Chart -->
            <div v-if="analysisResult.chartData" class="analysis-chart">
              <h3>数据可视化</h3>
              <div ref="chartRef" style="height: 400px"></div>
            </div>

            <!-- Key Findings -->
            <div v-if="analysisResult.findings?.length" class="analysis-findings">
              <h3>关键发现</h3>
              <a-list :data-source="analysisResult.findings" :split="false">
                <template #renderItem="{ item }">
                  <a-list-item>
                    <a-list-item-meta>
                      <template #avatar>
                        <a-avatar :style="{ background: item.type === 'positive' ? '#52c41a' : item.type === 'negative' ? '#ff4d4f' : '#1677ff' }">
                          {{ item.type === 'positive' ? '↑' : item.type === 'negative' ? '↓' : '→' }}
                        </a-avatar>
                      </template>
                      <template #title>{{ item.title }}</template>
                      <template #description>{{ item.description }}</template>
                    </a-list-item-meta>
                  </a-list-item>
                </template>
              </a-list>
            </div>

            <!-- Trace -->
            <a-collapse style="margin-top: 16px">
              <a-collapse-panel key="trace" header="AI执行详情">
                <a-descriptions :column="3" bordered size="small">
                  <a-descriptions-item label="模型">{{ analysisResult.trace?.modelName }}</a-descriptions-item>
                  <a-descriptions-item label="总Tokens">{{ analysisResult.trace?.totalTokens }}</a-descriptions-item>
                  <a-descriptions-item label="耗时">{{ analysisResult.trace?.duration }}ms</a-descriptions-item>
                  <a-descriptions-item label="费用">${{ (analysisResult.trace?.cost || 0).toFixed(4) }}</a-descriptions-item>
                </a-descriptions>
              </a-collapse-panel>
            </a-collapse>
          </div>
          <a-empty v-else description="配置分析参数后点击「开始分析」" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { ThunderboltOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { datasetApi, type Dataset } from '@/api/dataset'
import { post } from '@/api/request'
import * as echarts from 'echarts'

const datasets = ref<Dataset[]>([])
const selectedDatasetId = ref<string>('')
const analysisType = ref<string>('trend')
const analysisPrompt = ref('')
const analyzing = ref(false)
const chartRef = ref<HTMLElement>()

interface Finding {
  type: 'positive' | 'negative' | 'neutral'
  title: string
  description: string
}

interface AnalysisResult {
  narrative: string
  chartData?: { type: string; categories: string[]; series: Array<{ name: string; data: number[] }> }
  findings?: Finding[]
  trace?: { modelName: string; totalTokens: number; duration: number; cost: number }
}

const analysisResult = ref<AnalysisResult | null>(null)

async function handleAnalyze() {
  if (!selectedDatasetId.value) {
    message.warning('请选择数据集')
    return
  }
  analyzing.value = true
  analysisResult.value = null
  try {
    const res = await post<{ data: AnalysisResult }>('/ai/analysis', {
      datasetId: selectedDatasetId.value,
      type: analysisType.value,
      prompt: analysisPrompt.value,
    })
    analysisResult.value = res.data
    await nextTick()
    if (res.data.chartData) {
      renderChart(res.data.chartData)
    }
  } finally {
    analyzing.value = false
  }
}

function renderChart(chartData: AnalysisResult['chartData']) {
  if (!chartRef.value || !chartData) return
  const chart = echarts.init(chartRef.value)

  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: chartData.categories },
    yAxis: { type: 'value' },
    series: chartData.series.map((s) => ({
      name: s.name,
      type: (chartData.type || 'line') as 'line' | 'bar',
      data: s.data,
      smooth: true,
    })),
  }
  chart.setOption(option)
}

onMounted(async () => {
  try {
    const res = await datasetApi.list({ page: 1, pageSize: 100 })
    datasets.value = res.data.items
  } catch {}
})
</script>

<style lang="scss" scoped>
.analyzing-state {
  text-align: center;
  padding: 60px 0;

  p { margin-top: 16px; color: #999; }
}

.analysis-narrative {
  margin-bottom: 24px;

  h3 { margin-bottom: 12px; font-size: 16px; }
  .narrative-content {
    font-size: 14px;
    line-height: 1.8;
    color: #333;
  }
}

.analysis-chart {
  margin-bottom: 24px;

  h3 { margin-bottom: 12px; font-size: 16px; }
}

.analysis-findings {
  h3 { margin-bottom: 12px; font-size: 16px; }
}
</style>
