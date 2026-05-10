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
import { reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formState = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

async function handleLogin() {
  try {
    await authStore.login(formState.username, formState.password)
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch {
    // Error handled by interceptor
  }
}
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
