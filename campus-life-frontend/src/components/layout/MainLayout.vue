<template>
  <div class="app-layout">
    <!-- 顶部导航栏 -->
    <HeaderBar 
      :show-sidebar-toggle="true" 
      @toggle-sidebar="showDrawer = true" 
      @search-click="handleHeaderSearchClick"
    />
    
    <!-- 中间内容区域 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    
    <!-- 底部导航栏 -->
    <NavBar v-if="showNavBar" />
    
    <!-- 侧边菜单抽屉 -->
    <SideMenuDrawer v-model:visible="showDrawer" />
    
    <!-- 登录提示弹窗已移至NavBar组件统一管理 -->
  </div>
</template>

<script setup lang="ts">
import { ref, computed, provide, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import HeaderBar from '../HeaderBar.vue'
import NavBar from '../NavBar.vue'
import SideMenuDrawer from '../SideMenuDrawer.vue'
import LoginRequiredDialog from '../LoginRequiredDialog.vue'

const route = useRoute()
const router = useRouter()

// 侧边栏显示状态
const showDrawer = ref(false)

// 判断是否显示导航栏
const showNavBar = computed(() => {
  // 登录页面不显示导航栏
  return route.path !== '/login'
})

const handleHeaderSearchClick = () => {
  router.push('/search')
}

// 监听路由变化，更新显示状态
watch(() => route.path, () => {
  showDrawer.value = false
})
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
  position: relative;
}

.main-content {
  flex: 1;
  padding-bottom: 60px; /* 为底部导航栏留出空间 */
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

/* 暗色模式适配 */
:global(.dark-mode) .app-layout {
  background-color: #121212;
}
</style>
