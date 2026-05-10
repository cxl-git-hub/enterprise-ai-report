<template>
  <div class="page-container">
    <PageHeader title="AI执行追踪" subtitle="查看AI模型调用记录" />

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="search">
        <a-form-item>
          <a-select v-model:value="searchParams.aiTaskType" placeholder="追踪类型" allow-clear style="width: 150px">
            <a-select-option value="nl2sql">NL2SQL</a-select-option>
            <a-select-option value="analysis">分析</a-select-option>
            <a-select-option value="report">报表生成</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-select v-model:value="searchParams.status" placeholder="状态" allow-clear style="width: 120px">
            <a-select-option value="success">成功</a-select-option>
            <a-select-option value="failed">失败</a-select-option>
            <a-select-option value="pending">待处理</a-select-option>
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
          <template v-if="column.key === 'aiTaskType'">
            <a-tag :color="typeColor[record.aiTaskType] || 'default'">
              {{ typeLabel[record.aiTaskType] || record.aiTaskType }}
            </a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 'success' ? 'success' : record.status === 'failed' ? 'error' : 'processing'"
              :text="record.status === 'success' ? '成功' : record.status === 'failed' ? '失败' : '待处理'"
            />
          </template>
          <template v-if="column.key === 'totalTokens'">
            {{ record.totalTokens?.toLocaleString() || 0 }}
          </template>
          <template v-if="column.key === 'cost'">
            ${{ (record.cost || 0).toFixed(4) }}
          </template>
          <template v-if="column.key === 'latencyMs'">
            {{ record.latencyMs }}ms
          </template>
          <template v-if="column.key === 'action'">
            <router-link :to="`/ai/traces/${record.id}`">
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
import { aiTraceApi, type AiTrace } from '@/api/ai-trace'

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

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, search, resetSearch } =
  useTable<AiTrace>({
    fetchApi: (params) => aiTraceApi.list(params as { page: number; pageSize: number; aiTaskType?: string; status?: string }),
  })

const columns = [
  { title: '追踪类型', dataIndex: 'aiTaskType', key: 'aiTaskType', width: 120 },
  { title: '模型', dataIndex: 'modelName', key: 'modelName', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: 'Prompt Tokens', dataIndex: 'promptTokens', key: 'promptTokens', width: 120 },
  { title: 'Completion Tokens', dataIndex: 'completionTokens', key: 'completionTokens', width: 140 },
  { title: '总Tokens', dataIndex: 'totalTokens', key: 'totalTokens', width: 120 },
  { title: '费用', dataIndex: 'cost', key: 'cost', width: 100 },
  { title: '耗时', dataIndex: 'latencyMs', key: 'latencyMs', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
]
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 16px; }
</style>
