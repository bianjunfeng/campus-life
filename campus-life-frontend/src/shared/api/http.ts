// src/shared/api/http.ts
import axios from 'axios'
import { clearAuthState, getAuthToken, getRefreshToken, setAuthToken, setRefreshToken } from '../utils/authStorage'

const http = axios.create({
  baseURL: '/api',           // 所有环境统一经网关转发
  timeout: 10000             // 默认10秒超时，文件上传时会单独设置更长的超时时间
})

let isRefreshing = false
let pendingRequests: Array<(token: string | null) => void> = []

const resolvePendingRequests = (token: string | null) => {
  pendingRequests.forEach(callback => callback(token))
  pendingRequests = []
}

// 请求拦截：自动带 token
http.interceptors.request.use(config => {
  const token = getAuthToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一处理错误
http.interceptors.response.use(
  response => response,
  error => {
    const originalRequest = error.config
    // 处理 HTTP 错误响应
    if (error.response) {
      const { data } = error.response
      // 如果后端返回了错误信息，使用后端的错误信息
      if (data && data.message) {
        error.message = data.message
      } else if (data && data.msg) {
        // 兼容旧的 msg 字段
        error.message = data.msg
      } else if (error.response.status === 400) {
        error.message = '请求参数错误'
      } else if (error.response.status === 401) {
        const refreshToken = getRefreshToken()
        const isRefreshRequest = typeof originalRequest?.url === 'string' && originalRequest.url.includes('/auth/refresh')

        if (refreshToken && !originalRequest?._retry && !isRefreshRequest) {
          originalRequest._retry = true

          if (isRefreshing) {
            return new Promise((resolve, reject) => {
              pendingRequests.push((token) => {
                if (!token) {
                  reject(error)
                  return
                }
                originalRequest.headers = originalRequest.headers || {}
                originalRequest.headers.Authorization = `Bearer ${token}`
                resolve(http(originalRequest))
              })
            })
          }

          isRefreshing = true
          return axios.post('/api/auth/refresh', { refreshToken })
            .then(response => {
              const refreshData = response.data
              if (refreshData?.code !== 200 || !refreshData?.data?.accessToken) {
                throw new Error(refreshData?.message || '刷新登录态失败')
              }
              const newAccessToken = refreshData.data.accessToken
              const newRefreshToken = refreshData.data.refreshToken
              setAuthToken(newAccessToken)
              if (newRefreshToken) {
                setRefreshToken(newRefreshToken)
              }
              resolvePendingRequests(newAccessToken)
              originalRequest.headers = originalRequest.headers || {}
              originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
              return http(originalRequest)
            })
            .catch(refreshError => {
              resolvePendingRequests(null)
              clearAuthState()
              return Promise.reject(refreshError)
            })
            .finally(() => {
              isRefreshing = false
            })
        }
        error.message = '未授权，请重新登录'
        clearAuthState()
      } else if (error.response.status === 403) {
        error.message = '没有权限访问'
      } else if (error.response.status === 404) {
        error.message = '请求的资源不存在'
      } else if (error.response.status >= 500) {
        error.message = '服务器错误，请稍后重试'
      }
    } else if (error.code === 'ECONNABORTED') {
      error.message = '请求超时，请稍后重试'
    } else if (error.request) {
      error.message = '网络错误，请检查网络连接'
    }
    return Promise.reject(error)
  }
)

export default http
