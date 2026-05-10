<template>
  <div class="page-container">
    <PageHeader title="审计日志" subtitle="系统操作审计记录" />

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="search">
        <a-form-item>
          <a-select v-model:value="searchParams.action" placeholder="操作类型" allow-clear style="width: 150px">
            <a-select-option value="create">创建</a-select-option>
            <a-select-option value="update">更新</a-select-option>
            <a-select-option value="delete">删除</a-select-option>
            <a-select-option value="login">登录</a-select-option>
            <a-select-option value="logout">登出</a-select-option>
            <a-select-option value="execute">执行</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-select v-model:value="searchParams.resourceType" placeholder="资源类型" allow-clear style="width: 150px">
            <a-select-option value="tenant">租户</a-select-option>
            <a-select-option value="user">用户</a-select-option>
            <a-select-option value="datasource">数据源</a-select-option>
            <a-select-option value="dataset">数据集</a-select-option>
            <a-select-option value="schema">Schema</a-select-option>
            <a-select-option value="kpi">KPI</a-select-option>
            <a-select-option value="workflow">工作流</a-select-option>
            <a-select-option value="prompt">提示词</a-select-option>
            <a-select-option value="report_template">报表模板</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-range-picker
            v-model:value="dateRange"
            :placeholder="['开始日期', '结束日期']"
            style="width: 240px"
            @change="handleDateChange"
          />
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
          <template v-if="column.key === 'action'">
            <a-tag :color="actionColor[record.action] || 'default'">
              {{ actionLabel[record.action] || record.action }}
            </a-tag>
          </template>
          <template v-if="column.key === 'resourceType'">
            <a-tag>{{ resourceTypeLabel[record.resourceType] || record.resourceType }}</a-tag>
          </template>
          <template v-if="column.key === 'details'">
            <a-tooltip :title="formatJson(record.details)">
              <a-button type="link" size="small">查看详情</a-button>
            </a-tooltip>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { Dayjs } from 'dayjs'
import PageHeader from '@/components/common/PageHeader.vue'
import { useTable } from '@/composables/useTable'
import { auditApi, type AuditLog } from '@/api/audit'

const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const actionColor: Record<string, string> = {
  create: 'green',
  update: 'blue',
  delete: 'red',
  login: 'cyan',
  logout: 'orange',
  execute: 'purple',
}

const actionLabel: Record<string, string> = {
  create: '创建',
  update: '更新',
  delete: '删除',
  login: '登录',
  logout: '登出',
  execute: '执行',
}

const resourceTypeLabel: Record<string, string> = {
  tenant: '租户',
  user: '用户',
  datasource: '数据源',
  dataset: '数据集',
  schema: 'Schema',
  kpi: 'KPI',
  workflow: '工作流',
  prompt: '提示词',
  report_template: '报表模板',
}

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, search, resetSearch } =
  useTable<AuditLog>({
    fetchApi: (params) =>
      auditApi.list(
        params as {
          page: number
          pageSize: number
          action?: string
          resourceType?: string
          userId?: string
          startDate?: string
          endDate?: string
        }
      ),
  })

const columns = [
  { title: '操作人', dataIndex: 'username', key: 'username', width: 120 },
  { title: '操作类型', dataIndex: 'action', key: 'action', width: 100 },
  { title: '资源类型', dataIndex: 'resourceType', key: 'resourceType', width: 120 },
  { title: '资源名称', dataIndex: 'resourceName', key: 'resourceName', ellipsis: true },
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress', width: 140 },
  { title: '详情', dataIndex: 'details', key: 'details', width: 100 },
  { title: '操作时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
]

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates) {
    searchParams.startDate = dates[0].format('YYYY-MM-DD')
    searchParams.endDate = dates[1].format('YYYY-MM-DD')
  } else {
    searchParams.startDate = undefined
    searchParams.endDate = undefined
  }
  // Trigger search with updated date range
  search()
}

function formatJson(obj: unknown): string {
  try {
    return JSON.stringify(obj, null, 2)
  } catch {
    return String(obj)
  }
}
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 16px; }
</style>
