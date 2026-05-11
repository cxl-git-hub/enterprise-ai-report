<template>
  <div class="page-container">
    <PageHeader title="智能报警" subtitle="用自然语言定义报警规则，AI自动监控">
      <template #actions>
        <a-button type="primary" @click="openCreateModal">
          <PlusOutlined /> 新建规则
        </a-button>
      </template>
    </PageHeader>

    <!-- Quick Examples -->
    <a-card :bordered="false" class="page-card" style="margin-bottom: 16px">
      <div class="examples-section">
        <span class="examples-label">💡 示例规则：</span>
        <a-tag v-for="ex in examples" :key="ex" color="blue" class="example-tag" @click="useExample(ex)">{{ ex }}</a-tag>
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ruleExpression'">
            <div class="rule-expression">
              <BulbOutlined style="color: #faad14; margin-right: 6px" />
              {{ record.ruleExpression }}
            </div>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="statusBadge[record.status]"
              :text="statusLabel[record.status] || record.status"
            />
          </template>
          <template v-if="column.key === 'notifyChannel'">
            <a-tag :color="channelColor[record.notifyChannel]">
              {{ channelLabel[record.notifyChannel] || record.notifyChannel }}
            </a-tag>
          </template>
          <template v-if="column.key === 'triggerCount'">
            <a-badge :count="record.triggerCount" :overflow-count="99" :show-zero="true" />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleTest(record)" :loading="testingId === record.id">
                测试
              </a-button>
              <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
              <a-button
                type="link"
                size="small"
                @click="handleToggleStatus(record)"
              >
                {{ record.status === 'active' ? '暂停' : '恢复' }}
              </a-button>
              <a-button type="link" size="small" danger @click="handleDelete(record.id)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Create/Edit Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑报警规则' : '新建报警规则'"
      @ok="handleSubmit"
      width="700px"
    >
      <a-form ref="formRef" :model="formState" layout="vertical">
        <a-form-item label="规则名称" name="name" :rules="[{ required: true, message: '请输入名称' }]">
          <a-input v-model:value="formState.name" placeholder="如：销售额异常下降告警" />
        </a-form-item>

        <a-form-item label="自然语言规则" name="ruleExpression" :rules="[{ required: true, message: '请输入规则描述' }]">
          <a-textarea
            v-model:value="formState.ruleExpression"
            :rows="3"
            placeholder="用自然语言描述报警条件，如：当销售额连续3天下降超过10%时通知我"
          />
          <div class="field-hint">
            <a-button type="link" size="small" @click="handleParse" :loading="parsing">
              <ThunderboltOutlined /> AI解析规则
            </a-button>
          </div>
        </a-form-item>

        <!-- Parsed Config Preview -->
        <a-alert v-if="parsedConfig" type="info" show-icon style="margin-bottom: 16px">
          <template #message>
            <span>AI解析结果：</span>
            <a-tag color="blue">{{ parsedConfig.metric }}</a-tag>
            <a-tag color="green">{{ conditionLabel[parsedConfig.condition] }}</a-tag>
            <a-tag v-if="parsedConfig.threshold" color="orange">{{ parsedConfig.threshold }}%</a-tag>
            <a-tag v-if="parsedConfig.period" color="purple">{{ parsedConfig.period }}</a-tag>
          </template>
        </a-alert>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="数据集">
              <a-select v-model:value="formState.datasetId" placeholder="选择数据集" allow-clear>
                <a-select-option v-for="ds in datasets" :key="ds.id" :value="ds.id">{{ ds.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="检查频率">
              <a-select v-model:value="formState.checkFrequencyMin" placeholder="检查频率">
                <a-select-option :value="5">每5分钟</a-select-option>
                <a-select-option :value="15">每15分钟</a-select-option>
                <a-select-option :value="60">每小时</a-select-option>
                <a-select-option :value="1440">每天</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="通知方式">
              <a-select v-model:value="formState.notifyChannel" placeholder="选择通知方式">
                <a-select-option value="inapp">站内通知</a-select-option>
                <a-select-option value="email">邮件</a-select-option>
                <a-select-option value="webhook">Webhook</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item v-if="formState.notifyChannel === 'email'" label="通知邮箱">
              <a-input v-model:value="formState.recipients" placeholder="多个邮箱用逗号分隔" />
            </a-form-item>
            <a-form-item v-if="formState.notifyChannel === 'webhook'" label="Webhook URL">
              <a-input v-model:value="formState.webhookUrl" placeholder="https://hooks.example.com/alert" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, BulbOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useTable } from '@/composables/useTable'
import { get, post, put, del } from '@/api/request'
import { datasetApi, type Dataset } from '@/api/dataset'

interface AlertRule {
  id: string
  name: string
  ruleExpression: string
  parsedConfig: string
  datasetId: string
  notifyChannel: string
  recipients: string
  webhookUrl: string
  checkFrequencyMin: number
  status: string
  triggerCount: number
  lastCheckAt: string
  lastTriggerAt: string
}

const modalVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref()
const testingId = ref<string | null>(null)
const parsing = ref(false)
const parsedConfig = ref<any>(null)
const datasets = ref<Dataset[]>([])

const formState = reactive({
  name: '',
  ruleExpression: '',
  datasetId: '',
  notifyChannel: 'inapp',
  recipients: '',
  webhookUrl: '',
  checkFrequencyMin: 60,
})

const examples = [
  '当销售额连续3天下降超过10%时通知我',
  '如果DAU低于1000就发邮件告警',
  '当转化率环比下降超过5%时提醒',
  '库存低于安全库存时立即通知',
  '当退款率超过3%时告警',
]

const statusBadge: Record<string, string> = {
  active: 'success',
  paused: 'warning',
  triggered: 'error',
}

const statusLabel: Record<string, string> = {
  active: '监控中',
  paused: '已暂停',
  triggered: '已触发',
}

const channelColor: Record<string, string> = {
  inapp: 'blue',
  email: 'green',
  webhook: 'purple',
}

const channelLabel: Record<string, string> = {
  inapp: '站内',
  email: '邮件',
  webhook: 'Webhook',
}

const conditionLabel: Record<string, string> = {
  gt: '大于',
  lt: '小于',
  decrease: '下降',
  increase: '上升',
  eq: '等于',
}

const { loading, dataSource, pagination, fetchData, handleTableChange } =
  useTable<AlertRule>({
    fetchApi: (params) => get('/alert-rules', { page: params.page, size: params.pageSize }),
  })

const columns = [
  { title: '规则名称', dataIndex: 'name', key: 'name', width: 160 },
  { title: '规则表达式', dataIndex: 'ruleExpression', key: 'ruleExpression' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '通知方式', dataIndex: 'notifyChannel', key: 'notifyChannel', width: 100 },
  { title: '触发次数', dataIndex: 'triggerCount', key: 'triggerCount', width: 100 },
  { title: '上次检查', dataIndex: 'lastCheckAt', key: 'lastCheckAt', width: 160 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' as const },
]

function useExample(text: string) {
  formState.ruleExpression = text
  openCreateModal()
}

function openCreateModal() {
  editingId.value = null
  Object.assign(formState, {
    name: '', ruleExpression: formState.ruleExpression || '', datasetId: '',
    notifyChannel: 'inapp', recipients: '', webhookUrl: '', checkFrequencyMin: 60,
  })
  parsedConfig.value = null
  modalVisible.value = true
}

async function openEditModal(record: AlertRule) {
  editingId.value = record.id
  Object.assign(formState, record)
  if (record.parsedConfig) {
    try { parsedConfig.value = JSON.parse(record.parsedConfig) } catch { parsedConfig.value = null }
  }
  modalVisible.value = true
}

async function handleParse() {
  if (!formState.ruleExpression.trim()) {
    message.warning('请先输入规则描述')
    return
  }
  parsing.value = true
  try {
    const res = await post<{ data: string }>('/alert-rules/parse', { expression: formState.ruleExpression })
    parsedConfig.value = JSON.parse(res.data)
  } catch {
    message.error('解析失败')
  } finally {
    parsing.value = false
  }
}

async function handleSubmit() {
  try {
    if (editingId.value) {
      await put(`/alert-rules/${editingId.value}`, formState)
      message.success('更新成功')
    } else {
      await post('/alert-rules', formState)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch {
    message.error('操作失败')
  }
}

async function handleTest(record: AlertRule) {
  testingId.value = record.id
  try {
    const res = await post<{ data: string }>(`/alert-rules/${record.id}/test`, {})
    message.info(res.data)
  } catch {
    message.error('测试失败')
  } finally {
    testingId.value = null
  }
}

async function handleToggleStatus(record: AlertRule) {
  const action = record.status === 'active' ? 'pause' : 'resume'
  try {
    await post(`/alert-rules/${record.id}/${action}`, {})
    message.success(record.status === 'active' ? '已暂停' : '已恢复')
    fetchData()
  } catch {
    message.error('操作失败')
  }
}

async function handleDelete(id: string) {
  try {
    await del(`/alert-rules/${id}`)
    message.success('删除成功')
    fetchData()
  } catch {
    message.error('删除失败')
  }
}

onMounted(async () => {
  // useTable already calls fetchData on mount
  try {
    const res = await datasetApi.list({ page: 1, size: 100 })
    datasets.value = res.data.items
  } catch {}
})
</script>

<style lang="scss" scoped>
.examples-section {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;

  .examples-label {
    font-size: 13px;
    color: #666;
    font-weight: 500;
  }

  .example-tag {
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      transform: scale(1.05);
    }
  }
}

.rule-expression {
  font-size: 13px;
  display: flex;
  align-items: center;
}

.field-hint { margin-top: 4px; }
</style>
