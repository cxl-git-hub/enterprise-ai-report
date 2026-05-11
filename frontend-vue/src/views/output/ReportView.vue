<template>
  <div class="page-container">
    <PageHeader title="报表输出" subtitle="查看和下载生成的报表" />

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="search">
        <a-form-item>
          <a-input v-model:value="searchParams.keyword" placeholder="搜索报表名称" allow-clear>
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-select v-model:value="searchParams.format" placeholder="格式筛选" allow-clear style="width: 120px">
            <a-select-option value="pdf">PDF</a-select-option>
            <a-select-option value="excel">Excel</a-select-option>
            <a-select-option value="html">HTML</a-select-option>
            <a-select-option value="docx">Word</a-select-option>
            <a-select-option value="pptx">PPT</a-select-option>
          </a-select>
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
          <template v-if="column.key === 'format'">
            <a-tag :color="formatColor[record.format] || 'default'">
              {{ (record.format || '').toUpperCase() }}
            </a-tag>
          </template>
          <template v-if="column.key === 'fileSize'">
            {{ formatFileSize(record.fileSize) }}
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 1 ? 'success' : record.status === 0 ? 'processing' : 'default'"
              :text="record.status === 1 ? '已完成' : record.status === 0 ? '生成中' : '未知'"
            />
          </template>
          <template v-if="column.key === 'lineage'">
            <a-button type="link" size="small" @click="showLineage(record)">
              <NodeIndexOutlined /> 溯源
            </a-button>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                @click="handleDownload(record)"
                :loading="downloadingId === record.id"
                :disabled="record.status !== 1"
              >
                <DownloadOutlined /> 下载
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleShare(record)"
              >
                <ShareAltOutlined /> 分享
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Lineage Drawer -->
    <a-drawer
      v-model:open="lineageVisible"
      title="数据溯源"
      width="500"
    >
      <DataCitation v-if="lineageData.length" :citations="lineageData" />
      <a-empty v-else description="暂无溯源数据" />
    </a-drawer>

    <!-- Share Modal -->
    <a-modal
      v-model:open="shareModalVisible"
      title="分享报表"
      @ok="handleShareConfirm"
      width="400px"
    >
      <a-form layout="vertical">
        <a-form-item label="分享链接">
          <a-input :value="shareLink" disabled>
            <template #suffix>
              <a-button type="link" size="small" @click="copyShareLink">
                <CopyOutlined /> 复制
              </a-button>
            </template>
          </a-input>
        </a-form-item>
        <a-form-item label="有效期">
          <a-select v-model:value="shareExpiry" style="width: 100%">
            <a-select-option value="1d">1天</a-select-option>
            <a-select-option value="7d">7天</a-select-option>
            <a-select-option value="30d">30天</a-select-option>
            <a-select-option value="forever">永久</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-checkbox v-model:checked="shareIncludeDisclaimer">附带AI免责声明</a-checkbox>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  SearchOutlined,
  DownloadOutlined,
  NodeIndexOutlined,
  ShareAltOutlined,
  CopyOutlined,
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import DataCitation, { type Citation } from '@/components/common/DataCitation.vue'
import { useTable } from '@/composables/useTable'
import { reportOutputApi, type ReportOutput } from '@/api/report-output'
import { get } from '@/api/request'

const downloadingId = ref<string | null>(null)
const lineageVisible = ref(false)
const lineageData = ref<Citation[]>([])
const shareModalVisible = ref(false)
const shareLink = ref('')
const shareExpiry = ref('7d')
const shareIncludeDisclaimer = ref(true)
const currentShareRecord = ref<ReportOutput | null>(null)

const formatColor: Record<string, string> = {
  pdf: 'red',
  excel: 'green',
  html: 'blue',
  docx: 'purple',
  xlsx: 'green',
  pptx: 'orange',
}

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, search, resetSearch } =
  useTable<ReportOutput>({
    fetchApi: (params) => reportOutputApi.list(params as { page: number; pageSize: number; keyword?: string; format?: string }),
  })

const columns = [
  { title: '报表名称', dataIndex: 'name', key: 'name' },
  { title: '格式', dataIndex: 'format', key: 'format', width: 80 },
  { title: '文件大小', dataIndex: 'fileSize', key: 'fileSize', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '生成时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '数据溯源', key: 'lineage', width: 90 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
]

function formatFileSize(bytes: number): string {
  if (!bytes) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

async function handleDownload(record: ReportOutput) {
  downloadingId.value = record.id
  try {
    const res = await reportOutputApi.download(record.id)
    const blob = new Blob([res as unknown as BlobPart])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${record.name || 'report'}.${record.format || 'pdf'}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('下载成功')
  } catch {
    message.error('下载失败')
  } finally {
    downloadingId.value = null
  }
}

async function showLineage(record: ReportOutput) {
  try {
    const res = await get<{ data: any[] }>(`/data-lineage/output/report_output/${record.id}`)
    lineageData.value = (res?.data || []).map((item: any) => ({
      sourceName: item.sourceName || `数据集 #${item.datasetId}`,
      datasetId: item.datasetId,
      schemaId: item.schemaId,
      fields: item.fields ? item.fields.split(',') : [],
      timeRange: item.timeRange,
      rowCount: item.rowCount,
      lastUpdated: item.createdAt,
      quality: item.quality,
    }))
    lineageVisible.value = true
  } catch {
    lineageData.value = []
    lineageVisible.value = true
  }
}

function handleShare(record: ReportOutput) {
  currentShareRecord.value = record
  shareLink.value = `${window.location.origin}/report/shared/${record.id}?token=${Date.now().toString(36)}`
  shareModalVisible.value = true
}

function handleShareConfirm() {
  message.success('分享链接已生成')
  shareModalVisible.value = false
}

function copyShareLink() {
  navigator.clipboard.writeText(shareLink.value)
  message.success('链接已复制到剪贴板')
}

// useTable already calls fetchData on mount
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 16px; }
</style>
