<template>
  <div class="edit-profile-page">
    <!-- 顶部导航栏 -->
    <div class="header">
      <div class="back-button" @click="handleBack">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
      </div>
      <h1>编辑资料</h1>
      <div class="preview-button" @click="handlePreview">预览</div>
    </div>
    
    <!-- 编辑内容区域 -->
    <div class="content-container">
      <!-- 头像编辑 -->
      <div class="avatar-section">
        <div class="avatar-container">
          <img :src="userInfo.avatar || 'https://via.placeholder.com/100x100?text=User'" alt="用户头像" class="avatar" :class="{ 'uploading': uploadingAvatar }">
          <div class="camera-icon" @click="handleChangeAvatar" :class="{ 'uploading': uploadingAvatar }">
            <div v-if="uploadingAvatar" class="upload-spinner"></div>
            <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
              <circle cx="12" cy="13" r="4"></circle>
            </svg>
          </div>
        </div>
        <p class="avatar-hint">点击相机图标上传头像</p>
      </div>
      
      <!-- 基本信息编辑 -->
      <div class="info-section">
        <div class="info-item" @click="navigateToEdit('name')">
          <span class="label">昵称</span>
          <div class="value-container">
            <span class="value">{{ userInfo.name || '未设置' }}</span>
            <div class="arrow-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>
        </div>
        
        <div class="info-item info-item-readonly">
          <span class="label">小红书号</span>
          <div class="value-container">
            <span class="value">{{ userInfo.xiaohongshuId || '未设置' }}</span>
          </div>
        </div>
        
        <div class="info-item" @click="navigateToEdit('background')">
          <span class="label">背景图</span>
          <div class="value-container">
            <div class="background-thumbnail" v-if="userInfo.background"></div>
            <span class="value" v-else>未设置</span>
            <div class="arrow-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>
        </div>
        
        <div class="info-item" @click="navigateToEdit('bio')">
          <span class="label">简介</span>
          <div class="value-container">
            <span class="value">{{ userInfo.bio || '介绍一下自己' }}</span>
            <div class="arrow-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 个人信息编辑 -->
      <div class="info-section">
        <div class="info-item" @click="navigateToEdit('gender')">
          <span class="label">性别</span>
          <div class="value-container">
            <span class="value">{{ userInfo.gender || '选择性别' }}</span>
            <div class="arrow-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>
        </div>

        <div class="info-item" @click="navigateToEdit('birthday')">
          <span class="label">生日</span>
          <div class="value-container">
            <span class="value">{{ userInfo.birthday || '选择生日' }}</span>
            <div class="arrow-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>
        </div>
        
        <div class="info-item" @click="navigateToEdit('region')">
          <span class="label">地区</span>
          <div class="value-container">
            <span class="value">{{ userInfo.region || '选择所在的地区' }}</span>
            <div class="arrow-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>
        </div>
        
        <div class="info-item" @click="navigateToEdit('occupation')">
          <span class="label">职业</span>
          <div class="value-container">
            <span class="value">{{ userInfo.occupation || '选择职业' }}</span>
            <div class="arrow-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>
        </div>
        
      </div>
      
      <!-- 原创认证 -->
      <div class="info-section">
        <div class="info-item" @click="navigateToEdit('originalVerification')">
          <span class="label">原创信息</span>
          <div class="value-container">
            <span class="value">{{ userInfo.originalVerification || '暂未完成原创认证' }}</span>
            <div class="arrow-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 编辑弹窗 -->
    <div v-if="showEditDialog" class="edit-dialog-overlay" @click="cancelEdit">
      <div class="edit-dialog" @click.stop>
        <div class="dialog-header">
          <h3>编辑{{ getFieldName(editingField) }}</h3>
          <button class="close-btn" @click="cancelEdit">×</button>
        </div>
        <div class="dialog-body">
          <!-- 性别下拉选择 -->
          <select 
            v-if="editingField === 'gender'"
            v-model="editValue" 
            class="edit-select"
          >
            <option value="">请选择性别</option>
            <option value="保密">保密</option>
            <option value="男">男</option>
            <option value="女">女</option>
          </select>
          
          <!-- 生日日期选择 -->
          <input 
            v-else-if="editingField === 'birthday'"
            v-model="editValue" 
            type="date" 
            class="edit-input"
            :placeholder="`请选择${getFieldName(editingField)}`"
          />
          
          <!-- 地区下拉选择 -->
          <select 
            v-else-if="editingField === 'region'"
            v-model="editValue" 
            class="edit-select"
          >
            <option value="">请选择地区</option>
            <option value="北京">北京</option>
            <option value="上海">上海</option>
            <option value="天津">天津</option>
            <option value="重庆">重庆</option>
            <option value="河北">河北</option>
            <option value="山西">山西</option>
            <option value="内蒙古">内蒙古</option>
            <option value="辽宁">辽宁</option>
            <option value="吉林">吉林</option>
            <option value="黑龙江">黑龙江</option>
            <option value="江苏">江苏</option>
            <option value="浙江">浙江</option>
            <option value="安徽">安徽</option>
            <option value="福建">福建</option>
            <option value="江西">江西</option>
            <option value="山东">山东</option>
            <option value="河南">河南</option>
            <option value="湖北">湖北</option>
            <option value="湖南">湖南</option>
            <option value="广东">广东</option>
            <option value="广西">广西</option>
            <option value="海南">海南</option>
            <option value="四川">四川</option>
            <option value="贵州">贵州</option>
            <option value="云南">云南</option>
            <option value="西藏">西藏</option>
            <option value="陕西">陕西</option>
            <option value="甘肃">甘肃</option>
            <option value="青海">青海</option>
            <option value="宁夏">宁夏</option>
            <option value="新疆">新疆</option>
            <option value="香港">香港</option>
            <option value="澳门">澳门</option>
            <option value="台湾">台湾</option>
          </select>
          
          <!-- 简介文本域 -->
          <textarea 
            v-else-if="editingField === 'bio'"
            v-model="editValue" 
            class="edit-textarea"
            :placeholder="`请输入${getFieldName(editingField)}`"
            rows="4"
          ></textarea>
          
          <!-- 其他字段文本输入 -->
          <input 
            v-else
            v-model="editValue" 
            type="text" 
            class="edit-input"
            :placeholder="`请输入${getFieldName(editingField)}`"
            @keyup.enter="confirmEdit"
          />
        </div>
        <div class="dialog-footer">
          <button class="cancel-btn" @click="cancelEdit" :disabled="saving">取消</button>
          <button 
            class="confirm-btn" 
            @click="confirmEdit" 
            :disabled="saving || (editingField !== 'birthday' && !editValue.trim())"
          >
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCurrentUser } from '../../api/auth'
import type { UserInfo } from '../../api/auth'
import http from '../../api/http'
import { notify } from '@/utils/notify'

const router = useRouter()

// 加载状态
const loading = ref(true)

// 用户信息数据
const userInfo = reactive({
  avatar: '',
  name: '',
  xiaohongshuId: '',
  background: '',
  bio: '',
  gender: '',
  birthday: '',
  region: '',
  occupation: '',
  school: '',
  originalVerification: '暂未完成原创认证'
})

// 编辑弹窗状态
const showEditDialog = ref(false)
const editingField = ref('')
const editValue = ref('')
const saving = ref(false)
const uploadingAvatar = ref(false)

const normalizeGender = (value: unknown): string => {
  if (value === null || value === undefined || value === '') return ''
  if (typeof value === 'number') {
    if (value === 1) return '男'
    if (value === 2) return '女'
    return '保密'
  }
  const str = String(value).trim()
  if (str === '1' || str === '男') return '男'
  if (str === '2' || str === '女') return '女'
  if (str === '0' || str === '保密') return '保密'
  return str
}

const normalizeBirthday = (value: unknown): string => {
  if (value === null || value === undefined) return ''
  const str = String(value).trim()
  if (!str) return ''
  return str.length >= 10 ? str.slice(0, 10) : str
}

// 加载用户信息
const loadUserInfo = async () => {
  loading.value = true
  try {
    const result = await fetchCurrentUser()
    if (result.code === 200 && result.data) {
      const data = result.data as any
      userInfo.avatar = data.icon || data.avatarUrl || 'https://via.placeholder.com/100x100?text=User'
      userInfo.name = data.nickName || data.username || ''
      userInfo.bio = data.bio || data.intro || ''
      userInfo.xiaohongshuId = data.id ? String(data.id) : ''
      
      // 从数据库读取并标准化个人信息（兼容不同返回字段）
      userInfo.gender = normalizeGender(data.gender)
      userInfo.birthday = normalizeBirthday(data.birthday)
      userInfo.region = data.region || data.area || ''
      userInfo.occupation = data.occupation || ''
      
      // 学校信息可能在其他地方，暂时保留从localStorage读取
      const savedUserInfo = localStorage.getItem('userInfo')
      if (savedUserInfo) {
        try {
          const saved = JSON.parse(savedUserInfo)
          if (saved.school) userInfo.school = saved.school
        } catch (e) {
          console.warn('解析保存的用户信息失败:', e)
        }
      }
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
  } finally {
    loading.value = false
  }
}

// 处理返回
const handleBack = () => {
  router.back()
}

// 处理预览
const handlePreview = () => {
  router.push('/me')
}

// 处理更换头像
const handleChangeAvatar = () => {
  // 创建文件输入元素
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e: Event) => {
    const target = e.target as HTMLInputElement
    const file = target.files?.[0]
    if (!file) return

    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      notify('请选择图片文件')
      return
    }

    // 验证文件大小（5MB）
    if (file.size > 5 * 1024 * 1024) {
      notify('文件大小不能超过5MB')
      return
    }

    // 显示上传中
    uploadingAvatar.value = true

    try {
      // 创建FormData
      const formData = new FormData()
      formData.append('file', file)

      // 上传文件
      // 注意：http 的 baseURL 是 '/api'，所以这里只需要 '/upload/avatar'
      const { data } = await http.post('/upload/avatar', formData)
      
      console.log('上传响应:', data)

      if (data.code === 200 && data.data && data.data.url) {
        // 上传成功，更新头像
        const avatarUrl = data.data.url
        await saveField('avatar', avatarUrl)
      } else {
        notify('上传失败: ' + (data.msg || '未知错误'))
      }
    } catch (error: any) {
      console.error('上传头像失败:', error)
      notify('上传失败: ' + (error.message || '未知错误'))
    } finally {
      uploadingAvatar.value = false
    }
  }
  input.click()
}

// 导航到具体编辑页面
const navigateToEdit = (fieldType: string) => {
  editingField.value = fieldType
  editValue.value = getUserInfoValue(fieldType)
  showEditDialog.value = true
}

// 获取用户信息字段值
const getUserInfoValue = (fieldType: string): string => {
  switch (fieldType) {
    case 'name':
      return userInfo.name
    case 'xiaohongshuId':
      return userInfo.xiaohongshuId
    case 'background':
      return userInfo.background
    case 'bio':
      return userInfo.bio
    case 'birthday':
      return userInfo.birthday
    case 'gender':
      return userInfo.gender
    case 'region':
      return userInfo.region
    case 'occupation':
      return userInfo.occupation
    case 'school':
      return userInfo.school
    case 'originalVerification':
      return userInfo.originalVerification
    default:
      return ''
  }
}

// 保存字段
const saveField = async (fieldType: string, value: string) => {
  saving.value = true
  try {
    // 所有字段都调用后端API保存到数据库
    const payload: any = {}
    
    if (fieldType === 'name') {
      payload.nickName = value
    } else if (fieldType === 'bio') {
      payload.intro = value
    } else if (fieldType === 'avatar') {
      payload.icon = value
    } else if (fieldType === 'gender') {
      payload.gender = value
    } else if (fieldType === 'birthday') {
      payload.birthday = value
    } else if (fieldType === 'region') {
      payload.region = value
    } else if (fieldType === 'occupation') {
      payload.occupation = value
    }
    
    // 调用后端API更新用户资料
    const { data } = await http.put('/users/me/profile', payload)
    
    if (data.code === 200 && data.data) {
      // 更新本地用户信息
      const updatedData = data.data as any
      if (fieldType === 'name') {
        userInfo.name = updatedData.nickName || updatedData.username || value
      } else if (fieldType === 'bio') {
        userInfo.bio = updatedData.bio || updatedData.intro || value
      } else if (fieldType === 'avatar') {
        userInfo.avatar = updatedData.icon || updatedData.avatarUrl || value
      } else if (fieldType === 'gender') {
        userInfo.gender = normalizeGender(updatedData.gender || value)
      } else if (fieldType === 'birthday') {
        userInfo.birthday = normalizeBirthday(updatedData.birthday || value)
      } else if (fieldType === 'region') {
        userInfo.region = updatedData.region || updatedData.area || value
      } else if (fieldType === 'occupation') {
        userInfo.occupation = updatedData.occupation || value
      }
      
      // 更新localStorage中的用户信息（用于其他页面）
      const savedUserInfo = localStorage.getItem('userInfo')
      if (savedUserInfo) {
        try {
          const saved = JSON.parse(savedUserInfo)
          if (fieldType === 'name') saved.nickName = userInfo.name
          if (fieldType === 'bio') saved.bio = userInfo.bio
          if (fieldType === 'avatar') saved.icon = userInfo.avatar
          if (fieldType === 'gender') saved.gender = userInfo.gender
          if (fieldType === 'birthday') saved.birthday = userInfo.birthday
          if (fieldType === 'region') saved.region = userInfo.region
          if (fieldType === 'occupation') saved.occupation = userInfo.occupation
          localStorage.setItem('userInfo', JSON.stringify(saved))
        } catch (e) {
          console.warn('更新localStorage失败:', e)
        }
      }
      
      // 保存成功，静默关闭对话框
      showEditDialog.value = false
    } else {
      notify('保存失败: ' + (data.msg || '未知错误'))
    }
  } catch (error: any) {
    console.error('保存失败:', error)
    notify('保存失败: ' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// 确认编辑
const confirmEdit = () => {
  // 生日可以为空，其他字段需要验证
  if (editingField.value !== 'birthday' && !editValue.value.trim()) {
    notify('请输入内容')
    return
  }
  // 对于下拉选择，直接使用选中的值（可能为空字符串）
  const value = editingField.value === 'birthday' ? editValue.value : editValue.value.trim()
  saveField(editingField.value, value)
}

// 取消编辑
const cancelEdit = () => {
  showEditDialog.value = false
  editingField.value = ''
  editValue.value = ''
}

// 获取字段名称
const getFieldName = (fieldType: string): string => {
  const fieldNames: Record<string, string> = {
    name: '昵称',
    xiaohongshuId: '小红书号',
    background: '背景图',
    bio: '简介',
    gender: '性别',
    birthday: '生日',
    region: '地区',
    occupation: '职业',
    originalVerification: '原创信息',
    avatar: '头像'
  }
  return fieldNames[fieldType] || fieldType
}

// 页面加载时获取用户信息
onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.edit-profile-page {
  background-color: #f8f8f8;
  min-height: 100vh;
}

/* 顶部导航栏 */
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px;
  padding: 0 16px;
  background-color: white;
  border-bottom: 1px solid #e0e0e0;
  z-index: 100;
}

.back-button,
.preview-button {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #1677ff;
}

.preview-button {
  font-size: 16px;
  width: auto;
}

.back-button svg {
  width: 24px;
  height: 24px;
}

.header h1 {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

/* 内容容器 */
.content-container {
  padding-top: 50px;
}

/* 头像部分 */
.avatar-section {
  background-color: white;
  padding: 20px 0;
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}

.avatar-container {
  position: relative;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
}

.camera-icon {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background-color: #1677ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
  cursor: pointer;
}

.camera-icon svg {
  width: 16px;
  height: 16px;
}

.camera-icon.uploading {
  opacity: 0.6;
  cursor: wait;
}

.avatar.uploading {
  opacity: 0.6;
}

.upload-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.avatar-hint {
  text-align: center;
  color: #999;
  font-size: 12px;
  margin-top: 8px;
}

/* 信息项部分 */
.info-section {
  background-color: white;
  margin-bottom: 10px;
  padding: 0 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item:active {
  background-color: #f5f5f5;
}

.info-item-readonly {
  cursor: default;
}

.info-item-readonly:active {
  background-color: transparent;
}

.label {
  font-size: 16px;
  color: #333;
}

.value-container {
  display: flex;
  align-items: center;
}

.value {
  font-size: 16px;
  color: #666;
  margin-right: 8px;
}

.background-thumbnail {
  width: 30px;
  height: 30px;
  background-color: #e0e0e0;
  border-radius: 4px;
  margin-right: 8px;
}

.arrow-icon {
  color: #999;
}

.arrow-icon svg {
  width: 20px;
  height: 20px;
}

/* 编辑弹窗 */
.edit-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.edit-dialog {
  background-color: white;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.dialog-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #333;
}

.dialog-body {
  padding: 20px;
  flex: 1;
  overflow-y: auto;
}

.edit-input,
.edit-textarea,
.edit-select {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 16px;
  font-family: inherit;
  resize: none;
  background-color: white;
}

.edit-input:focus,
.edit-textarea:focus,
.edit-select:focus {
  outline: none;
  border-color: #1677ff;
}

.edit-select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23333' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 40px;
}

.edit-select option {
  padding: 8px;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
}

.cancel-btn,
.confirm-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #333;
}

.cancel-btn:hover:not(:disabled) {
  background-color: #e0e0e0;
}

.confirm-btn {
  background-color: #1677ff;
  color: white;
}

.confirm-btn:hover:not(:disabled) {
  background-color: #0958d9;
}

.cancel-btn:disabled,
.confirm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 加载状态 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1677ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
