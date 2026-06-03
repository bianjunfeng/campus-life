<template>
  <div class="admin-layout ops-layout">
    <!-- 移动端遮罩层 -->
    <div 
      v-if="isMobile && sidebarVisible" 
      class="sidebar-overlay"
      @click="closeSidebar"
    ></div>

    <!-- 侧边栏导航 -->
    <aside 
      class="admin-sidebar" 
      :class="{ 
        collapsed: sidebarCollapsed && !isMobile,
        'mobile-visible': isMobile && sidebarVisible,
        'mobile-hidden': isMobile && !sidebarVisible
      }"
    >
      <div class="sidebar-header">
        <h2 v-if="!sidebarCollapsed || isMobile">管理后台</h2>
        <button class="sidebar-toggle" @click="toggleSidebar">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path v-if="sidebarCollapsed && !isMobile" d="M9 18l6-6-6-6"/>
            <path v-else d="M15 18l-6-6 6-6"/>
          </svg>
        </button>
      </div>
      
      <nav class="sidebar-nav">
        <!-- 仪表盘 - 可折叠菜单 -->
        <div class="nav-menu-item">
          <div 
            class="nav-item nav-menu-header"
            :class="{ active: isMenuActive('dashboard') }"
            @click="toggleMenu('dashboard')"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7"></rect>
              <rect x="14" y="3" width="7" height="7"></rect>
              <rect x="14" y="14" width="7" height="7"></rect>
              <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
            <span v-if="!sidebarCollapsed || isMobile">仪表盘</span>
            <svg 
              v-if="!sidebarCollapsed || isMobile"
              class="nav-arrow"
              :class="{ expanded: expandedMenus.dashboard }"
              xmlns="http://www.w3.org/2000/svg" 
              viewBox="0 0 24 24" 
              fill="none" 
              stroke="currentColor" 
              stroke-width="2"
            >
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
          <div 
            v-if="expandedMenus.dashboard && (!sidebarCollapsed || isMobile)"
            class="nav-submenu"
          >
            <router-link 
              to="/admin/home" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/home' }"
            >
              <span>首页</span>
            </router-link>
            <router-link 
              to="/admin/dashboard" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/dashboard' || $route.path === '/admin' }"
            >
              <span>工作台</span>
            </router-link>
          </div>
        </div>

        <!-- 会员中心 - 可折叠菜单 -->
        <div class="nav-menu-item">
          <div 
            class="nav-item nav-menu-header"
            :class="{ active: isMenuActive('member') }"
            @click="toggleMenu('member')"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
            <span v-if="!sidebarCollapsed || isMobile">会员中心</span>
            <svg 
              v-if="!sidebarCollapsed || isMobile"
              class="nav-arrow"
              :class="{ expanded: expandedMenus.member }"
              xmlns="http://www.w3.org/2000/svg" 
              viewBox="0 0 24 24" 
              fill="none" 
              stroke="currentColor" 
              stroke-width="2"
            >
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
          <div 
            v-if="expandedMenus.member && (!sidebarCollapsed || isMobile)"
            class="nav-submenu"
          >
            <router-link 
              to="/admin/members" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/members' }"
            >
              <span>会员列表</span>
            </router-link>
            <router-link 
              to="/admin/member-stats" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/member-stats' }"
            >
              <span>会员统计</span>
            </router-link>
          </div>
        </div>

        <!-- 笔记管理 - 可折叠菜单 -->
        <div class="nav-menu-item">
          <div 
            class="nav-item nav-menu-header"
            :class="{ active: isMenuActive('posts') }"
            @click="toggleMenu('posts')"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="16" y1="13" x2="8" y2="13"></line>
              <line x1="16" y1="17" x2="8" y2="17"></line>
            </svg>
            <span v-if="!sidebarCollapsed || isMobile">笔记管理</span>
            <svg 
              v-if="!sidebarCollapsed || isMobile"
              class="nav-arrow"
              :class="{ expanded: expandedMenus.posts }"
              xmlns="http://www.w3.org/2000/svg" 
              viewBox="0 0 24 24" 
              fill="none" 
              stroke="currentColor" 
              stroke-width="2"
            >
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
          <div 
            v-if="expandedMenus.posts && (!sidebarCollapsed || isMobile)"
            class="nav-submenu"
          >
            <router-link 
              to="/admin/posts" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/posts' }"
            >
              <span>帖子管理</span>
            </router-link>
            <router-link 
              to="/admin/comments" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/comments' }"
            >
              <span>评论管理</span>
            </router-link>
            <router-link 
              to="/admin/reports" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/reports' }"
            >
              <span>举报管理</span>
            </router-link>
          </div>
        </div>

        <!-- 券管理 - 可折叠菜单 -->
        <div class="nav-menu-item">
          <div 
            class="nav-item nav-menu-header"
            :class="{ active: isMenuActive('welfare') }"
            @click="toggleMenu('welfare')"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="8" width="18" height="4" rx="1"></rect>
              <path d="M12 8v13"></path>
              <path d="M19 12v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7"></path>
              <path d="M7.5 8a2.5 2.5 0 0 1 0-5A4.8 8 0 0 1 12 8a4.8 8 0 0 1 4.5-5 2.5 2.5 0 0 1 0 5"></path>
            </svg>
            <span v-if="!sidebarCollapsed || isMobile">券管理</span>
            <svg 
              v-if="!sidebarCollapsed || isMobile"
              class="nav-arrow"
              :class="{ expanded: expandedMenus.welfare }"
              xmlns="http://www.w3.org/2000/svg" 
              viewBox="0 0 24 24" 
              fill="none" 
              stroke="currentColor" 
              stroke-width="2"
            >
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
          <div 
            v-if="expandedMenus.welfare && (!sidebarCollapsed || isMobile)"
            class="nav-submenu"
          >
            <router-link 
              to="/admin/vouchers" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/vouchers' }"
            >
              <span>优惠券管理</span>
            </router-link>
            <router-link 
              to="/admin/voucher-orders" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/voucher-orders' }"
            >
              <span>订单管理</span>
            </router-link>
            <router-link 
              to="/admin/refunds" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/refunds' }"
            >
              <span>退款审核</span>
            </router-link>
            <router-link 
              to="/admin/payment-reconciliation" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/payment-reconciliation' }"
            >
              <span>支付对账</span>
            </router-link>
            <router-link 
              to="/admin/merchants" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/merchants' }"
            >
              <span>商家管理</span>
            </router-link>
          </div>
        </div>

        <!-- AI管理 -->
        <router-link 
          to="/admin/ai" 
          class="nav-item"
          :class="{ active: $route.path === '/admin/ai' }"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="9" y1="9" x2="15" y2="9"></line>
            <line x1="9" y1="15" x2="15" y2="15"></line>
            <circle cx="12" cy="12" r="1"></circle>
            <path d="M3 12h18"></path>
            <path d="M12 3v18"></path>
          </svg>
          <span v-if="!sidebarCollapsed || isMobile">AI管理</span>
        </router-link>

        <router-link
          to="/admin/ai/agents"
          class="nav-item"
          :class="{ active: $route.path === '/admin/ai/agents' || $route.path.startsWith('/admin/ai/agent') }"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 3v3"></path>
            <path d="M12 18v3"></path>
            <rect x="5" y="6" width="14" height="12" rx="3"></rect>
            <path d="M9 11h.01"></path>
            <path d="M15 11h.01"></path>
            <path d="M9 15h6"></path>
          </svg>
          <span v-if="!sidebarCollapsed || isMobile">Agent中心</span>
        </router-link>

        <!-- 系统管理 - 可折叠菜单 -->
        <div class="nav-menu-item">
          <div 
            class="nav-item nav-menu-header"
            :class="{ active: isMenuActive('system') }"
            @click="toggleMenu('system')"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="3"></circle>
              <path d="M12 1v6m0 6v6M5.64 5.64l4.24 4.24m4.24 4.24l4.24 4.24M1 12h6m6 0h6M5.64 18.36l4.24-4.24m4.24-4.24l4.24-4.24"></path>
            </svg>
            <span v-if="!sidebarCollapsed || isMobile">系统管理</span>
            <svg 
              v-if="!sidebarCollapsed || isMobile"
              class="nav-arrow"
              :class="{ expanded: expandedMenus.system }"
              xmlns="http://www.w3.org/2000/svg" 
              viewBox="0 0 24 24" 
              fill="none" 
              stroke="currentColor" 
              stroke-width="2"
            >
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
          <div 
            v-if="expandedMenus.system && (!sidebarCollapsed || isMobile)"
            class="nav-submenu"
          >
            <router-link 
              to="/admin/system/users" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/system/users' }"
            >
              <span>用户管理</span>
            </router-link>
            <router-link 
              to="/admin/student-auth" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/student-auth' }"
            >
              <span>学生认证</span>
            </router-link>
            <router-link 
              to="/admin/merchant-auth" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/merchant-auth' }"
            >
              <span>商家认证</span>
            </router-link>
            <router-link 
              to="/admin/system/settings" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/system/settings' }"
            >
              <span>系统设置</span>
            </router-link>
            <!-- 日志管理子菜单 -->
            <div class="nav-submenu-nested">
              <div 
                class="nav-subitem nav-submenu-header"
                :class="{ active: isLogMenuActive() }"
                @click.stop="toggleNestedMenu('log')"
              >
                <span>日志管理</span>
                <svg 
                  class="nav-arrow"
                  :class="{ expanded: expandedMenus.log }"
                  xmlns="http://www.w3.org/2000/svg" 
                  viewBox="0 0 24 24" 
                  fill="none" 
                  stroke="currentColor" 
                  stroke-width="2"
                >
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
              </div>
              <div 
                v-if="expandedMenus.log"
                class="nav-submenu-inner"
              >
                <router-link 
                  to="/admin/logs/member-login" 
                  class="nav-subitem-inner"
                  :class="{ active: $route.path === '/admin/logs/member-login' }"
                >
                  <span>会员登录日志</span>
                </router-link>
                <router-link 
                  to="/admin/logs/admin-login" 
                  class="nav-subitem-inner"
                  :class="{ active: $route.path === '/admin/logs/admin-login' }"
                >
                  <span>管理员登录日志</span>
                </router-link>
                <router-link 
                  to="/admin/logs/admin-operation" 
                  class="nav-subitem-inner"
                  :class="{ active: $route.path === '/admin/logs/admin-operation' }"
                >
                  <span>管理员操作日志</span>
                </router-link>
                <router-link 
                  to="/admin/logs/audit" 
                  class="nav-subitem-inner"
                  :class="{ active: $route.path === '/admin/logs/audit' }"
                >
                  <span>审核日志</span>
                </router-link>
              </div>
            </div>
          </div>
        </div>

        <!-- 系统监控 - 可折叠菜单 -->
        <div class="nav-menu-item">
          <div 
            class="nav-item nav-menu-header"
            :class="{ active: isMenuActive('monitor') }"
            @click="toggleMenu('monitor')"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
              <line x1="8" y1="21" x2="16" y2="21"></line>
              <line x1="12" y1="17" x2="12" y2="21"></line>
            </svg>
            <span v-if="!sidebarCollapsed || isMobile">系统监控</span>
            <svg 
              v-if="!sidebarCollapsed || isMobile"
              class="nav-arrow"
              :class="{ expanded: expandedMenus.monitor }"
              xmlns="http://www.w3.org/2000/svg" 
              viewBox="0 0 24 24" 
              fill="none" 
              stroke="currentColor" 
              stroke-width="2"
            >
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
          <div 
            v-if="expandedMenus.monitor && (!sidebarCollapsed || isMobile)"
            class="nav-submenu"
          >
            <router-link 
              to="/admin/monitor/online-users" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/monitor/online-users' }"
            >
              <span>在线用户</span>
            </router-link>
            <router-link 
              to="/admin/monitor/cache" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/monitor/cache' }"
            >
              <span>缓存列表</span>
            </router-link>
          </div>
        </div>

        <!-- 系统工具 - 可折叠菜单 -->
        <div class="nav-menu-item">
          <div 
            class="nav-item nav-menu-header"
            :class="{ active: isMenuActive('tools') }"
            @click="toggleMenu('tools')"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"></path>
            </svg>
            <span v-if="!sidebarCollapsed || isMobile">系统工具</span>
            <svg 
              v-if="!sidebarCollapsed || isMobile"
              class="nav-arrow"
              :class="{ expanded: expandedMenus.tools }"
              xmlns="http://www.w3.org/2000/svg" 
              viewBox="0 0 24 24" 
              fill="none" 
              stroke="currentColor" 
              stroke-width="2"
            >
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
          <div 
            v-if="expandedMenus.tools && (!sidebarCollapsed || isMobile)"
            class="nav-submenu"
          >
            <router-link 
              to="/admin/tools/apis" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/tools/apis' }"
            >
              <span>系统接口</span>
            </router-link>
          </div>
        </div>

        <!-- 个人中心 - 可折叠菜单 -->
        <div class="nav-menu-item">
          <div 
            class="nav-item nav-menu-header"
            :class="{ active: isMenuActive('profile') }"
            @click="toggleMenu('profile')"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            <span v-if="!sidebarCollapsed || isMobile">个人中心</span>
            <svg 
              v-if="!sidebarCollapsed || isMobile"
              class="nav-arrow"
              :class="{ expanded: expandedMenus.profile }"
              xmlns="http://www.w3.org/2000/svg" 
              viewBox="0 0 24 24" 
              fill="none" 
              stroke="currentColor" 
              stroke-width="2"
            >
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
          <div 
            v-if="expandedMenus.profile && (!sidebarCollapsed || isMobile)"
            class="nav-submenu"
          >
            <router-link 
              to="/admin/profile/info" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/profile/info' }"
            >
              <span>个人信息</span>
            </router-link>
            <router-link 
              to="/admin/profile/settings" 
              class="nav-subitem"
              :class="{ active: $route.path === '/admin/profile/settings' }"
            >
              <span>修改资料</span>
            </router-link>
          </div>
        </div>
      </nav>
    </aside>

    <!-- 主内容区域 -->
    <div class="admin-main">
      <!-- 顶部导航栏 -->
      <header class="admin-header">
        <div class="header-left">
          <button 
            v-if="isMobile" 
            class="mobile-menu-btn"
            @click="openSidebar"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="3" y1="12" x2="21" y2="12"></line>
              <line x1="3" y1="6" x2="21" y2="6"></line>
              <line x1="3" y1="18" x2="21" y2="18"></line>
            </svg>
          </button>
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>
        <div class="header-right">
          <button class="ops-workbench-button" type="button" @click="goBack" aria-label="跳转工作台">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7" rx="1.5"></rect>
              <rect x="14" y="3" width="7" height="7" rx="1.5"></rect>
              <rect x="14" y="14" width="7" height="7" rx="1.5"></rect>
              <rect x="3" y="14" width="7" height="7" rx="1.5"></rect>
            </svg>
            <span>工作台</span>
          </button>
          <AccountUserMenu portal="admin" />
        </div>
      </header>

      <!-- 内容区域 -->
      <main class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AccountUserMenu from '../../components/account/AccountUserMenu.vue'

const route = useRoute()
const router = useRouter()
const sidebarCollapsed = ref(false)
const sidebarVisible = ref(false)
const isMobile = ref(false)

// 子菜单展开状态
const expandedMenus = ref<Record<string, boolean>>({
  dashboard: false,
  member: false,
  posts: false,
  welfare: false,
  system: false,
  log: false,
  monitor: false,
  tools: false,
  profile: false
})

// 检测是否为移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) {
    sidebarVisible.value = false
  }
}

// 打开侧边栏（移动端）
const openSidebar = () => {
  sidebarVisible.value = true
}

// 关闭侧边栏（移动端）
const closeSidebar = () => {
  sidebarVisible.value = false
}

// 切换侧边栏
const toggleSidebar = () => {
  if (isMobile.value) {
    sidebarVisible.value = !sidebarVisible.value
  } else {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }
}

// 切换子菜单
const toggleMenu = (menuKey: string) => {
  expandedMenus.value[menuKey] = !expandedMenus.value[menuKey]
}

// 切换嵌套子菜单
const toggleNestedMenu = (menuKey: string) => {
  expandedMenus.value[menuKey] = !expandedMenus.value[menuKey]
}

// 检查日志菜单是否激活
const isLogMenuActive = (): boolean => {
  return ['/admin/logs/member-login', '/admin/logs/admin-login', '/admin/logs/admin-operation', '/admin/logs/audit'].some(path => route.path === path || route.path.startsWith(path + '/'))
}

// 检查菜单是否激活（包括子菜单）
const isMenuActive = (menuKey: string): boolean => {
  const menuRoutes: Record<string, string[]> = {
    dashboard: ['/admin/home', '/admin/dashboard', '/admin'],
    member: ['/admin/members', '/admin/member-stats'],
    posts: ['/admin/posts', '/admin/comments', '/admin/reports'],
    welfare: ['/admin/vouchers', '/admin/voucher-orders', '/admin/refunds', '/admin/payment-reconciliation', '/admin/merchants'],
    system: ['/admin/system/users', '/admin/student-auth', '/admin/merchant-auth', '/admin/system/settings', '/admin/logs/member-login', '/admin/logs/admin-login', '/admin/logs/admin-operation', '/admin/logs/audit'],
    monitor: ['/admin/monitor/online-users', '/admin/monitor/cache'],
    tools: ['/admin/tools/apis'],
    profile: ['/admin/profile/info', '/admin/profile/settings']
  }
  return menuRoutes[menuKey]?.some(path => route.path === path || route.path.startsWith(path + '/')) || false
}

// 根据当前路由自动展开对应的菜单
const autoExpandMenu = () => {
  Object.keys(expandedMenus.value).forEach(key => {
    if (isMenuActive(key)) {
      expandedMenus.value[key] = true
    }
  })
}

// 监听路由变化
router.afterEach(() => {
  // 移动端自动关闭侧边栏
  if (isMobile.value) {
    sidebarVisible.value = false
  }
  // 自动展开对应菜单
  autoExpandMenu()
})

// 页面标题映射
const pageTitleMap: Record<string, string> = {
  '/admin': '工作台',
  '/admin/dashboard': '工作台',
  '/admin/home': '首页',
  '/admin/members': '会员管理',
  '/admin/member-stats': '会员统计',
  '/admin/posts': '帖子管理',
  '/admin/comments': '评论管理',
  '/admin/reports': '举报管理',
  '/admin/vouchers': '优惠券管理',
  '/admin/voucher-orders': '订单管理',
  '/admin/refunds': '退款审核',
  '/admin/payment-reconciliation': '支付对账',
  '/admin/merchants': '商家管理',
  '/admin/ai': 'AI管理',
  '/admin/system/users': '用户管理',
  '/admin/student-auth': '学生认证审核',
  '/admin/merchant-auth': '商家认证审核',
  '/admin/system/settings': '系统设置',
  '/admin/logs/member-login': '会员登录日志',
  '/admin/logs/admin-login': '管理员登录日志',
  '/admin/logs/admin-operation': '管理员操作日志',
  '/admin/logs/audit': '审核日志',
  '/admin/monitor/online-users': '在线用户',
  '/admin/monitor/cache': '缓存列表',
  '/admin/tools/apis': '系统接口',
  '/admin/profile/info': '个人信息',
  '/admin/profile/settings': '修改资料',
  '/admin/monitor': '系统监控',
  '/admin/tools': '系统工具',
  '/admin/profile': '个人中心'
}

const pageTitle = computed(() => {
  return pageTitleMap[route.path] || '管理后台'
})

const goBack = () => {
  router.push('/admin/dashboard')
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  autoExpandMenu()
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background-color: var(--ops-bg, #f3f5f8);
}

/* 侧边栏样式 */
.admin-sidebar {
  width: 240px;
  background: #ffffff;
  border-right: 1px solid var(--ops-border, #dbe3ee);
  box-shadow: none;
  transition: width 0.2s ease;
  overflow: hidden;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 1000;
}

.admin-sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--ops-border-soft, #e7edf5);
}

.sidebar-header h2 {
  margin: 0;
  font-size: 15px;
  color: var(--ops-text, #172033);
  white-space: nowrap;
}

.sidebar-toggle {
  width: 32px;
  height: 32px;
  background: none;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ops-text-muted, #667085);
  transition: border-color 0.15s ease, background 0.15s ease, color 0.15s ease;
}

.sidebar-toggle:hover {
  border-color: var(--ops-border, #dbe3ee);
  background: var(--ops-surface-muted, #f8fafc);
  color: var(--ops-primary, #1677ff);
}

.sidebar-toggle svg {
  width: 20px;
  height: 20px;
}

.sidebar-nav {
  padding: 10px 8px;
  overflow-y: auto;
  height: calc(100vh - 64px);
}

.nav-group {
  padding: 8px 20px;
  margin-top: 16px;
}

.nav-group-title {
  font-size: 12px;
  color: #999;
  text-transform: uppercase;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  margin: 2px 0;
  padding: 0 12px;
  border-radius: 6px;
  color: #475467;
  text-decoration: none;
  transition: background 0.15s ease, color 0.15s ease;
  border-left: none;
  font-size: 14px;
  font-weight: 500;
}

.nav-item:hover {
  background-color: var(--ops-surface-muted, #f8fafc);
  color: var(--ops-primary, #1677ff);
}

.nav-item.active {
  background-color: var(--ops-primary-soft, #eef5ff);
  color: var(--ops-primary, #1677ff);
  font-weight: 700;
}

.nav-item svg {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.nav-item span {
  white-space: nowrap;
  flex: 1;
}

/* 可折叠菜单项 */
.nav-menu-item {
  margin-bottom: 4px;
}

.nav-menu-header {
  cursor: pointer;
  position: relative;
}

.nav-arrow {
  width: 16px;
  height: 16px;
  margin-left: auto;
  transition: transform 0.3s;
  flex-shrink: 0;
}

.nav-arrow.expanded {
  transform: rotate(90deg);
}

/* 子菜单 */
.nav-submenu {
  background-color: transparent;
  padding: 2px 0 4px;
  margin-left: 0;
  border-left: none;
  animation: slideDown 0.2s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    max-height: 0;
  }
  to {
    opacity: 1;
    max-height: 500px;
  }
}

.nav-subitem {
  display: block;
  min-height: 34px;
  padding: 8px 12px 8px 40px;
  border-radius: 6px;
  color: #667085;
  text-decoration: none;
  transition: background 0.15s ease, color 0.15s ease;
  font-size: 13px;
  position: relative;
}

.nav-subitem:hover {
  background-color: var(--ops-surface-muted, #f8fafc);
  color: var(--ops-primary, #1677ff);
}

.nav-subitem.active {
  background-color: var(--ops-primary-soft, #eef5ff);
  color: var(--ops-primary, #1677ff);
  font-weight: 700;
}

.nav-subitem.active::before {
  content: none;
}

.nav-subitem span {
  display: block;
}

/* 嵌套子菜单 */
.nav-submenu-nested {
  margin-top: 4px;
}

.nav-submenu-header {
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-submenu-header .nav-arrow {
  width: 14px;
  height: 14px;
  transition: transform 0.3s;
}

.nav-submenu-header .nav-arrow.expanded {
  transform: rotate(90deg);
}

.nav-submenu-inner {
  background-color: transparent;
  padding: 4px 0;
  margin-left: 0;
  animation: slideDown 0.2s ease;
}

.nav-subitem-inner {
  display: block;
  min-height: 32px;
  padding: 7px 12px 7px 54px;
  border-radius: 6px;
  color: #667085;
  text-decoration: none;
  transition: background 0.15s ease, color 0.15s ease;
  font-size: 13px;
  position: relative;
}

.nav-subitem-inner:hover {
  background-color: var(--ops-surface-muted, #f8fafc);
  color: var(--ops-primary, #1677ff);
}

.nav-subitem-inner.active {
  background-color: var(--ops-primary-soft, #eef5ff);
  color: var(--ops-primary, #1677ff);
  font-weight: 700;
}

.nav-subitem-inner.active::before {
  content: none;
}

/* 折叠状态下隐藏子菜单 */
.admin-sidebar.collapsed .nav-submenu {
  display: none;
}

/* 主内容区域 */
.admin-main {
  flex: 1;
  margin-left: 240px;
  transition: margin-left 0.2s ease;
  display: flex;
  flex-direction: column;
}

.admin-sidebar.collapsed ~ .admin-main {
  margin-left: 64px;
}

.admin-header {
  min-height: 64px;
  background: #ffffff;
  border-bottom: 1px solid var(--ops-border, #dbe3ee);
  padding: 0 24px;
  box-shadow: none;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.page-title {
  margin: 0;
  font-size: 18px;
  color: var(--ops-text, #172033);
  font-weight: 700;
}

.admin-content {
  flex: 1;
  padding: 20px 24px 28px;
  overflow-y: auto;
}

/* 页面过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 移动端遮罩层 */
.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 移动端菜单按钮 */
.mobile-menu-btn {
  display: none;
  background: none;
  border: none;
  padding: 8px;
  margin-right: 12px;
  cursor: pointer;
  color: #666;
  transition: color 0.3s;
}

.mobile-menu-btn svg {
  width: 24px;
  height: 24px;
}

.mobile-menu-btn:hover {
  color: #1677ff;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .mobile-menu-btn {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  /* 侧边栏移动端样式 */
  .admin-sidebar {
    width: 280px;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }

  .admin-sidebar.mobile-visible {
    transform: translateX(0);
  }

  .admin-sidebar.mobile-hidden {
    transform: translateX(-100%);
  }

  .admin-sidebar.collapsed {
    width: 280px;
  }

  /* 主内容区域 */
  .admin-main {
    margin-left: 0;
    width: 100%;
  }

  /* 顶部导航栏 */
  .admin-header {
    padding: 12px 16px;
  }

  .page-title {
    font-size: 18px;
  }

  /* 内容区域 */
  .admin-content {
    padding: 12px;
  }

  /* 侧边栏导航项 */
  .nav-item {
    padding: 14px 20px;
  }

  .nav-item span {
    display: inline;
  }
}

@media (max-width: 480px) {
  .admin-sidebar {
    width: 100%;
    max-width: 320px;
  }

  .admin-header {
    padding: 10px 12px;
  }

  .page-title {
    font-size: 16px;
  }

  .admin-content {
    padding: 8px;
  }

  .sidebar-header {
    padding: 16px;
  }

  .sidebar-header h2 {
    font-size: 16px;
  }
}
</style>
