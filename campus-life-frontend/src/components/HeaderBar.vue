<template>
  <!-- 只有在非特定页面（如发布页面）才显示HeaderBar -->
  <header v-if="shouldShowHeader" class="page-header">
    <!-- 左侧区域：侧边栏按钮 -->
    <div class="header-left">
      <button 
        v-if="showSidebarToggle" 
        class="sidebar-toggle" 
        @click="$emit('toggle-sidebar')"
      >
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="3" y1="12" x2="21" y2="12"></line>
          <line x1="3" y1="6" x2="21" y2="6"></line>
          <line x1="3" y1="18" x2="21" y2="18"></line>
        </svg>
      </button>
    </div>
    
    <!-- 中间区域：简单导航按钮 -->
    <div class="header-nav-container">
      <div class="header-nav-buttons">
        <button 
          v-for="nav in navigationButtons" 
          :key="nav.id"
          class="nav-button" 
          :class="{ 'active': activeNav === nav.id }"
          @click="handleNavClick(nav.id)"
        >
          {{ nav.name }}
        </button>
      </div>
    </div>
    
    <!-- 右侧区域：搜索图标按钮 -->
    <div class="header-right">
      <button class="search-btn" @click="handleSearchClick">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"></circle>
          <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
        </svg>
      </button>
    </div>
  </header>
  
  <!-- 登录提示对话框 - 禁用自动显示，仅在用户点击需要登录的功能时显示 -->
  <LoginRequiredDialog ref="loginDialog" :auto-show="false" />
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoginRequiredDialog from './LoginRequiredDialog.vue'
import { checkLoginStatus } from '../shared/router/auth'

// 定义属性
const props = defineProps<{
  // 是否显示侧边栏开关按钮
  showSidebarToggle?: boolean
  // 是否显示搜索框
  showSearch?: boolean
  // 搜索框占位符
  searchPlaceholder?: string
  // 是否强制显示，覆盖路由判断
  forceShow?: boolean
}>()

// 定义事件
const emit = defineEmits<{
  'toggle-sidebar': []
  'search': [keyword: string]
  'search-click': []
  'nav-click': [navId: string]
}>()

// 路由实例
const route = useRoute()
const router = useRouter()

// 导航按钮数据 - 更新为关注、发现、热点
const navigationButtons = ref([
  { id: 'follow', name: '关注' },
  { id: 'discover', name: '发现' },
  { id: 'hot', name: '热点' }
])

// 当前激活的导航
const activeNav = ref('discover')

// 登录提示对话框引用
const loginDialog = ref()

// 根据当前路由设置激活的导航
const setActiveNavByRoute = () => {
  const path = route.path
  if (path === '/hot') {
    activeNav.value = 'hot'
  } else if (path === '/home') {
    activeNav.value = 'discover'
  } else if (path === '/follow') {
    activeNav.value = 'follow'
  } else {
    // 其他页面默认激活发现
    activeNav.value = 'discover'
  }
}

// 监听路由变化，更新激活的导航
watch(() => route.path, () => {
  setActiveNavByRoute()
})

// 定义不应该显示HeaderBar的页面路由
const hiddenRoutes = [
  '/posts/new', // 发布页面
  '/login', // 登录页面
  '/me/edit', // 编辑资料页面
  '/me/student-auth', // 学生身份认证页面
  '/privacy-settings', // 隐私设置页面
  '/general-settings' // 通用设置页面
]

// 判断当前页面是否应该显示HeaderBar
const shouldShowHeader = computed(() => {
  // 如果强制显示，则返回true
  if (props.forceShow) return true
  
  // 在关注、发现、热点页面显示顶部导航栏
  const path = route.path
  return path === '/home' || path === '/follow' || path === '/hot'
})

// 组件挂载时设置激活的导航
onMounted(() => {
  setActiveNavByRoute()
})

// 处理导航按钮点击
const handleNavClick = (navId: string) => {
  emit('nav-click', navId)
  
  // 根据导航ID执行路由跳转
  if (navId === 'follow') {
    // 关注页面需要登录
    if (!checkLoginStatus()) {
      // 未登录，显示登录提示
      loginDialog.value?.show()
    } else {
      // 已登录，导航到关注页面
      router.push('/follow')
    }
  } else if (navId === 'discover') {
    // 发现页面无需登录，直接跳转
    router.push('/home')
  } else if (navId === 'hot') {
    // 热点页面无需登录，直接跳转
    router.push('/hot')
  }
}

// 处理搜索按钮点击
const handleSearchClick = () => {
  emit('search-click')
}

</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background-color: #ffffff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
  height: 52px;
}

/* 左侧区域 */
.header-left {
  display: flex;
  align-items: center;
}

/* 侧边栏开关按钮 */
.sidebar-toggle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: none;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.3s;
}

.sidebar-toggle:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.sidebar-toggle svg {
  width: 20px;
  height: 20px;
  color: #333;
}

/* 中间导航按钮区域 */
.header-nav-container {
  flex: 1;
  display: flex;
  justify-content: center;
}

.header-nav-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
}

/* 导航按钮样式 */
.nav-button {
  background: none;
  border: none;
  font-size: 15px;
  font-weight: 500;
  color: #666;
  padding: 6px 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.nav-button:hover {
  color: #333;
}

.nav-button.active {
  color: #333;
  font-weight: 600;
  position: relative;
}

.nav-button.active::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  background-color: #333;
  border-radius: 1.5px;
}

/* 右侧区域 */
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 搜索按钮 */
.search-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: none;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.3s;
}

.search-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.search-btn svg {
  width: 20px;
  height: 20px;
  color: #333;
}

/* 消息按钮 */
.message-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: none;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.3s;
}

.message-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.message-btn svg {
  width: 20px;
  height: 20px;
  color: #333;
}

/* 暗色模式适配 */
:global(.dark-mode) .page-header {
  background-color: #1e1e1e;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

:global(.dark-mode) .sidebar-toggle svg,
:global(.dark-mode) .search-btn svg {
  color: #a0a0a0;
}

:global(.dark-mode) .nav-button {
  color: #a0a0a0;
}

:global(.dark-mode) .nav-button:hover {
  color: #e0e0e0;
}

:global(.dark-mode) .nav-button.active {
  color: #e0e0e0;
}

:global(.dark-mode) .nav-button.active::after {
  background-color: #e0e0e0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    padding: 8px 12px;
    height: 48px;
  }
  
  .header-nav-buttons {
    gap: 6px;
  }
  
  .nav-button {
    font-size: 14px;
    padding: 5px 8px;
  }
  
  .sidebar-toggle svg,
  .search-btn svg {
    width: 18px;
    height: 18px;
  }
}

@media (max-width: 480px) {
  .nav-button {
    font-size: 13px;
    padding: 4px 6px;
  }
}
</style>
