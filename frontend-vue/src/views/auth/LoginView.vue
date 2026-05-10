<template>
  <BasicLayout>
    <div class="login-wrapper">
      <a-card class="login-card" :bordered="false">
        <div class="login-header">
          <ThunderboltOutlined class="login-logo" />
          <h1>Enterprise AI Report</h1>
          <p>智能报表平台</p>
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
            >
              登录
            </a-button>
          </a-form-item>
        </a-form>
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
    // Auto-select first tenant if only one
    if (tenants.value.length === 1) {
      formState.tenantId = tenants.value[0].id
    }
  } catch {
    // Fallback: allow login without tenant selection
    tenants.value = []
  } finally {
    tenantsLoading.value = false
  }
}

async function handleLogin() {
  try {
    await authStore.login(formState.username, formState.password)
    // Set tenantId from selected tenant
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
  } catch (err: unknown) {
    // Error already handled by request interceptor
  }
}

onMounted(() => {
  loadTenants()
  // Restore remembered username
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
}

.login-card {
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  padding: 12px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;

  .login-logo {
    font-size: 48px;
    color: #1677ff;
    margin-bottom: 12px;
  }

  h1 {
    font-size: 24px;
    font-weight: 700;
    color: #1a1a1a;
    margin: 0;
  }

  p {
    color: #999;
    margin-top: 4px;
  }
}
</style>
