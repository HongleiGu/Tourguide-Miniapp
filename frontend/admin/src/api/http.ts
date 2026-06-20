import axios, { type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse } from './types'

/** Shared axios instance. Base path is proxied to the backend (see vite.config.ts). */
const http: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    // Safety net: a 2xx that still carries a non-zero business code.
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return response
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络错误'
    const auth = useAuthStore()
    // 401 while already logged in => session expired: drop it and bounce to login.
    if (status === 401 && auth.isAuthenticated) {
      auth.logout()
      router.push('/login')
    }
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default http
