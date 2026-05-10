import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false })

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const instance: AxiosInstance = axios.create({
  baseURL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

// Request interceptor
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    NProgress.start()
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    const tenantId = localStorage.getItem('tenantId')
    if (tenantId) {
      config.headers['X-Tenant-Id'] = tenantId
    }
    return config
  },
  (error) => {
    NProgress.done()
    return Promise.reject(error)
  }
)

// Response interceptor
let isRefreshing = false
let failedQueue: Array<{ resolve: (value: unknown) => void; reject: (reason?: unknown) => void }> = []

function processQueue(error: unknown) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(undefined)
    }
  })
  failedQueue = []
}

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    NProgress.done()
    return response.data
  },
  async (error) => {
    NProgress.done()
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(() => instance(originalRequest))
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const refreshToken = localStorage.getItem('refreshToken')
        if (!refreshToken) throw new Error('No refresh token')
        const res = await axios.post(`${baseURL}/auth/refresh`, { refreshToken })
        const { accessToken, refreshToken: newRefreshToken } = res.data.data
        localStorage.setItem('token', accessToken)
        localStorage.setItem('refreshToken', newRefreshToken)
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        processQueue(null)
        return instance(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError)
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    const msg = error.response?.data?.message || error.message || '请求失败'
    message.error(msg)
    return Promise.reject(error)
  }
)

export function get<T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig) {
  return instance.get<unknown, T>(url, { params, ...config })
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return instance.post<unknown, T>(url, data, config)
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return instance.put<unknown, T>(url, data, config)
}

export function del<T>(url: string, config?: AxiosRequestConfig) {
  return instance.delete<unknown, T>(url, config)
}

export default instance
