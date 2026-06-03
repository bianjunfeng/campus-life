<!-- 导航栏组件 -->
<template>
  <nav class="nav-bar">
    <router-link to="/home" class="nav-item">
      <div class="nav-icon">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
      </div>
      <span>首页</span>
    </router-link>
    
    <div class="nav-item" @click="handleWelfareClick">
      <div class="nav-icon">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="23 4 23 10 17 10"></polyline>
          <polyline points="1 20 1 14 7 14"></polyline>
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
        </svg>
      </div>
      <span>福利</span>
    </div>
    
    <div class="nav-item post-button" @click="handlePostClick">
      <div class="nav-icon post-icon">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
      </div>
      <span>发布</span>
    </div>
    
    <div class="nav-item" @click="handleMessageClick">
      <div class="nav-icon">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
        </svg>
      </div>
      <span>消息</span>
    </div>
    
    <div class="nav-item" @click="handleProfileClick">
      <div class="nav-icon">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
          <circle cx="12" cy="7" r="4"></circle>
        </svg>
      </div>
      <span>我的</span>
    </div>
  </nav>
  
  <!-- 发布选项弹出层 -->
  <div v-if="showPostOptions" class="post-options-overlay" @click="showPostOptions = false">
    <div class="post-options" @click.stop>
      <div class="option-item" @click="navigateToPost('album')">
        <div class="option-icon album-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
            <circle cx="8.5" cy="8.5" r="1.5"></circle>
            <polyline points="21 15 16 10 5 21"></polyline>
          </svg>
        </div>
        <span>从相册选择</span>
      </div>
      
      <div class="option-item" @click="navigateToPost('camera')">
        <div class="option-icon camera-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
            <circle cx="12" cy="13" r="4"></circle>
          </svg>
        </div>
        <span>相机</span>
      </div>
      
      <div class="option-item" @click="navigateToPost('text')">
        <div class="option-icon text-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
            <polyline points="10 9 9 9 8 9"></polyline>
          </svg>
        </div>
        <span>写文字</span>
      </div>
      
      <div class="cancel-button" @click="showPostOptions = false">
        <span>取消</span>
      </div>
    </div>
  </div>
  
  <!-- 登录提示对话框 - 禁用自动显示，仅在用户点击需要登录的功能时显示 -->
  <LoginRequiredDialog ref="loginDialog" :auto-show="false" />
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import LoginRequiredDialog from './LoginRequiredDialog.vue';
import { checkLoginStatus } from '../shared/router/auth';
import { notify } from '@/utils/notify'

// 导航栏组件

// 路由实例
const router = useRouter();

// 是否显示发布选项
const showPostOptions = ref(false);

// 登录提示对话框引用
const loginDialog = ref();

// 点击发布按钮
const handlePostClick = () => {
  // 检查登录状态
  if (!checkLoginStatus()) {
    // 未登录，显示登录提示
    loginDialog.value?.show();
  } else {
    // 已登录，显示发布选项
    showPostOptions.value = true;
  }
};

// 点击福利按钮
const handleWelfareClick = () => {
  // 检查登录状态
  if (!checkLoginStatus()) {
    // 未登录，显示登录提示
    loginDialog.value?.show();
  } else {
    // 已登录，导航到福利页面
    router.push('/welfare');
  }
};

// 点击消息按钮
const handleMessageClick = () => {
  // 检查登录状态
  if (!checkLoginStatus()) {
    // 未登录，显示登录提示
    loginDialog.value?.show();
  } else {
    // 已登录，导航到消息页面
    router.push('/message');
  }
};

// 点击我的按钮
const handleProfileClick = () => {
  // 检查登录状态
  if (!checkLoginStatus()) {
    // 未登录，显示登录提示
    loginDialog.value?.show();
  } else {
    // 已登录，导航到个人中心页面
    router.push('/me');
  }
};

// 导航到发布页面
const navigateToPost = (type: string) => {
  showPostOptions.value = false;
  
  if (type === 'text') {
    // 导航到写文字页面
    router.push('/posts/new');
  } else if (type === 'album') {
    // 从相册选择，暂时只实现写文字功能
    notify('从相册选择功能即将上线');
  } else if (type === 'camera') {
    // 相机功能，暂时只实现写文字功能
    notify('相机功能即将上线');
  }
};
</script>

<style scoped>
.nav-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 56px;
  background-color: #ffffff;
  border-top: 1px solid #e0e0e0;
  z-index: 1000;
  padding-bottom: env(safe-area-inset-bottom);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: #666666;
  text-decoration: none;
  font-size: 12px;
  transition: color 0.3s;
}

.nav-item.router-link-active {
  color: #1677ff;
}

.nav-icon {
  width: 24px;
  height: 24px;
  margin-bottom: 4px;
}

.nav-icon svg {
  width: 100%;
  height: 100%;
}

/* 发布按钮样式 */
.post-button {
  cursor: pointer;
}

.post-button:hover {
  color: #1677ff;
}

.post-icon {
  background-color: #1677ff;
  color: white;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.3);
}

.post-icon svg {
  width: 20px;
  height: 20px;
}

/* 发布选项弹出层样式 */
.post-options-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.post-options {
  width: 100%;
  background-color: white;
  border-top-left-radius: 16px;
  border-top-right-radius: 16px;
  padding: 20px;
  padding-bottom: calc(20px + env(safe-area-inset-bottom));
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.option-item {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.option-item:last-child {
  border-bottom: none;
}

.option-item:active {
  background-color: #f5f5f5;
}

.option-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}

.album-icon {
  background-color: #f0f9ff;
  color: #1677ff;
}

.camera-icon {
  background-color: #fff7e6;
  color: #fa8c16;
}

.text-icon {
  background-color: #f6ffed;
  color: #52c41a;
}

.option-icon svg {
  width: 24px;
  height: 24px;
}

.option-item span {
  font-size: 16px;
  color: #333;
}

.cancel-button {
  margin-top: 16px;
  padding: 16px 0;
  text-align: center;
  background-color: #f5f5f5;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  color: #333;
}

.cancel-button:active {
  background-color: #e0e0e0;
}
</style>
