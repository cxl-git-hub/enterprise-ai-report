<template>
  <div class="page-container">
    <PageHeader title="KPI管理" subtitle="管理KPI指标定义和版本">
      <template #actions>
        <a-button type="primary" @click="openCreateModal">
          <PlusOutlined /> 新建KPI
        </a-button>
      </template>
    </PageHeader>

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="search">
        <a-form-item>
          <a-input v-model:value="searchParams.keyword" placeholder="搜索KPI名称" allow-clear>
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
          <template v-if="column.key === 'expression'">
            <a-tooltip :title="record.expression">
              <code class="expression-code">{{ truncate(record.expression, 40) }}</code>
            </a-tooltip>
          </template>
          <template v-if="column.key === 'version'">
            <a-tag color="blue">v{{ record.version }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 'active' ? 'success' : 'default'"
              :text="record.status === 'active' ? '已发布' : '草稿'"
            />
          </template>
          <template v-if="column.key === 'action'">
            <div class="table-actions">
              <a-button type="link" size="small" @click="handleExecute(record)" :loading="executingId === record.id">
                执行
              </a-button>
              <a-button type="link" size="small" @click="openTrendChart(record)">趋势</a-button>
              <a-button type="link" size="small" @click="openVersionHistory(record)">版本</a-button>
              <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
              <ConfirmDelete @confirm="handleDelete(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </ConfirmDelete>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Execute Result Modal -->
    <a-modal v-model:open="executeResultVisible" title="KPI执行结果" :footer="null" width="500px">
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="指标值">
          <span style="font-size: 24px; font-weight: 700; color: #1677ff">
            {{ executeResult?.value }} {{ executeResult?.unit }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="执行时间">{{ executeResult?.executedAt }}</a-descriptions-item>
        <a-descriptions-item label="耗时">{{ executeResult?.duration }}ms</a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <!-- Execute Params Modal -->
    <a-modal v-model:open="executeParamsVisible" title="执行KPI" :confirm-loading="executingId !== null" @ok="handleExecuteWithParams" width="500px">
      <a-form layout="vertical">
        <a-form-item label="日期范围">
          <a-range-picker v-model:value="executeDateRange" style="width: 100%" />
        </a-form-item>
        <a-form-item label="分组维度">
          <a-input v-model:value="executeGroupBy" placeholder="如：status, category（逗号分隔）" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Form Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑KPI' : '新建KPI'"
      :confirm-loading="confirmLoading"
      @ok="handleSubmit"
      width="800px"
    >
      <a-form ref="formRef" :model="formState" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="KPI名称" name="name">
              <a-input v-model:value="formState.name" placeholder="请输入名称">
                <template #tooltip><span>KPI指标的显示名称</span></template>
              </a-input>
              <div class="field-hint">示例：日均订单金额</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="关联Schema" name="schemaId">
              <a-select v-model:value="formState.schemaId" placeholder="请选择Schema">
                <a-select-option v-for="s in schemaOptions" :key="s.id" :value="s.id">
                  {{ s.name }}
                </a-select-option>
              </a-select>
              <template #tooltip><span>KPI基于哪个Schema的数据进行计算</span></template>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="聚合类型" name="aggregationType">
              <a-select v-model:value="formState.aggregationType" placeholder="请选择">
                <a-select-option value="sum">SUM - 求和</a-select-option>
                <a-select-option value="avg">AVG - 平均值</a-select-option>
                <a-select-option value="count">COUNT - 计数</a-select-option>
                <a-select-option value="max">MAX - 最大值</a-select-option>
                <a-select-option value="min">MIN - 最小值</a-select-option>
                <a-select-option value="custom">自定义表达式</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="单位" name="unit">
              <a-input v-model:value="formState.unit" placeholder="如：元、次、%" />
              <div class="field-hint">示例：元/天</div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="DSL表达式" name="expression">
          <template #tooltip><span>KPI的计算表达式，支持SQL聚合函数和自定义DSL</span></template>
          <div class="code-editor">
            <a-textarea
              v-model:value="formState.expression"
              :rows="4"
              placeholder="SUM(order_amount) / COUNT(DISTINCT order_id)"
              style="font-family: monospace; font-size: 13px"
            />
          </div>
          <div class="field-hint">
            示例：SUM(amount) WHERE status = 'completed' AND date >= '2024-01-01'
            <a-button type="link" size="small" @click="aiSuggestExpression">
              <ThunderboltOutlined /> AI建议表达式
            </a-button>
          </div>
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="2" placeholder="请输入KPI描述">
            <template #tooltip><span>KPI的业务含义说明</span></template>
          </a-textarea>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Trend Chart Modal -->
    <a-modal v-model:open="trendModalVisible" :title="`KPI趋势: ${trendKpiName}`" :footer="null" width="800px">
      <a-spin :spinning="trendLoading">
        <div v-if="trendData.length > 0" class="trend-chart">
          <div class="trend-summary">
            <a-row :gutter="16">
              <a-col :span="8">
                <a-statistic title="最新值" :value="trendData[trendData.length - 1]?.value" :suffix="trendKpiUnit" />
              </a-col>
              <a-col :span="8">
                <a-statistic title="数据点" :value="trendData.length" />
              </a-col>
              <a-col :span="8">
                <a-statistic title="时间范围" :value="`${trendData[0]?.date} ~ ${trendData[trendData.length - 1]?.date}`" />
              </a-col>
            </a-row>
          </div>
          <div class="trend-visual">
            <div class="trend-bars">
              <div
                v-for="(point, idx) in trendData"
                :key="idx"
                class="trend-bar-wrapper"
              >
                <div
                  class="trend-bar"
                  :style="{ height: getBarHeight(point.value) + 'px', background: '#1677ff' }"
                  :title="`${point.date}: ${point.value}`"
                />
                <span class="trend-label">{{ point.date?.slice(5) }}</span>
              </div>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无历史数据" />
      </a-spin>
    </a-modal>

    <!-- Version History Modal -->
    <a-modal v-model:open="versionModalVisible" title="版本历史" :footer="null" width="700px">
      <a-timeline>
        <a-timeline-item
          v-for="ver in versionHistory"
          :key="ver.version"
          :color="ver.version === currentKpi?.version ? 'blue' : 'gray'"
        >
          <div class="version-item">
            <div class="version-header">
              <a-tag :color="ver.version === currentKpi?.version ? 'blue' : 'default'">v{{ ver.version }}</a-tag>
              <span class="version-time">{{ ver.createdAt }}</span>
              <span class="version-user">{{ ver.createdBy }}</span>
            </div>
            <p class="version-note">{{ ver.changeNote || '无变更说明' }}</p>
            <code class="expression-code">{{ ver.expression }}</code>
          </div>
        </a-timeline-item>
      </a-timeline>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ConfirmDelete from '@/components/common/ConfirmDelete.vue'
import { useTable } from '@/composables/useTable'
import { useModal } from '@/composables/useModal'
import { kpiApi, type Kpi, type KpiForm, type KpiVersion, type KpiExecuteResult } from '@/api/kpi'
import { schemaApi, type Schema } from '@/api/schema'
import { post } from '@/api/request'
import type { FormInstance } from 'ant-design-vue'

const formRef = ref<FormInstance>()
const executingId = ref<string | null>(null)
const executeResultVisible = ref(false)
const executeResult = ref<KpiExecuteResult | null>(null)
const executeParamsVisible = ref(false)
const executeDateRange = ref<any>(null)
const executeGroupBy = ref('')
const pendingExecuteKpi = ref<Kpi | null>(null)
const versionModalVisible = ref(false)
const versionHistory = ref<KpiVersion[]>([])
const currentKpi = ref<Kpi | null>(null)
const schemaOptions = ref<Schema[]>([])
const trendModalVisible = ref(false)
const trendLoading = ref(false)
const trendData = ref<Array<{ date: string; value: number; formattedValue?: string }>>([])
const trendKpiName = ref('')
const trendKpiUnit = ref('')

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, search, resetSearch } =
  useTable<Kpi>({
    fetchApi: (params) => kpiApi.list(params as { page: number; pageSize: number; keyword?: string }),
  })

const { visible: modalVisible, confirmLoading, editingId, openModal, handleOk } = useModal()

const formState = reactive<KpiForm>({
  name: '',
  description: '',
  expression: '',
  unit: '',
  aggregationType: 'sum',
  schemaId: '',
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  schemaId: [{ required: true, message: '请选择Schema', trigger: 'change' }],
  expression: [{ required: true, message: '请输入表达式', trigger: 'blur' }],
  aggregationType: [{ required: true, message: '请选择聚合类型', trigger: 'change' }],
}

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: 'Schema', dataIndex: 'schemaName', key: 'schemaName' },
  { title: '表达式', dataIndex: 'expression', key: 'expression', ellipsis: true },
  { title: '聚合', dataIndex: 'aggregationType', key: 'aggregationType', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 280, fixed: 'right' as const },
]

function truncate(str: string, len: number) {
  return str && str.length > len ? str.slice(0, len) + '...' : str
}

async function openTrendChart(record: Kpi) {
  trendKpiName.value = record.name
  trendKpiUnit.value = record.unit || ''
  trendData.value = []
  trendModalVisible.value = true
  trendLoading.value = true
  try {
    const res = await kpiApi.getTrend(record.id, { limit: 30 })
    trendData.value = res.data || []
  } catch {
    trendData.value = []
  } finally {
    trendLoading.value = false
  }
}

function getBarHeight(value: number): number {
  if (!trendData.value.length) return 0
  const maxVal = Math.max(...trendData.value.map((d) => d.value), 1)
  return Math.max(4, (value / maxVal) * 150)
}

function openCreateModal() {
  editingId.value = null
  Object.assign(formState, { name: '', description: '', expression: '', unit: '', aggregationType: 'sum', schemaId: '' })
  openModal()
}

async function openEditModal(record: Kpi) {
  openModal(record.id)
  const res = await kpiApi.detail(record.id)
  Object.assign(formState, res.data)
}

async function openVersionHistory(record: Kpi) {
  currentKpi.value = record
  try {
    const res = await kpiApi.getVersions(record.id)
    versionHistory.value = res.data
  } catch {}
  versionModalVisible.value = true
}

async function handleExecute(record: Kpi) {
  pendingExecuteKpi.value = record
  executeDateRange.value = null
  executeGroupBy.value = ''
  executeParamsVisible.value = true
}

async function handleExecuteWithParams() {
  if (!pendingExecuteKpi.value) return
  executingId.value = pendingExecuteKpi.value.id
  executeParamsVisible.value = false
  try {
    const params: Record<string, unknown> = {}
    if (executeDateRange.value && executeDateRange.value.length === 2) {
      params.periodStart = executeDateRange.value[0].format('YYYY-MM-DD')
      params.periodEnd = executeDateRange.value[1].format('YYYY-MM-DD')
    }
    if (executeGroupBy.value) {
      params.groupBy = executeGroupBy.value.split(',').map((s: string) => s.trim())
    }
    const res = await kpiApi.execute(pendingExecuteKpi.value.id, params)
    executeResult.value = res.data
    executeResultVisible.value = true
  } finally {
    executingId.value = null
    pendingExecuteKpi.value = null
  }
}

function aiSuggestExpression() {
  if (!formState.name) {
    message.warning('请先输入KPI名称')
    return
  }
  message.loading({ content: 'AI正在分析并建议表达式...', key: 'ai-suggest', duration: 0 })
  post<{ data: { expression: string; explanation: string } }>('/ai/suggest-expression', {
    kpi_name: formState.name,
    description: formState.description,
    schema_id: formState.schemaId,
    aggregation_type: formState.aggregationType,
  }).then((res) => {
    if (res?.data?.expression) {
      formState.expression = res.data.expression
      message.success({ content: `AI建议: ${res.data.explanation || '已生成表达式'}`, key: 'ai-suggest' })
    } else {
      message.info({ content: 'AI未能生成建议，请手动输入', key: 'ai-suggest' })
    }
  }).catch(() => {
    message.error({ content: 'AI建议服务暂时不可用', key: 'ai-suggest' })
  })
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  await handleOk(async () => {
    if (editingId.value) {
      await kpiApi.update(editingId.value, formState)
      message.success('更新成功')
    } else {
      await kpiApi.create(formState)
      message.success('创建成功')
    }
    fetchData()
  })
}

async function handleDelete(id: string) {
  try {
    await kpiApi.remove(id)
    message.success('删除成功')
    fetchData()
  } catch {}
}

onMounted(async () => {
  try {
    const res = await schemaApi.list({ page: 1, pageSize: 100 })
    schemaOptions.value = res.data.items
  } catch {}
})
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 16px; }
.field-hint { font-size: 12px; color: #999; margin-top: 4px; }
.expression-code {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #d63384;
}
.version-item {
  .version-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
  .version-time { color: #999; font-size: 12px; }
  .version-user { color: #666; font-size: 12px; }
  .version-note { color: #666; font-size: 13px; margin: 4px 0; }
}

.trend-chart {
  .trend-summary { margin-bottom: 24px; }
  .trend-visual { padding: 16px 0; }
  .trend-bars {
    display: flex;
    align-items: flex-end;
    gap: 4px;
    height: 200px;
    padding: 0 8px;
  }
  .trend-bar-wrapper {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-end;
    height: 100%;
  }
  .trend-bar {
    width: 100%;
    min-width: 8px;
    max-width: 40px;
    border-radius: 4px 4px 0 0;
    transition: height 0.3s;
    cursor: pointer;
    &:hover { opacity: 0.8; }
  }
  .trend-label {
    font-size: 10px;
    color: #999;
    margin-top: 4px;
    white-space: nowrap;
  }
}
</style>
