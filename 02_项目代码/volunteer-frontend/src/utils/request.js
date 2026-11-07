import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 每次请求自动附加本地 JWT，业务页面无需重复处理登录头。
service.interceptors.request.use((config) => {
  const token = localStorage.getItem('volunteer_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const payload = response.data
    // 后端业务状态统一放在 code 字段，401 时清理失效会话并返回登录页。
    if (payload?.code !== 0) {
      if (payload?.code === 401) {
        localStorage.removeItem('volunteer_token')
        localStorage.removeItem('volunteer_user')
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
      }
      ElMessage.error(payload?.message || '请求失败')
      return Promise.reject(new Error(payload?.message || '请求失败'))
    }
    return payload.data
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络连接异常'
    // 兼容后端返回 HTTP 401 或响应体中 code 为 401 的情况。
    if (error.response?.status === 401 || error.response?.data?.code === 401) {
      localStorage.removeItem('volunteer_token')
      localStorage.removeItem('volunteer_user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service
