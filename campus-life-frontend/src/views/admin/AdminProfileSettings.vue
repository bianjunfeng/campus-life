<template>
  <div class="admin-profile-settings">
    <div class="profile-summary">
      <div class="avatar-section">
        <img
          v-if="userInfo.icon"
          :src="userInfo.icon"
          alt="头像"
          class="summary-avatar"
        />
        <div v-else class="summary-avatar-placeholder">
          {{ userInfo.nickName ? userInfo.nickName.charAt(0) : 'U' }}
        </div>
        <button
          class="avatar-upload-btn"
          :disabled="uploadingAvatar"
          @click="handleAvatarUpload"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
            <circle cx="12" cy="13" r="4"></circle>
          </svg>
        </button>
        <input
          ref="avatarInput"
          type="file"
          accept="image/png,image/jpeg,image/jpg,image/gif,image/webp"
          class="hidden-file-input"
          @change="handleAvatarSelected"
        />
      </div>
      <div class="summary-info">
        <div class="info-row">
          <div class="info-item">
            <span class="info-label">用户名:</span>
            <span class="info-value">{{ userInfo.nickName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">账号ID:</span>
            <span class="info-value">{{ userInfo.id || '-' }}</span>
          </div>
        </div>
        <div class="info-row">
          <div class="info-item">
            <span class="info-label">账号角色:</span>
            <span class="info-value">{{ roleTextMap[userInfo.role] || '未知' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">手机号码:</span>
            <span class="info-value">{{ userInfo.phone || '-' }}</span>
          </div>
        </div>
        <div class="info-row">
          <div class="info-item">
            <span class="info-label">所在区域:</span>
            <span class="info-value">{{ userInfo.region || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">职业:</span>
            <span class="info-value">{{ userInfo.occupation || '-' }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="settings-content">
      <div class="form-section">
        <h3 class="form-title">基础信息</h3>
        <form @submit.prevent="handleSaveBasic">
          <div class="form-row">
            <div class="form-item">
              <label class="form-label">
                昵称
                <span class="required">*</span>
              </label>
              <input
                v-model="basicForm.nickname"
                type="text"
                placeholder="请输入您的昵称"
                class="form-input"
                required
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-item">
              <label class="form-label">所在区域</label>
              <input
                v-model="basicForm.region"
                type="text"
                placeholder="请输入所在区域"
                class="form-input"
              />
            </div>
            <div class="form-item">
              <label class="form-label">职业</label>
              <input
                v-model="basicForm.occupation"
                type="text"
                placeholder="请输入职业信息"
                class="form-input"
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-item">
              <label class="form-label">性别</label>
              <select v-model="basicForm.gender" class="form-select">
                <option value="">请选择</option>
                <option value="保密">保密</option>
                <option value="男">男</option>
                <option value="女">女</option>
              </select>
            </div>
            <div class="form-item">
              <label class="form-label">生日</label>
              <input
                v-model="basicForm.birthday"
                type="date"
                class="form-input"
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-item full-width">
              <label class="form-label">个人简介</label>
              <textarea
                v-model="basicForm.bio"
                placeholder="请输入您的个人简介，最多不超过200字。"
                class="form-textarea"
                maxlength="200"
                rows="4"
              ></textarea>
              <div class="char-count">{{ basicForm.bio.length }}/200</div>
            </div>
          </div>

          <div class="account-tip">
            <div class="account-tip-title">当前可用能力</div>
            <div class="account-tip-desc">
              本页当前仅保留后端已接通的头像上传和基础资料修改能力。密码、手机号、邮箱和实名认证没有管理员后台独立接口，因此不再展示误导性的“开发中”入口。
            </div>
          </div>

          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
            <button type="button" class="btn btn-secondary" :disabled="saving" @click="handleResetBasic">
              重置
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getUserInfo, updateUserProfile, uploadAvatar, type UserInfo } from '../../api/auth'
import { showAlert } from '../../utils/dialog'

type EditableUserInfo = UserInfo & {
  birthday?: string
  gender?: string
  occupation?: string
  region?: string
  bio?: string
}

const avatarInput = ref<HTMLInputElement | null>(null)
const saving = ref(false)
const uploadingAvatar = ref(false)

const roleTextMap: Record<string, string> = {
  GUEST: '游客',
  STUDENT: '学生',
  MERCHANT: '商家',
  ADMIN: '管理员'
}

const userInfo = ref<EditableUserInfo>({
  id: 0,
  phone: '',
  nickName: '',
  icon: '',
  bio: '',
  gender: '',
  birthday: '',
  region: '',
  occupation: '',
  role: 'GUEST'
})

const basicForm = ref({
  nickname: '',
  region: '',
  occupation: '',
  gender: '',
  birthday: '',
  bio: ''
})

const applyUserInfo = (info: Partial<EditableUserInfo>) => {
  userInfo.value = {
    ...userInfo.value,
    ...info
  }
  basicForm.value.nickname = info.nickName || ''
  basicForm.value.region = info.region || ''
  basicForm.value.occupation = info.occupation || ''
  basicForm.value.gender = info.gender || ''
  basicForm.value.birthday = info.birthday || ''
  basicForm.value.bio = info.bio || ''
}

const loadUserInfo = async () => {
  try {
    const info = await getUserInfo()
    applyUserInfo(info)
  } catch (error) {
    console.error('加载用户信息失败:', error)
    try {
      const userInfoStr = localStorage.getItem('userInfo')
      if (userInfoStr) {
        const info = JSON.parse(userInfoStr)
        applyUserInfo({
          id: info.id || 0,
          phone: info.phone || '',
          nickName: info.nickName || info.username || info.name || '',
          icon: info.icon || info.avatar || '',
          bio: info.bio || '',
          gender: info.gender || '',
          birthday: info.birthday || '',
          region: info.region || '',
          occupation: info.occupation || '',
          role: info.role || 'GUEST'
        })
      }
    } catch (parseError) {
      console.error('解析用户信息失败:', parseError)
    }
  }
}

const handleSaveBasic = async () => {
  try {
    saving.value = true
    const updated = await updateUserProfile({
      nickName: basicForm.value.nickname.trim(),
      intro: basicForm.value.bio.trim(),
      region: basicForm.value.region.trim(),
      occupation: basicForm.value.occupation.trim(),
      gender: basicForm.value.gender || undefined,
      birthday: basicForm.value.birthday || undefined
    })
    applyUserInfo(updated)
    await showAlert('保存成功')
  } catch (error) {
    console.error('保存失败:', error)
    await showAlert(error instanceof Error ? error.message : '保存失败，请重试')
  } finally {
    saving.value = false
  }
}

const handleResetBasic = () => {
  void loadUserInfo()
}

const handleAvatarUpload = () => {
  if (!uploadingAvatar.value) {
    avatarInput.value?.click()
  }
}

const handleAvatarSelected = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  target.value = ''

  if (!file) {
    return
  }

  try {
    uploadingAvatar.value = true
    const uploadResult = await uploadAvatar(file)
    const updated = await updateUserProfile({ icon: uploadResult.url })
    applyUserInfo(updated)
    await showAlert('头像更新成功')
  } catch (error) {
    console.error('头像上传失败:', error)
    await showAlert(error instanceof Error ? error.message : '头像上传失败，请稍后重试')
  } finally {
    uploadingAvatar.value = false
  }
}

onMounted(() => {
  void loadUserInfo()
})
</script>

<style scoped>
.admin-profile-settings {
  width: 100%;
}

.profile-summary {
  background: white;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  gap: 24px;
}

.avatar-section {
  position: relative;
  flex-shrink: 0;
}

.summary-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
}

.summary-avatar-placeholder {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 40px;
  font-weight: 700;
}

.avatar-upload-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #1677ff;
  color: white;
  border: 2px solid white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.avatar-upload-btn:hover:not(:disabled) {
  background: #0958d9;
}

.avatar-upload-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.avatar-upload-btn svg {
  width: 16px;
  height: 16px;
}

.hidden-file-input {
  display: none;
}

.summary-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  gap: 32px;
}

.info-item {
  display: flex;
  gap: 8px;
}

.info-label {
  color: #666;
  font-size: 14px;
}

.info-value {
  color: #333;
  font-size: 14px;
  font-weight: 500;
}

.settings-content {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.form-section {
  max-width: 800px;
}

.form-title {
  margin: 0 0 24px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.form-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.form-item {
  flex: 1;
}

.form-item.full-width {
  width: 100%;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.required {
  color: #ff4d4f;
  margin-left: 4px;
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  outline: none;
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.1);
}

.form-textarea {
  resize: vertical;
  font-family: inherit;
}

.char-count {
  text-align: right;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.account-tip {
  margin-top: 8px;
  padding: 16px 18px;
  border-radius: 8px;
  background: #f6f8fb;
  border: 1px solid #e8edf5;
}

.account-tip-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.account-tip-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 32px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-primary {
  background: #1677ff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #0958d9;
}

.btn-secondary {
  background: #f0f0f0;
  color: #333;
}

.btn-secondary:hover:not(:disabled) {
  background: #e0e0e0;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

@media (max-width: 768px) {
  .profile-summary {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .info-row {
    flex-direction: column;
    gap: 12px;
  }

  .form-row,
  .form-actions {
    flex-direction: column;
  }
}
</style>
