<template>
  <div class="page-container">
    <PageHeader title="个人中心" subtitle="管理您的个人信息和偏好设置" />

    <a-row :gutter="24">
      <!-- Profile Card -->
      <a-col :xs="24" :md="8">
        <a-card :bordered="false" class="page-card profile-card">
          <div class="avatar-section">
            <a-avatar :size="96" :style="{ backgroundColor: '#1677ff', fontSize: '40px' }">
              {{ user?.realName?.charAt(0) || user?.username?.charAt(0) || 'U' }}
            </a-avatar>
            <h2 class="user-name">{{ user?.realName || user?.username }}</h2>
            <p class="user-role">
              <a-tag v-for="role in user?.roles" :key="role" color="blue">{{ role }}</a-tag>
            </p>
          </div>
          <a-divider />
          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="用户名">{{ user?.username }}</a-descriptions-item>
            <a-descriptions-item label="邮箱">{{ user?.email || '-' }}</a-descriptions-item>
            <a-descriptions-item label="租户">{{ user?.tenantName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="注册时间">{{ user?.createdAt || '-' }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- Quick Stats -->
        <a-card title="我的统计" :bordered="false" class="page-card" style="margin-top: 16px">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-statistic title="创建的KPI" :value="myStats.kpiCount" />
            </a-col>
            <a-col :span="8">
              <a-statistic title="工作流" :value="myStats.workflowCount" />
            </a-col>
            <a-col :span="8">
              <a-statistic title="报表" :value="myStats.reportCount" />
            </a-col>
          </a-row>
        </a-card>
      </a-col>

      <!-- Settings Sections -->
      <a-col :xs="24" :md="16">
        <!-- Basic Info -->
        <a-card title="基本信息" :bordered="false" class="page-card">
          <a-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            layout="vertical"
            @finish="handleUpdateProfile"
          >
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="显示名称" name="realName">
                  <a-input v-model:value="profileForm.realName" placeholder="请输入显示名称" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="邮箱" name="email">
                  <a-input v-model:value="profileForm.email" placeholder="请输入邮箱" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="手机号" name="phone">
                  <a-input v-model:value="profileForm.phone" placeholder="请输入手机号" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="语言" name="locale">
                  <a-select v-model:value="profileForm.locale">
                    <a-select-option value="zh-CN">简体中文</a-select-option>
                    <a-select-option value="en-US">English</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item>
              <a-button type="primary" html-type="submit" :loading="saving">保存修改</a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <!-- Change Password -->
        <a-card title="修改密码" :bordered="false" class="page-card" style="margin-top: 16px">
          <a-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            layout="vertical"
            @finish="handleChangePassword"
          >
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="当前密码" name="oldPassword">
                  <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入当前密码" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="新密码" name="newPassword">
                  <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="确认新密码" name="confirmPassword">
                  <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item>
              <a-button type="primary" html-type="submit" :loading="changingPassword">修改密码</a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <!-- Notification Preferences -->
        <a-card title="通知偏好" :bordered="false" class="page-card" style="margin-top: 16px">
          <a-form layout="vertical">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="工作流完成通知">
                  <a-switch v-model:checked="notifyPrefs.workflowComplete" />
                  <span class="switch-desc">工作流执行完成时通知</span>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="工作流失败通知">
                  <a-switch v-model:checked="notifyPrefs.workflowFailed" />
                  <span class="switch-desc">工作流执行失败时通知</span>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="报表生成通知">
                  <a-switch v-model:checked="notifyPrefs.reportReady" />
                  <span class="switch-desc">报表生成完成时通知</span>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="系统告警通知">
                  <a-switch v-model:checked="notifyPrefs.systemAlert" />
                  <span class="switch-desc">系统异常时通知</span>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item>
              <a-button type="primary" @click="saveNotifyPrefs" :loading="savingNotify">保存偏好</a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <!-- Keyboard Shortcuts -->
        <a-card title="键盘快捷键" :bordered="false" class="page-card" style="margin-top: 16px">
          <a-table :columns="shortcutColumns" :data-source="shortcuts" :pagination="false" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'shortcut'">
                <a-tag v-for="key in record.keys" :key="key" style="margin-right: 4px">{{ key }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useAuthStore } from '@/stores/auth'
import { put, get } from '@/api/request'
import type { FormInstance } from 'ant-design-vue'

const authStore = useAuthStore()
const user = authStore.user

const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const saving = ref(false)
const changingPassword = ref(false)
const savingNotify = ref(false)

const profileForm = reactive({
  realName: user?.realName || '',
  email: user?.email || '',
  phone: '',
  locale: 'zh-CN',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const notifyPrefs = reactive({
  workflowComplete: true,
  workflowFailed: true,
  reportReady: true,
  systemAlert: false,
})

const myStats = reactive({
  kpiCount: 0,
  workflowCount: 0,
  reportCount: 0,
})

const profileRules = {
  realName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email' as const, message: '请输入有效邮箱', trigger: 'blur' },
  ],
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string) => {
        if (value && value !== passwordForm.newPassword) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
      trigger: 'blur',
    },
  ],
}

const shortcuts = [
  { action: '全局搜索', keys: ['Ctrl', 'K'] },
  { action: '新建工作流', keys: ['Ctrl', 'N'] },
  { action: '返回仪表盘', keys: ['Ctrl', 'D'] },
  { action: '切换侧边栏', keys: ['Ctrl', 'B'] },
  { action: '打开设置', keys: ['Ctrl', ','] },
  { action: '切换深色模式', keys: ['Ctrl', 'Shift', 'D'] },
]

const shortcutColumns = [
  { title: '操作', dataIndex: 'action', key: 'action' },
  { title: '快捷键', dataIndex: 'shortcut', key: 'shortcut' },
]

async function handleUpdateProfile() {
  saving.value = true
  try {
    await put('/auth/profile', profileForm)
    message.success('个人信息已更新')
  } catch {
    message.error('更新失败')
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  changingPassword.value = true
  try {
    await put('/auth/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    message.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch {
    message.error('密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

async function saveNotifyPrefs() {
  savingNotify.value = true
  try {
    await put('/auth/notification-preferences', notifyPrefs)
    message.success('通知偏好已保存')
  } catch {
    message.error('保存失败')
  } finally {
    savingNotify.value = false
  }
}

onMounted(async () => {
  try {
    const res = await get<{ data: typeof myStats }>('/auth/my-stats')
    if (res?.data) Object.assign(myStats, res.data)
  } catch {}
})
</script>

<style lang="scss" scoped>
.profile-card {
  text-align: center;
}

.avatar-section {
  padding: 24px 0 16px;

  .user-name {
    margin-top: 16px;
    font-size: 20px;
    font-weight: 600;
    color: #1a1a1a;
  }

  .user-role {
    margin-top: 8px;
  }
}

.switch-desc {
  margin-left: 8px;
  font-size: 13px;
  color: #999;
}
</style>
