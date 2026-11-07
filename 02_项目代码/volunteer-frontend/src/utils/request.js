import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

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
