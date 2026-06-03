<template>
  <div ref="menuRef" class="account-user-menu">
    <button class="account-user-trigger" type="button" @click="toggleMenu">
      <span class="account-avatar">
        <img v-if="avatarUrl" :src="avatarUrl" alt="用户头像" />
        <span v-else>{{ avatarText }}</span>
      </span>
      <span class="account-identity">
        <strong>{{ displayName }}</strong>
        <span>{{ roleLabel }}</span>
      </span>
      <span class="account-role-tag">{{ roleTag }}</span>
      <svg class="account-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="m6 9 6 6 6-6" />
      </svg>
    </button>

    <div v-if="menuOpen" class="account-menu-panel">
      <button type="button" class="account-menu-item" @click="openProfile">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
        <span>个人信息</span>
      </button>
      <button type="button" class="account-menu-item" @click="openEdit">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 20h9" />
          <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
        </svg>
        <span>修改资料</span>
      </button>
      <button type="button" class="account-menu-item" @click="openPassword">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="11" width="18" height="10" rx="2" />
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </svg>
        <span>修改密码</span>
      </button>
      <button type="button" class="account-menu-item danger" @click="showLogoutConfirm = true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
          <path d="m16 17 5-5-5-5" />
          <path d="M21 12H9" />
        </svg>
        <span>退出登录</span>
      </button>
    </div>

    <div v-if="showLogoutConfirm" class="account-dialog-mask" @click.self="showLogoutConfirm = false">
      <section class="account-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="logout-title">
        <h3 id="logout-title">确认退出登录</h3>
        <p>退出后所有设备都需要重新登录当前账号。</p>
        <div class="account-dialog-actions">
          <button class="account-dialog-btn secondary" type="button" :disabled="loggingOut" @click="showLogoutConfirm = false">
            取消
          </button>
          <button class="account-dialog-btn danger" type="button" :disabled="loggingOut" @click="confirmLogout">
            {{ loggingOut ? '退出中...' : '确认退出' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserInfo, logoutAllApi, type UserInfo } from '../../api/auth'
import { clearAuthState, getStoredUserInfoObject } from '../../shared/utils/authStorage'

const props = defineProps<{
  portal: 'admin' | 'merchant'
}>()

const route = useRoute()
const router = useRouter()
const menuRef = ref<HTMLElement | null>(null)
const menuOpen = ref(false)
const showLogoutConfirm = ref(false)
const loggingOut = ref(false)
const userInfo = ref<Partial<UserInfo> | null>(getStoredUserInfoObject<UserInfo>())

const profilePath = computed(() => props.portal === 'admin' ? '/admin/profile/info' : '/merchant/profile/info')
const loginPath = computed(() => '/login')

const normalizeRole = (role?: string) => String(role || '').replace(/^ROLE_/, '').toUpperCase()

const roleLabel = computed(() => {
  const roleMap: Record<string, string> = {
    ADMIN: '平台管理员',
    MERCHANT: '商家账号',
    STUDENT: '学生用户',
    GUEST: '访客'
  }
  return roleMap[normalizeRole(userInfo.value?.role)] || (props.portal === 'admin' ? '管理员' : '商家')
})

const roleTag = computed(() => {
  const tagMap: Record<string, string> = {
    ADMIN: '管理员',
    MERCHANT: '商家',
    STUDENT: '学生',
    GUEST: '访客'
  }
  return tagMap[normalizeRole(userInfo.value?.role)] || (props.portal === 'admin' ? '管理员' : '商家')
})
const displayName = computed(() => userInfo.value?.nickName || userInfo.value?.phone || (props.portal === 'admin' ? '管理员' : '商家用户'))
const avatarUrl = computed(() => userInfo.value?.icon || '')
const avatarText = computed(() => (displayName.value || 'U').trim().charAt(0).toUpperCase())

const loadUser = async () => {
  try {
    userInfo.value = await getUserInfo()
  } catch {
    userInfo.value = getStoredUserInfoObject<UserInfo>() || userInfo.value
  }
}

const closeMenu = () => {
  menuOpen.value = false
}

const toggleMenu = () => {
  menuOpen.value = !menuOpen.value
}

const openProfile = () => {
  closeMenu()
  void router.push(profilePath.value)
}

const openEdit = () => {
  closeMenu()
  void router.push({ path: profilePath.value, query: { panel: 'edit' } })
}

const openPassword = () => {
  closeMenu()
  void router.push({ path: profilePath.value, query: { panel: 'password' } })
}

const confirmLogout = async () => {
  loggingOut.value = true
  try {
    await logoutAllApi()
  } catch {
    // 本地登录态仍需清除，避免接口失败阻断退出。
  } finally {
    clearAuthState()
    localStorage.removeItem('redirectAfterLogin')
    localStorage.removeItem('requireLogin')
    loggingOut.value = false
    showLogoutConfirm.value = false
    closeMenu()
    void router.replace(loginPath.value)
  }
}

const handleDocumentClick = (event: MouseEvent) => {
  const target = event.target as Node
  if (menuRef.value && !menuRef.value.contains(target)) {
    closeMenu()
  }
}

watch(() => route.fullPath, () => {
  closeMenu()
  if (route.path === profilePath.value) {
    void loadUser()
  }
})

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  window.addEventListener('account-profile-updated', loadUser)
  void loadUser()
})

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick)
  window.removeEventListener('account-profile-updated', loadUser)
})
</script>

<style scoped>
.account-user-menu {
  position: relative;
  display: inline-flex;
}

.account-user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  padding: 4px 8px 4px 6px;
  border: 1px solid var(--ops-border, #dbe3ee);
  border-radius: 8px;
  background: #ffffff;
  color: var(--ops-text, #172033);
  cursor: pointer;
}

.account-user-trigger:hover {
  border-color: var(--ops-primary, #1677ff);
}

.account-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  overflow: hidden;
  border-radius: 50%;
  background: var(--ops-primary-soft, #eef5ff);
  color: var(--ops-primary, #1677ff);
  font-size: 13px;
  font-weight: 700;
}

.account-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.account-identity {
  display: grid;
  gap: 1px;
  min-width: 78px;
  text-align: left;
}

.account-identity strong {
  overflow: hidden;
  color: var(--ops-text, #172033);
  font-size: 13px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-identity span {
  color: var(--ops-text-muted, #667085);
  font-size: 12px;
  line-height: 1.2;
}

.account-role-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 7px;
  border-radius: 6px;
  background: var(--ops-primary-soft, #eef5ff);
  color: var(--ops-primary, #1677ff);
  font-size: 12px;
  font-weight: 700;
}

:global(.merchant-layout) .account-role-tag {
  background: var(--ops-accent-soft, #e6fffb);
  color: var(--ops-accent, #0f766e);
}

.account-chevron {
  width: 14px;
  height: 14px;
  color: var(--ops-text-muted, #667085);
}

.account-menu-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 1200;
  width: 176px;
  padding: 6px;
  border: 1px solid var(--ops-border, #dbe3ee);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
}

.account-menu-item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  min-height: 36px;
  padding: 0 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--ops-text, #172033);
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}

.account-menu-item:hover {
  background: var(--ops-surface-muted, #f8fafc);
  color: var(--ops-primary, #1677ff);
}

.account-menu-item.danger {
  color: var(--ops-danger, #dc2626);
}

.account-menu-item svg {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
}

.account-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.42);
}

.account-confirm-dialog {
  width: min(420px, 100%);
  padding: 20px;
  border: 1px solid var(--ops-border, #dbe3ee);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.16);
}

.account-confirm-dialog h3 {
  margin: 0;
  color: var(--ops-text, #172033);
  font-size: 18px;
  font-weight: 700;
}

.account-confirm-dialog p {
  margin: 10px 0 0;
  color: var(--ops-text-muted, #667085);
  font-size: 14px;
  line-height: 1.6;
}

.account-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.account-dialog-btn {
  min-width: 92px;
  height: 34px;
  border: 1px solid transparent;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.account-dialog-btn.secondary {
  border-color: var(--ops-border, #dbe3ee);
  background: #ffffff;
  color: var(--ops-text, #172033);
}

.account-dialog-btn.danger {
  border-color: var(--ops-danger, #dc2626);
  background: var(--ops-danger, #dc2626);
  color: #ffffff;
}

.account-dialog-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

@media (max-width: 720px) {
  .account-identity,
  .account-role-tag {
    display: none;
  }
}
</style>
