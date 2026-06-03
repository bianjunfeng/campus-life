<!-- 标签栏组件 -->
<template>
  <div class="tab-bar-container">
    <div class="tabs-scroll-wrapper">
      <div class="tab-list">
        <button 
          v-for="tab in tabs" 
          :key="tab.id"
          class="tab-button" 
          :class="{ 'active': activeTab === tab.id }"
          @click="handleTabClick(tab.id)"
        >
          {{ tab.name }}
        </button>
        <!-- 更多按钮移到分类标签后面 -->
        <div class="hidden-tabs" v-if="hiddenTabs.length > 0">
          <button class="more-tabs-btn" @click="showMoreTabs = !showMoreTabs">
            更多
          </button>
          <div class="more-tabs-dropdown" v-if="showMoreTabs">
            <button 
              v-for="tab in hiddenTabs" 
              :key="tab.id"
              class="dropdown-tab"
              @click="handleTabClick(tab.id)"
            >
              {{ tab.name }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getPostCategories, type PostCategory } from '../api/categoryApi'

// 定义属性
const props = defineProps<{
  // 默认激活的标签ID
  defaultActiveTab?: string
}>()

// 定义事件
const emit = defineEmits<{
  'tab-click': [tabId: string]
  'tab-changed': [tabId: string]
}>()

// 标签数据结构
interface Tab {
  id: string
  name: string
  type: string
}

// 标签列表 - 从数据库加载
const tabs = ref<Tab[]>([
  { id: 'recommend', name: '推荐', type: 'content' } // 默认推荐标签
])

// 加载状态
const loading = ref(false)

// 当前激活的标签
const activeTab = ref(props.defaultActiveTab || 'discover')

// 是否显示更多标签下拉菜单
const showMoreTabs = ref(false)

// 可显示的标签数量（根据容器宽度动态计算）
const visibleTabsCount = ref(4)

// 可见的标签
const visibleTabs = computed(() => {
  return tabs.value.slice(0, visibleTabsCount.value)
})

// 隐藏的标签
const hiddenTabs = computed(() => {
  return tabs.value.slice(visibleTabsCount.value)
})

// 处理标签点击
const handleTabClick = (tabId: string) => {
  activeTab.value = tabId
  emit('tab-click', tabId)
  emit('tab-changed', tabId)
  showMoreTabs.value = false
}

// 计算可见标签数量
const calculateVisibleTabs = () => {
  const containerWidth = document.querySelector('.tab-list')?.clientWidth || 400
  const tabWidth = 80 // 估算每个标签的宽度
  const visibleCount = Math.max(2, Math.floor(containerWidth / tabWidth) - 1) // 减1为了给更多标签按钮留出空间
  visibleTabsCount.value = Math.min(visibleCount, tabs.value.length)
}

// 监听窗口大小变化
const handleResize = () => {
  calculateVisibleTabs()
}

// 点击外部关闭下拉菜单
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.hidden-tabs')) {
    showMoreTabs.value = false
  }
}

// 加载分类数据
const loadCategories = async () => {
  loading.value = true
  try {
    const categories = await getPostCategories()
    if (categories && categories.length > 0) {
      tabs.value = categories.map(cat => ({
        id: cat.id,
        name: cat.name,
        type: cat.type || 'category'
      }))
    } else {
      // 如果API失败，使用默认数据
      tabs.value = [
        { id: 'recommend', name: '推荐', type: 'content' },
        { id: 'tech', name: '科技', type: 'category' },
        { id: 'entertainment', name: '娱乐', type: 'category' },
        { id: 'sports', name: '体育', type: 'category' },
        { id: 'study', name: '学习', type: 'category' },
        { id: 'life', name: '生活', type: 'category' }
      ]
    }
  } catch (error) {
    console.error('加载分类失败，使用默认数据:', error)
    // 使用默认数据
    tabs.value = [
      { id: 'recommend', name: '推荐', type: 'content' },
      { id: 'tech', name: '科技', type: 'category' },
      { id: 'entertainment', name: '娱乐', type: 'category' },
      { id: 'sports', name: '体育', type: 'category' },
      { id: 'study', name: '学习', type: 'category' },
      { id: 'life', name: '生活', type: 'category' }
    ]
  } finally {
    loading.value = false
    calculateVisibleTabs()
  }
}

onMounted(() => {
  loadCategories()
  window.addEventListener('resize', handleResize)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.tab-bar-container {
  background-color: #ffffff;
  border-bottom: 1px solid #f0f0f0;
  padding: 10px 16px;
  position: sticky;
  top: 52px; /* 与HeaderBar高度一致 */
  z-index: 90;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  backdrop-filter: blur(8px);
  background-color: rgba(255, 255, 255, 0.95);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tabs-scroll-wrapper {
  overflow-x: auto;
  overflow-y: visible; /* 确保下拉菜单可以正常显示 */
  scrollbar-width: none; /* Firefox */
}

.tabs-scroll-wrapper::-webkit-scrollbar {
  display: none; /* Chrome, Safari */
}

.tab-list {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
}

/* 标签按钮样式 */
.tab-button {
  background: none;
  border: none;
  font-size: 15px;
  font-weight: 500;
  color: #666;
  padding: 10px 20px;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}

.tab-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(0, 0, 0, 0.05), transparent);
  transition: left 0.5s;
}

.tab-button:hover {
  background-color: #f5f5f5;
  color: #333;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.tab-button:hover::before {
  left: 100%;
}

.tab-button.active {
  background-color: #333;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  transform: translateY(-2px);
}

.tab-button.active::before {
  display: none;
}

/* 隐藏的标签区域 */
.hidden-tabs {
  /* 移除绝对定位，使其重新参与flex布局 */
  display: flex;
  align-items: center;
  justify-content: center;
}

.more-tabs-btn {
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  padding: 8px 14px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
  font-weight: 500;
}

.more-tabs-btn:hover {
  background-color: #f5f5f5;
  border-color: #999;
}

/* 更多标签下拉菜单 */
.more-tabs-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  min-width: 120px;
  max-height: 300px;
  overflow-y: auto;
}

.dropdown-tab {
  display: block;
  width: 100%;
  padding: 12px 16px;
  text-align: left;
  background: none;
  border: none;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: background-color 0.3s;
}

.dropdown-tab:hover {
  background-color: #f5f5f5;
}

.dropdown-tab:first-child {
  border-radius: 8px 8px 0 0;
}

.dropdown-tab:last-child {
  border-radius: 0 0 8px 8px;
}

/* 暗色模式适配 */
:global(.dark-mode) .tab-bar-container {
  background-color: #1e1e1e;
  border-bottom-color: #333;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(8px);
  background-color: rgba(30, 30, 30, 0.95);
}

:global(.dark-mode) .tab-button {
  color: #a0a0a0;
}

:global(.dark-mode) .tab-button:hover {
  background-color: #333;
  color: #e0e0e0;
}

:global(.dark-mode) .tab-button.active {
  background-color: #e0e0e0;
  color: #1e1e1e;
}

/* 暗色模式下的更多标签按钮 */
:global(.dark-mode) .more-tabs-btn {
  border-color: #444;
  color: #a0a0a0;
}

:global(.dark-mode) .more-tabs-btn:hover {
  background-color: #333;
  border-color: #666;
}

/* 暗色模式下的下拉菜单 */
:global(.dark-mode) .more-tabs-dropdown {
  background: #222;
  border-color: #444;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

:global(.dark-mode) .dropdown-tab {
  color: #a0a0a0;
}

:global(.dark-mode) .dropdown-tab:hover {
  background-color: #333;
  color: #e0e0e0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .tab-list {
    gap: 6px;
    padding: 6px 12px;
  }
  
  .tab-button {
    font-size: 14px;
    padding: 6px 12px;
  }
  
  .more-tabs-btn {
    font-size: 12px;
    padding: 4px 8px;
  }
}

@media (max-width: 480px) {
  .tab-button {
    font-size: 13px;
    padding: 5px 10px;
  }
  
  .more-tabs-btn {
    font-size: 11px;
    padding: 3px 6px;
  }
}
</style>