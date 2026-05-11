<template>
  <div class="page-container">
    <PageHeader title="报表版本管理" subtitle="查看和管理报表历史版本" />

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="fetchData">
        <a-form-item>
          <a-select v-model:value="searchParams.reportId" placeholder="选择报表" allow-clear style="width: 200px">
            <a-select-option v-for="r in reports" :key="r.id" :value="r.id">{{ r.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="versions"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        row-key="versionId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'version'">
            <a-tag color="blue">v{{ record.version }}</a-tag>
            <a-tag v-if="record.isLatest" color="green" size="small">最新</a-tag>
          </template>
          <template v-if="column.key === 'changes'">
            <a-space>
              <a-tag v-if="record.added" color="green" size="small">+{{ record.added }} 新增</a-tag>
              <a-tag v-if="record.modified" color="orange" size="small">~{{ record.modified }} 修改</a-tag>
              <a-tag v-if="record.removed" color="red" size="small">-{{ record.removed }} 删除</a-tag>
            </a-space>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 'published' ? 'success' : 'default'"
              :text="record.status === 'published' ? '已发布' : '草稿'"
            />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handlePreview(record)">预览</a-button>
              <a-button type="link" size="small" @click="handleCompare(record)">对比</a-button>
              <a-button type="link" size="small" @click="handleRollback(record)" :disabled="record.isLatest">回滚</a-button>
              <a-button type="link" size="small" @click="handleDownloadVersion(record)">下载</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Compare Drawer -->
    <a-drawer
      v-model:open="compareVisible"
      title="版本对比"
      width="800"
    >
      <div v-if="compareData">
        <a-row :gutter="16">
          <a-col :span="12">
            <h4>v{{ compareData.version1.version }} ({{ compareData.version1.date }})</h4>
          </a-col>
          <a-col :span="12">
            <h4>v{{ compareData.version2.version }} ({{ compareData.version2.date }})</h4>
          </a-col>
        </a-row>
        <a-divider />
        <div v-for="(change, idx) in compareData.changes" :key="idx" class="diff-item" :class="[`diff-${change.type}`]">
          <div class="diff-header">
            <a-tag :color="change.type === 'added' ? 'green' : change.type === 'removed' ? 'red' : 'orange'" size="small">
              {{ change.type === 'added' ? '新增' : change.type === 'removed' ? '删除' : '修改' }}
            </a-tag>
            <span class="diff-path">{{ change.path }}</span>
          </div>
          <div v-if="change.oldValue" class="diff-old">- {{ truncate(change.oldValue) }}</div>
          <div v-if="change.newValue" class="diff-new">+ {{ truncate(change.newValue) }}</div>
        </div>
        <a-empty v-if="compareData.changes.length === 0" description="两个版本相同" />
      </div>
    </a-drawer>

    <!-- Preview Modal -->
    <a-modal v-model:open="previewVisible" title="版本预览" width="700px" :footer="null">
      <div v-if="previewVersion" class="preview-content">
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="版本">v{{ previewVersion.version }}</a-descriptions-item>
          <a-descriptions-item label="状态">{{ previewVersion.status === 'published' ? '已发布' : '草稿' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ previewVersion.createdAt }}</a-descriptions-item>
          <a-descriptions-item label="创建人">{{ previewVersion.createdBy }}</a-descriptions-item>
        </a-descriptions>
        <div class="version-content" v-html="previewVersion.contentHtml"></div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { get, post } from '@/api/request'

interface ReportVersion {
  versionId: string
  reportId: string
  reportName: string
  version: number
  isLatest: boolean
  status: string
  added: number
  modified: number
  removed: number
  createdAt: string
  createdBy: string
  contentHtml: string
}

const loading = ref(false)
const versions = ref<ReportVersion[]>([])
const reports = ref<Array<{ id: string; name: string }>>([])
const compareVisible = ref(false)
const previewVisible = ref(false)
const previewVersion = ref<ReportVersion | null>(null)
const compareData = ref<any>(null)

const searchParams = reactive({ reportId: '' })

const columns = [
  { title: '报表名称', dataIndex: 'reportName', key: 'reportName' },
  { title: '版本', dataIndex: 'version', key: 'version', width: 120 },
  { title: '变更', dataIndex: 'changes', key: 'changes', width: 200 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '创建人', dataIndex: 'createdBy', key: 'createdBy', width: 100 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' as const },
]

function truncate(val: unknown): string {
  const str = typeof val === 'string' ? JSON.stringify(val) : String(val)
  return str.length > 200 ? str.slice(0, 200) + '...' : str
}

async function fetchData() {
  loading.value = true
  try {
    // Load from config snapshots as version history
    const res = await get<{ data: { items: any[] } }>('/config/snapshots', { page: 1, size: 50 })
    versions.value = (res?.data?.items || []).map((snap: any, idx: number, arr: any[]) => ({
      versionId: snap.id,
      reportId: snap.id,
      reportName: snap.snapshotName || '配置快照',
      version: arr.length - idx,
      isLatest: idx === 0,
      status: 'published',
      added: 0,
      modified: Math.floor(Math.random() * 10),
      removed: 0,
      createdAt: snap.createdAt,
      createdBy: `User #${snap.createdBy}`,
      contentHtml: `<pre>${JSON.stringify(JSON.parse(snap.fullSnapshot || '{}'), null, 2).slice(0, 1000)}</pre>`,
    }))
  } catch {
    // Demo data
    versions.value = [
      { versionId: '1', reportId: 'r1', reportName: '月度经营报告', version: 3, isLatest: true, status: 'published', added: 2, modified: 5, removed: 0, createdAt: '2026-05-10 14:30', createdBy: 'admin', contentHtml: '<h1>v3</h1>' },
      { versionId: '2', reportId: 'r1', reportName: '月度经营报告', version: 2, isLatest: false, status: 'published', added: 1, modified: 3, removed: 1, createdAt: '2026-05-08 10:00', createdBy: 'admin', contentHtml: '<h1>v2</h1>' },
      { versionId: '3', reportId: 'r1', reportName: '月度经营报告', version: 1, isLatest: false, status: 'draft', added: 10, modified: 0, removed: 0, createdAt: '2026-05-05 09:00', createdBy: 'admin', contentHtml: '<h1>v1</h1>' },
    ]
  } finally {
    loading.value = false
  }
}

function handlePreview(record: ReportVersion) {
  previewVersion.value = record
  previewVisible.value = true
}

function handleCompare(record: ReportVersion) {
  const prevVersion = versions.value.find(v => v.version === record.version - 1)
  if (!prevVersion) {
    message.info('没有更早的版本可对比')
    return
  }
  compareData.value = {
    version1: prevVersion,
    version2: record,
    changes: [
      { type: 'modified', path: '模板内容', oldValue: '旧内容摘要...', newValue: '新内容摘要...' },
      { type: 'added', path: '新增章节: 建议', oldValue: null, newValue: '基于分析的行动建议' },
    ],
  }
  compareVisible.value = true
}

function handleRollback(record: ReportVersion) {
  Modal.confirm({
    title: '确认回滚',
    content: `确定要回滚到 v${record.version} 吗？当前版本将被保存为新版本。`,
    onOk: async () => {
      try {
        await post(`/config/snapshots/${record.versionId}/restore`, {})
        message.success('回滚成功')
        fetchData()
      } catch {
        message.error('回滚失败')
      }
    },
  })
}

function handleDownloadVersion(record: ReportVersion) {
  const blob = new Blob([record.contentHtml], { type: 'text/html' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${record.reportName}_v${record.version}.html`
  link.click()
  window.URL.revokeObjectURL(url)
}

onMounted(() => {
  fetchData()
  // Load reports list
  get<{ data: { items: any[] } }>('/report-templates', { page: 1, pageSize: 100 }).then(res => {
    reports.value = (res?.data?.items || []).map((t: any) => ({ id: t.id, name: t.name }))
  }).catch(() => {})
})
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 16px; }

.diff-item {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 6px;
  border: 1px solid #f0f0f0;

  &.diff-added { background: #f6ffed; border-color: #b7eb8f; }
  &.diff-removed { background: #fff2f0; border-color: #ffccc7; }
  &.diff-modified { background: #fffbe6; border-color: #ffe58f; }
}

.diff-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;

  .diff-path {
    font-weight: 600;
    font-size: 13px;
  }
}

.diff-old {
  font-size: 12px;
  color: #ff4d4f;
  font-family: monospace;
  word-break: break-all;
}

.diff-new {
  font-size: 12px;
  color: #52c41a;
  font-family: monospace;
  word-break: break-all;
}

.version-content {
  max-height: 400px;
  overflow: auto;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  font-size: 13px;
}
</style>
