<!-- 登录提示弹窗组件 -->
<template>
  <div v-if="showPrompt" class="login-prompt-overlay" @click.self="handleOverlayClick">
    <div class="login-prompt">
      <div class="prompt-icon">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
          <circle cx="12" cy="7" r="4"></circle>
        </svg>
      </div>
      <h3 class="prompt-title">{{ promptTitle }}</h3>
      <p class="prompt-message">{{ promptMessage }}</p>
      <div class="prompt-actions">
        <button class="cancel-btn" @click="handleCancel">{{ cancelText }}</button>
        <button class="login-btn" @click="handleLogin">{{ loginText }}</button>
      </div>
      <button v-if="allowClose" class="close-btn" @click="handleClose">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { checkLoginStatus } from '../shared/router/auth'

// 组件属性定义
interface Props {
  autoShow?: boolean
  allowClose?: boolean
  promptTitle?: string
  promptMessage?: string
  loginText?: string
  cancelText?: string
}

const props = withDefaults(defineProps<Props>(), {
  autoShow: true,
  allowClose: true,
  promptTitle: '需要登录',
  promptMessage: '登录后可以使用更多功能，享受完整体验',
  loginText: '立即登录',
  cancelText: '稍后再说'
})

// 组件事件定义
const emit = defineEmits(['login', 'cancel', 'close'])

const router = useRouter()
const showPrompt = ref(false)

// 自动显示提示
onMounted(() => {
  if (props.autoShow && !checkLoginStatus()) {
    // 每次未登录访问时都显示提示
    showPrompt.value = true
  }
})

// 显示提示方法
defineExpose({
  show: () => {
    showPrompt.value = true
  },
  hide: () => {
    showPrompt.value = false
  }
})

// 处理登录按钮点击
const handleLogin = () => {
  emit('login')
  showPrompt.value = false
  router.push('/login')
}

// 处理取消按钮点击
const handleCancel = () => {
  emit('cancel')
  showPrompt.value = false
}

// 处理关闭按钮点击
const handleClose = () => {
  emit('close')
  showPrompt.value = false
}

// 处理遮罩层点击（如果允许点击遮罩层关闭）
const handleOverlayClick = () => {
  if (props.allowClose) {
    handleClose()
  }
}
</script>

<style scoped>
.login-prompt-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.login-prompt {
  position: relative;
  background-color: #ffffff;
  border-radius: 12px;
  padding: 24px;
  width: 90%;
  max-width: 360px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  text-align: center;
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.prompt-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background-color: #e6f7ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.prompt-icon svg {
  width: 32px;
  height: 32px;
  color: #1890ff;
}

.prompt-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.prompt-message {
  font-size: 14px;
  color: #666;
  margin-bottom: 24px;
  line-height: 1.5;
}

.prompt-actions {
  display: flex;
  gap: 12px;
}

.cancel-btn,
.login-btn {
  flex: 1;
  padding: 10px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #666;
  border: none;
}

.cancel-btn:hover {
  background-color: #e8e8e8;
}

.login-btn {
  background-color: #1890ff;
  color: white;
  border: none;
}

.login-btn:hover {
  background-color: #40a9ff;
}

.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 24px;
  height: 24px;
  background: none;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #999;
  transition: all 0.3s;
}

.close-btn:hover {
  background-color: #f5f5f5;
  color: #666;
}

.close-btn svg {
  width: 16px;
  height: 16px;
}

/* 暗色模式样式 */
:global(.dark-mode) .login-prompt-overlay {
  background-color: rgba(0, 0, 0, 0.7);
}

:global(.dark-mode) .login-prompt {
  background-color: #1f1f1f;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

:global(.dark-mode) .prompt-title {
  color: #ffffff;
}

:global(.dark-mode) .prompt-message {
  color: #e0e0e0;
}

:global(.dark-mode) .cancel-btn {
  background-color: #2d2d2d;
  color: #ffffff;
}

:global(.dark-mode) .cancel-btn:hover {
  background-color: #3a3a3a;
}

:global(.dark-mode) .login-btn {
  background-color: #40a9ff;
}

:global(.dark-mode) .login-btn:hover {
  background-color: #69c0ff;
}

:global(.dark-mode) .close-btn {
  color: #666;
}

:global(.dark-mode) .close-btn:hover {
  background-color: #333;
  color: #ffffff;
}

:global(.dark-mode) .prompt-icon {
  background-color: rgba(24, 144, 255, 0.15);
}

:global(.dark-mode) .prompt-icon svg {
  color: #40a9ff;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-prompt {
    width: 95%;
    padding: 20px;
    margin: 0 20px;
    max-width: 100%;
  }
  
  .prompt-icon {
    width: 56px;
    height: 56px;
  }
  
  .prompt-icon svg {
    width: 28px;
    height: 28px;
  }
  
  .prompt-title {
    font-size: 16px;
  }
  
  .prompt-message {
    font-size: 13px;
  }
  
  .prompt-actions {
    flex-direction: column-reverse;
  }
  
  .cancel-btn,
  .login-btn {
    width: 100%;
    padding: 10px;
  }
}
</style>
