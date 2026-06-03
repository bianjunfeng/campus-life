<template>
  <div class="settings-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
        </svg>
      </button>
      <h1 class="page-title">隐私设置</h1>
      <div class="placeholder"></div>
    </div>

    <!-- 设置内容区域 -->
    <div class="settings-content">
      <!-- 互动设置 -->
      <div class="settings-section">
        <h2 class="section-title">互动</h2>
        
        <div class="setting-item">
          <div class="setting-info">
            <h3 class="setting-label">一键防护</h3>
            <p class="setting-description">开启后，7天内将不接收未关注人的私信/评论/分享</p>
          </div>
          <toggle-switch v-model="oneKeyProtection"></toggle-switch>
        </div>

        <div class="setting-item" @click="openOnlineStatus">
          <div class="setting-info">
            <h3 class="setting-label">在线状态</h3>
          </div>
          <div class="setting-arrow">
            <span class="setting-value">好友</span>
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/>
            </svg>
          </div>
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <h3 class="setting-label">展示聊天标识</h3>
          </div>
          <toggle-switch v-model="showChatIndicator"></toggle-switch>
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <h3 class="setting-label">只允许我关注的人评论我</h3>
          </div>
          <toggle-switch v-model="onlyFollowersComment"></toggle-switch>
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <h3 class="setting-label">只允许我关注的人给我发弹幕</h3>
          </div>
          <toggle-switch v-model="onlyFollowersDanmu"></toggle-switch>
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <h3 class="setting-label">只允许我关注的人 @ 我</h3>
          </div>
          <toggle-switch v-model="onlyFollowersMention"></toggle-switch>
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <h3 class="setting-label">允许下载我的全部视频</h3>
          </div>
          <toggle-switch v-model="allowDownloadVideos"></toggle-switch>
        </div>

        <div class="setting-item" @click="openWhoCanPrivateMessage">
          <div class="setting-info">
            <h3 class="setting-label">谁可以私信我</h3>
          </div>
          <div class="setting-arrow">
            <span class="setting-value">默认</span>
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/>
            </svg>
          </div>
        </div>
      </div>

      <!-- 内容可见性设置 -->
      <div class="settings-section">
        <h2 class="section-title">内容可见性</h2>
        
        <div class="setting-item" @click="openCollectionVisibility">
          <div class="setting-info">
            <h3 class="setting-label">我的收藏</h3>
          </div>
          <div class="setting-arrow">
            <span class="setting-value">不公开</span>
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/>
            </svg>
          </div>
        </div>

        <div class="setting-item" @click="openVisitedVisibility">
          <div class="setting-info">
            <h3 class="setting-label">我的去过</h3>
          </div>
          <div class="setting-arrow">
            <span class="setting-value">不公开</span>
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/>
            </svg>
          </div>
        </div>

        <div class="setting-item" @click="openEvaluationVisibility">
          <div class="setting-info">
            <h3 class="setting-label">我的评价</h3>
          </div>
          <div class="setting-arrow">
            <span class="setting-value">已公开</span>
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/>
            </svg>
          </div>
        </div>
      </div>

      <!-- 关系设置 -->
      <div class="settings-section">
        <h2 class="section-title">关系</h2>
        
        <div class="setting-item" @click="openFindMeWays">
          <div class="setting-info">
            <h3 class="setting-label">找到我的方式</h3>
          </div>
          <div class="setting-arrow">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/>
            </svg>
          </div>
        </div>

        <div class="setting-item" @click="openFollowList">
          <div class="setting-info">
            <h3 class="setting-label">关注与粉丝列表</h3>
          </div>
          <div class="setting-arrow">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/>
            </svg>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showAlert } from '../../utils/dialog'

const router = useRouter()

// 隐私设置状态
const oneKeyProtection = ref(false)
const showChatIndicator = ref(true)
const onlyFollowersComment = ref(false)
const onlyFollowersDanmu = ref(false)
const onlyFollowersMention = ref(false)
const allowDownloadVideos = ref(false)

// 返回上一页
const goBack = () => {
  router.back()
}

// 处理子页面导航和选项切换
const openOnlineStatus = () => {
  void showAlert('在线状态设置功能开发中')
}

const openWhoCanPrivateMessage = () => {
  void showAlert('谁可以私信我设置功能开发中')
}

const openCollectionVisibility = () => {
  void showAlert('我的收藏可见性设置功能开发中')
}

const openVisitedVisibility = () => {
  void showAlert('我的去过可见性设置功能开发中')
}

const openEvaluationVisibility = () => {
  void showAlert('我的评价可见性设置功能开发中')
}

const openFindMeWays = () => {
  void showAlert('找到我的方式设置功能开发中')
}

const openFollowList = () => {
  void showAlert('关注与粉丝列表设置功能开发中')
}
</script>

<style scoped>
.settings-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background-color: white;
  border-bottom: 1px solid #e0e0e0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: #333;
  cursor: pointer;
  border-radius: 50%;
  transition: background-color 0.3s;
}

.back-btn:hover {
  background-color: #f5f5f5;
}

.back-btn svg {
  width: 24px;
  height: 24px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.placeholder {
  width: 40px;
  height: 40px;
}

/* 设置内容区域 */
.settings-content {
  padding-bottom: 20px;
}

.settings-section {
  background-color: white;
  margin-bottom: 10px;
  padding-top: 10px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #999;
  margin: 0 0 10px 0;
  padding: 0 20px;
}

/* 设置项 */
.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.3s;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-item:hover {
  background-color: #f5f5f5;
}

.setting-info {
  flex: 1;
}

.setting-label {
  font-size: 15px;
  color: #333;
  margin: 0 0 4px 0;
  font-weight: 500;
}

.setting-description {
  font-size: 13px;
  color: #999;
  margin: 0;
  line-height: 1.4;
}

.setting-arrow {
  display: flex;
  align-items: center;
  color: #999;
}

.setting-value {
  font-size: 14px;
  color: #999;
  margin-right: 8px;
}

.setting-arrow svg {
  width: 16px;
  height: 16px;
}

/* 切换开关组件样式 */
:deep(.toggle-switch) {
  position: relative;
  display: inline-block;
  width: 52px;
  height: 28px;
}

:deep(.toggle-switch input) {
  opacity: 0;
  width: 0;
  height: 0;
}

:deep(.toggle-slider) {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 28px;
}

:deep(.toggle-slider:before) {
  position: absolute;
  content: "";
  height: 24px;
  width: 24px;
  left: 2px;
  bottom: 2px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

:deep(input:checked + .toggle-slider) {
  background-color: #1890ff;
}

:deep(input:checked + .toggle-slider:before) {
  transform: translateX(24px);
}

/* 暗色模式样式 */
:global(.dark-mode) .settings-container {
  background-color: #1a1a1a;
}

:global(.dark-mode) .page-header {
  background-color: #1f1f1f;
  border-color: #333;
}

:global(.dark-mode) .page-title {
  color: #ffffff;
}

:global(.dark-mode) .back-btn {
  color: #ffffff;
}

:global(.dark-mode) .back-btn:hover {
  background-color: #333;
}

:global(.dark-mode) .settings-section {
  background-color: #1f1f1f;
}

:global(.dark-mode) .section-title {
  color: #666;
}

:global(.dark-mode) .setting-item {
  border-color: #333;
}

:global(.dark-mode) .setting-item:hover {
  background-color: #333;
}

:global(.dark-mode) .setting-label {
  color: #ffffff;
}

:global(.dark-mode) .setting-description,
:global(.dark-mode) .setting-value,
:global(.dark-mode) .setting-arrow {
  color: #999;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .settings-section {
    padding-top: 8px;
  }
  
  .section-title {
    padding: 0 16px;
    font-size: 13px;
  }
  
  .setting-item {
    padding: 12px 16px;
  }
  
  .setting-label {
    font-size: 14px;
  }
  
  .setting-description {
    font-size: 12px;
  }
  
  .setting-value {
    font-size: 13px;
  }
}
</style>
