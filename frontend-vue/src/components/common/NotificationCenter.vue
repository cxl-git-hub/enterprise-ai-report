<template>
  <a-dropdown v-model:open="visible" trigger="click" placement="bottomRight">
    <a-badge :count="unreadCount" :overflow-count="99">
      <BellOutlined class="notification-bell" :class="{ active: visible }" />
    </a-badge>
    <template #overlay>
      <div class="notification-panel">
        <div class="notification-header">
          <span class="notification-title">通知</span>
          <a-space>
            <a-button type="link" size="small" @click="markAllRead" :disabled="unreadCount === 0">
              全部已读
            </a-button>
            <a-button type="link" size="small" @click="goToAll">
              查看全部
            </a-button>
          </a-space>
        </div>
        <a-tabs v-model:activeKey="activeTab" size="small" class="notification-tabs">
          <a-tab-pane key="all" tab="全部" />
          <a-tab-pane key="unread" :tab="`未读(${unreadCount})`" />
        </a-tabs>
        <div class="notification-list">
          <div v-if="filteredNotifications.length === 0" class="notification-empty">
            <InboxOutlined style="font-size: 32px; color: #d9d9d9" />
            <p>暂无通知</p>
          </div>
          <div
            v-for="item in filteredNotifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: !item.read }"
            @click="handleClick(item)"
          >
            <div class="notification-icon" :style="{ background: getIconBg(item.type) }">
              <CheckCircleOutlined v-if="item.type === 'success'" style="color: #52c41a" />
              <CloseCircleOutlined v-else-if="item.type === 'error'" style="color: #ff4d4f" />
              <WarningOutlined v-else-if="item.type === 'warning'" style="color: #faad14" />
              <InfoCircleOutlined v-else style="color: #1677ff" />
            </div>
            <div class="notification-content">
              <div class="notification-msg">{{ item.message }}</div>
              <div class="notification-time">{{ formatTime(item.createdAt) }}</div>
            </div>
            <div v-if="!item.read" class="notification-dot" />
          </div>
        </div>
      </div>
    </template>
  </a-dropdown>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  BellOutlined, InboxOutlined, CheckCircleOutlined, CloseCircleOutlined,
  WarningOutlined, InfoCircleOutlined,
} from '@ant-design/icons-vue'
import { get, put } from '@/api/request'

const router = useRouter()
const visible = ref(false)
const activeTab = ref('all')
let pollTimer: ReturnType<typeof setInterval> | null = null

interface Notification {
  id: string
  type: 'success' | 'error' | 'warning' | 'info'
  message: string
  read: boolean
  createdAt: string
  link?: string
}

const notifications = ref<Notification[]>([])

const unreadCount = computed(() => notifications.value.filter((n) => !n.read).length)

const filteredNotifications = computed(() => {
  if (activeTab.value === 'unread') {
    return notifications.value.filter((n) => !n.read)
  }
  return notifications.value
})

function getIconBg(type: string) {
  const map: Record<string, string> = {
    success: '#f6ffed',
    error: '#fff2f0',
    warning: '#fffbe6',
    info: '#e6f4ff',
  }
  return map[type] || '#f5f5f5'
}

function formatTime(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

async function handleClick(item: Notification) {
  if (!item.read) {
    try {
      await put(`/notifications/${item.id}/read`, {})
      item.read = true
    } catch {}
  }
  if (item.link) {
    router.push(item.link)
    visible.value = false
  }
}

async function markAllRead() {
  try {
    await put('/notifications/read-all', {})
    notifications.value.forEach((n) => (n.read = true))
  } catch {}
}

function goToAll() {
  router.push('/notifications')
  visible.value = false
}

async function loadNotifications() {
  try {
    const res = await get<{ data: Notification[] }>('/notifications', { limit: 20 })
    if (res?.data) {
      notifications.value = res.data
    }
  } catch {}
}

onMounted(() => {
  loadNotifications()
  pollTimer = setInterval(loadNotifications, 60000) // Poll every minute
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style lang="scss" scoped>
.notification-bell {
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;

  &:hover,
  &.active {
    background: #f5f5f5;
    color: #1677ff;
  }
}

.notification-panel {
  width: 360px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;

  .notification-title {
    font-size: 16px;
    font-weight: 600;
  }
}

.notification-tabs {
  :deep(.ant-tabs-nav) {
    margin: 0;
    padding: 0 16px;
  }
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
}

.notification-empty {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;

  &:hover {
    background: #fafafa;
  }

  &.unread {
    background: #f0f7ff;
  }
}

.notification-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}

.notification-content {
  flex: 1;
  min-width: 0;

  .notification-msg {
    font-size: 13px;
    color: #333;
    line-height: 1.5;
  }

  .notification-time {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }
}

.notification-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1677ff;
  flex-shrink: 0;
  margin-top: 6px;
}
</style>
