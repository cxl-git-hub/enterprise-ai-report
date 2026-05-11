<template>
  <div class="page-container">
    <PageHeader title="定时报表" subtitle="配置报表自动生成和邮件分发">
      <template #actions>
        <a-button type="primary" @click="openCreateModal">
          <PlusOutlined /> 新建定时任务
        </a-button>
      </template>
    </PageHeader>

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
          <template v-if="column.key === 'status'">
            <a-badge
              :status="statusBadge[record.status]"
              :text="statusLabel[record.status] || record.status"
            />
          </template>
          <template v-if="column.key === 'format'">
            <a-tag :color="formatColor[record.format] || 'default'">{{ record.format?.toUpperCase() }}</a-tag>
          </template>
          <template v-if="column.key === 'recipients'">
            <a-tooltip :title="record.recipients">
              {{ truncateEmails(record.recipients) }}
            </a-tooltip>
          </template>
          <template v-if="column.key === 'nextRunAt'">
            {{ record.nextRunAt || '-' }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleExecuteNow(record)" :loading="executingId === record.id">
                立即执行
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
      :title="editingId ? '编辑定时任务' : '新建定时任务'"
      @ok="handleSubmit"
      width="700px"
    >
      <a-form ref="formRef" :model="formState" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="任务名称" name="name" :rules="[{ required: true, message: '请输入名称' }]">
              <a-input v-model:value="formState.name" placeholder="月度销售报表" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="报表模板" name="reportTemplateId">
              <a-select v-model:value="formState.reportTemplateId" placeholder="选择报表模板">
                <a-select-option v-for="t in templates" :key="t.id" :value="t.id">{{ t.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="输出格式" name="format">
              <a-select v-model:value="formState.format" placeholder="选择格式">
                <a-select-option value="pdf">PDF</a-select-option>
                <a-select-option value="docx">Word</a-select-option>
                <a-select-option value="excel">Excel</a-select-option>
                <a-select-option value="pptx">PPT</a-select-option>
                <a-select-option value="html">HTML</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="调度表达式" name="cronExpression">
              <a-input v-model:value="formState.cronExpression" placeholder="0 9 1 * *" />
              <div class="field-hint">Cron表达式，如 0 9 1 * * 表示每月1号9点</div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="收件人邮箱" name="recipients" :rules="[{ required: true, message: '请输入收件人' }]">
          <a-textarea v-model:value="formState.recipients" :rows="2" placeholder="多个邮箱用逗号分隔，如: user1@example.com, user2@example.com" />
        </a-form-item>
        <a-form-item label="抄送邮箱" name="ccRecipients">
          <a-input v-model:value="formState.ccRecipients" placeholder="可选" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="邮件主题">
              <a-input v-model:value="formState.emailSubject" placeholder="{{name}} - {{date}}" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="时区">
              <a-select v-model:value="formState.timezone" placeholder="选择时区">
                <a-select-option value="Asia/Shanghai">Asia/Shanghai</a-select-option>
                <a-select-option value="UTC">UTC</a-select-option>
                <a-select-option value="America/New_York">America/New_York</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="选项">
          <a-space>
            <a-checkbox v-model:checked="formState.includeDisclaimer">附带AI免责声明</a-checkbox>
            <a-checkbox v-model:checked="formState.includeLineage">附带数据溯源</a-checkbox>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useTable } from '@/composables/useTable'
import { get, post, put, del } from '@/api/request'

interface ReportSchedule {
  id: string
  name: string
  reportTemplateId: string
  format: string
  cronExpression: string
  timezone: string
  recipients: string
  ccRecipients: string
  emailSubject: string
  emailBody: string
  includeDisclaimer: boolean
  includeLineage: boolean
  status: string
  lastRunAt: string
  nextRunAt: string
}

const modalVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref()
const executingId = ref<string | null>(null)
const templates = ref<Array<{ id: string; name: string }>>([])

const formState = reactive({
  name: '',
  reportTemplateId: '',
  format: 'pdf',
  cronExpression: '',
  timezone: 'Asia/Shanghai',
  recipients: '',
  ccRecipients: '',
  emailSubject: '',
  includeDisclaimer: true,
  includeLineage: true,
})

const statusBadge: Record<string, string> = {
  active: 'success',
  paused: 'warning',
  error: 'error',
}

const statusLabel: Record<string, string> = {
  active: '运行中',
  paused: '已暂停',
  error: '错误',
}

const formatColor: Record<string, string> = {
  pdf: 'red',
  docx: 'purple',
  excel: 'green',
  pptx: 'orange',
  html: 'blue',
}

const { loading, dataSource, pagination, fetchData, handleTableChange } =
  useTable<ReportSchedule>({
    fetchApi: (params) => get('/report-schedules', { page: params.page, size: params.pageSize }),
  })

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '格式', dataIndex: 'format', key: 'format', width: 80 },
  { title: '调度', dataIndex: 'cronExpression', key: 'cronExpression', width: 140 },
  { title: '收件人', dataIndex: 'recipients', key: 'recipients', width: 200 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '下次执行', dataIndex: 'nextRunAt', key: 'nextRunAt', width: 170 },
  { title: '操作', key: 'action', width: 280, fixed: 'right' as const },
]

function truncateEmails(recipients: string): string {
  if (!recipients) return '-'
  const emails = recipients.split(',').map(e => e.trim())
  if (emails.length <= 2) return recipients
  return `${emails[0]}, ${emails[1]} +${emails.length - 2}`
}

function openCreateModal() {
  editingId.value = null
  Object.assign(formState, {
    name: '', reportTemplateId: '', format: 'pdf', cronExpression: '',
    timezone: 'Asia/Shanghai', recipients: '', ccRecipients: '', emailSubject: '',
    includeDisclaimer: true, includeLineage: true,
  })
  modalVisible.value = true
}

async function openEditModal(record: ReportSchedule) {
  editingId.value = record.id
  Object.assign(formState, record)
  modalVisible.value = true
}

async function handleSubmit() {
  try {
    if (editingId.value) {
      await put(`/report-schedules/${editingId.value}`, formState)
      message.success('更新成功')
    } else {
      await post('/report-schedules', formState)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch {
    message.error('操作失败')
  }
}

async function handleExecuteNow(record: ReportSchedule) {
  executingId.value = record.id
  try {
    await post(`/report-schedules/${record.id}/execute`, {})
    message.success('任务已触发执行')
  } catch {
    message.error('执行失败')
  } finally {
    executingId.value = null
  }
}

async function handleToggleStatus(record: ReportSchedule) {
  const action = record.status === 'active' ? 'pause' : 'resume'
  try {
    await post(`/report-schedules/${record.id}/${action}`, {})
    message.success(record.status === 'active' ? '已暂停' : '已恢复')
    fetchData()
  } catch {
    message.error('操作失败')
  }
}

async function handleDelete(id: string) {
  try {
    await del(`/report-schedules/${id}`)
    message.success('删除成功')
    fetchData()
  } catch {
    message.error('删除失败')
  }
}

onMounted(async () => {
  // useTable already calls fetchData on mount
  try {
    const res = await get<{ data: { items: any[] } }>('/report-templates', { page: 1, size: 100 })
    templates.value = res?.data?.items || []
  } catch {}
})
</script>

<style lang="scss" scoped>
.field-hint { font-size: 12px; color: #999; margin-top: 4px; }
</style>
