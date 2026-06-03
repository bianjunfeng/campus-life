<template>
  <div class="ops-layout merchant-layout">
    <div
      v-if="isMobile && sidebarVisible"
      class="ops-sidebar-overlay"
      @click="closeSidebar"
    ></div>

    <aside
      class="ops-sidebar merchant-sidebar"
      :class="{
        collapsed: sidebarCollapsed && !isMobile,
        'mobile-visible': isMobile && sidebarVisible,
        'mobile-hidden': isMobile && !sidebarVisible
      }"
    >
      <div class="ops-sidebar-header">
        <div v-if="!sidebarCollapsed || isMobile" class="ops-brand">
          <span class="ops-brand-mark">商</span>
          <div>
            <strong>商家端</strong>
            <span>运营后台</span>
          </div>
        </div>
        <button class="ops-icon-button" type="button" @click="toggleSidebar" aria-label="切换导航">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path v-if="sidebarCollapsed && !isMobile" d="M9 18l6-6-6-6" />
            <path v-else d="M15 18l-6-6 6-6" />
          </svg>
        </button>
      </div>

      <nav class="ops-sidebar-nav" aria-label="商家管理导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          class="ops-nav-item"
          :class="{ active: isNavActive(item.match) }"
          :to="item.path"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path v-for="path in item.iconPaths" :key="path" :d="path" />
          </svg>
          <span v-if="!sidebarCollapsed || isMobile">{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <div class="ops-main">
      <header class="ops-header">
        <div class="ops-header-left">
          <button
            v-if="isMobile"
            class="ops-icon-button mobile-menu-button"
            type="button"
            @click="openSidebar"
            aria-label="打开导航"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </button>
          <div>
            <p class="ops-header-kicker">Merchant Console</p>
            <h1>{{ pageTitle }}</h1>
          </div>
        </div>
        <div class="ops-header-right">
          <button class="ops-workbench-button" type="button" @click="goHome" aria-label="跳转工作台">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7" rx="1.5" />
              <rect x="14" y="3" width="7" height="7" rx="1.5" />
              <rect x="14" y="14" width="7" height="7" rx="1.5" />
              <rect x="3" y="14" width="7" height="7" rx="1.5" />
            </svg>
            <span>工作台</span>
          </button>
          <AccountUserMenu portal="merchant" />
        </div>
      </header>

      <main class="ops-content">
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
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AccountUserMenu from '../../components/account/AccountUserMenu.vue'

const route = useRoute()
const router = useRouter()
const sidebarCollapsed = ref(false)
const sidebarVisible = ref(false)
const isMobile = ref(false)
let removeAfterEach: (() => void) | undefined

const navItems = [
  {
    label: '工作台',
    path: '/merchant/home',
    match: ['/merchant/home'],
    iconPaths: [
      'M3 13h8V3H3v10z',
      'M13 21h8V11h-8v10z',
      'M13 3v6h8V3h-8z',
      'M3 21h8v-6H3v6z'
    ]
  },
  {
    label: '商品管理',
    path: '/merchant/vouchers',
    match: ['/merchant/vouchers'],
    iconPaths: [
      'M20 7h-9',
      'M14 17H5',
      'M17 4l3 3-3 3',
      'M8 14l-3 3 3 3'
    ]
  },
  {
    label: '退款审核',
    path: '/merchant/refunds',
    match: ['/merchant/refunds'],
    iconPaths: [
      'M21 12a9 9 0 1 1-2.64-6.36',
      'M21 3v6h-6',
      'M12 7v5l3 2'
    ]
  },
  {
    label: 'AI 工作台',
    path: '/merchant/ai',
    match: ['/merchant/ai'],
    iconPaths: [
      'M12 3v3',
      'M12 18v3',
      'M5 8a3 3 0 0 1 3-3h8a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V8z',
      'M9 11h.01',
      'M15 11h.01',
      'M9 15h6'
    ]
  },
  {
    label: '个人信息',
    path: '/merchant/profile/info',
    match: ['/merchant/profile'],
    iconPaths: [
      'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2',
      'M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8'
    ]
  }
]

const pageTitleMap: Record<string, string> = {
  '/merchant/home': '商家工作台',
  '/merchant/vouchers': '商品管理',
  '/merchant/refunds': '退款审核',
  '/merchant/ai': 'AI 工作台',
  '/merchant/ai/agent': 'Agent 中心',
  '/merchant/ai/agent/chat': 'AI 对话',
  '/merchant/ai/agent/knowledge-bases': '知识库管理',
  '/merchant/profile/info': '个人信息',
  '/merchant/profile/settings': '修改资料'
}

const pageTitle = computed(() => {
  const exactTitle = pageTitleMap[route.path]
  if (exactTitle) return exactTitle
  if (route.path.startsWith('/merchant/ai/agent/knowledge-bases/')) return '知识库详情'
  return '商家后台'
})

const isNavActive = (paths: string[]) => {
  return paths.some(path => route.path === path || route.path.startsWith(path + '/'))
}

const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) {
    sidebarVisible.value = false
  }
}

const openSidebar = () => {
  sidebarVisible.value = true
}

const closeSidebar = () => {
  sidebarVisible.value = false
}

const toggleSidebar = () => {
  if (isMobile.value) {
    sidebarVisible.value = !sidebarVisible.value
  } else {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }
}

const goHome = () => {
  void router.push('/merchant/home')
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  removeAfterEach = router.afterEach(() => {
    if (isMobile.value) {
      sidebarVisible.value = false
    }
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  removeAfterEach?.()
})
</script>

<style scoped>
.merchant-layout {
  --ops-accent: #0f766e;
  --ops-accent-soft: #e6fffb;
}
</style>
