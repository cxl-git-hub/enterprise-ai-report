<template>
  <div class="page-container">
    <PageHeader title="通知中心" subtitle="查看所有系统通知">
      <template #actions>
        <a-space>
          <a-button @click="markAllRead" :disabled="unreadCount === 0">
            <CheckOutlined /> 全部已读
          </a-button>
          <a-button @click="handleClearAll" danger>
            <DeleteOutlined /> 清空
          </a-button>
        </a-space>
      </template>
    </PageHeader>

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="fetchData">
        <a-form-item>
          <a-select v-model:value="searchParams.type" placeholder="通知类型" allow-clear style="width: 150px">
            <a-select-option value="success">成功</a-select-option>
            <a-select-option value="error">错误</a-select-option>
            <a-select-option value="warning">警告</a-select-option>
            <a-select-option value="info">信息</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-select v-model:value="searchParams.read" placeholder="已读状态" allow-clear style="width: 120px">
            <a-select-option :value="true">已读</a-select-option>
            <a-select-option :value="false">未读</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">筛选</a-button>
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
          <template v-if="column.key === 'type'">
            <a-tag :color="typeColor[record.type] || 'default'">
              {{ typeLabel[record.type] || record.type }}
            </a-tag>
          </template>
          <template v-if="column.key === 'message'">
            <span :class="{ 'unread-text': !record.read }">{{ record.message }}</span>
          </template>
          <template v-if="column.key === 'read'">
            <a-badge :status="record.read ? 'default' : 'processing'" :text="record.read ? '已读' : '未读'" />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button v-if="!record.read" type="link" size="small" @click="markRead(record.id)">
                标为已读
              </a-button>
              <a-button v-if="record.link" type="link" size="small" @click="goTo(record.link)">
                查看
              </a-button>
              <a-button type="link" size="small" danger @click="deleteNotif(record.id)">
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { CheckOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useTable } from '@/composables/useTable'
import { put, del } from '@/api/request'

const router = useRouter()

const typeColor: Record<string, string> = {
  success: 'green',
  error: 'red',
  warning: 'orange',
  info: 'blue',
}

const typeLabel: Record<string, string> = {
  success: '成功',
  error: '错误',
  warning: '警告',
  info: '信息',
}

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, resetSearch } =
  useTable<any>({
    fetchApi: (params) => {
      const { get } = require('@/api/request')
      return get('/notifications', params)
    },
  })

const unreadCount = computed(() => dataSource.value.filter((n: any) => !n.read).length)

const columns = [
  { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '内容', dataIndex: 'message', key: 'message' },
  { title: '状态', dataIndex: 'read', key: 'read', width: 100 },
  { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const },
]

async function markRead(id: string) {
  try {
    await put(`/notifications/${id}/read`, {})
    fetchData()
  } catch {}
}

async function markAllRead() {
  try {
    await put('/notifications/read-all', {})
    message.success('已全部标为已读')
    fetchData()
  } catch {}
}

function goTo(link: string) {
  router.push(link)
}

async function deleteNotif(id: string) {
  try {
    await del(`/notifications/${id}`)
    fetchData()
  } catch {}
}

function handleClearAll() {
  Modal.confirm({
    title: '确认清空通知',
    content: '清空后无法恢复，确定继续？',
    onOk: async () => {
      try {
        await put('/notifications/clear', {})
        message.success('已清空通知')
        fetchData()
      } catch {}
    },
  })
}
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 16px; }
.unread-text { font-weight: 600; }
</style>
