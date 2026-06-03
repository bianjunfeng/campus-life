<template>
  <div class="admin-home">
    <!-- 项目标题和描述 -->
    <div class="project-header">
      <h1 class="project-title">校园生活服务平台</h1>
      <p class="project-description">
        此项目是基于 Spring Boot + Vue 架构的前后端分离系统。
        Web 端使用 Vue3 + TypeScript + Vite 进行开发，后端使用 Spring Boot + MyBatis-Plus 进行开发。
        使用 MySQL 作为数据库，支持 OAuth 第三方登录和 WebSocket 实时通信。
      </p>
      <div class="version-info">
        <span class="version-label">当前版本：</span>
        <span class="version-value">v1.0.0</span>
      </div>
      <div class="project-actions">
        <a href="https://github.com" target="_blank" class="action-btn btn-open-source">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"></path>
          </svg>
          免费开源
        </a>
        <a href="https://gitee.com" target="_blank" class="action-btn btn-gitee">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
            <path d="M2 17l10 5 10-5"></path>
            <path d="M2 12l10 5 10-5"></path>
          </svg>
          访问码云
        </a>
        <RouterLink to="/admin/dashboard" class="action-btn btn-homepage">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
            <polyline points="9 22 9 12 15 12 15 22"></polyline>
          </svg>
          访问主页
        </RouterLink>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content-grid">
      <!-- 左侧：项目部署 -->
      <div class="content-card deployment-card">
        <h2 class="card-title">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
            <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
            <line x1="12" y1="22.08" x2="12" y2="12"></line>
          </svg>
          项目部署
        </h2>
        <div class="card-content">
          <p class="deployment-tip">
            部署该项目选择 <strong>阿里云/腾讯云 "2核2G"</strong> 配置即可
          </p>
          
          <div class="coupon-section">
            <h3>云产品优惠券</h3>
            <div class="coupon-item">
              <p>领取阿里云通用云产品1888优惠券</p>
              <a href="https://www.aliyun.com" target="_blank" class="coupon-link">点我进入</a>
            </div>
            <div class="coupon-item">
              <p>领取腾讯云通用云产品2860优惠券</p>
              <a href="https://cloud.tencent.com" target="_blank" class="coupon-link">点我进入</a>
            </div>
          </div>

          <div class="server-section">
            <h3>服务器折扣</h3>
            <div class="server-item">
              <p>阿里云服务器折扣区</p>
              <a href="https://www.aliyun.com" target="_blank" class="server-link">点我进入</a>
            </div>
            <div class="server-item">
              <p>腾讯云服务器秒杀区</p>
              <a href="https://cloud.tencent.com" target="_blank" class="server-link">点我进入</a>
            </div>
          </div>

          <p class="coupon-note">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="16" x2="12" y2="12"></line>
              <line x1="12" y1="8" x2="12.01" y2="8"></line>
            </svg>
            云产品通用红包，可叠加官网常规优惠使用。（仅限新用户）
          </p>
        </div>
      </div>

      <!-- 中间：技术选型 -->
      <div class="content-card tech-card">
        <h2 class="card-title">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
            <path d="M2 17l10 5 10-5"></path>
            <path d="M2 12l10 5 10-5"></path>
          </svg>
          技术选型
        </h2>
        <div class="card-content">
          <div class="tech-section">
            <h3>后端技术</h3>
            <div class="tech-tags">
              <span class="tech-tag">Spring Boot</span>
              <span class="tech-tag">Spring Security</span>
              <span class="tech-tag">JWT</span>
              <span class="tech-tag">MyBatis-Plus</span>
              <span class="tech-tag">MySQL</span>
              <span class="tech-tag">Druid</span>
              <span class="tech-tag">Fastjson</span>
              <span class="tech-tag">WebSocket</span>
              <span class="tech-tag">OAuth2</span>
            </div>
          </div>
          <div class="tech-section">
            <h3>前端技术</h3>
            <div class="tech-tags">
              <span class="tech-tag">Vue 3</span>
              <span class="tech-tag">TypeScript</span>
              <span class="tech-tag">Vite</span>
              <span class="tech-tag">Vue Router</span>
              <span class="tech-tag">Axios</span>
              <span class="tech-tag">ECharts</span>
              <span class="tech-tag">WebSocket</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：更新日志 -->
      <div class="content-card changelog-card">
        <h2 class="card-title">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
          </svg>
          更新日志
        </h2>
        <div class="card-content">
          <div class="changelog-list">
            <div class="changelog-item">
              <div class="changelog-header">
                <span class="changelog-version">v1.0.0</span>
                <span class="changelog-date">2024-12-01</span>
              </div>
              <div class="changelog-content">
                <ul>
                  <li>✅ 完成基础功能开发</li>
                  <li>✅ 实现用户认证和授权</li>
                  <li>✅ 完成帖子管理功能</li>
                  <li>✅ 实现评论系统</li>
                  <li>✅ 完成商家和优惠券管理</li>
                  <li>✅ 实现管理后台</li>
                  <li>✅ 支持 OAuth 第三方登录</li>
                  <li>✅ 实现 WebSocket 实时通信</li>
                </ul>
              </div>
            </div>
            <div class="changelog-item">
              <div class="changelog-header">
                <span class="changelog-version">v0.9.0</span>
                <span class="changelog-date">2024-11-15</span>
              </div>
              <div class="changelog-content">
                <ul>
                  <li>✅ 完成前端页面开发</li>
                  <li>✅ 实现响应式设计</li>
                  <li>✅ 完成 API 接口对接</li>
                </ul>
              </div>
            </div>
            <div class="changelog-item">
              <div class="changelog-header">
                <span class="changelog-version">v0.8.0</span>
                <span class="changelog-date">2024-11-01</span>
              </div>
              <div class="changelog-content">
                <ul>
                  <li>✅ 完成数据库设计</li>
                  <li>✅ 实现后端基础功能</li>
                  <li>✅ 完成项目架构搭建</li>
                </ul>
              </div>
            </div>
          </div>
          <button class="show-more-btn" @click="showMoreChangelog = !showMoreChangelog">
            {{ showMoreChangelog ? '收起' : '显示更多' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const showMoreChangelog = ref(false)
</script>

<style scoped>
.admin-home {
  width: 100%;
}

/* 项目标题区域 */
.project-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 40px;
  margin-bottom: 24px;
  color: white;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.3);
}

.project-title {
  margin: 0 0 16px 0;
  font-size: 32px;
  font-weight: 700;
  text-align: center;
}

.project-description {
  margin: 0 0 20px 0;
  font-size: 16px;
  line-height: 1.8;
  text-align: center;
  opacity: 0.95;
}

.version-info {
  text-align: center;
  margin-bottom: 24px;
}

.version-label {
  font-size: 14px;
  opacity: 0.9;
}

.version-value {
  font-size: 18px;
  font-weight: 600;
  margin-left: 8px;
}

.project-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 8px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
  color: white;
}

.action-btn svg {
  width: 18px;
  height: 18px;
}

.btn-open-source {
  background: #e74c3c;
}

.btn-open-source:hover {
  background: #c0392b;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(231, 76, 60, 0.4);
}

.btn-gitee {
  background: #1677ff;
}

.btn-gitee:hover {
  background: #0958d9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.4);
}

.btn-homepage {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.btn-homepage:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

/* 主要内容网格 */
.main-content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 24px;
}

.content-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 20px 0;
  font-size: 20px;
  color: #333;
  font-weight: 600;
}

.card-title svg {
  width: 24px;
  height: 24px;
  color: #1677ff;
}

.card-content {
  color: #666;
  line-height: 1.8;
}

/* 部署卡片 */
.deployment-tip {
  margin: 0 0 24px 0;
  font-size: 14px;
}

.deployment-tip strong {
  color: #1677ff;
  font-weight: 600;
}

.coupon-section,
.server-section {
  margin-bottom: 20px;
}

.coupon-section h3,
.server-section h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.coupon-item,
.server-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-bottom: 8px;
}

.coupon-item p,
.server-item p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.coupon-link,
.server-link {
  color: #1677ff;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.3s;
}

.coupon-link:hover,
.server-link:hover {
  color: #0958d9;
  text-decoration: underline;
}

.coupon-note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 20px 0 0 0;
  padding: 12px;
  background: #fff7e6;
  border-left: 3px solid #faad14;
  border-radius: 4px;
  font-size: 12px;
  color: #856404;
}

.coupon-note svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  margin-top: 2px;
}

/* 技术选型卡片 */
.tech-section {
  margin-bottom: 24px;
}

.tech-section:last-child {
  margin-bottom: 0;
}

.tech-section h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.tech-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tech-tag {
  padding: 6px 12px;
  background: #f0f7ff;
  color: #1677ff;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid #d6e4ff;
}

/* 更新日志卡片 */
.changelog-list {
  max-height: 400px;
  overflow-y: auto;
}

.changelog-item {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.changelog-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.changelog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.changelog-version {
  font-size: 16px;
  font-weight: 600;
  color: #1677ff;
}

.changelog-date {
  font-size: 12px;
  color: #999;
}

.changelog-content ul {
  margin: 0;
  padding-left: 20px;
}

.changelog-content li {
  margin-bottom: 6px;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.show-more-btn {
  width: 100%;
  margin-top: 16px;
  padding: 10px;
  background: #f8f9fa;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.show-more-btn:hover {
  background: #e9ecef;
  color: #333;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .project-header {
    padding: 24px 20px;
  }

  .project-title {
    font-size: 24px;
  }

  .project-description {
    font-size: 14px;
  }

  .project-actions {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
    justify-content: center;
  }

  .main-content-grid {
    grid-template-columns: 1fr;
  }

  .content-card {
    padding: 20px;
  }
}
</style>
