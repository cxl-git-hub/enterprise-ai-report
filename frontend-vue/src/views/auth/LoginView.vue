<template>
  <BasicLayout>
    <div class="login-wrapper">
      <a-card class="login-card" :bordered="false">
        <div class="login-header">
          <div class="login-logo-ring">
            <ThunderboltOutlined class="login-logo" />
          </div>
          <h1>Enterprise AI Report</h1>
          <p>智能报表平台 · 驱动数据决策</p>
        </div>
        <a-form
          :model="formState"
          :rules="rules"
          layout="vertical"
          @finish="handleLogin"
        >
          <a-form-item label="租户" name="tenantId">
            <a-select
              v-model:value="formState.tenantId"
              size="large"
              placeholder="请选择租户"
              :loading="tenantsLoading"
            >
              <a-select-option v-for="t in tenants" :key="t.id" :value="t.id">
                {{ getTenantDisplayName(t) }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="用户名" name="username">
            <a-input
              v-model:value="formState.username"
              size="large"
              placeholder="请输入用户名"
            >
              <template #prefix><UserOutlined /></template>
            </a-input>
          </a-form-item>
          <a-form-item label="密码" name="password">
            <a-input-password
              v-model:value="formState.password"
              size="large"
              placeholder="请输入密码"
            >
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px">
              <a-checkbox v-model:checked="formState.remember">记住我</a-checkbox>
              <a type="link" style="font-size: 13px">忘记密码？</a>
            </div>
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              block
              :loading="authStore.loading"
              class="login-btn"
            >
              登录
            </a-button>
          </a-form-item>
        </a-form>
        <div class="login-footer">
          <span>Enterprise AI Report Platform v1.0</span>
        </div>
      </a-card>
    </div>
  </BasicLayout>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { get } from '@/api/request'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

interface Tenant {
  id: string
  name: string
  code: string
  tenantName: string
  tenantCode: string
}

const tenants = ref<Tenant[]>([])
const tenantsLoading = ref(false)

const formState = reactive({
  username: '',
  password: '',
  tenantId: undefined as string | undefined,
  remember: false,
})

const rules = {
  tenantId: [{ required: true, message: '请选择租户', trigger: 'change' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

function getTenantDisplayName(t: Tenant): string {
  return t.tenantName || t.name || t.tenantCode || t.code || '未知租户'
}

async function loadTenants() {
  tenantsLoading.value = true
  try {
    const res = await get<{ data: { items: Tenant[] } }>('/tenants', { page: 1, pageSize: 100 })
    tenants.value = res?.data?.items ?? []
    if (tenants.value.length === 1) {
      formState.tenantId = tenants.value[0].id
    }
  } catch {
    tenants.value = []
  } finally {
    tenantsLoading.value = false
  }
}

async function handleLogin() {
  try {
    await authStore.login(formState.username, formState.password)
    if (formState.tenantId) {
      localStorage.setItem('tenantId', String(formState.tenantId))
    }
    if (formState.remember) {
      localStorage.setItem('rememberedUser', formState.username)
    } else {
      localStorage.removeItem('rememberedUser')
    }
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch {
    // Error handled by interceptor
  }
}

onMounted(() => {
  loadTenants()
  const remembered = localStorage.getItem('rememberedUser')
  if (remembered) {
    formState.username = remembered
    formState.remember = true
  }
})
</script>

<style lang="scss" scoped>
.login-wrapper {
  width: 100%;
  max-width: 420px;
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-card {
  border-radius: 16px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;

  .login-logo-ring {
    width: 72px;
    height: 72px;
    border-radius: 50%;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 16px;
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
    animation: pulse 2s ease-in-out infinite;
  }

  .login-logo {
    font-size: 32px;
    color: #fff;
  }

  h1 {
    font-size: 22px;
    font-weight: 700;
    color: #1a1a1a;
    margin: 0;
    letter-spacing: -0.5px;
  }

  p {
    color: #888;
    margin-top: 6px;
    font-size: 13px;
    letter-spacing: 1px;
  }
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4); }
  50% { box-shadow: 0 8px 32px rgba(102, 126, 234, 0.6); }
}

.login-btn {
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
  transition: all 0.3s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
  }

  &:active {
    transform: translateY(0);
  }
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;

  span {
    font-size: 12px;
    color: #bbb;
  }
}
</style>
