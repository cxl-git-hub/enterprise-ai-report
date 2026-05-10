<template>
  <div class="page-container">
    <PageHeader title="工作流运行记录" subtitle="查看工作流运行状态和历史" />

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="search">
        <a-form-item>
          <a-input v-model:value="searchParams.keyword" placeholder="搜索工作流名称" allow-clear>
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-select v-model:value="searchParams.status" placeholder="状态筛选" allow-clear style="width: 150px">
            <a-select-option value="success">成功</a-select-option>
            <a-select-option value="failed">失败</a-select-option>
            <a-select-option value="running">运行中</a-select-option>
            <a-select-option value="pending">等待中</a-select-option>
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
          <template v-if="column.key === 'status'">
            <a-badge
              :status="getStatusBadge(record.status)"
              :text="getStatusText(record.status)"
            />
          </template>
          <template v-if="column.key === 'triggerType'">
            <a-tag :color="record.triggerType === 'scheduled' ? 'blue' : 'green'">
              {{ record.triggerType === 'scheduled' ? '定时触发' : '手动触发' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'duration'">
            {{ formatDuration(record.duration) }}
          </template>
          <template v-if="column.key === 'totalCost'">
            ${{ (record.totalCost || 0).toFixed(4) }}
          </template>
          <template v-if="column.key === 'action'">
            <router-link :to="`/workflow/run/${record.id}`">
              <a-button type="link" size="small">查看详情</a-button>
            </router-link>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { SearchOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useTable } from '@/composables/useTable'
import { workflowApi, type WorkflowRun } from '@/api/workflow'

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, search, resetSearch } =
  useTable<WorkflowRun>({
    fetchApi: (params) => workflowApi.listRuns(params as { page: number; pageSize: number; workflowId?: string; status?: string }),
  })

const columns = [
  { title: '工作流', dataIndex: 'workflowName', key: 'workflowName' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '触发方式', dataIndex: 'triggerType', key: 'triggerType', width: 120 },
  { title: '开始时间', dataIndex: 'startedAt', key: 'startedAt', width: 180 },
  { title: '结束时间', dataIndex: 'finishedAt', key: 'finishedAt', width: 180 },
  { title: '耗时', dataIndex: 'duration', key: 'duration', width: 120 },
  { title: 'Token用量', dataIndex: 'totalTokens', key: 'totalTokens', width: 120 },
  { title: '费用', dataIndex: 'totalCost', key: 'totalCost', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const },
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
  if (!ms) return '-'
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${Math.floor(ms / 60000)}m ${Math.round((ms % 60000) / 1000)}s`
}
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 16px; }
</style>
