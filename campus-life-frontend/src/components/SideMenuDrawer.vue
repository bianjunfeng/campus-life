<!-- 侧边栏抽屉组件 -->
<template>
  <div>
    <!-- 遮罩层 -->
    <div v-if="visible" class="sidebar-overlay" @click="closeSidebar"></div>
    <transition name="sidebar-slide">
      <div v-if="visible" class="sidebar">
    <!-- 侧边栏头部 -->
    <div class="sidebar-header">
      <div class="sidebar-title">菜单</div>
    </div>
    
    <!-- 侧边栏菜单项 -->
    <div class="sidebar-menu">
      <div v-if="isLoggedIn">
        <!-- 添加好友 -->
        <div class="menu-item">
          <router-link to="/add-friend" class="menu-link" @click="handleMenuItemClick">
            <svg class="menu-icon" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 11h-4v4h-2v-4H7v-2h4V7h2v4h4v2z"/>
            </svg>
            <span class="menu-text">添加好友</span>
          </router-link>
        </div>
        
        <!-- 智能体助手 -->
        <div class="menu-item">
          <router-link to="/agent" class="menu-link" @click="handleMenuItemClick">
            <svg class="menu-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
            </svg>
            <span class="menu-text">智能体助手</span>
          </router-link>
        </div>
        
        <!-- 我的草稿 -->
        <div class="menu-item">
          <router-link to="/drafts" class="menu-link" @click="handleMenuItemClick">
            <svg class="menu-icon" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zM17 12h-4v4h-2v-4H7v-2h4V7h2v4h4v2z"/>
            </svg>
            <span class="menu-text">我的草稿</span>
          </router-link>
        </div>
        
        <!-- 浏览记录 -->
        <div class="menu-item">
          <router-link to="/history" class="menu-link" @click="handleMenuItemClick">
            <svg class="menu-icon" viewBox="0 0 24 24" fill="currentColor">
              <path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/>
            </svg>
            <span class="menu-text">浏览记录</span>
          </router-link>
        </div>
        
        <!-- 订单 -->
        <div class="menu-item">
          <router-link to="/me/orders" class="menu-link" @click="handleMenuItemClick">
            <svg class="menu-icon" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm-2 14l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/>
            </svg>
            <span class="menu-text">订单</span>
          </router-link>
        </div>
        
        <!-- 钱包 -->
        <div class="menu-item">
          <router-link to="/wallet" class="menu-link" @click="handleMenuItemClick">
            <svg class="menu-icon" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"/>
            </svg>
            <span class="menu-text">钱包</span>
          </router-link>
        </div>
      </div>
      <div v-else class="login-prompt">
        <span class="login-prompt-text">请先登录</span>
        <router-link to="/login" class="login-btn" @click="handleMenuItemClick">
          <span>立即登录</span>
        </router-link>
      </div>
    </div>
    
    <!-- 侧边栏底部按钮 -->
    <div class="sidebar-footer" v-if="isLoggedIn">
      <!-- 扫一扫 -->
      <div class="footer-item">
        <button class="footer-btn" @click="handleScan">
          <svg class="footer-icon" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19.75 19.75l-5.5-5.5a.75.75 0 0 1 1.06-1.06l5.5 5.5a.75.75 0 0 1-.05 1.06 10.07 10.07 0 0 1-1.06-.05zm-1.06-14.44l-5.5 5.5a.75.75 0 0 1-1.06-1.06l5.5-5.5a.75.75 0 1 1 1.06 1.06zM4.25 4.25l5.5 5.5a.75.75 0 0 1-1.06 1.06l-5.5-5.5a.75.75 0 1 1 1.06-1.06zM5.31 19.75a10.07 10.07 0 0 1-.05-1.06l5.5-5.5a.75.75 0 0 1 1.06 1.06l-5.5 5.5a.75.75 0 0 1-1.06-.05zM12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0-2a2 2 0 1 1 0-4 2 2 0 0 1 0 4z"/>
          </svg>
          <span class="footer-text">扫一扫</span>
        </button>
      </div>
      
      <!-- 帮助与客服 -->
      <div class="footer-item">
        <button class="footer-btn" @click="handleHelp">
          <svg class="footer-icon" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zM12 12c1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3 1.34 3 3 3zm0-5c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1z"/>
          </svg>
          <span class="footer-text">帮助与客服</span>
        </button>
      </div>
      
      <!-- 设置 -->
      <div class="footer-item">
        <button class="footer-btn" @click="handleSettings">
          <svg class="footer-icon" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c-.24 0-.44-.17-.47-.41l-.36-2.54c-.59-.24-1.13-.56-1.62-.94l-2.39.96c-.22-.08-.47 0-.59.22l-1.92-3.32c-.12.22-.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/>
          </svg>
          <span class="footer-text">设置</span>
        </button>
      </div>
    </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { checkLoginStatus } from '../shared/router/auth';
import { notify } from '@/utils/notify'

export default {
  name: 'SideMenuDrawer',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:visible'],
  computed: {
    // 检查用户登录状态
    isLoggedIn() {
      return checkLoginStatus();
    }
  },
  methods: {
    // 关闭侧边栏的方法
    closeSidebar() {
      this.$emit('update:visible', false);
    },
    
    // 菜单项点击处理 - 点击后自动关闭侧边栏
    handleMenuItemClick() {
      // 短暂延迟后关闭侧边栏，确保路由跳转正常执行
      setTimeout(() => {
        this.closeSidebar();
      }, 100);
    },
    
    handleScan() {
      // 扫一扫功能占位符
      notify('扫一扫功能开发中');
      // 点击后关闭侧边栏
      this.closeSidebar();
    },
    
    handleHelp() {
      // 帮助与客服功能占位符
      this.$router.push('/help');
      // 点击后关闭侧边栏
      this.closeSidebar();
    },
    
    handleSettings() {
      // 设置功能跳转到通用设置页面
      this.$router.push('/general-settings');
      // 点击后关闭侧边栏
      this.closeSidebar();
    }
  }
}
</script>

<style scoped>
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  width: 75vw; /* 占左边的四分之三 */
  max-width: 400px;
  height: 100vh;
  background-color: #ffffff;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  z-index: 1100; /* 提高优先级，确保高于底部栏 */
  display: flex;
  flex-direction: column;
}

/* 侧边栏滑动动画 */
.sidebar-slide-enter-active,
.sidebar-slide-leave-active {
  transition: transform 0.3s ease;
}

.sidebar-slide-enter-from {
  transform: translateX(-100%);
}

.sidebar-slide-leave-to {
  transform: translateX(-100%);
}

/* 遮罩层 */
.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 1099; /* 遮罩层z-index略低于侧边栏但高于底部栏 */
}

.sidebar-slide-enter-active ~ .sidebar-overlay,
.sidebar-slide-leave-active ~ .sidebar-overlay {
  transition: opacity 0.3s ease;
}

.sidebar-slide-enter-from ~ .sidebar-overlay {
  opacity: 0;
}

.sidebar-slide-leave-to ~ .sidebar-overlay {
  opacity: 0;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.sidebar-menu {
  flex: 1;
  padding: 10px 0;
  overflow-y: auto;
}

.menu-item {
  margin: 4px 0;
}

.menu-link {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  color: #666;
  text-decoration: none;
  transition: all 0.3s ease;
}

.menu-link:hover {
  background-color: #f5f5f5;
  color: #1890ff;
}

.menu-icon {
  width: 20px;
  height: 20px;
  margin-right: 12px;
}

.menu-text {
  font-size: 14px;
}

.login-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.login-prompt-text {
  display: block;
  margin-bottom: 15px;
  font-size: 14px;
  color: #666;
}

.login-btn {
  display: inline-block;
  padding: 8px 20px;
  background-color: #4CAF50;
  color: white;
  border-radius: 20px;
  text-decoration: none;
  font-size: 14px;
  transition: background-color 0.3s;
}

.login-btn:hover {
  background-color: #45a049;
}

.sidebar-footer {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 20px 15px;
  border-top: 1px solid #e0e0e0;
  background-color: #ffffff;
}

.footer-item {
  margin-bottom: 12px;
}

.footer-item:last-child {
  margin-bottom: 0;
}

.footer-btn {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 10px 16px;
  background: none;
  border: none;
  border-radius: 4px;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.footer-btn:hover {
  background-color: #f5f5f5;
  color: #1890ff;
}

.footer-icon {
  width: 18px;
  height: 18px;
  margin-right: 10px;
}

.footer-text {
  font-size: 14px;
}

/* 暗色模式样式 */
:global(.dark-mode) .sidebar {
  background-color: #1f1f1f;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.3);
  z-index: 1100; /* 保持z-index优先级 */
}

:global(.dark-mode) .sidebar-header,
:global(.dark-mode) .sidebar-footer {
  border-color: #333;
}

:global(.dark-mode) .sidebar-title {
  color: #ffffff;
}

:global(.dark-mode) .menu-link {
  color: #aaa;
}

:global(.dark-mode) .menu-link:hover {
  background-color: #333;
  color: #1890ff;
}

:global(.dark-mode) .footer-btn {
  color: #aaa;
}

:global(.dark-mode) .footer-btn:hover {
  background-color: #333;
  color: #1890ff;
}

:global(.dark-mode) .sidebar-overlay {
  background-color: rgba(0, 0, 0, 0.7);
  z-index: 1099; /* 保持遮罩层z-index */
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    width: 200px;
  }
  
  .sidebar-header,
  .sidebar-menu,
  .sidebar-footer {
    padding: 15px;
  }
  
  :global(.dark-mode) .sidebar {
    width: 200px;
  }
}
</style>
