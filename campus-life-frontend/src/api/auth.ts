// src/api/auth.ts
import http from './http'
import {
  getStoredUserInfoObject,
  setAuthToken,
  setCurrentUser,
  setRefreshToken,
  setStoredUserInfo,
  setStoredUserRole
} from '../utils/authStorage'

export interface LoginPayload {
  phone: string
  password?: string
  code?: string  // 验证码登录时使用
  scene?: VerificationScene
}

export type VerificationScene = 'register' | 'login' | 'reset_password' | 'bind_phone'

export interface RegisterPayload {
  phone: string
  password: string
  nickName: string
  code: string  // 验证码
  scene?: VerificationScene
  role?: 'student' | 'merchant'  // 注册类型：学生或商家
  
  // 学生注册字段
  school?: string  // 学校
  studentId?: string  // 学号
  college?: string  // 学院
  major?: string  // 专业
  grade?: string  // 年级
  className?: string  // 班级
  realName?: string  // 真实姓名
  
  // 商家注册字段
  merchantName?: string  // 商家名称
  merchantTypeId?: number  // 商家类型ID
  contactName?: string  // 联系人姓名
  contactPhone?: string  // 联系电话
  address?: string  // 详细地址
}

export interface UserInfo {
  id: number
  phone: string
  email?: string
  nickName: string
  icon?: string
  bio?: string
  gender?: string
  birthday?: string
  region?: string
  occupation?: string
  status?: number
  createTime?: string
  updateTime?: string
  role: 'GUEST' | 'STUDENT' | 'MERCHANT' | 'ADMIN'
}

export interface UploadAvatarResult {
  url: string
  filename: string
}

export interface AuthSessionInfo {
  sessionId: string
  userId: number
  device?: string
  ip?: string
  userAgent?: string
  status?: number
  loginTime?: string
  lastSeenTime?: string
  expireAt?: string
}

function persistAuthPayload(data: any) {
  if (!data) {
    return
  }

  const accessToken = data.accessToken || data.token
  if (accessToken) {
    setAuthToken(accessToken)
  }

  if (data.refreshToken) {
    setRefreshToken(data.refreshToken)
  }

  if (data.user) {
    setStoredUserInfo(data.user)
    setCurrentUser(data.user)
    if (data.user.role) {
      setStoredUserRole(data.user.role)
    }
  }
}

export async function loginApi(payload: LoginPayload) {
  // 验证手机号是否存在且不为空
  if (!payload.phone || payload.phone.trim() === '') {
    throw new Error('登录账号不能为空')
  }

  const phoneRegex = /^1[3-9]\d{9}$/
  const account = payload.phone.trim()

  // 验证登录方式
  if (!payload.password && !payload.code) {
    throw new Error('请提供密码或验证码')
  }

  if (payload.code && !phoneRegex.test(account)) {
    throw new Error('验证码登录仅支持手机号')
  }
  
  // 对接后端：POST /api/auth/tokens
  const { data } = await http.post('/auth/tokens', {
    ...payload,
    phone: account
  })
  
  // 登录成功后保存token（如果API返回了token）
  if (data.code === 200 && data.data) {
    persistAuthPayload(data.data)
  }

  return data as { code: number; message: string; data: { token: string; accessToken?: string; refreshToken?: string; user: UserInfo } }
}

export async function registerApi(payload: RegisterPayload) {
  // 前端验证手机号格式
  const phoneRegex = /^1[3-9]\d{9}$/
  if (!phoneRegex.test(payload.phone)) {
    throw new Error('手机号格式不正确')
  }
  
  // 验证密码强度
  const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/
  if (!passwordRegex.test(payload.password)) {
    throw new Error('密码必须包含字母和数字，长度6-20位')
  }
  
  const { data } = await http.post('/auth/users', payload)
  
  // 注册成功后返回token和用户信息
  if (data.code === 200 && data.data) {
    const result = data.data as { token: string; user: UserInfo }
    persistAuthPayload(data.data)
    return { code: data.code, message: data.message || '注册成功', data: result.user }
  }
  
  return data as { code: number; message: string; data: UserInfo }
}

export async function fetchCurrentUser() {
  const { data } = await http.get('/users/me')
  if (data.code === 200 && data.data) {
    persistAuthPayload({ user: data.data })
  }
  return data as { code: number; message: string; data: UserInfo }
}

export async function logoutApi() {
  const { data } = await http.post('/auth/logout')
  return data as { code: number; message: string }
}

export async function logoutAllApi() {
  const { data } = await http.post('/auth/logout-all')
  return data as { code: number; message: string }
}

export async function logoutOthersApi() {
  const { data } = await http.post('/auth/logout-others')
  return data as { code: number; message: string }
}

export async function getSessionsApi() {
  const { data } = await http.get('/auth/sessions')
  return data as { code: number; message: string; data: AuthSessionInfo[] }
}

export async function revokeSessionApi(sessionId: string) {
  const { data } = await http.delete(`/auth/sessions/${encodeURIComponent(sessionId)}`)
  return data as { code: number; message: string }
}

/**
 * 发送验证码
 */
export async function sendVerificationCode(phone: string, scene: VerificationScene): Promise<void> {
  // 前端验证手机号格式
  const phoneRegex = /^1[3-9]\d{9}$/
  if (!phoneRegex.test(phone)) {
    throw new Error('手机号格式不正确')
  }

  if (!scene) {
    throw new Error('验证码场景不能为空')
  }
  
  const { data } = await http.post('/auth/send-code', { phone, scene })
  
  if (data.code !== 200) {
    throw new Error(data.message || '验证码发送失败')
  }
}

/**
 * 获取微信登录授权URL
 */
export async function getWechatAuthUrl(redirectUri: string): Promise<string> {
  const { data } = await http.get('/auth/oauth/wechat/authorize', {
    params: { redirectUri }
  })
  
  if (data.code === 200 && data.data && data.data.authUrl) {
    return data.data.authUrl
  }
  
  throw new Error(data.message || '获取微信授权URL失败')
}

/**
 * 获取QQ登录授权URL
 */
export async function getQQAuthUrl(redirectUri: string): Promise<string> {
  const { data } = await http.get('/auth/oauth/qq/authorize', {
    params: { redirectUri }
  })
  
  if (data.code === 200 && data.data && data.data.authUrl) {
    return data.data.authUrl
  }
  
  throw new Error(data.message || '获取QQ授权URL失败')
}

/**
 * 微信OAuth回调登录
 */
export async function wechatCallback(code: string, state: string, redirectUri: string) {
  const { data } = await http.get('/auth/oauth/wechat/callback', {
    params: { code, state, redirectUri }
  })
  
  if (data.code === 200 && data.data) {
    const result = data.data as { token: string; user: UserInfo }
    persistAuthPayload(data.data)
    return result
  }
  
  throw new Error(data.message || '微信登录失败')
}

/**
 * QQ OAuth回调登录
 */
export async function qqCallback(code: string, state: string, redirectUri: string) {
  const { data } = await http.get('/auth/oauth/qq/callback', {
    params: { code, state, redirectUri }
  })
  
  if (data.code === 200 && data.data) {
    const result = data.data as { token: string; user: UserInfo }
    persistAuthPayload(data.data)
    return result
  }
  
  throw new Error(data.message || 'QQ登录失败')
}

/**
 * 获取当前用户信息
 */
export async function getUserInfo(): Promise<UserInfo> {
  try {
    const { data } = await http.get('/users/me')
    if (data.code === 200 && data.data) {
      const userInfo = data.data as UserInfo
      setStoredUserInfo(userInfo)
      setCurrentUser(userInfo)
      return userInfo
    }
    throw new Error(data.message || '获取用户信息失败')
  } catch (error) {
    // 如果API失败，尝试从 localStorage 读取
    const cachedUserInfo = getStoredUserInfoObject<UserInfo>()
    if (cachedUserInfo) {
      return cachedUserInfo
    }
    throw error
  }
}

/**
 * 更新当前用户资料
 */
export async function updateUserProfile(payload: {
  nickName?: string
  phone?: string
  email?: string
  icon?: string
  intro?: string
  gender?: string
  region?: string
  birthday?: string
  occupation?: string
}): Promise<UserInfo> {
  const { data } = await http.put('/users/me/profile', payload)
  if (data.code === 200 && data.data) {
    const userInfo = data.data as UserInfo
    setStoredUserInfo(userInfo)
    setCurrentUser(userInfo)
    return userInfo
  }
  throw new Error(data.message || '更新资料失败')
}

export async function changePassword(payload: {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}): Promise<void> {
  const { data } = await http.put('/users/me/password', payload)
  if (data.code !== 200) {
    throw new Error(data.message || '修改密码失败')
  }
}

export async function uploadAvatar(file: File): Promise<UploadAvatarResult> {
  const formData = new FormData()
  formData.append('file', file)

  const { data } = await http.post('/upload/avatar', formData)
  if (data.code === 200 && data.data) {
    return data.data as UploadAvatarResult
  }
  throw new Error(data.message || '头像上传失败')
}
