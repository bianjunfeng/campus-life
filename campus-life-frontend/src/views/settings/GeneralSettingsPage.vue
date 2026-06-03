<template>
  <div class="settings-container">
    <header class="page-header">
      <button class="back-button" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="m15 18-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <h1 class="page-title">通用设置</h1>
      <div class="header-right"></div>
    </header>

    <div class="settings-content">
      <!-- 通用设置组 -->
      <div class="settings-group">
        <!-- 深色模式 -->
        <div class="setting-item">
          <div class="setting-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9z"/>
            </svg>
          </div>
          <span class="setting-title">深色模式</span>
          <div class="setting-right">
            <ToggleSwitch v-model="darkMode" />
          </div>
        </div>

        <!-- 清理缓存 -->
        <div class="setting-item" @click="clearCache">
          <div class="setting-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
            </svg>
          </div>
          <span class="setting-title">清理缓存</span>
          <div class="setting-right">
            <span class="setting-value">{{ cacheSize }}</span>
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M3.5 2L8.5 6L3.5 10" stroke="#999" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>

        <!-- 字体大小 -->
        <div class="setting-item" @click="navigateTo('/font-size')">
          <div class="setting-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M9 4v3h5v12h3V7h5V4H9zm-6 8h3v7h3v-7h3V9H3v3z"/>
            </svg>
          </div>
          <span class="setting-title">字体大小</span>
          <div class="setting-right">
            <span class="setting-value">{{ fontSizeValue }}</span>
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M3.5 2L8.5 6L3.5 10" stroke="#999" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>

        <!-- 语言 -->
        <div class="setting-item" @click="navigateTo('/language')">
          <div class="setting-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95c-.32-1.25-.78-2.45-1.38-3.56 1.84.63 3.37 1.91 4.33 3.56zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56-1.84-.63-3.37-1.9-4.33-3.56zm2.95-8H5.08c.96-1.66 2.49-2.93 4.33-3.56C8.81 5.55 8.35 6.75 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95c-.96 1.65-2.49 2.93-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z"/>
            </svg>
          </div>
          <span class="setting-title">语言</span>
          <div class="setting-right">
            <span class="setting-value">{{ language }}</span>
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M3.5 2L8.5 6L3.5 10" stroke="#999" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>
      </div>

      <!-- 其他设置组 -->
      <div class="settings-group">
        <!-- 关于我们 -->
        <div class="setting-item" @click="navigateTo('/about')">
          <div class="setting-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
            </svg>
          </div>
          <span class="setting-title">关于我们</span>
          <div class="setting-right">
            <span class="setting-value">{{ appVersion }}</span>
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M3.5 2L8.5 6L3.5 10" stroke="#999" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>

        <!-- 检查更新 -->
        <div class="setting-item" @click="checkUpdate">
          <div class="setting-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 12h-2v3h-3v2h5v-5zm-6 7c-3.31 0-6-2.69-6-6s2.69-6 6-6 6 2.69 6 6-2.69 6-6 6zm-1-11h2v3h3v2h-5V8z"/>
            </svg>
          </div>
          <span class="setting-title">检查更新</span>
          <div class="setting-right">
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M3.5 2L8.5 6L3.5 10" stroke="#999" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, inject, computed, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import ToggleSwitch from '../../components/ToggleSwitch.vue'
import { showAlert, showConfirm } from '../../utils/dialog'

const router = useRouter()

// 注入全局深色模式状态和方法
const globalDarkMode = inject<Ref<boolean>>('darkMode')
const globalToggleDarkMode = inject<(value: boolean) => void>('toggleDarkMode')

// 状态变量
const darkMode = computed({
  get: () => globalDarkMode?.value || false,
  set: (value) => globalToggleDarkMode?.(value)
})
const cacheSize = ref('128 MB')
const fontSizeValue = ref('标准')
const language = ref('简体中文')
const appVersion = ref('v1.0.0')

// 导航函数
const goBack = () => {
  router.back()
}

const navigateTo = (path: string) => {
  console.log(`Navigate to: ${path}`)
  // 在实际应用中，这里应该导航到相应的页面
  // router.push(path)
}

// 功能函数
// 深色模式切换通过computed属性直接绑定到全局状态

const clearCache = async () => {
  if (await showConfirm('确定要清理缓存吗？')) {
    console.log('Cache cleared')
    cacheSize.value = '0 KB'
    // 在实际应用中，这里应该添加清理缓存的逻辑
  }
}

const checkUpdate = async () => {
  console.log('Checking for updates')
  await showAlert('当前已是最新版本')
  // 在实际应用中，这里应该添加检查更新的逻辑
}

// 初始化
onMounted(() => {
  // 现在使用全局深色模式状态，不需要手动检查documentElement
})


</script>

<style scoped>
.settings-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f7f7f7;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 12px;
  background-color: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #333;
  border-radius: 50%;
}

.back-button:hover {
  background: #f5f5f5;
}

.page-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

.header-right {
  width: 40px;
}

/* 设置内容 */
.settings-content {
  flex: 1;
  padding: 0;
  overflow-y: auto;
}

/* 设置组样式 */
.settings-group {
  background-color: #fff;
  margin-bottom: 10px;
  border-radius: 8px;
  overflow: hidden;
}

/* 设置项 */
.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

/* 移除最后一个设置项的底边框 */
.settings-group:last-child .setting-item:last-child,
.setting-item:last-child {
  border-bottom: none;
}

.setting-item:hover,
.setting-item:active {
  background-color: #fafafa;
}

.setting-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  color: #666;
}

.setting-title {
  flex: 1;
  font-size: 16px;
  color: #333;
  font-weight: 400;
}

.setting-right {
  display: flex;
  align-items: center;
  color: #999;
}

.setting-value {
  font-size: 14px;
  margin-right: 8px;
}

/* 箭头样式优化 */
.setting-right svg {
  width: 12px;
  height: 12px;
  color: #ccc;
}

/* 退出按钮 */
.logout-button-container {
  margin-top: 24px;
  padding: 0 16px;
}

.logout-button {
  width: 100%;
  padding: 12px;
  background-color: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  font-size: 16px;
  color: #ff4d4f;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-button:hover,
.logout-button:active {
  background-color: #fff0f0;
}

/* 优化滚动条样式 */
.settings-content::-webkit-scrollbar {
  width: 6px;
}

.settings-content::-webkit-scrollbar-track {
  background: transparent;
}

.settings-content::-webkit-scrollbar-thumb {
  background-color: #ccc;
  border-radius: 3px;
}

.settings-content::-webkit-scrollbar-thumb:hover {
  background-color: #999;
}

/* 响应式设计 */
@media (min-width: 768px) {
  .settings-container {
    max-width: 600px;
    margin: 0 auto;
    height: 100vh;
    border-left: 1px solid #eee;
    border-right: 1px solid #eee;
  }
}

/* 暗色模式 */
:global(.dark-mode) .settings-container {
  background-color: #1a1a1a;
}

:global(.dark-mode) .page-header {
  background-color: #262626;
  border-bottom-color: #333;
}

:global(.dark-mode) .back-button {
  color: #ddd;
}

:global(.dark-mode) .page-title {
  color: #ddd;
}

:global(.dark-mode) .settings-group {
  background-color: #262626;
}

:global(.dark-mode) .setting-item {
  background-color: #262626;
  border-bottom-color: #333;
}

:global(.dark-mode) .setting-item:hover,
:global(.dark-mode) .setting-item:active {
  background-color: #333;
}

:global(.dark-mode) .setting-icon {
  color: #999;
}

:global(.dark-mode) .setting-title {
  color: #ddd;
}

:global(.dark-mode) .setting-right {
  color: #666;
}

:global(.dark-mode) .logout-button {
  background-color: #262626;
  border-color: #333;
  color: #ff7875;
}

:global(.dark-mode) .logout-button:hover,
:global(.dark-mode) .logout-button:active {
  background-color: #333;
}
</style>
