<template>
  <div class="register-container">
    <div class="register-header">
      <h1>校园生活平台</h1>
      <p>发现美好世界  聚焦青春校园</p>
    </div>
    
    <div class="register-content">
      <div class="register-title">
        <h2>注册账号</h2>
        <p class="subtitle">填写以下信息完成注册</p>
      </div>

      <!-- 注册类型切换 -->
      <div class="register-tabs">
        <button 
          :class="['tab-btn', { active: registerType === 'student' }]"
          @click="registerType = 'student'"
        >
          学生注册
        </button>
        <button 
          :class="['tab-btn', { active: registerType === 'merchant' }]"
          @click="registerType = 'merchant'"
        >
          商家注册
        </button>
      </div>

      <!-- 注册表单 -->
      <div class="form-register">
        <!-- 公共字段：手机号、验证码、密码 -->
        <div class="form-group">
          <input 
            type="text" 
            v-model="registerForm.phone" 
            placeholder="手机号"
            class="form-input"
            :class="{ 'error': registerErrors.phone }"
            maxlength="11"
          >
          <span v-if="registerErrors.phone" class="error-text">{{ registerErrors.phone }}</span>
        </div>
        
        <div class="form-group">
          <input 
            type="text" 
            v-model="registerForm.code" 
            placeholder="验证码"
            class="form-input code-input"
            :class="{ 'error': registerErrors.code }"
          >
          <button 
            @click="sendRegisterCode" 
            :disabled="registerCountdown > 0"
            class="code-btn"
          >
            {{ registerCountdown > 0 ? `${registerCountdown}s` : '获取验证码' }}
          </button>
          <span v-if="registerErrors.code" class="error-text">{{ registerErrors.code }}</span>
        </div>
        
        <div class="form-group">
          <input 
            type="text" 
            v-model="registerForm.nickName" 
            placeholder="昵称/用户名"
            class="form-input"
            :class="{ 'error': registerErrors.nickName }"
          >
          <span v-if="registerErrors.nickName" class="error-text">{{ registerErrors.nickName }}</span>
        </div>
        
        <div class="form-group">
          <input 
            type="password" 
            v-model="registerForm.password" 
            placeholder="设置密码（6-20位字母数字组合）"
            class="form-input"
            :class="{ 'error': registerErrors.password }"
          >
          <span v-if="registerErrors.password" class="error-text">{{ registerErrors.password }}</span>
        </div>
        
        <div class="form-group">
          <input 
            type="password" 
            v-model="registerForm.confirmPassword" 
            placeholder="确认密码"
            class="form-input"
            :class="{ 'error': registerErrors.confirmPassword }"
          >
          <span v-if="registerErrors.confirmPassword" class="error-text">{{ registerErrors.confirmPassword }}</span>
        </div>

        <!-- 学生注册字段 -->
        <template v-if="registerType === 'student'">
          <div class="form-group">
            <input 
              type="text" 
              v-model="registerForm.realName" 
              placeholder="真实姓名"
              class="form-input"
              :class="{ 'error': registerErrors.realName }"
            >
            <span v-if="registerErrors.realName" class="error-text">{{ registerErrors.realName }}</span>
          </div>
          
          <div class="form-group">
            <input 
              type="text" 
              v-model="registerForm.school" 
              placeholder="学校名称"
              class="form-input"
              :class="{ 'error': registerErrors.school }"
            >
            <span v-if="registerErrors.school" class="error-text">{{ registerErrors.school }}</span>
          </div>
          
          <div class="form-group">
            <input 
              type="text" 
              v-model="registerForm.studentId" 
              placeholder="学号"
              class="form-input"
              :class="{ 'error': registerErrors.studentId }"
            >
            <span v-if="registerErrors.studentId" class="error-text">{{ registerErrors.studentId }}</span>
          </div>
          
          <div class="form-group">
            <input 
              type="text" 
              v-model="registerForm.college" 
              placeholder="学院（可选）"
              class="form-input"
              :class="{ 'error': registerErrors.college }"
            >
            <span v-if="registerErrors.college" class="error-text">{{ registerErrors.college }}</span>
          </div>
          
          <div class="form-group">
            <input 
              type="text" 
              v-model="registerForm.major" 
              placeholder="专业（可选）"
              class="form-input"
              :class="{ 'error': registerErrors.major }"
            >
            <span v-if="registerErrors.major" class="error-text">{{ registerErrors.major }}</span>
          </div>
          
          <div class="form-group">
            <input 
              type="text" 
              v-model="registerForm.grade" 
              placeholder="年级（可选，如：2022）"
              class="form-input"
              :class="{ 'error': registerErrors.grade }"
            >
            <span v-if="registerErrors.grade" class="error-text">{{ registerErrors.grade }}</span>
          </div>
          
          <div class="form-group">
            <input 
              type="text" 
              v-model="registerForm.className" 
              placeholder="班级（可选）"
              class="form-input"
              :class="{ 'error': registerErrors.className }"
            >
            <span v-if="registerErrors.className" class="error-text">{{ registerErrors.className }}</span>
          </div>
        </template>

        <!-- 商家注册字段 -->
        <template v-if="registerType === 'merchant'">
          <div class="form-group">
            <input 
              type="text" 
              v-model="registerForm.merchantName" 
              placeholder="商家名称"
              class="form-input"
              :class="{ 'error': registerErrors.merchantName }"
            >
            <span v-if="registerErrors.merchantName" class="error-text">{{ registerErrors.merchantName }}</span>
          </div>
          
          <div class="form-group">
            <select 
              v-model="registerForm.merchantTypeId" 
              class="form-input"
              :class="{ 'error': registerErrors.merchantTypeId }"
            >
              <option value="">请选择商家类型</option>
              <option v-for="type in merchantTypes" :key="type.id" :value="type.id">
                {{ type.name }}
              </option>
            </select>
            <span v-if="registerErrors.merchantTypeId" class="error-text">{{ registerErrors.merchantTypeId }}</span>
          </div>
          
          <div class="form-group">
            <input 
              type="text" 
              v-model="registerForm.contactName" 
              placeholder="联系人姓名"
              class="form-input"
              :class="{ 'error': registerErrors.contactName }"
            >
            <span v-if="registerErrors.contactName" class="error-text">{{ registerErrors.contactName }}</span>
          </div>
          
          <div class="form-group">
            <input 
              type="text" 
              v-model="registerForm.contactPhone" 
              placeholder="联系电话"
              class="form-input"
              :class="{ 'error': registerErrors.contactPhone }"
              maxlength="11"
            >
            <span v-if="registerErrors.contactPhone" class="error-text">{{ registerErrors.contactPhone }}</span>
          </div>
          
          <div class="form-group">
            <textarea 
              v-model="registerForm.address" 
              placeholder="详细地址"
              class="form-input form-textarea"
              :class="{ 'error': registerErrors.address }"
              rows="3"
            ></textarea>
            <span v-if="registerErrors.address" class="error-text">{{ registerErrors.address }}</span>
          </div>
        </template>
        
        <button @click="handleRegister" class="register-btn" :disabled="registerLoading">
          {{ registerLoading ? '注册中...' : '注册' }}
        </button>
      </div>

      <div class="register-footer">
        <p class="login-link">
          已有账号？<router-link to="/login">去登录</router-link>
        </p>
        <p class="agreement">
          注册即表示您同意 <a href="#">《用户协议》</a> 和 <a href="#">《隐私政策》</a>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { registerApi, type RegisterPayload } from '../../api/auth'
import { sendVerificationCode } from '../../api/auth'
import { getMerchantTypes, type MerchantType } from '../../api/merchant'
import { notify } from '@/utils/notify'

const router = useRouter()

// 注册类型：student 或 merchant
const registerType = ref<'student' | 'merchant'>('student')

// 商家类型列表
const merchantTypes = ref<MerchantType[]>([])

// 注册表单
const registerForm = ref({
  phone: '',
  code: '',
  nickName: '',
  password: '',
  confirmPassword: '',
  // 学生字段
  realName: '',
  school: '',
  studentId: '',
  college: '',
  major: '',
  grade: '',
  className: '',
  // 商家字段
  merchantName: '',
  merchantTypeId: null as number | null,
  contactName: '',
  contactPhone: '',
  address: ''
})

// 注册验证码倒计时
const registerCountdown = ref(0)

// 注册表单验证错误
const registerErrors = ref({
  phone: '',
  code: '',
  nickName: '',
  password: '',
  confirmPassword: '',
  realName: '',
  school: '',
  studentId: '',
  college: '',
  major: '',
  grade: '',
  className: '',
  merchantName: '',
  merchantTypeId: '',
  contactName: '',
  contactPhone: '',
  address: ''
})

// 注册加载状态
const registerLoading = ref(false)

// 加载商家类型列表
const loadMerchantTypes = async () => {
  try {
    merchantTypes.value = await getMerchantTypes()
  } catch (error) {
    console.error('加载商家类型失败:', error)
  }
}

// 初始化
onMounted(() => {
  loadMerchantTypes()
})

// 验证手机号格式
const isValidPhone = (phone: string): boolean => {
  const phoneRegex = /^1[3-9]\d{9}$/
  return phoneRegex.test(phone)
}

// 发送注册验证码
const sendRegisterCode = async () => {
  registerErrors.value.phone = ''
  
  if (!registerForm.value.phone) {
    registerErrors.value.phone = '请输入手机号'
    return
  }
  
  if (!isValidPhone(registerForm.value.phone)) {
    registerErrors.value.phone = '请输入正确的手机号格式'
    return
  }
  
  try {
    await sendVerificationCode(registerForm.value.phone, 'register')
    
    registerCountdown.value = 60
    const timer = setInterval(() => {
      registerCountdown.value--
      if (registerCountdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
    
    notify('验证码已发送，请查收短信')
  } catch (error: any) {
    registerErrors.value.phone = error.message || '验证码发送失败，请重试'
  }
}

// 验证密码强度
const validatePassword = (password: string): boolean => {
  const hasLetter = /[a-zA-Z]/.test(password)
  const hasNumber = /\d/.test(password)
  const isValidLength = password.length >= 6 && password.length <= 20
  return hasLetter && hasNumber && isValidLength
}

// 验证学号格式
const isValidStudentId = (studentId: string): boolean => {
  const studentIdRegex = /^[A-Za-z0-9]{4,20}$/
  return studentIdRegex.test(studentId)
}

// 注册处理
const handleRegister = async () => {
  // 重置错误信息
  Object.keys(registerErrors.value).forEach(key => {
    registerErrors.value[key as keyof typeof registerErrors.value] = ''
  })
  
  // 表单验证
  let isValid = true
  
  // 公共字段验证
  if (!registerForm.value.phone) {
    registerErrors.value.phone = '请输入手机号'
    isValid = false
  } else if (!isValidPhone(registerForm.value.phone)) {
    registerErrors.value.phone = '请输入正确的手机号格式'
    isValid = false
  }
  
  if (!registerForm.value.code) {
    registerErrors.value.code = '请输入验证码'
    isValid = false
  } else if (!/^\d{6}$/.test(registerForm.value.code)) {
    registerErrors.value.code = '验证码格式不正确'
    isValid = false
  }
  
  if (!registerForm.value.nickName) {
    registerErrors.value.nickName = '请输入昵称'
    isValid = false
  } else if (registerForm.value.nickName.length < 2 || registerForm.value.nickName.length > 20) {
    registerErrors.value.nickName = '昵称长度必须在2-20个字符之间'
    isValid = false
  }
  
  if (!registerForm.value.password) {
    registerErrors.value.password = '请设置密码'
    isValid = false
  } else if (!validatePassword(registerForm.value.password)) {
    registerErrors.value.password = '密码必须包含字母和数字，长度6-20位'
    isValid = false
  }
  
  if (!registerForm.value.confirmPassword) {
    registerErrors.value.confirmPassword = '请确认密码'
    isValid = false
  } else if (registerForm.value.confirmPassword !== registerForm.value.password) {
    registerErrors.value.confirmPassword = '两次输入的密码不一致'
    isValid = false
  }
  
  // 学生注册字段验证
  if (registerType.value === 'student') {
    if (!registerForm.value.realName) {
      registerErrors.value.realName = '请输入真实姓名'
      isValid = false
    }
    
    if (!registerForm.value.school) {
      registerErrors.value.school = '请输入学校名称'
      isValid = false
    }
    
    if (!registerForm.value.studentId) {
      registerErrors.value.studentId = '请输入学号'
      isValid = false
    } else if (!isValidStudentId(registerForm.value.studentId)) {
      registerErrors.value.studentId = '学号格式不正确，只能包含字母和数字，长度4-20位'
      isValid = false
    }
  }
  
  // 商家注册字段验证
  if (registerType.value === 'merchant') {
    if (!registerForm.value.merchantName) {
      registerErrors.value.merchantName = '请输入商家名称'
      isValid = false
    }
    
    if (!registerForm.value.merchantTypeId) {
      registerErrors.value.merchantTypeId = '请选择商家类型'
      isValid = false
    }
    
    if (!registerForm.value.contactName) {
      registerErrors.value.contactName = '请输入联系人姓名'
      isValid = false
    }
    
    if (!registerForm.value.contactPhone) {
      registerErrors.value.contactPhone = '请输入联系电话'
      isValid = false
    } else if (!isValidPhone(registerForm.value.contactPhone)) {
      registerErrors.value.contactPhone = '请输入正确的手机号格式'
      isValid = false
    }
    
    if (!registerForm.value.address) {
      registerErrors.value.address = '请输入详细地址'
      isValid = false
    }
  }
  
  if (!isValid) {
    return
  }
  
  // 调用注册API
  registerLoading.value = true
  
  try {
    const payload: RegisterPayload = {
      phone: registerForm.value.phone,
      password: registerForm.value.password,
      nickName: registerForm.value.nickName,
      code: registerForm.value.code,
      scene: 'register',
      role: registerType.value
    }
    
    // 添加学生字段
    if (registerType.value === 'student') {
      payload.realName = registerForm.value.realName
      payload.school = registerForm.value.school
      payload.studentId = registerForm.value.studentId
      payload.college = registerForm.value.college || undefined
      payload.major = registerForm.value.major || undefined
      payload.grade = registerForm.value.grade || undefined
      payload.className = registerForm.value.className || undefined
    }
    
    // 添加商家字段
    if (registerType.value === 'merchant') {
      payload.merchantName = registerForm.value.merchantName
      payload.merchantTypeId = registerForm.value.merchantTypeId || undefined
      payload.contactName = registerForm.value.contactName
      payload.contactPhone = registerForm.value.contactPhone
      payload.address = registerForm.value.address
    }
    
    const result = await registerApi(payload)
    
    if (result.code === 200) {
      const successMsg = registerType.value === 'student' 
        ? `注册成功！\n\n学生信息已保存:\n姓名: ${registerForm.value.realName}\n学校: ${registerForm.value.school}\n学号: ${registerForm.value.studentId}\n\n正在为您登录...`
        : `注册成功！\n\n商家信息已保存:\n商家名称: ${registerForm.value.merchantName}\n\n注意：商家账号需要管理员审核后才能正常使用。\n\n正在为您登录...`
      
      notify(successMsg)
      router.push('/home')
    } else {
      notify(result.message || '注册失败，请重试')
    }
  } catch (error: any) {
    console.error('注册失败:', error)
    
    const errorMessage = error.message || error.response?.data?.message || error.response?.data?.msg || '注册失败，请重试'
    
    if (errorMessage.includes('手机号') || errorMessage.includes('已被注册')) {
      registerErrors.value.phone = errorMessage
    } else if (errorMessage.includes('验证码')) {
      registerErrors.value.code = errorMessage
    } else if (errorMessage.includes('密码')) {
      registerErrors.value.password = errorMessage
    } else if (errorMessage.includes('学号')) {
      registerErrors.value.studentId = errorMessage
    } else if (errorMessage.includes('用户名') || errorMessage.includes('昵称')) {
      registerErrors.value.nickName = errorMessage
    } else {
      notify(errorMessage)
    }
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.register-header {
  text-align: center;
  padding: 60px 20px 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.register-header h1 {
  font-size: 36px;
  margin: 0 0 10px;
}

.register-header p {
  font-size: 16px;
  margin: 0;
  opacity: 0.9;
}

.register-content {
  flex: 1;
  background-color: white;
  margin: -30px 20px 20px;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.register-title {
  text-align: center;
  margin-bottom: 30px;
}

.register-title h2 {
  font-size: 24px;
  margin: 0 0 8px;
  color: #333;
}

.subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
}

/* 注册类型切换标签 */
.register-tabs {
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

.form-register {
  padding: 10px 0;
}

.form-group {
  margin-bottom: 20px;
  position: relative;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 0.3s;
  box-sizing: border-box;
  font-family: inherit;
}

.form-input:focus {
  outline: none;
  border-color: #1677ff;
}

.form-input.error {
  border-color: #ff4d4f;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

select.form-input {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23666' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 40px;
}

.error-text {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 5px;
  display: block;
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

.register-btn {
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

.register-btn:hover:not(:disabled) {
  background-color: #0958d9;
}

.register-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.register-footer {
  margin-top: 30px;
  text-align: center;
}

.login-link {
  font-size: 14px;
  color: #666;
  margin-bottom: 15px;
}

.login-link a {
  color: #1677ff;
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}

.agreement {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.agreement a {
  color: #1677ff;
  text-decoration: none;
}

.agreement a:hover {
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .register-header {
    padding: 40px 20px 30px;
  }
  
  .register-content {
    margin: -25px 15px 20px;
    padding: 20px;
  }
  
  .register-title h2 {
    font-size: 20px;
  }
  
  .tab-btn {
    font-size: 14px;
  }
}
</style>
