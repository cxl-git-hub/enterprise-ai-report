<template>
  <a-layout class="main-layout">
    <a-layout-sider
      v-model:collapsed="appStore.sidebarCollapsed"
      :trigger="null"
      collapsible
      :width="256"
      :collapsed-width="80"
      theme="dark"
      class="main-sider"
    >
      <div class="logo">
        <ThunderboltOutlined class="logo-icon" />
        <span v-if="!appStore.sidebarCollapsed" class="logo-text">AI Report</span>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        mode="inline"
        theme="dark"
        @click="handleMenuClick"
      >
        <a-menu-item key="/dashboard">
          <DashboardOutlined />
          <span>仪表盘</span>
        </a-menu-item>

        <a-sub-menu key="admin">
          <template #title>
            <SettingOutlined />
            <span>系统管理</span>
          </template>
          <a-menu-item key="/admin/tenants">租户管理</a-menu-item>
          <a-menu-item key="/admin/users">用户管理</a-menu-item>
          <a-menu-item key="/admin/roles">角色管理</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="datahub">
          <template #title>
            <DatabaseOutlined />
            <span>数据管理</span>
          </template>
          <a-menu-item key="/datahub/datasources">数据源管理</a-menu-item>
          <a-menu-item key="/datahub/datasets">数据集管理</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="config">
          <template #title>
            <NodeIndexOutlined />
            <span>配置中心</span>
          </template>
          <a-menu-item key="/config/schemas">Schema管理</a-menu-item>
          <a-menu-item key="/config/kpis">KPI管理</a-menu-item>
          <a-menu-item key="/config/prompts">提示词模板</a-menu-item>
          <a-menu-item key="/config/report-templates">报表模板</a-menu-item>
          <a-menu-item key="/config/consistency">配置一致性</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="workflow">
          <template #title>
            <ApartmentOutlined />
            <span>工作流</span>
          </template>
          <a-menu-item key="/workflow/definitions">工作流定义</a-menu-item>
          <a-menu-item key="/workflow/runs">运行记录</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="ai">
          <template #title>
            <ThunderboltOutlined />
            <span>AI能力</span>
          </template>
          <a-menu-item key="/ai/nl2sql">NL2SQL</a-menu-item>
          <a-menu-item key="/ai/analysis">AI分析</a-menu-item>
          <a-menu-item key="/ai/traces">执行追踪</a-menu-item>
        </a-sub-menu>

        <a-menu-item key="/output/reports">
          <DownloadOutlined />
          <span>报表输出</span>
        </a-menu-item>

        <a-menu-item key="/audit">
          <AuditOutlined />
          <span>审计日志</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="main-header" :style="{ marginLeft: contentMargin }">
        <div class="header-left">
          <MenuFoldOutlined
            v-if="!appStore.sidebarCollapsed"
            class="trigger"
            @click="appStore.toggleSidebar()"
          />
          <MenuUnfoldOutlined v-else class="trigger" @click="appStore.toggleSidebar()" />
          <a-breadcrumb class="breadcrumb">
            <a-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        <div class="header-right">
          <a-input-search
            v-model:value="globalSearch"
            placeholder="搜索功能..."
            style="width: 200px; margin-right: 16px"
            @search="handleGlobalSearch"
            allow-clear
          />
          <NotificationCenter style="margin-right: 16px" />
          <a-dropdown>
            <span class="user-info">
              <a-avatar :size="28" style="background-color: #1677ff">
                {{ authStore.displayName?.charAt(0) || 'U' }}
              </a-avatar>
              <span class="username">{{ authStore.displayName || '用户' }}</span>
            </span>
            <template #overlay>
              <a-menu @click="handleUserMenuClick">
                <a-menu-item key="profile">个人中心</a-menu-item>
                <a-menu-item key="settings">系统设置</a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="main-content" :style="{ marginLeft: contentMargin }">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <keep-alive :max="10">
              <component :is="Component" />
            </keep-alive>
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  DashboardOutlined,
  SettingOutlined,
  DatabaseOutlined,
  NodeIndexOutlined,
  ApartmentOutlined,
  ThunderboltOutlined,
  DownloadOutlined,
  AuditOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons-vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import NotificationCenter from '@/components/common/NotificationCenter.vue'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const authStore = useAuthStore()

const selectedKeys = ref<string[]>([route.path])
const openKeys = ref<string[]>([])
const globalSearch = ref('')

const contentMargin = computed(() => appStore.sidebarCollapsed ? '80px' : '256px')

const breadcrumbs = computed(() => {
  const matched = route.matched.filter((item) => item.meta?.title)
  return matched.map((item) => ({
    path: item.path,
    title: item.meta.title as string,
  }))
})

watch(
  () => route.path,
  (path) => {
    selectedKeys.value = [path]
    const parts = path.split('/')
    if (parts.length > 2) {
      openKeys.value = [parts[1]]
    }
  },
  { immediate: true }
)

function handleMenuClick({ key }: { key: string }) {
  router.push(key)
}

function handleUserMenuClick({ key }: { key: string }) {
  if (key === 'logout') {
    authStore.logout()
    router.push('/login')
  } else if (key === 'profile') {
    router.push('/profile')
  } else if (key === 'settings') {
    router.push('/settings')
  }
}

function handleGlobalSearch(value: string) {
  if (!value.trim()) return
  // Search through menu items
  const searchMap: Record<string, string> = {
    '数据源': '/datahub/datasources',
    '数据集': '/datahub/datasets',
    'Schema': '/config/schemas',
    'KPI': '/config/kpis',
    '提示词': '/config/prompts',
    '模板': '/config/report-templates',
    '一致性': '/config/consistency',
    '工作流': '/workflow/definitions',
    '运行': '/workflow/runs',
    'NL2SQL': '/ai/nl2sql',
    '分析': '/ai/analysis',
    '追踪': '/ai/traces',
    '报表': '/output/reports',
    '审计': '/audit',
    '用户': '/admin/users',
    '租户': '/admin/tenants',
    '角色': '/admin/roles',
    '仪表盘': '/dashboard',
  }
  const keyword = value.toLowerCase()
  for (const [key, path] of Object.entries(searchMap)) {
    if (key.toLowerCase().includes(keyword)) {
      router.push(path)
      globalSearch.value = ''
      return
    }
  }
  message.info('未找到匹配的功能页面')
}
</script>

<style lang="scss" scoped>
.main-layout {
  min-height: 100vh;
}

.main-sider {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 10;
  overflow-y: auto;
  overflow-x: hidden;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.05);

  .logo-icon {
    font-size: 24px;
    color: #1677ff;
  }

  .logo-text {
    font-size: 18px;
    font-weight: 700;
    color: #fff;
  }
}

.main-header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 5;
  transition: margin-left 0.2s;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.trigger {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
  color: #333;

  &:hover {
    color: #1677ff;
  }
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 0 12px;
  border-radius: 6px;
  transition: background 0.3s;

  &:hover {
    background: #f5f5f5;
  }

  .username {
    font-size: 14px;
    color: #333;
  }
}

.main-content {
  padding: 24px;
  min-height: calc(100vh - 64px);
  transition: margin-left 0.2s;
  background: #f5f5f5;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
