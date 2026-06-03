<template>
  <div class="oauth-callback">
    <div class="loading-container">
      <div class="spinner"></div>
      <p>{{ message }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { wechatCallback, qqCallback } from '../../api/auth'

const route = useRoute()
const router = useRouter()

const message = ref('正在登录中...')

onMounted(async () => {
  const code = route.query.code as string
  const state = route.query.state as string
  const redirectUri = `${window.location.origin}${route.path}`
  
  if (!code) {
    message.value = '授权失败，未获取到授权码'
    setTimeout(() => {
      router.push('/login')
    }, 2000)
    return
  }
  
  if (!state) {
    message.value = '授权失败，状态参数缺失或已过期'
    setTimeout(() => {
      router.push('/login')
    }, 2000)
    return
  }
  
  try {
    let result
    if (route.path.includes('/wechat')) {
      // 微信登录回调
      result = await wechatCallback(code, state, redirectUri)
    } else if (route.path.includes('/qq')) {
      // QQ登录回调
      result = await qqCallback(code, state, redirectUri)
    } else {
      throw new Error('未知的OAuth类型')
    }
    
    if (result && (result.token || result.accessToken)) {
      message.value = '登录成功！'
      
      // 检查是否有重定向页面
      const redirectPath = localStorage.getItem('redirectAfterLogin')
      if (redirectPath && redirectPath !== '/') {
        localStorage.removeItem('redirectAfterLogin')
        setTimeout(() => {
          router.push(redirectPath)
        }, 1000)
      } else {
        setTimeout(() => {
          router.push('/home')
        }, 1000)
      }
    }
  } catch (error: any) {
    console.error('OAuth登录失败:', error)
    message.value = error.message || '登录失败，请重试'
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  }
})
</script>

<style scoped>
.oauth-callback {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
}

.loading-container {
  text-align: center;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1677ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-container p {
  font-size: 16px;
  color: #666;
  margin: 0;
}
</style>


