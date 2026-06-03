<template>
  <div class="admin-home">
    <!-- 项目标题和描述 -->
    <div class="project-header">
      <h1 class="project-title">Campus Life 校园生活服务平台</h1>
      <p class="project-description">
        Campus Life 是一个前后端分离的校园生活全栈项目，包含用户端、商家端、管理端、主业务后端、AI 服务和 API 网关。
        平台支持校园社区、商家优惠券、订单支付、退款处理、消息通知、后台审核、AI 对话和知识库问答等能力。
      </p>
      <div class="version-info">
        <span class="version-label">当前版本：</span>
        <span class="version-value">v1.0.0</span>
      </div>
      <div class="project-actions">
        <a href="https://github.com/bianjunfeng/campus-life" target="_blank" rel="noopener noreferrer" class="action-btn btn-open-source">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"></path>
          </svg>
          免费开源
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
      <!-- 左侧：项目模块 -->
      <div class="content-card deployment-card">
        <h2 class="card-title">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
            <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
            <line x1="12" y1="22.08" x2="12" y2="12"></line>
          </svg>
          项目模块
        </h2>
        <div class="card-content">
          <p class="deployment-tip">
            当前项目由 <strong>前端、主业务后端、AI 服务、API 网关</strong> 组成，管理端通过网关访问业务与 AI 能力。
          </p>

          <div class="server-section">
            <h3>本地服务地址</h3>
            <div class="server-item">
              <p>用户端 Web</p>
              <span class="server-link">http://127.0.0.1:5173</span>
            </div>
            <div class="server-item">
              <p>管理端 Web</p>
              <span class="server-link">http://127.0.0.1:5174</span>
            </div>
            <div class="server-item">
              <p>商家端 Web</p>
              <span class="server-link">http://127.0.0.1:5175</span>
            </div>
            <div class="server-item">
              <p>API 网关</p>
              <span class="server-link">http://127.0.0.1:8090</span>
            </div>
            <div class="server-item">
              <p>主业务后端</p>
              <span class="server-link">http://127.0.0.1:8080</span>
            </div>
            <div class="server-item">
              <p>AI 服务</p>
              <span class="server-link">http://127.0.0.1:8083</span>
            </div>
          </div>

          <p class="coupon-note">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="16" x2="12" y2="12"></line>
              <line x1="12" y1="8" x2="12.01" y2="8"></line>
            </svg>
            线上部署时建议通过 API 网关统一入口暴露服务，并为 MySQL、Redis、消息队列和 AI 配置独立环境变量。
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
              <span class="tech-tag">Spring Boot 3.4</span>
              <span class="tech-tag">Spring Cloud Gateway</span>
              <span class="tech-tag">Spring Security</span>
              <span class="tech-tag">JWT</span>
              <span class="tech-tag">MyBatis</span>
              <span class="tech-tag">MySQL</span>
              <span class="tech-tag">Redis</span>
              <span class="tech-tag">RabbitMQ</span>
              <span class="tech-tag">Kafka</span>
              <span class="tech-tag">Elasticsearch</span>
              <span class="tech-tag">Spring AI</span>
            </div>
          </div>
          <div class="tech-section">
            <h3>前端技术</h3>
            <div class="tech-tags">
              <span class="tech-tag">Vue 3</span>
              <span class="tech-tag">Vite</span>
              <span class="tech-tag">Vue Router</span>
              <span class="tech-tag">Axios</span>
              <span class="tech-tag">ECharts</span>
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
                <span class="changelog-date">2026-01-20</span>
              </div>
              <div class="changelog-content">
                <ul>
                  <li>✅ 完成用户端、商家端、管理端三端入口</li>
                  <li>✅ 完成校园社区、评论、点赞、收藏和举报管理</li>
                  <li>✅ 完成商家、优惠券、订单、退款和支付对账管理</li>
                  <li>✅ 完成用户认证、学生认证和商家认证审核</li>
                  <li>✅ 接入 API 网关、JWT 鉴权和 Redis 限流</li>
                  <li>✅ 接入 AI 智能体、知识库检索和模型网关</li>
                </ul>
              </div>
            </div>
            <div class="changelog-item">
              <div class="changelog-header">
                <span class="changelog-version">v0.9.0</span>
                <span class="changelog-date">2025-12-15</span>
              </div>
              <div class="changelog-content">
                <ul>
                  <li>✅ 完成主业务后端、AI 服务和 API 网关拆分</li>
                  <li>✅ 完成 MySQL、Redis、消息队列和搜索能力接入</li>
                  <li>✅ 完成管理端核心业务接口对接</li>
                </ul>
              </div>
            </div>
            <div class="changelog-item">
              <div class="changelog-header">
                <span class="changelog-version">v0.8.0</span>
                <span class="changelog-date">2025-11-30</span>
              </div>
              <div class="changelog-content">
                <ul>
                  <li>✅ 完成项目基础架构搭建</li>
                  <li>✅ 完成用户、内容、商家和订单基础模型设计</li>
                  <li>✅ 完成前端三端路由和基础布局</li>
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

.server-section {
  margin-bottom: 20px;
}

.server-section h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.server-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-bottom: 8px;
}

.server-item p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.server-link {
  color: #1677ff;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.3s;
}

.server-link:hover {
  color: #0958d9;
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
