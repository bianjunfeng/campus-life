<template>
  <div class="login-container">
  <div class="login-header">
    <span class="portal-badge">{{ portalTitle }}</span>
    <h1>校园生活平台</h1>
    <p>{{ portalSubtitle }}</p>
    <div class="demo-account" aria-label="演示账号">
      <div class="demo-account-title">演示账号（密码：{{ demoPassword }}）</div>
      <div class="demo-account-list">
        <button
          v-for="account in demoAccounts"
          :key="account.role"
          type="button"
          class="demo-account-item"
          @click="fillDemoAccount(account)"
        >
          <span class="demo-role">{{ account.label }}</span>
          <span class="demo-phone">{{ account.phone }}</span>
        </button>
      </div>
    </div>
  </div>
    
    <div class="login-content">
      <div v-if="showLoginRequiredMessage" class="login-required-message">
        您需要登录才能访问此内容
      </div>
      <div v-if="loginMessage" class="login-status-message" :class="{ error: loginMessageType === 'error' }">
        {{ loginMessage }}
      </div>
      <!-- 登录方式切换 -->
      <div class="login-tabs">
        <button 
          :class="['tab-btn', { active: activeTab === 'password' }]"
          type="button"
          @click="activeTab = 'password'"
        >
          密码登录
        </button>
        <button 
          :class="['tab-btn', { active: activeTab === 'code' }]"
          type="button"
          @click="activeTab = 'code'"
        >
          验证码登录
        </button>
      </div>
      

      
      <!-- 密码登录 -->
      <div v-if="activeTab === 'password'" class="form-login">
        <div class="form-group">
          <input 
            type="text" 
            v-model="passwordForm.username" 
            placeholder="手机号/邮箱/用户名"
            class="form-input"
            :class="{ 'error': errors.username }"
          >
          <span v-if="errors.username" class="error-text">{{ errors.username }}</span>
        </div>
        <div class="form-group">
          <div class="password-input-wrapper">
            <input 
              :type="showPassword ? 'text' : 'password'" 
              v-model="passwordForm.password" 
              placeholder="密码"
              class="form-input password-input"
              :class="{ 'error': errors.password }"
            >
            <button 
              type="button"
              class="password-toggle-btn"
              @click="togglePasswordVisibility"
              tabindex="-1"
            >
              <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                <line x1="1" y1="1" x2="23" y2="23"></line>
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
            </button>
          </div>
          <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
        </div>
        <div class="login-options">
          <label class="remember-me">
            <input type="checkbox" v-model="rememberMe">
            记住我
          </label>
          <a href="#" class="forgot-password">忘记密码?</a>
        </div>
        <button type="button" @click="handlePasswordLogin" class="login-btn" :disabled="loginLoading">
          {{ loginLoading ? '正在登录...' : portalLoginText }}
        </button>
      </div>
      
      <!-- 验证码登录 -->
      <div v-if="activeTab === 'code'" class="form-login">
        <div class="form-group">
          <input 
            type="text" 
            v-model="codeForm.phone" 
            placeholder="手机号"
            class="form-input"
            :class="{ 'error': errors.phone }"
            maxlength="11"
          >
          <span v-if="errors.phone" class="error-text">{{ errors.phone }}</span>
        </div>
        <div class="form-group">
          <input 
            type="text" 
            v-model="codeForm.code" 
            placeholder="验证码"
            class="form-input code-input"
            :class="{ 'error': errors.code }"
          >
          <button
            type="button"
            @click="sendCode" 
            :disabled="countdown > 0"
            class="code-btn"
          >
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </button>
          <span v-if="errors.code" class="error-text">{{ errors.code }}</span>
        </div>
        <button type="button" @click="handleCodeLogin" class="login-btn" :disabled="loginLoading">
          {{ loginLoading ? '正在登录...' : portalLoginText }}
        </button>
      </div>
      
      <div class="other-login">
        <div class="divider">
          <span>其他登录方式</span>
        </div>
        <div class="login-options">
          <button type="button" class="option-btn" @click="handleWechatLogin" title="微信登录">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24">
              <path fill="currentColor" d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.35-8.596-6.35zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.179c0-.651.52-1.18 1.162-1.18zm6.673 2.136c-2.649 0-4.742 1.978-4.742 4.466 0 2.489 2.093 4.466 4.742 4.466.887 0 1.717-.22 2.432-.598a.59.59 0 0 1 .616.033l1.504.88a.295.295 0 0 0 .29-.003.247.247 0 0 0 .12-.213l-.188-1.117a.662.662 0 0 1 .27-.7c1.521-1.058 2.466-2.72 2.466-4.566 0-2.49-2.093-4.467-4.742-4.467zm-2.906 3.2c.435 0 .788.355.788.792a.783.783 0 0 1-.788.79.783.783 0 0 1-.787-.79c0-.437.353-.792.787-.792zm3.634 0c.435 0 .788.355.788.792a.783.783 0 0 1-.788.79.783.783 0 0 1-.787-.79c0-.437.352-.792.787-.792z"/>
            </svg>
          </button>
          <button type="button" class="option-btn" @click="handleQQLogin" title="QQ登录">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24">
              <!-- 腾讯QQ企鹅图标 -->
              <path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12c0 1.54.36 2.98.97 4.29L1 23l6.71-1.97C9.02 21.64 10.46 22 12 22c5.52 0 10-4.48 10-10S17.52 2 12 2zm4.11 6.11c1.56 0 2.83-1.27 2.83-2.83S17.67 2.45 16.11 2.45 13.28 3.72 13.28 5.28s1.27 2.83 2.83 2.83zm-8.22 0c1.56 0 2.83-1.27 2.83-2.83S9.45 2.45 7.89 2.45 5.06 3.72 5.06 5.28s1.27 2.83 2.83 2.83z"/>
            </svg>
          </button>
        </div>
      </div>

      <div class="login-footer">
        <p class="register-link">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </p>
        <p class="agreement">
          登录即表示您同意 <a href="#">《用户协议》</a> 和 <a href="#">《隐私政策》</a>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loginApi, type LoginPayload, getWechatAuthUrl, getQQAuthUrl } from '../../api/auth'
import { sendVerificationCode } from '../../api/auth'
import { notify } from '@/utils/notify'
import { clearAuthState } from '../../utils/authStorage'
import { checkLoginStatus, getUserRoles } from '../../shared/router/auth'

const router = useRouter()
const route = useRoute()

// 登录相关状态
const activeTab = ref('password')
const countdown = ref(0)
const rememberMe = ref(false)
const showLoginRequiredMessage = ref(false)
const showPassword = ref(false)
const loginLoading = ref(false)
const loginMessage = ref('')
const loginMessageType = ref<'info' | 'error'>('info')

// 密码登录表单
const passwordForm = ref({
  username: '',
  password: ''
})

// 验证码登录表单
const codeForm = ref({
  phone: '',
  code: ''
})

// 表单验证错误
const errors = ref({
  username: '',
  password: '',
  phone: '',
  code: ''
})

type PortalRole = 'STUDENT' | 'MERCHANT' | 'ADMIN'

const demoPassword = '123456'
const demoAccounts = [
  { role: 'ADMIN', label: '管理员', phone: '13800000001' },
  { role: 'MERCHANT', label: '商家', phone: '13800138001' },
  { role: 'STUDENT', label: '学生用户', phone: '13800138000' }
] as const

const fillDemoAccount = (account: typeof demoAccounts[number]) => {
  activeTab.value = 'password'
  passwordForm.value.username = account.phone
  passwordForm.value.password = demoPassword
  errors.value.username = ''
  errors.value.password = ''
  loginMessage.value = ''
  loginMessageType.value = 'info'
}

const normalizeRole = (role?: string) => {
  const normalizedRole = String(role || '').toUpperCase()
  return normalizedRole.startsWith('ROLE_') ? normalizedRole.slice(5) : normalizedRole
}

const isExpectedRole = (actualRole: string | undefined, expectedRole: PortalRole) => {
  const normalizedRole = normalizeRole(actualRole)
  if (expectedRole === 'STUDENT') {
    return normalizedRole === 'STUDENT' || normalizedRole === '0'
  }
  if (expectedRole === 'MERCHANT') {
    return normalizedRole === 'MERCHANT' || normalizedRole === '1'
  }
  return normalizedRole === 'ADMIN' || normalizedRole === '2'
}

const getRoleMismatchMessage = (expectedRole: PortalRole, actualRole?: string) => {
  const normalizedRole = normalizeRole(actualRole)
  if (expectedRole === 'STUDENT') {
    if (normalizedRole === 'ADMIN' || normalizedRole === '2') {
      return '管理员账号请进入管理端登录'
    }
    if (normalizedRole === 'MERCHANT' || normalizedRole === '1') {
      return '商家账号请进入商家端登录'
    }
    return '学生账号才能登录用户端'
  }
  if (expectedRole === 'MERCHANT') {
    if (normalizedRole === 'ADMIN' || normalizedRole === '2') {
      return '管理员账号请进入管理端登录'
    }
    return '商家账号才能登录商家端'
  }
  return '管理员账号才能登录管理端'
}

const loginReasonMessage: Record<string, string> = {
  'student-required': '学生账号才能进入用户端，管理员账号请进入管理端登录，商家账号请进入商家端登录',
  'merchant-required': '商家账号才能进入商家端',
  'merchant-unverified': '商家账号认证通过后才能进入商家端',
  'admin-required': '管理员账号才能进入管理端'
}

const getPortalRoleRequirement = () => {
  if (router.hasRoute('AdminDashboard')) {
    return { role: 'ADMIN', label: '管理员', defaultPath: '/admin/dashboard', allowedPrefix: '/admin' }
  }
  if (router.hasRoute('MerchantVoucherManage')) {
    return { role: 'MERCHANT', label: '商家', defaultPath: '/merchant/vouchers', allowedPrefix: '/merchant' }
  }
  return { role: '', label: '', defaultPath: '/home', allowedPrefix: '' }
}

const portalDisplay = computed(() => {
  const requirement = getPortalRoleRequirement()
  if (requirement.role === 'ADMIN') {
    return {
      title: '管理端',
      subtitle: '平台运营与系统管理工作台',
      loginText: '登录管理端'
    }
  }
  if (requirement.role === 'MERCHANT') {
    return {
      title: '商家端',
      subtitle: '管理优惠券、订单与店铺服务',
      loginText: '登录商家端'
    }
  }
  return {
    title: '学生端',
    subtitle: '发现美好世界  聚焦青春校园',
    loginText: '登录学生端'
  }
})

const portalTitle = computed(() => portalDisplay.value.title)
const portalSubtitle = computed(() => portalDisplay.value.subtitle)
const portalLoginText = computed(() => portalDisplay.value.loginText)

const getLoginTargetPath = (userRole?: string) => {
  const requirement = getPortalRoleRequirement()
  if (requirement.role && !isExpectedRole(userRole, requirement.role as PortalRole)) {
    clearAuthState()
    throw new Error(getRoleMismatchMessage(requirement.role as PortalRole, userRole))
  }

  const redirectPath = localStorage.getItem('redirectAfterLogin')
  const isAuthPath = redirectPath === '/login'
    || redirectPath === '/register'
    || Boolean(redirectPath?.startsWith('/oauth/'))
  const isCrossPortalPath = !requirement.allowedPrefix
    ? Boolean(redirectPath?.startsWith('/admin') || redirectPath?.startsWith('/merchant'))
    : Boolean(redirectPath && !redirectPath.startsWith(requirement.allowedPrefix))

  if (redirectPath && redirectPath !== '/' && !isAuthPath && !isCrossPortalPath) {
    localStorage.removeItem('redirectAfterLogin')
    return redirectPath
  }

  localStorage.removeItem('redirectAfterLogin')
  return requirement.defaultPath
}

const redirectToLoginTarget = async (targetPath: string) => {
  localStorage.removeItem('requireLogin')
  await router.replace(targetPath)
  await nextTick()

  if (router.currentRoute.value.path !== targetPath) {
    window.location.replace(targetPath)
  }
}

const redirectIfAlreadyLoggedIn = async () => {
  if (!checkLoginStatus()) {
    return
  }

  const requirement = getPortalRoleRequirement()
  if (!requirement.role) {
    await redirectToLoginTarget(requirement.defaultPath)
    return
  }
  const { isStudent, isAdmin, isMerchant } = getUserRoles()
  const roleMatched = (requirement.role === 'STUDENT' && isStudent)
    || (requirement.role === 'MERCHANT' && isMerchant)
    || (requirement.role === 'ADMIN' && isAdmin)
  if (!roleMatched) {
    clearAuthState()
    return
  }

  await redirectToLoginTarget(requirement.defaultPath)
}

// 切换密码显示/隐藏
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

// 验证手机号格式
const isValidPhone = (phone: string): boolean => {
  const phoneRegex = /^1[3-9]\d{9}$/
  return phoneRegex.test(phone)
}

// 验证邮箱格式
const isValidEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

// 发送验证码
const sendCode = async () => {
  errors.value.phone = ''
  
  if (!codeForm.value.phone) {
    errors.value.phone = '请输入手机号'
    return
  }
  
  if (!isValidPhone(codeForm.value.phone)) {
    errors.value.phone = '请输入正确的手机号格式'
    return
  }
  
  // 调用后端发送验证码API
  try {
    await sendVerificationCode(codeForm.value.phone, 'login')
    
    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
    
    // 提示用户（开发环境可以显示验证码，生产环境应该移除）
    console.log('验证码已发送到:', codeForm.value.phone)
  } catch (error: any) {
    console.error('发送验证码失败:', error)
    const errorMessage = error.message || error.response?.data?.msg || '发送验证码失败'
    errors.value.phone = errorMessage
  }
}

// 初始化 - 检查是否有登录要求
onMounted(() => {
  const requireLogin = localStorage.getItem('requireLogin')
  if (requireLogin === 'true') {
    showLoginRequiredMessage.value = true
    // 自动切换到密码登录
    activeTab.value = 'password'
  }
  const reason = typeof route.query.reason === 'string' ? route.query.reason : ''
  if (loginReasonMessage[reason]) {
    loginMessage.value = loginReasonMessage[reason]
    loginMessageType.value = 'error'
  }
  void redirectIfAlreadyLoggedIn()
})

// 密码登录
const handlePasswordLogin = async () => {
  if (loginLoading.value) {
    return
  }

  // 重置错误信息
  errors.value.username = ''
  errors.value.password = ''
  loginMessage.value = ''
  loginMessageType.value = 'info'
  
  // 表单验证
  let isValid = true
  
  if (!passwordForm.value.username) {
    errors.value.username = '请输入手机号/邮箱/用户名'
    isValid = false
  }
  
  if (!passwordForm.value.password) {
    errors.value.password = '请输入密码'
    isValid = false
  } else if (passwordForm.value.password.length < 6) {
    errors.value.password = '密码长度不能少于6位'
    isValid = false
  }
  
  if (!isValid) {
    return
  }
  
  // 调用后端登录API
  try {
    loginLoading.value = true
    loginMessage.value = '正在登录...'
    clearAuthState()
    const result = await loginApi({
      phone: passwordForm.value.username,
      password: passwordForm.value.password
    })
    
    if (result.code === 200 && result.data) {
      // token已在 loginApi 中保存，这里直接跳转
      
      // 如果选择记住我，保存用户信息
      if (rememberMe.value) {
        localStorage.setItem('rememberedUser', passwordForm.value.username)
      }
      
      const targetPath = getLoginTargetPath(result.data.user?.role)
      loginMessage.value = '登录成功，正在跳转...'
      await redirectToLoginTarget(targetPath)
    } else {
      errors.value.password = result.message || '登录失败，请重试'
      loginMessage.value = errors.value.password
      loginMessageType.value = 'error'
    }
  } catch (error: any) {
    console.error('登录失败:', error)
    const errorMessage = error.message || error.response?.data?.message || error.response?.data?.msg || '登录失败，请重试'
    loginMessage.value = errorMessage
    loginMessageType.value = 'error'
    
    // 根据错误信息显示到对应字段
    if (errorMessage.includes('用户不存在')) {
      errors.value.username = errorMessage
    } else if (errorMessage.includes('密码错误')) {
      errors.value.password = errorMessage
    } else {
      errors.value.password = errorMessage
    }
  } finally {
    loginLoading.value = false
  }
}

// 验证码登录
const handleCodeLogin = async () => {
  if (loginLoading.value) {
    return
  }

  // 重置错误信息
  errors.value.phone = ''
  errors.value.code = ''
  loginMessage.value = ''
  loginMessageType.value = 'info'
  
  // 表单验证
  let isValid = true
  
  if (!codeForm.value.phone) {
    errors.value.phone = '请输入手机号'
    isValid = false
  } else if (!isValidPhone(codeForm.value.phone)) {
    errors.value.phone = '请输入正确的手机号格式'
    isValid = false
  }
  
  if (!codeForm.value.code) {
    errors.value.code = '请输入验证码'
    isValid = false
  } else if (!/^\d{6}$/.test(codeForm.value.code)) {
    errors.value.code = '验证码格式不正确'
    isValid = false
  }
  
  if (!isValid) {
    return
  }
  
  // 调用后端验证码登录API
  try {
    loginLoading.value = true
    loginMessage.value = '正在登录...'
    clearAuthState()
    const result = await loginApi({
      phone: codeForm.value.phone,
      code: codeForm.value.code,
      scene: 'login'
    } as LoginPayload)
    
    if (result.code === 200 && result.data) {
      // token已在 loginApi 中保存，这里直接跳转

      const targetPath = getLoginTargetPath(result.data.user?.role)
      loginMessage.value = '登录成功，正在跳转...'
      await redirectToLoginTarget(targetPath)
    } else {
      errors.value.code = result.message || '登录失败，请重试'
      loginMessage.value = errors.value.code
      loginMessageType.value = 'error'
    }
  } catch (error: any) {
    console.error('验证码登录失败:', error)
    const errorMessage = error.message || error.response?.data?.message || error.response?.data?.msg || '登录失败，请重试'
    loginMessage.value = errorMessage
    loginMessageType.value = 'error'
    
    // 根据错误信息显示到对应字段
    if (errorMessage.includes('用户不存在')) {
      errors.value.phone = errorMessage
    } else if (errorMessage.includes('验证码')) {
      errors.value.code = errorMessage
    } else {
      errors.value.code = errorMessage
    }
  } finally {
    loginLoading.value = false
  }
}

// 微信登录
const handleWechatLogin = async () => {
  try {
    // 构建回调地址
    const redirectUri = `${window.location.origin}/oauth/wechat/callback`
    const authUrl = await getWechatAuthUrl(redirectUri)
    // 跳转到微信授权页面
    window.location.href = authUrl
  } catch (error: any) {
    console.error('微信登录失败:', error)
    notify(error.message || '微信登录失败，请重试')
  }
}

// QQ登录
const handleQQLogin = async () => {
  try {
    // 构建回调地址
    const redirectUri = `${window.location.origin}/oauth/qq/callback`
    const authUrl = await getQQAuthUrl(redirectUri)
    // 跳转到QQ授权页面
    window.location.href = authUrl
  } catch (error: any) {
    console.error('QQ登录失败:', error)
    notify(error.message || 'QQ登录失败，请重试')
  }
}
</script>

<style scoped>
.login-container {
  position: fixed;
  inset: 0;
  min-height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow-y: auto;
  padding: 32px 16px;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 20% 20%, rgba(22, 119, 255, 0.12), transparent 32%),
    linear-gradient(135deg, #f7fbff 0%, #f4f7fb 52%, #eef4ff 100%);
}

.login-header {
  width: min(520px, 100%);
  box-sizing: border-box;
  text-align: center;
  padding: 28px 28px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 18px 18px 0 0;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.16);
}

.portal-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 12px;
  margin-bottom: 14px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
}

.demo-account {
  background-color: rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  padding: 12px;
  margin-top: 20px;
  font-size: 14px;
  color: white;
  backdrop-filter: blur(5px);
}

.demo-account-title {
  margin-bottom: 10px;
  font-weight: 600;
  opacity: 0.95;
}

.demo-account-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.demo-account-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;
}

.demo-account-item:hover {
  border-color: rgba(255, 255, 255, 0.45);
  background: rgba(255, 255, 255, 0.2);
}

.demo-role {
  font-size: 12px;
  font-weight: 600;
  opacity: 0.86;
}

.demo-phone {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.2;
  word-break: break-all;
}

.login-header h1 {
  font-size: 30px;
  margin: 0 0 8px;
}

.login-header p {
  font-size: 15px;
  margin: 0;
  opacity: 0.9;
}

.login-content {
  width: min(520px, 100%);
  box-sizing: border-box;
  flex: none;
  background-color: white;
  margin: 0;
  border-radius: 0 0 18px 18px;
  padding: 28px;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.16);
}

.login-tabs {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30px;
  border-bottom: 1px solid #e0e0e0;
}

.tab-btn {
  flex: 1;
  padding: 10px 0;
  background: none;
  border: none;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: color 0.3s;
}

.tab-btn.active {
  color: #1677ff;
  font-weight: 500;
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 2px;
  background-color: #1677ff;
}



/* 表单登录样式 */
.form-login {
  padding: 10px 0;
}

.form-group {
  margin-bottom: 20px;
  position: relative;
}

.password-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 0.3s;
  box-sizing: border-box;
}

.password-input {
  padding-right: 45px;
}

.password-toggle-btn {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: color 0.3s;
  outline: none;
}

.password-toggle-btn:hover {
  color: #1677ff;
}

.password-toggle-btn:focus {
  outline: none;
}

.password-toggle-btn svg {
  display: block;
}

.form-input:focus {
  outline: none;
  border-color: #1677ff;
}

.form-input.error {
  border-color: #ff4d4f;
}

.error-text {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 5px;
  display: block;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 14px;
}

.remember-me {
  display: flex;
  align-items: center;
  color: #666;
  cursor: pointer;
}

.remember-me input {
  margin-right: 8px;
}

.forgot-password {
  color: #1677ff;
  text-decoration: none;
}

.forgot-password:hover {
  text-decoration: underline;
}

.code-input {
  width: calc(100% - 120px);
  margin-right: 10px;
}

.code-btn {
  width: 100px;
  padding: 12px;
  background-color: #f0f9ff;
  border: 1px solid #91d5ff;
  color: #1677ff;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.code-btn:hover:not(:disabled) {
  background-color: #e6f7ff;
}

.code-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-btn {
  width: 100%;
  padding: 12px;
  background-color: #1677ff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
  margin-top: 10px;
}

.login-btn:hover {
  background-color: #0958d9;
}

.login-btn:disabled {
  background-color: #91caff;
  cursor: not-allowed;
}

.login-status-message {
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 8px;
  background-color: #e6f4ff;
  color: #0958d9;
  font-size: 14px;
  text-align: center;
}

.login-status-message.error {
  background-color: #fff2f0;
  color: #cf1322;
}

/* 其他登录方式 */
.other-login {
  margin-top: 28px;
}

.divider {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background-color: #e0e0e0;
}

.divider span {
  padding: 0 15px;
  color: #999;
  font-size: 14px;
}

.other-login .login-options {
  display: flex;
  justify-content: center;
  gap: 40px;
}

.option-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background-color: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.3s;
}

.option-btn:hover {
  background-color: #e0e0e0;
}

.option-btn svg {
  width: 24px;
  height: 24px;
  color: #666;
}

/* 登录页底部 */
.login-footer {
  margin-top: 30px;
  text-align: center;
}

.agreement {
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}

.agreement a {
  color: #1677ff;
  text-decoration: none;
}

.register-link {
  font-size: 14px;
  color: #666;
  margin-bottom: 15px;
}

.register-link a {
  color: #1677ff;
  text-decoration: none;
  font-weight: 500;
}

.register-link a:hover {
  text-decoration: underline;
}

.tip {
  font-size: 12px;
  color: #999;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-container {
    justify-content: flex-start;
    padding: 16px 12px;
  }

  .login-header {
    padding: 22px 18px 20px;
  }

  .login-header h1 {
    font-size: 24px;
  }

  .demo-account-list {
    grid-template-columns: 1fr;
  }
  
  .login-content {
    margin: 0;
    padding: 20px;
  }
  
  .tab-btn {
    font-size: 14px;
  }
  
  .login-options {
    gap: 30px;
  }
}
</style>
