<template>
  <div class="account-center-page">
    <div class="page-header">
      <div>
        <h1>个人信息</h1>
        <p class="subtitle">{{ isMerchantPortal ? '查看并维护当前商家账号与店铺基础资料' : '查看当前管理账号的基础资料与安全设置' }}</p>
      </div>
      <div class="page-actions">
        <button v-if="loadError" class="btn btn-secondary" type="button" @click="loadProfile">
          重新加载
        </button>
        <button v-else class="btn btn-primary" type="button" :disabled="loading" @click="openEditDrawer">
          编辑资料
        </button>
      </div>
    </div>

    <section v-if="loading" class="account-card account-skeleton">
      <div class="skeleton-avatar"></div>
      <div class="skeleton-lines">
        <span></span>
        <span></span>
        <span></span>
      </div>
      <div class="skeleton-grid">
        <span v-for="item in 8" :key="item"></span>
      </div>
    </section>

    <section v-else-if="loadError" class="account-state error">
      <h3>加载失败</h3>
      <p>{{ loadError }}</p>
      <button class="btn btn-secondary" type="button" @click="loadProfile">重新加载</button>
    </section>

    <section v-else class="account-card">
      <div class="account-profile-head">
        <div class="profile-avatar">
          <img v-if="avatarUrl" :src="avatarUrl" alt="头像" />
          <span v-else>{{ avatarText }}</span>
        </div>
        <div class="profile-summary">
          <div class="profile-title-row">
            <h2>{{ primaryName }}</h2>
            <span class="role-badge">{{ roleText }}</span>
          </div>
          <p>{{ secondaryLine }}</p>
        </div>
      </div>

      <div class="account-detail-grid">
        <div v-for="item in detailItems" :key="item.label" class="detail-item">
          <dt>{{ item.label }}</dt>
          <dd>
            <span v-if="item.badge" class="status-badge" :class="item.badgeClass">{{ item.value }}</span>
            <span v-else>{{ item.value || '-' }}</span>
          </dd>
        </div>
      </div>
    </section>

    <div v-if="drawerOpen" class="account-drawer-mask" @click.self="closeEditDrawer">
      <aside class="drawer-panel account-drawer" aria-label="修改资料">
        <form class="account-drawer-form" @submit.prevent="saveProfile">
          <div class="drawer-header">
            <div>
              <h2>修改资料</h2>
              <p>{{ isMerchantPortal ? '仅修改账号资料和店铺展示信息，不影响商家认证状态。' : '仅允许修改基础个人资料，角色和权限由系统管理员维护。' }}</p>
            </div>
            <button class="icon-close" type="button" :disabled="saving" @click="closeEditDrawer" aria-label="关闭">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6 6 18" />
                <path d="m6 6 12 12" />
              </svg>
            </button>
          </div>

          <div class="drawer-body">
            <div v-if="saveError" class="form-error">{{ saveError }}</div>

            <section class="form-section">
              <h3>基础资料</h3>
              <div class="avatar-edit-row">
                <div class="profile-avatar small">
                  <img v-if="editAvatarPreview" :src="editAvatarPreview" alt="头像预览" />
                  <span v-else>{{ editAvatarText }}</span>
                </div>
                <div>
                  <button class="btn btn-secondary" type="button" :disabled="saving" @click="avatarInput?.click()">
                    更换头像
                  </button>
                  <p>支持 JPG、PNG、WebP，建议使用 1:1 图片。</p>
                  <input
                    ref="avatarInput"
                    class="hidden-input"
                    type="file"
                    accept="image/png,image/jpeg,image/jpg,image/webp"
                    @change="handleAvatarSelected"
                  />
                </div>
              </div>

              <div class="form-grid">
                <label class="form-field">
                  <span>{{ isMerchantPortal ? '联系人姓名' : '姓名' }}</span>
                  <input v-model.trim="editForm.name" type="text" maxlength="30" placeholder="请输入姓名" />
                </label>
                <label class="form-field">
                  <span>手机号</span>
                  <input v-model.trim="editForm.phone" type="tel" maxlength="11" placeholder="请输入手机号" />
                </label>
                <label class="form-field">
                  <span>邮箱</span>
                  <input v-model.trim="editForm.email" type="email" maxlength="80" placeholder="请输入邮箱" />
                </label>
              </div>
            </section>

            <section v-if="isMerchantPortal" class="form-section">
              <h3>店铺资料</h3>
              <div class="form-grid one-column">
                <label class="form-field">
                  <span>店铺简介</span>
                  <textarea v-model.trim="editForm.shopIntro" maxlength="200" rows="4" placeholder="请输入店铺简介"></textarea>
                  <em>{{ editForm.shopIntro.length }}/200</em>
                </label>
                <label class="form-field">
                  <span>联系地址</span>
                  <input v-model.trim="editForm.address" type="text" maxlength="120" placeholder="请输入联系地址" />
                </label>
                <label class="form-field">
                  <span>营业时间</span>
                  <input v-model.trim="editForm.businessHours" type="text" maxlength="60" placeholder="例如 09:00-22:00" />
                </label>
              </div>
            </section>
          </div>

          <div class="drawer-footer">
            <button class="btn btn-secondary" type="button" :disabled="saving" @click="closeEditDrawer">取消</button>
            <button class="btn btn-primary" type="submit" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </aside>
    </div>

    <div v-if="passwordDialogOpen" class="dialog-overlay account-modal-mask" @click.self="closePasswordDialog">
      <section class="modal-content password-modal" role="dialog" aria-modal="true" aria-labelledby="password-title">
        <div class="modal-header">
          <h2 id="password-title">修改密码</h2>
          <button class="icon-close" type="button" :disabled="changingPassword" @click="closePasswordDialog" aria-label="关闭">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6 6 18" />
              <path d="m6 6 12 12" />
            </svg>
          </button>
        </div>

        <form class="password-form" @submit.prevent="submitPassword">
          <div v-if="passwordError" class="form-error">{{ passwordError }}</div>
          <label class="form-field">
            <span>原密码</span>
            <input v-model="passwordForm.oldPassword" type="password" autocomplete="current-password" placeholder="请输入原密码" />
          </label>
          <label class="form-field">
            <span>新密码</span>
            <input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" placeholder="请输入新密码" />
          </label>
          <div class="password-strength" :class="passwordStrength.level">
            <div class="strength-bars">
              <span></span>
              <span></span>
              <span></span>
            </div>
            <p>密码强度：{{ passwordStrength.text }}</p>
          </div>
          <label class="form-field">
            <span>确认新密码</span>
            <input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" placeholder="请再次输入新密码" />
          </label>

          <div class="modal-actions">
            <button class="btn btn-secondary" type="button" :disabled="changingPassword" @click="closePasswordDialog">取消</button>
            <button class="btn btn-primary" type="submit" :disabled="changingPassword">
              {{ changingPassword ? '提交中...' : '保存' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { changePassword, getUserInfo, updateUserProfile, uploadAvatar, type UserInfo } from '../../api/auth'
import { getMerchantHome, updateMerchantProfile, type MerchantHomeMerchantInfo } from '../../api/merchant'
import { showAlert } from '../../utils/dialog'

type DetailItem = {
  label: string
  value: string
  badge?: boolean
  badgeClass?: string
}

const route = useRoute()
const router = useRouter()

const isMerchantPortal = computed(() => route.path.startsWith('/merchant'))
const loading = ref(false)
const loadError = ref('')
const saving = ref(false)
const saveError = ref('')
const drawerOpen = ref(false)
const passwordDialogOpen = ref(false)
const changingPassword = ref(false)
const passwordError = ref('')
const avatarInput = ref<HTMLInputElement | null>(null)
const selectedAvatarFile = ref<File | null>(null)
const selectedAvatarUrl = ref('')

const account = ref<Partial<UserInfo>>({})
const merchantInfo = ref<MerchantHomeMerchantInfo | null>(null)

const editForm = ref({
  name: '',
  phone: '',
  email: '',
  shopIntro: '',
  address: '',
  businessHours: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const roleTextMap: Record<string, string> = {
  GUEST: '游客',
  STUDENT: '学生',
  MERCHANT: '商家',
  ADMIN: '管理员'
}

const normalizeRole = (role?: string) => String(role || '').replace(/^ROLE_/, '').toUpperCase()

const roleText = computed(() => roleTextMap[normalizeRole(account.value.role)] || (isMerchantPortal.value ? '商家' : '管理员'))
const avatarUrl = computed(() => account.value.icon || '')
const primaryName = computed(() => {
  if (isMerchantPortal.value) {
    return merchantInfo.value?.contactName || account.value.nickName || '商家用户'
  }
  return account.value.nickName || '管理员'
})
const secondaryLine = computed(() => {
  if (isMerchantPortal.value) {
    return merchantInfo.value?.name || '商家账号'
  }
  return account.value.email || account.value.phone || '平台管理账号'
})
const avatarText = computed(() => (primaryName.value || 'U').trim().charAt(0).toUpperCase())
const editAvatarPreview = computed(() => selectedAvatarUrl.value || account.value.icon || '')
const editAvatarText = computed(() => (editForm.value.name || primaryName.value || 'U').trim().charAt(0).toUpperCase())

const statusText = computed(() => account.value.status === 0 ? '禁用' : '正常')
const statusBadgeClass = computed(() => account.value.status === 0 ? 'status-danger' : 'status-success')
const merchantStatusText = computed(() => merchantInfo.value?.statusLabel || merchantStatusLabel(merchantInfo.value?.status))
const merchantCertText = computed(() => merchantStatusLabel(merchantInfo.value?.status, true))

const detailItems = computed<DetailItem[]>(() => {
  if (isMerchantPortal.value) {
    return [
      { label: '联系人姓名', value: merchantInfo.value?.contactName || account.value.nickName || '-' },
      { label: '手机号', value: account.value.phone || merchantInfo.value?.contactPhone || '-' },
      { label: '商家名称', value: merchantInfo.value?.name || '-' },
      { label: '店铺状态', value: merchantStatusText.value, badge: true, badgeClass: merchantInfo.value?.status === 1 ? 'status-success' : 'status-warning' },
      { label: '认证状态', value: merchantCertText.value, badge: true, badgeClass: merchantInfo.value?.status === 1 ? 'status-success' : 'status-warning' },
      { label: '所属分类', value: merchantInfo.value?.typeName || '-' },
      { label: '最近登录时间', value: recentLoginTime.value },
      { label: '联系地址', value: merchantInfo.value?.address || '-' }
    ]
  }

  return [
    { label: '姓名', value: account.value.nickName || '-' },
    { label: '手机号', value: account.value.phone || '-' },
    { label: '角色', value: roleText.value, badge: true, badgeClass: 'status-success' },
    { label: '所属部门', value: account.value.occupation || '-' },
    { label: '账号状态', value: statusText.value, badge: true, badgeClass: statusBadgeClass.value },
    { label: '最近登录时间', value: recentLoginTime.value },
    { label: '创建时间', value: formatTime(account.value.createTime) },
    { label: '邮箱', value: account.value.email || '-' }
  ]
})

const recentLoginTime = computed(() => {
  const rawValue = (account.value as Record<string, unknown>).lastLoginTime
  return formatTime(typeof rawValue === 'string' ? rawValue : undefined) || '本次会话'
})

const passwordStrength = computed(() => {
  const value = passwordForm.value.newPassword
  let score = 0
  if (value.length >= 8) score += 1
  if (/[A-Za-z]/.test(value) && /\d/.test(value)) score += 1
  if (/[^A-Za-z0-9]/.test(value) || value.length >= 12) score += 1
  if (!value) return { level: 'empty', text: '未输入' }
  if (score <= 1) return { level: 'weak', text: '弱' }
  if (score === 2) return { level: 'medium', text: '中' }
  return { level: 'strong', text: '强' }
})

const merchantStatusLabel = (status?: number, certification = false) => {
  if (status === 1) return certification ? '已认证' : '正常'
  if (status === 0) return certification ? '待审核' : '待审核'
  if (status === 2) return certification ? '已冻结' : '冻结'
  if (status === 3) return certification ? '已关闭' : '关闭'
  return '未知'
}

const formatTime = (value?: string) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

const resetEditForm = () => {
  editForm.value = {
    name: isMerchantPortal.value
      ? merchantInfo.value?.contactName || account.value.nickName || ''
      : account.value.nickName || '',
    phone: account.value.phone || merchantInfo.value?.contactPhone || '',
    email: account.value.email || '',
    shopIntro: account.value.bio || '',
    address: merchantInfo.value?.address || '',
    businessHours: merchantInfo.value?.businessHours || ''
  }
  clearSelectedAvatar()
  saveError.value = ''
}

const loadProfile = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const user = await getUserInfo()
    account.value = user
    if (isMerchantPortal.value) {
      const snapshot = await getMerchantHome()
      merchantInfo.value = snapshot?.merchant || null
    } else {
      merchantInfo.value = null
    }
    resetEditForm()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '账户资料加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const openEditDrawer = () => {
  resetEditForm()
  drawerOpen.value = true
  void router.replace({ path: route.path, query: { ...route.query, panel: 'edit' } })
}

const closeEditDrawer = () => {
  if (saving.value) return
  drawerOpen.value = false
  saveError.value = ''
  clearSelectedAvatar()
  clearPanelQuery()
}

const openPasswordDialog = () => {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  passwordError.value = ''
  passwordDialogOpen.value = true
  void router.replace({ path: route.path, query: { ...route.query, panel: 'password' } })
}

const closePasswordDialog = () => {
  if (changingPassword.value) return
  passwordDialogOpen.value = false
  passwordError.value = ''
  clearPanelQuery()
}

const clearPanelQuery = () => {
  if (!route.query.panel) return
  const nextQuery: Record<string, string | string[]> = { ...route.query } as Record<string, string | string[]>
  delete nextQuery.panel
  void router.replace({ path: route.path, query: nextQuery })
}

const handleAvatarSelected = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    saveError.value = '请选择有效的图片文件'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    saveError.value = '头像文件不能超过 5MB'
    return
  }
  clearSelectedAvatar()
  selectedAvatarFile.value = file
  selectedAvatarUrl.value = URL.createObjectURL(file)
}

const clearSelectedAvatar = () => {
  if (selectedAvatarUrl.value) {
    URL.revokeObjectURL(selectedAvatarUrl.value)
  }
  selectedAvatarUrl.value = ''
  selectedAvatarFile.value = null
}

const validateProfileForm = () => {
  if (!editForm.value.name) {
    return isMerchantPortal.value ? '联系人姓名不能为空' : '姓名不能为空'
  }
  if (editForm.value.phone && !/^1[3-9]\d{9}$/.test(editForm.value.phone)) {
    return '手机号格式不正确'
  }
  if (editForm.value.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editForm.value.email)) {
    return '邮箱格式不正确'
  }
  if (isMerchantPortal.value && !editForm.value.address) {
    return '联系地址不能为空'
  }
  return ''
}

const saveProfile = async () => {
  const validationError = validateProfileForm()
  if (validationError) {
    saveError.value = validationError
    return
  }

  saving.value = true
  saveError.value = ''
  try {
    let icon = account.value.icon
    if (selectedAvatarFile.value) {
      const uploadResult = await uploadAvatar(selectedAvatarFile.value)
      icon = uploadResult.url
    }

    const updatedUser = await updateUserProfile({
      nickName: editForm.value.name,
      phone: editForm.value.phone,
      email: editForm.value.email || undefined,
      icon,
      intro: isMerchantPortal.value ? editForm.value.shopIntro : account.value.bio
    })
    account.value = updatedUser

    if (isMerchantPortal.value) {
      const updatedMerchant = await updateMerchantProfile({
        contactName: editForm.value.name,
        contactPhone: editForm.value.phone,
        address: editForm.value.address,
        businessHours: editForm.value.businessHours
      })
      merchantInfo.value = {
        ...(merchantInfo.value || updatedMerchant),
        ...updatedMerchant
      }
    }

    resetEditForm()
    drawerOpen.value = false
    clearPanelQuery()
    window.dispatchEvent(new Event('account-profile-updated'))
    await showAlert('资料已更新')
  } catch (error) {
    saveError.value = error instanceof Error ? error.message : '资料保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

const validatePasswordForm = () => {
  if (!passwordForm.value.oldPassword) return '请输入原密码'
  if (!passwordForm.value.newPassword) return '请输入新密码'
  if (passwordForm.value.newPassword.length < 8) return '新密码至少需要 8 位'
  if (!/[A-Za-z]/.test(passwordForm.value.newPassword) || !/\d/.test(passwordForm.value.newPassword)) {
    return '新密码需要同时包含字母和数字'
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) return '两次输入的新密码不一致'
  if (passwordForm.value.oldPassword === passwordForm.value.newPassword) return '新密码不能与原密码相同'
  return ''
}

const submitPassword = async () => {
  const validationError = validatePasswordForm()
  if (validationError) {
    passwordError.value = validationError
    return
  }

  changingPassword.value = true
  passwordError.value = ''
  try {
    await changePassword(passwordForm.value)
    passwordDialogOpen.value = false
    clearPanelQuery()
    await showAlert('密码已修改')
  } catch (error) {
    passwordError.value = error instanceof Error ? error.message : '修改密码失败，请稍后重试'
  } finally {
    changingPassword.value = false
  }
}

watch(() => route.query.panel, (panel) => {
  if (panel === 'edit') {
    resetEditForm()
    drawerOpen.value = true
    passwordDialogOpen.value = false
  } else if (panel === 'password') {
    drawerOpen.value = false
    passwordDialogOpen.value = true
  } else {
    drawerOpen.value = false
    passwordDialogOpen.value = false
  }
}, { immediate: true })

watch(isMerchantPortal, () => {
  void loadProfile()
})

onMounted(() => {
  void loadProfile()
})

onUnmounted(() => {
  clearSelectedAvatar()
})
</script>

<style scoped>
.account-center-page {
  min-height: 100%;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.account-card {
  padding: 18px;
  border: 1px solid var(--ops-border, #dbe3ee);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: var(--ops-shadow, 0 1px 2px rgba(15, 23, 42, 0.04));
}

.account-profile-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--ops-border-soft, #e7edf5);
}

.profile-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  overflow: hidden;
  border-radius: 50%;
  background: var(--ops-primary-soft, #eef5ff);
  color: var(--ops-primary, #1677ff);
  font-size: 24px;
  font-weight: 700;
}

.profile-avatar.small {
  width: 56px;
  height: 56px;
  font-size: 18px;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-summary {
  min-width: 0;
}

.profile-title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.profile-title-row h2 {
  margin: 0;
  color: var(--ops-text, #172033);
  font-size: 20px;
  font-weight: 700;
}

.profile-summary p {
  margin: 6px 0 0;
  color: var(--ops-text-muted, #667085);
  font-size: 13px;
}

.account-detail-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 0;
  margin-top: 4px;
}

.detail-item {
  padding: 16px 18px;
  border-bottom: 1px solid var(--ops-border-soft, #e7edf5);
}

.detail-item dt {
  margin-bottom: 8px;
  color: var(--ops-text-muted, #667085);
  font-size: 12px;
  font-weight: 700;
}

.detail-item dd {
  min-height: 24px;
  margin: 0;
  color: var(--ops-text, #172033);
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.account-state {
  padding: 30px;
  border: 1px solid var(--ops-border, #dbe3ee);
  border-radius: 8px;
  background: #ffffff;
  text-align: center;
}

.account-state.error {
  border-color: #fecdd3;
  background: #fff1f3;
}

.account-state h3 {
  margin: 0;
  color: var(--ops-text, #172033);
  font-size: 16px;
}

.account-state p {
  margin: 8px 0 18px;
  color: var(--ops-text-muted, #667085);
  font-size: 14px;
}

.account-skeleton {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 16px;
}

.skeleton-avatar,
.skeleton-lines span,
.skeleton-grid span {
  display: block;
  border-radius: 6px;
  background: linear-gradient(90deg, #eef2f7 0%, #f7f9fc 50%, #eef2f7 100%);
  background-size: 220% 100%;
  animation: skeleton-loading 1.3s ease-in-out infinite;
}

.skeleton-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
}

.skeleton-lines {
  display: grid;
  align-content: center;
  gap: 10px;
}

.skeleton-lines span {
  width: 260px;
  max-width: 80%;
  height: 14px;
}

.skeleton-lines span:first-child {
  width: 180px;
  height: 20px;
}

.skeleton-grid {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-top: 16px;
}

.skeleton-grid span {
  height: 58px;
}

@keyframes skeleton-loading {
  from {
    background-position: 100% 0;
  }
  to {
    background-position: -100% 0;
  }
}

.account-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 1500;
  display: flex;
  justify-content: flex-end;
  background: rgba(15, 23, 42, 0.38);
}

.account-drawer {
  width: min(560px, 100%);
  height: 100%;
  border-radius: 0;
  box-shadow: -14px 0 34px rgba(15, 23, 42, 0.14);
}

.account-drawer-form {
  display: flex;
  height: 100%;
  flex-direction: column;
}

.drawer-header,
.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--ops-border-soft, #e7edf5);
}

.drawer-header h2,
.modal-header h2 {
  margin: 0;
  color: var(--ops-text, #172033);
  font-size: 18px;
  font-weight: 700;
}

.drawer-header p {
  margin: 6px 0 0;
  color: var(--ops-text-muted, #667085);
  font-size: 13px;
  line-height: 1.5;
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 20px 90px;
}

.drawer-footer {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--ops-border-soft, #e7edf5);
  background: #ffffff;
}

.icon-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--ops-border, #dbe3ee);
  border-radius: 6px;
  background: #ffffff;
  color: var(--ops-text-muted, #667085);
  cursor: pointer;
}

.icon-close svg {
  width: 16px;
  height: 16px;
}

.icon-close:hover:not(:disabled) {
  border-color: var(--ops-primary, #1677ff);
  color: var(--ops-primary, #1677ff);
}

.form-section {
  padding: 0 0 18px;
}

.form-section + .form-section {
  padding-top: 18px;
  border-top: 1px solid var(--ops-border-soft, #e7edf5);
}

.form-section h3 {
  margin: 0 0 14px;
  color: var(--ops-text, #172033);
  font-size: 15px;
  font-weight: 700;
}

.avatar-edit-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.avatar-edit-row p {
  margin: 8px 0 0;
  color: var(--ops-text-muted, #667085);
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-grid.one-column {
  grid-template-columns: 1fr;
}

.form-field {
  display: grid;
  gap: 7px;
}

.form-field span {
  color: var(--ops-text, #172033);
  font-size: 13px;
  font-weight: 700;
}

.form-field input,
.form-field textarea {
  width: 100%;
  padding: 8px 10px;
  box-sizing: border-box;
}

.form-field textarea {
  resize: vertical;
  line-height: 1.6;
}

.form-field em {
  color: var(--ops-text-weak, #98a2b3);
  font-size: 12px;
  font-style: normal;
  text-align: right;
}

.form-error {
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid #fecdd3;
  border-radius: 6px;
  background: #fff1f3;
  color: #c01048;
  font-size: 13px;
}

.status-warning {
  background: #fffbeb;
  color: #b45309;
}

.hidden-input {
  display: none;
}

.account-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1600;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.password-modal {
  width: min(440px, 100%);
}

.password-form {
  display: grid;
  gap: 14px;
  padding: 18px 20px 20px;
}

.password-strength {
  display: grid;
  gap: 6px;
}

.strength-bars {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.strength-bars span {
  height: 6px;
  border-radius: 6px;
  background: #e7edf5;
}

.password-strength.weak .strength-bars span:first-child {
  background: #dc2626;
}

.password-strength.medium .strength-bars span:nth-child(-n + 2) {
  background: #d97706;
}

.password-strength.strong .strength-bars span {
  background: #16a34a;
}

.password-strength p {
  margin: 0;
  color: var(--ops-text-muted, #667085);
  font-size: 12px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

@media (max-width: 1024px) {
  .account-detail-grid,
  .skeleton-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .account-profile-head,
  .avatar-edit-row {
    align-items: flex-start;
  }

  .account-detail-grid,
  .form-grid,
  .skeleton-grid {
    grid-template-columns: 1fr;
  }

  .page-header {
    align-items: stretch;
  }

  .page-actions {
    justify-content: flex-start;
  }
}
</style>
