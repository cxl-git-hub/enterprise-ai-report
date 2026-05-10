<template>
  <div class="page-container">
    <PageHeader title="系统设置" subtitle="配置系统参数和偏好" />

    <a-row :gutter="24">
      <a-col :xs="24" :md="6">
        <a-menu v-model:selectedKeys="activeSection" mode="inline" class="settings-menu">
          <a-menu-item key="appearance">
            <SkinOutlined /> 外观设置
          </a-menu-item>
          <a-menu-item key="ai">
            <ThunderboltOutlined /> AI 配置
          </a-menu-item>
          <a-menu-item key="notification">
            <BellOutlined /> 通知设置
          </a-menu-item>
          <a-menu-item key="security">
            <SafetyOutlined /> 安全设置
          </a-menu-item>
          <a-menu-item key="advanced">
            <SettingOutlined /> 高级设置
          </a-menu-item>
        </a-menu>
      </a-col>

      <a-col :xs="24" :md="18">
        <!-- Appearance -->
        <a-card v-if="activeSection[0] === 'appearance'" title="外观设置" :bordered="false" class="page-card">
          <a-form layout="vertical">
            <a-form-item label="主题模式">
              <a-radio-group v-model:value="settings.theme" @change="handleThemeChange">
                <a-radio-button value="light">
                  <SunOutlined /> 浅色模式
                </a-radio-button>
                <a-radio-button value="dark">
                  <MoonOutlined /> 深色模式
                </a-radio-button>
                <a-radio-button value="auto">
                  <DesktopOutlined /> 跟随系统
                </a-radio-button>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="主题色">
              <div class="color-picker">
                <div
                  v-for="color in themeColors"
                  :key="color"
                  class="color-swatch"
                  :class="{ active: settings.primaryColor === color }"
                  :style="{ background: color }"
                  @click="handleColorChange(color)"
                />
              </div>
            </a-form-item>
            <a-form-item label="侧边栏位置">
              <a-radio-group v-model:value="settings.sidebarPosition">
                <a-radio value="left">左侧</a-radio>
                <a-radio value="right">右侧</a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="页面动画">
              <a-switch v-model:checked="settings.enableAnimation" />
              <span class="switch-desc">页面切换时启用过渡动画</span>
            </a-form-item>
            <a-form-item label="紧凑模式">
              <a-switch v-model:checked="settings.compactMode" />
              <span class="switch-desc">减小间距和字体，显示更多内容</span>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="saveSettings">保存设置</a-button>
              <a-button style="margin-left: 8px" @click="resetSettings">恢复默认</a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <!-- AI Config -->
        <a-card v-if="activeSection[0] === 'ai'" title="AI 配置" :bordered="false" class="page-card">
          <a-alert type="info" show-icon style="margin-bottom: 16px">
            <template #message>AI 模型配置需要管理员权限修改，此处仅显示当前配置</template>
          </a-alert>
          <a-form layout="vertical">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="默认模型">
                  <a-input :value="aiConfig.model" disabled />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="API Base URL">
                  <a-input :value="aiConfig.baseUrl" disabled />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="Temperature">
                  <a-slider v-model:value="aiConfig.temperature" :min="0" :max="2" :step="0.1" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="Max Tokens">
                  <a-input-number v-model:value="aiConfig.maxTokens" :min="256" :max="128000" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="超时时间 (秒)">
                  <a-input-number v-model:value="aiConfig.timeout" :min="10" :max="300" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="SQL 安全策略">
              <a-checkbox-group v-model:value="aiConfig.sqlSafety">
                <a-checkbox value="require_where">强制 WHERE 子句</a-checkbox>
                <a-checkbox value="limit_enforcement">强制 LIMIT 限制</a-checkbox>
                <a-checkbox value="select_only">仅允许 SELECT</a-checkbox>
                <a-checkbox value="injection_detection">注入检测</a-checkbox>
              </a-checkbox-group>
            </a-form-item>
            <a-form-item label="每日 AI 调用上限">
              <a-input-number v-model:value="aiConfig.dailyLimit" :min="100" :max="100000" style="width: 200px" />
              <span class="switch-desc">当前租户每日最大 AI 调用次数</span>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="saveAiConfig">保存配置</a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <!-- Notification Settings -->
        <a-card v-if="activeSection[0] === 'notification'" title="通知设置" :bordered="false" class="page-card">
          <a-form layout="vertical">
            <a-divider orientation="left">通知渠道</a-divider>
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="站内通知">
                  <a-switch v-model:checked="notifSettings.inApp" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="邮件通知">
                  <a-switch v-model:checked="notifSettings.email" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="Webhook">
                  <a-switch v-model:checked="notifSettings.webhook" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item v-if="notifSettings.webhook" label="Webhook URL">
              <a-input v-model:value="notifSettings.webhookUrl" placeholder="https://hooks.example.com/notify" />
            </a-form-item>

            <a-divider orientation="left">通知事件</a-divider>
            <a-table :columns="eventColumns" :data-source="notifEvents" :pagination="false" size="small">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'inApp'">
                  <a-switch v-model:checked="record.inApp" size="small" />
                </template>
                <template v-if="column.key === 'email'">
                  <a-switch v-model:checked="record.email" size="small" />
                </template>
                <template v-if="column.key === 'webhook'">
                  <a-switch v-model:checked="record.webhook" size="small" />
                </template>
              </template>
            </a-table>
            <a-form-item style="margin-top: 16px">
              <a-button type="primary" @click="saveNotifSettings">保存设置</a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <!-- Security Settings -->
        <a-card v-if="activeSection[0] === 'security'" title="安全设置" :bordered="false" class="page-card">
          <a-form layout="vertical">
            <a-form-item label="登录超时">
              <a-select v-model:value="security.sessionTimeout" style="width: 200px">
                <a-select-option :value="30">30 分钟</a-select-option>
                <a-select-option :value="60">1 小时</a-select-option>
                <a-select-option :value="120">2 小时</a-select-option>
                <a-select-option :value="480">8 小时</a-select-option>
                <a-select-option :value="1440">24 小时</a-select-option>
              </a-select>
              <span class="switch-desc">无操作后自动登出的时间</span>
            </a-form-item>
            <a-form-item label="双因素认证">
              <a-switch v-model:checked="security.twoFactor" />
              <span class="switch-desc">登录时需要验证码确认</span>
            </a-form-item>
            <a-form-item label="IP 白名单">
              <a-textarea
                v-model:value="security.ipWhitelist"
                :rows="3"
                placeholder="每行一个 IP 地址，留空表示不限制&#10;192.168.1.0/24&#10;10.0.0.1"
              />
            </a-form-item>
            <a-form-item label="密码策略">
              <a-checkbox-group v-model:value="security.passwordPolicy">
                <a-checkbox value="uppercase">包含大写字母</a-checkbox>
                <a-checkbox value="lowercase">包含小写字母</a-checkbox>
                <a-checkbox value="number">包含数字</a-checkbox>
                <a-checkbox value="special">包含特殊字符</a-checkbox>
              </a-checkbox-group>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="saveSecurity">保存设置</a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <!-- Advanced Settings -->
        <a-card v-if="activeSection[0] === 'advanced'" title="高级设置" :bordered="false" class="page-card">
          <a-alert type="warning" show-icon style="margin-bottom: 16px">
            <template #message>修改高级设置可能影响系统稳定性，请谨慎操作</template>
          </a-alert>
          <a-form layout="vertical">
            <a-form-item label="数据保留策略">
              <a-select v-model:value="advanced.dataRetention" style="width: 200px">
                <a-select-option :value="30">30 天</a-select-option>
                <a-select-option :value="90">90 天</a-select-option>
                <a-select-option :value="180">180 天</a-select-option>
                <a-select-option :value="365">1 年</a-select-option>
                <a-select-option :value="-1">永久保留</a-select-option>
              </a-select>
              <span class="switch-desc">自动清理超过指定天数的执行日志和追踪记录</span>
            </a-form-item>
            <a-form-item label="最大并发工作流">
              <a-input-number v-model:value="advanced.maxConcurrentWorkflows" :min="1" :max="50" style="width: 200px" />
            </a-form-item>
            <a-form-item label="报表存储上限 (MB)">
              <a-input-number v-model:value="advanced.maxReportStorage" :min="100" :max="10240" style="width: 200px" />
            </a-form-item>
            <a-form-item label="调试模式">
              <a-switch v-model:checked="advanced.debugMode" />
              <span class="switch-desc">启用详细日志记录，仅建议开发环境使用</span>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="saveAdvanced">保存设置</a-button>
              <a-button danger style="margin-left: 8px" @click="handleClearCache">
                <DeleteOutlined /> 清除缓存
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  SkinOutlined, ThunderboltOutlined, BellOutlined, SafetyOutlined, SettingOutlined,
  SunOutlined, MoonOutlined, DesktopOutlined, DeleteOutlined,
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useAppStore } from '@/stores/app'
import { put, get } from '@/api/request'

const appStore = useAppStore()
const activeSection = ref(['appearance'])

const themeColors = ['#1677ff', '#722ed1', '#13c2c2', '#52c41a', '#fa541c', '#eb2f96', '#faad14', '#2f54eb']

const settings = reactive({
  theme: appStore.theme || 'light',
  primaryColor: '#1677ff',
  sidebarPosition: 'left',
  enableAnimation: true,
  compactMode: false,
})

const aiConfig = reactive({
  model: 'gpt-4',
  baseUrl: 'https://api.openai.com/v1',
  temperature: 0.7,
  maxTokens: 4096,
  timeout: 60,
  sqlSafety: ['require_where', 'limit_enforcement', 'select_only', 'injection_detection'],
  dailyLimit: 10000,
})

const notifSettings = reactive({
  inApp: true,
  email: false,
  webhook: false,
  webhookUrl: '',
})

const notifEvents = ref([
  { event: '工作流执行完成', inApp: true, email: false, webhook: false },
  { event: '工作流执行失败', inApp: true, email: true, webhook: true },
  { event: '报表生成完成', inApp: true, email: false, webhook: false },
  { event: '系统异常告警', inApp: true, email: true, webhook: true },
  { event: 'AI 调用超限', inApp: true, email: false, webhook: false },
  { event: '存储空间不足', inApp: true, email: true, webhook: false },
])

const eventColumns = [
  { title: '事件', dataIndex: 'event', key: 'event' },
  { title: '站内', dataIndex: 'inApp', key: 'inApp', width: 80 },
  { title: '邮件', dataIndex: 'email', key: 'email', width: 80 },
  { title: 'Webhook', dataIndex: 'webhook', key: 'webhook', width: 100 },
]

const security = reactive({
  sessionTimeout: 120,
  twoFactor: false,
  ipWhitelist: '',
  passwordPolicy: ['lowercase', 'number'],
})

const advanced = reactive({
  dataRetention: 90,
  maxConcurrentWorkflows: 5,
  maxReportStorage: 1024,
  debugMode: false,
})

function handleThemeChange(e: { target: { value: string } }) {
  const theme = e.target.value as 'light' | 'dark' | 'auto'
  appStore.setTheme(theme)
}

function handleColorChange(color: string) {
  settings.primaryColor = color
  appStore.setPrimaryColor(color)
}

async function saveSettings() {
  try {
    await put('/settings/appearance', settings)
    appStore.setTheme(settings.theme as 'light' | 'dark')
    message.success('外观设置已保存')
  } catch {
    message.error('保存失败')
  }
}

function resetSettings() {
  settings.theme = 'light'
  settings.primaryColor = '#1677ff'
  settings.sidebarPosition = 'left'
  settings.enableAnimation = true
  settings.compactMode = false
  appStore.setTheme('light')
  message.success('已恢复默认设置')
}

async function saveAiConfig() {
  try {
    await put('/settings/ai', aiConfig)
    message.success('AI 配置已保存')
  } catch {
    message.error('保存失败')
  }
}

async function saveNotifSettings() {
  try {
    await put('/settings/notifications', { ...notifSettings, events: notifEvents.value })
    message.success('通知设置已保存')
  } catch {
    message.error('保存失败')
  }
}

async function saveSecurity() {
  try {
    await put('/settings/security', security)
    message.success('安全设置已保存')
  } catch {
    message.error('保存失败')
  }
}

async function saveAdvanced() {
  try {
    await put('/settings/advanced', advanced)
    message.success('高级设置已保存')
  } catch {
    message.error('保存失败')
  }
}

function handleClearCache() {
  Modal.confirm({
    title: '确认清除缓存',
    content: '清除缓存将刷新所有缓存数据，可能需要重新加载页面。确定继续？',
    onOk: async () => {
      try {
        await put('/settings/clear-cache', {})
        message.success('缓存已清除')
      } catch {
        message.error('清除失败')
      }
    },
  })
}

onMounted(async () => {
  try {
    const res = await get<{ data: Record<string, unknown> }>('/settings')
    if (res?.data) {
      if (res.data.appearance) Object.assign(settings, res.data.appearance)
      if (res.data.ai) Object.assign(aiConfig, res.data.ai)
      if (res.data.security) Object.assign(security, res.data.security)
      if (res.data.advanced) Object.assign(advanced, res.data.advanced)
    }
  } catch {}
})
</script>

<style lang="scss" scoped>
.settings-menu {
  border-right: none;
  background: transparent;
}

.color-picker {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.color-swatch {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  cursor: pointer;
  border: 3px solid transparent;
  transition: all 0.2s;

  &:hover {
    transform: scale(1.1);
  }

  &.active {
    border-color: #333;
    box-shadow: 0 0 0 2px #fff, 0 0 0 4px currentColor;
  }
}

.switch-desc {
  margin-left: 8px;
  font-size: 13px;
  color: #999;
}
</style>
