<template>
  <div class="page-container">
    <PageHeader
      title="AI追踪详情"
      :subtitle="`追踪ID: ${traceId}`"
      :breadcrumbs="[
        { title: 'AI执行追踪', path: '/ai/traces' },
        { title: '追踪详情' },
      ]"
    />

    <a-spin :spinning="loading">
      <a-row :gutter="16" v-if="traceDetail">
        <!-- Summary -->
        <a-col :span="24">
          <a-card :bordered="false" class="page-card">
            <a-row :gutter="16">
              <a-col :span="4">
                <div class="stat-item">
                  <div class="stat-label">追踪类型</div>
                  <a-tag :color="typeColor[traceDetail.traceType] || 'default'" style="font-size: 14px">
                    {{ typeLabel[traceDetail.traceType] || traceDetail.traceType }}
                  </a-tag>
                </div>
              </a-col>
              <a-col :span="4">
                <div class="stat-item">
                  <div class="stat-label">模型</div>
                  <div class="stat-value">{{ traceDetail.modelName }}</div>
                </div>
              </a-col>
              <a-col :span="3">
                <div class="stat-item">
                  <div class="stat-label">状态</div>
                  <a-badge
                    :status="traceDetail.status === 'success' ? 'success' : 'error'"
                    :text="traceDetail.status === 'success' ? '成功' : '失败'"
                  />
                </div>
              </a-col>
              <a-col :span="3">
                <div class="stat-item">
                  <div class="stat-label">Prompt Tokens</div>
                  <div class="stat-value">{{ traceDetail.promptTokens?.toLocaleString() }}</div>
                </div>
              </a-col>
              <a-col :span="3">
                <div class="stat-item">
                  <div class="stat-label">Completion Tokens</div>
                  <div class="stat-value">{{ traceDetail.completionTokens?.toLocaleString() }}</div>
                </div>
              </a-col>
              <a-col :span="3">
                <div class="stat-item">
                  <div class="stat-label">总Tokens</div>
                  <div class="stat-value" style="color: #1677ff">{{ traceDetail.totalTokens?.toLocaleString() }}</div>
                </div>
              </a-col>
              <a-col :span="2">
                <div class="stat-item">
                  <div class="stat-label">费用</div>
                  <div class="stat-value" style="color: #fa8c16">${{ (traceDetail.cost || 0).toFixed(4) }}</div>
                </div>
              </a-col>
              <a-col :span="2">
                <div class="stat-item">
                  <div class="stat-label">耗时</div>
                  <div class="stat-value">{{ traceDetail.duration }}ms</div>
                </div>
              </a-col>
            </a-row>
          </a-card>
        </a-col>

        <a-col :span="16">
          <!-- Full Prompt -->
          <a-card title="完整Prompt" :bordered="false" class="page-card">
            <div class="prompt-display">
              <pre>{{ traceDetail.fullPrompt }}</pre>
            </div>
          </a-card>

          <!-- Raw Output vs Validated Output -->
          <a-card title="输出对比" :bordered="false" class="page-card" style="margin-top: 16px">
            <a-tabs>
              <a-tab-pane key="raw" tab="原始输出">
                <div class="output-display">
                  <pre>{{ traceDetail.rawOutput }}</pre>
                </div>
              </a-tab-pane>
              <a-tab-pane key="validated" tab="验证后输出">
                <div class="output-display validated">
                  <pre>{{ traceDetail.validatedOutput || '无验证输出' }}</pre>
                </div>
              </a-tab-pane>
            </a-tabs>
          </a-card>

          <!-- Validation Errors -->
          <a-card
            v-if="traceDetail.validationErrors?.length"
            title="验证错误"
            :bordered="false"
            class="page-card"
            style="margin-top: 16px"
          >
            <a-list :data-source="traceDetail.validationErrors" :split="false">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-alert type="error" :message="item" show-icon style="width: 100%" />
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </a-col>

        <a-col :span="8">
          <!-- Cost Breakdown -->
          <a-card title="费用明细" :bordered="false" class="page-card">
            <div ref="costChartRef" style="height: 200px"></div>
            <a-descriptions :column="1" size="small" style="margin-top: 12px">
              <a-descriptions-item label="Prompt费用">
                ${{ ((traceDetail.promptTokens || 0) * 0.00001).toFixed(6) }}
              </a-descriptions-item>
              <a-descriptions-item label="Completion费用">
                ${{ ((traceDetail.completionTokens || 0) * 0.00003).toFixed(6) }}
              </a-descriptions-item>
              <a-descriptions-item label="总费用">
                <strong>${{ (traceDetail.cost || 0).toFixed(4) }}</strong>
              </a-descriptions-item>
            </a-descriptions>
          </a-card>

          <!-- Retry History -->
          <a-card title="重试历史" :bordered="false" class="page-card" style="margin-top: 16px">
            <div v-if="traceDetail.retries?.length">
              <a-timeline>
                <a-timeline-item
                  v-for="retry in traceDetail.retries"
                  :key="retry.attempt"
                  :color="retry.error ? 'red' : 'green'"
                >
                  <div>
                    <strong>第{{ retry.attempt }}次尝试</strong>
                    <span style="color: #999; font-size: 12px; margin-left: 8px">{{ retry.timestamp }}</span>
                  </div>
                  <div v-if="retry.error" style="color: #ff4d4f; font-size: 12px; margin-top: 4px">
                    {{ retry.error }}
                  </div>
                  <div v-else style="color: #52c41a; font-size: 12px; margin-top: 4px">
                    成功 · {{ retry.tokens }} tokens
                  </div>
                </a-timeline-item>
              </a-timeline>
            </div>
            <a-empty v-else description="无重试记录" :image-style="{ height: '40px' }" />
          </a-card>

          <!-- Metadata -->
          <a-card title="元数据" :bordered="false" class="page-card" style="margin-top: 16px">
            <div class="json-viewer">{{ formatJson(traceDetail.metadata) }}</div>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '@/components/common/PageHeader.vue'
import { aiTraceApi, type AiTraceDetail } from '@/api/ai-trace'
import * as echarts from 'echarts'

const route = useRoute()
const traceId = route.params.traceId as string

const loading = ref(false)
const traceDetail = ref<AiTraceDetail | null>(null)
const costChartRef = ref<HTMLElement>()

const typeColor: Record<string, string> = {
  nl2sql: 'blue',
  analysis: 'green',
  report: 'orange',
}

const typeLabel: Record<string, string> = {
  nl2sql: 'NL2SQL',
  analysis: '分析',
  report: '报表生成',
}

function formatJson(obj: unknown): string {
  try {
    return JSON.stringify(obj, null, 2)
  } catch {
    return String(obj)
  }
}

function renderCostChart() {
  if (!costChartRef.value || !traceDetail.value) return
  const chart = echarts.init(costChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        data: [
          { value: traceDetail.value.promptTokens, name: 'Prompt Tokens', itemStyle: { color: '#1677ff' } },
          { value: traceDetail.value.completionTokens, name: 'Completion Tokens', itemStyle: { color: '#52c41a' } },
        ],
        label: { show: false },
      },
    ],
  })
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await aiTraceApi.detail(traceId)
    traceDetail.value = res.data
    await nextTick()
    renderCostChart()
  } catch {} finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.stat-item {
  text-align: center;

  .stat-label { font-size: 12px; color: #999; margin-bottom: 4px; }
  .stat-value { font-size: 18px; font-weight: 700; color: #1a1a1a; }
}

.prompt-display,
.output-display {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 8px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  max-height: 400px;
  overflow-y: auto;

  &.validated {
    background: #0d1117;
    border: 1px solid #52c41a;
  }
}

.json-viewer {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 6px;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
}
</style>
