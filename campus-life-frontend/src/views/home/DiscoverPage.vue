<template>
  <div class="discover-container">
    <!-- 标签栏 -->
    <div 
      class="tab-bar-wrapper"
      :style="{
        opacity: tabBarOpacity,
        transform: tabBarVisible ? 'translateY(0)' : 'translateY(-100%)',
        transition: 'all 0.3s ease'
      }"
    >
      <TabBar 
        :default-active-tab="activeTab"
        @tab-changed="handleTabChanged"
      />
    </div>
    
    <!-- 热点排行榜标题 -->
    <div v-if="activeTab === 'hot'" class="hot-header">
      <h2 class="hot-title">🔥 热点排行榜</h2>
      <p class="hot-subtitle">实时更新最热门的校园动态</p>
    </div>

    <!-- 加载 & 错误 -->
    <div v-if="loading" class="tab-content">
      <div style="text-align: center; padding: 40px; color: #999;">
        正在加载帖子...
      </div>
    </div>
    <div v-else-if="error" class="tab-content">
      <div style="text-align: center; padding: 40px; color: #f56c6c;">
        {{ error }}
        <br>
        <button @click="reloadCurrentTab" style="margin-top: 10px; padding: 8px 16px; background: #1677ff; color: white; border: none; border-radius: 4px; cursor: pointer;">
          重试
        </button>
      </div>
    </div>
    <!-- 关注视图 -->
    <div v-else-if="activeTab === 'follow'" class="tab-content follow-view">
      <div v-if="filteredPosts.length === 0" class="placeholder-content">
        <h3>这里是关注流（暂时没有数据）</h3>
        <p>当你关注其他同学后，这里会展示他们的最新动态</p>
        <div class="post-card-skeleton"></div>
        <div class="post-card-skeleton"></div>
      </div>
      <div v-else class="content-list">
        <div 
          v-for="(post, index) in filteredPosts" 
          :key="post.id"
          class="post-card"
          @click="goToPostDetail(post.id)"
          style="cursor: pointer;"
        >
          <!-- 作者信息 -->
          <div class="post-header">
            <img :src="post.authorAvatar || defaultAvatar" alt="用户头像" class="user-avatar">
            <div class="user-info">
              <h3 class="user-name">{{ post.authorName }}</h3>
              <p class="post-time">{{ formatTime(post.createdAt) }}</p>
            </div>
          </div>

          <!-- 帖子内容 - 只显示封面图或标题占位 -->
          <div class="post-content">
            <div class="post-images single-image">
              <img 
                v-if="post.coverImage && post.coverImage.trim()"
                :src="post.coverImage" 
                alt="封面图"
                class="post-image"
                loading="lazy"
                decoding="async"
                @error="handleImageError($event, post)"
                @load="handleImageLoad($event, post)"
              >
              <div v-else class="post-image post-image-placeholder">
                {{ post.title ? post.title.slice(0, 6) : '无标题' }}
              </div>
            </div>
          </div>

          <!-- 互动区域 - 点赞 + 评论数 -->
          <div class="post-actions">
            <button class="action-btn like-btn" @click.stop="toggleLike(post)">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
              </svg>
              <span>{{ formatNumber(post.likeCount) }}</span>
            </button>
            <span class="comment-count">💬 {{ formatNumber(post.commentCount) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分类视图（非特殊标签） -->
    <div v-else-if="filteredPosts.length > 0 && activeTab !== 'follow' && activeTab !== 'hot' && activeTab !== 'recommend' && activeTab !== 'discover'" class="tab-content discover-view">
      <div class="content-list">
        <div 
          v-for="(post, index) in filteredPosts" 
          :key="post.id"
          class="post-card"
          @click="goToPostDetail(post.id)"
          style="cursor: pointer;"
        >
          <!-- 作者信息 -->
          <div class="post-header">
            <img :src="post.authorAvatar || defaultAvatar" alt="用户头像" class="user-avatar">
            <div class="user-info">
              <h3 class="user-name">{{ post.authorName }}</h3>
              <p class="post-time">{{ formatTime(post.createdAt) }}</p>
            </div>
          </div>

          <!-- 帖子内容 - 只显示首图或标题占位 -->
          <div class="post-content">
            <div class="post-images single-image">
              <img 
                v-if="post.coverImage && post.coverImage.trim()"
                :src="post.coverImage" 
                alt="封面图"
                class="post-image"
                loading="lazy"
                decoding="async"
                @error="handleImageError($event, post)"
                @load="handleImageLoad($event, post)"
              >
              <div v-else class="post-image post-image-placeholder">
                {{ post.title ? post.title.slice(0, 6) : '无标题' }}
              </div>
            </div>
          </div>

          <!-- 互动区域 - 点赞 + 评论数 -->
          <div class="post-actions">
            <button class="action-btn like-btn" @click.stop="toggleLike(post)">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
              </svg>
              <span>{{ formatNumber(post.likeCount) }}</span>
            </button>
            <span class="comment-count">💬 {{ formatNumber(post.commentCount) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 推荐 / 发现视图 -->
    <div v-else-if="(activeTab === 'recommend' || activeTab === 'discover') && filteredPosts.length > 0" class="tab-content discover-view">
      <div class="content-list">
        <div 
          v-for="(post, index) in filteredPosts" 
          :key="post.id"
          class="post-card"
          @click="goToPostDetail(post.id)"
          style="cursor: pointer;"
        >
          <!-- 作者信息 -->
          <div class="post-header">
            <img :src="post.authorAvatar || defaultAvatar" alt="用户头像" class="user-avatar">
            <div class="user-info">
              <h3 class="user-name">{{ post.authorName }}</h3>
              <p class="post-time">{{ formatTime(post.createdAt) }}</p>
            </div>
          </div>

          <!-- 帖子内容 - 只显示首图或标题占位 -->
          <div class="post-content">
            <div class="post-images single-image">
              <img 
                v-if="post.coverImage && post.coverImage.trim()"
                :src="post.coverImage" 
                alt="封面图"
                class="post-image"
                loading="lazy"
                decoding="async"
                @error="handleImageError($event, post)"
                @load="handleImageLoad($event, post)"
              >
              <div v-else class="post-image post-image-placeholder">
                {{ post.title ? post.title.slice(0, 6) : '无标题' }}
              </div>
            </div>
          </div>

          <!-- 互动区域 - 点赞 + 评论数 -->
          <div class="post-actions">
            <button class="action-btn like-btn" @click.stop="toggleLike(post)">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
              </svg>
              <span>{{ formatNumber(post.likeCount) }}</span>
            </button>
            <span class="comment-count">💬 {{ formatNumber(post.commentCount) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 热点视图 -->
    <div v-else-if="activeTab === 'hot' && filteredPosts.length > 0" class="tab-content hot-view">
      <div class="content-list">
        <div 
          v-for="(post, index) in filteredPosts" 
          :key="post.id"
          class="post-card"
          @click="goToPostDetail(post.id)"
          style="cursor: pointer;"
        >
          <!-- 排名标识 - 仅在热点标签显示 -->
          <div 
            class="rank-badge" 
            :class="getRankClass(index)"
          >
            {{ index + 1 }}
          </div>
          
          <!-- 作者信息 -->
          <div class="post-header hot-post-header">
            <img :src="post.authorAvatar || defaultAvatar" alt="用户头像" class="user-avatar">
            <div class="user-info">
              <h3 class="user-name">{{ post.authorName }}</h3>
              <p class="post-time">{{ formatTime(post.createdAt) }}</p>
            </div>
          </div>

          <!-- 帖子内容 - 只显示首图 -->
          <div class="post-content hot-post-content">
            <div class="post-images single-image">
              <img 
                v-if="post.coverImage && post.coverImage.trim()"
                :src="post.coverImage" 
                alt="封面图"
                class="post-image"
                loading="lazy"
                decoding="async"
                @error="handleImageError($event, post)"
                @load="handleImageLoad($event, post)"
              >
              <div v-else class="post-image post-image-placeholder">
                {{ post.title ? post.title.slice(0, 6) : '无标题' }}
              </div>
            </div>
          </div>

          <!-- 互动区域 - 点赞 + 评论数 -->
          <div class="post-actions">
            <button class="action-btn like-btn" @click.stop="toggleLike(post)">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
              </svg>
              <span>{{ formatNumber(post.likeCount) }}</span>
            </button>
            <span class="comment-count">💬 {{ formatNumber(post.commentCount) }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 空数据提示（当所有视图都不满足条件时显示） -->
    <div v-if="!loading && !error && filteredPosts.length === 0 && activeTab !== 'follow'" class="tab-content">
      <div style="text-align: center; padding: 40px; color: #999;">
        <p style="font-size: 16px; margin-bottom: 10px;">暂无帖子数据</p>
        <p style="font-size: 12px; margin-bottom: 20px;">当前分类暂无内容，请稍后刷新或切换标签查看</p>
        <button @click="loadPosts" style="padding: 8px 16px; background: #1677ff; color: white; border: none; border-radius: 4px; cursor: pointer;">
          刷新
        </button>
      </div>
    </div>

    <div
      v-if="!loading && !error && filteredPosts.length > 0 && activeTab !== 'follow'"
      class="load-more-wrap"
    >
      <button
        class="load-more-btn"
        :disabled="loadingMore || !hasMore"
        @click="loadMorePosts"
      >
        {{ loadingMore ? '加载中...' : (hasMore ? '加载更多' : '没有更多了') }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import TabBar from '../../components/TabBar.vue'
import type { ForumPost } from '../../api/forum'
import { fetchPosts, fetchFollowingPosts, togglePostLike } from '../../api/forum'
import { getStoredUserInfoObject } from '../../utils/authStorage'
import { getPostCategories, type PostCategory } from '../../api/categoryApi'
import { notify } from '@/utils/notify'

// 路由管理
const router = useRouter()

// 状态管理
const activeTab = ref('discover')
const scrollPosition = ref(0)
const tabBarOpacity = ref(1)
const tabBarVisible = ref(true)

// 帖子数据（来自 forum.ts）
const posts = ref<ForumPost[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const error = ref('')
const page = ref(1)
const PAGE_SIZE = 12
const currentQuery = ref<{ categoryId?: string | number; order: 'latest' | 'hot' | 'follow' }>({
  categoryId: undefined,
  order: 'latest'
})
const likeLoadingMap = ref<Record<number, boolean>>({})

// 分类映射（用于存储分类ID和code的对应关系）
const categoryMap = ref<Map<string, string>>(new Map())

// 默认头像
const defaultAvatar = 'https://via.placeholder.com/50x50?text=U'

// 根据标签过滤的帖子
const filteredPosts = computed(() => {
  if (activeTab.value === 'follow') {
    // 关注视图：目前还没接关注逻辑，先返回全部，将来改成只显示我关注的作者的帖子
    return posts.value
  } else if (activeTab.value === 'discover') {
    // 发现视图：按发布时间倒序（最新发布优先）
    return [...posts.value].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  } else if (activeTab.value === 'hot') {
    // 热点视图：直接使用后端热度算法排序结果
    return posts.value
  } else if (activeTab.value === 'recommend') {
    // 推荐视图：按时间倒序
    return [...posts.value].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  } else {
    // 分类视图：如果已经通过API按分类加载了，直接返回
    // 因为后端已经筛选过了，前端不需要再次筛选
    return [...posts.value].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }
})

// 从 API 加载帖子
const loadPosts = async (
  categoryId: string | number | undefined = currentQuery.value.categoryId,
  order: 'latest' | 'hot' | 'follow' = currentQuery.value.order,
  append = false
) => {
  if (append) {
    if (loadingMore.value || !hasMore.value) return
    loadingMore.value = true
  } else {
    loading.value = true
    page.value = 1
    hasMore.value = true
  }
  error.value = ''
  try {
    const targetPage = append ? page.value + 1 : 1
    const list = order === 'follow'
      ? await fetchFollowingPosts(targetPage, PAGE_SIZE)
      : await fetchPosts({ categoryId, order, page: targetPage, size: PAGE_SIZE })
    posts.value = append ? [...posts.value, ...list] : list
    page.value = targetPage
    hasMore.value = list.length === PAGE_SIZE
    currentQuery.value = { categoryId, order }
  } catch (e: any) {
    error.value = e?.message || '加载帖子失败'
  } finally {
    if (append) {
      loadingMore.value = false
    } else {
      loading.value = false
    }
  }
}

const loadMorePosts = async () => {
  await loadPosts(currentQuery.value.categoryId, currentQuery.value.order, true)
}

const reloadCurrentTab = async () => {
  await loadPosts(currentQuery.value.categoryId, currentQuery.value.order, false)
}

// 跳转到帖子详情页
const goToPostDetail = (postId: string | number) => {
  router.push(`/posts/${postId}`)
}

// 格式化时间
const formatTime = (dateString: string): string => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 60) {
    return `${minutes}分钟前`
  } else if (hours < 24) {
    return `${hours}小时前`
  } else if (days < 30) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

// 格式化数字
const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

const isPostLiked = (post: ForumPost) => {
  const anyPost = post as any
  return Boolean(post.isLiked || anyPost._isLiked)
}

const toggleLike = async (post: ForumPost) => {
  const postId = Number(post.id)
  if (likeLoadingMap.value[postId]) return
  const currentUser = getStoredUserInfoObject<any>()
  if (!currentUser?.id) {
    notify('请先登录')
    return
  }

  const anyPost = post as any
  const oldLiked = isPostLiked(post)
  const oldCount = Number(post.likeCount || 0)
  anyPost._isLiked = !oldLiked
  post.isLiked = !oldLiked
  post.likeCount = Math.max(0, oldCount + (oldLiked ? -1 : 1))
  likeLoadingMap.value = { ...likeLoadingMap.value, [postId]: true }

  try {
    const result = await togglePostLike(postId)
    anyPost._isLiked = Boolean(result.liked)
    post.isLiked = Boolean(result.liked)
    post.likeCount = Number(result.likeCount || 0)
  } catch (e: any) {
    anyPost._isLiked = oldLiked
    post.isLiked = oldLiked
    post.likeCount = oldCount
    notify(e?.message || '点赞失败，请稍后重试')
  } finally {
    const nextMap = { ...likeLoadingMap.value }
    delete nextMap[postId]
    likeLoadingMap.value = nextMap
  }
}

// 处理图片加载错误
const handleImageError = (event: Event, post: ForumPost) => {
  // 图片加载失败时，可以显示占位符或默认图片
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
}

// 处理图片加载成功
const handleImageLoad = (_event: Event, _post: ForumPost) => {}

// 获取排名样式 - 热点标签使用
const getRankClass = (index: number): string => {
  if (index === 0) return 'rank-gold'
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return ''
}

// 处理标签切换
const handleTabChanged = async (tabName: string) => {
  activeTab.value = tabName
  
  // 如果是分类标签（不是特殊标签如discover、hot、recommend、follow），则按分类加载帖子
  const specialTabs = ['discover', 'hot', 'recommend', 'follow']
  if (!specialTabs.includes(tabName)) {
    // 分类标签，传递分类ID给后端API
    await loadPosts(tabName, 'latest', false)
  } else {
    // 特殊标签：热点使用 hot，关注使用 follow，其余使用 latest
    const order = tabName === 'hot' ? 'hot' : (tabName === 'follow' ? 'follow' : 'latest')
    await loadPosts(undefined, order, false)
  }
}

// 处理滚动事件
const handleScroll = () => {
  const currentPosition = window.scrollY
  const scrollDiff = currentPosition - scrollPosition.value
  
  // 向上滚动时，恢复TabBar可见性
  if (scrollDiff < -20) {
    tabBarVisible.value = true
    tabBarOpacity.value = 1
  } 
  // 向下滚动时，根据滚动距离逐渐隐藏TabBar
  else if (scrollDiff > 20) {
    tabBarVisible.value = false
    tabBarOpacity.value = Math.max(0, 1 - (currentPosition / 200))
  }
  
  scrollPosition.value = currentPosition
}

// 加载分类映射
const loadCategoryMap = async () => {
  try {
    const categories = await getPostCategories()
    categories.forEach(cat => {
      categoryMap.value.set(cat.id.toString(), cat.code)
    })
  } catch (error) {
    // 分类映射加载失败不应影响帖子主流程
  }
}

// 组件挂载时添加滚动监听 + 加载帖子
onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  loadCategoryMap()
  loadPosts(undefined, 'latest', false)
})

// 组件卸载时移除滚动监听
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.discover-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 60px;
}

/* 内容列表 */
.content-list {
  padding: 0 12px;
  display: grid;
  grid-template-columns: repeat(2, 1fr); /* 固定显示两列 */
  gap: 12px;
}

/* 帖子卡片 */
.post-card {
  background-color: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  height: auto; /* 根据内容自适应高度 */
}

/* 帖子头部 */
.post-header {
  display: flex;
  align-items: center;
  padding: 12px;
  flex-shrink: 0; /* 头部不收缩 */
}

/* 热点帖子头部 */
.post-header.hot-post-header {
  padding: 12px 12px 12px 40px;
  position: relative;
}

/* 热点头部 */
.hot-header {
  background-color: white;
  padding: 20px 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.hot-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px 0;
}

.hot-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
}

/* 排名标识 */
.rank-badge {
  position: absolute;
  left: 12px;
  top: 16px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #666;
  background-color: #f5f5f5;
}

.rank-gold {
  background-color: #fffbe6;
  color: #faad14;
  border: 1px solid #ffe7ba;
}

.rank-silver {
  background-color: #f0f2f5;
  color: #8c8c8c;
  border: 1px solid #d9d9d9;
}

.rank-bronze {
  background-color: #fff2e8;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.load-more-wrap {
  display: flex;
  justify-content: center;
  padding: 16px 12px 24px;
}

.load-more-btn {
  min-width: 120px;
  height: 36px;
  border: none;
  border-radius: 18px;
  background: #1677ff;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.load-more-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 12px;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin: 0 0 2px 0;
}

.post-time {
  font-size: 12px;
  color: #999;
  margin: 0;
}

/* 帖子内容 */
.post-content {
  padding: 0 12px 12px;
  flex: 0 0 auto; /* 不伸缩，根据内容自适应 */
}

/* 图片网格（这里只用单图） */
.post-images {
  display: grid;
  grid-template-columns: 1fr;
  position: relative;
}

.post-images.single-image {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 8px;
  background-color: #f5f5f5;
  display: flex;
  align-items: flex-start;
  justify-content: center;
}

.post-image {
  width: 100%;
  height: auto;
  min-height: 150px;
  max-height: 600px;
  object-fit: contain;
  border-radius: 8px;
  display: block;
}

/* 没有封面图时用标题做占位 */
.post-image-placeholder {
  width: 100%;
  min-height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
  background-color: #f5f5f5;
  border-radius: 8px;
}

/* 互动区域 */
.post-actions {
  display: flex;
  align-items: center;
  padding: 12px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0; /* 互动区域不收缩 */
  margin-top: auto; /* 推到底部 */
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  margin-right: 16px;
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.action-btn:hover {
  color: #1677ff;
  transform: scale(1.1);
}

.like-btn.liked {
  color: #ff4d4f;
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.action-btn svg {
  width: 20px;
  height: 20px;
}

.action-btn span {
  font-size: 12px;
  margin-left: 4px;
}

.comment-count {
  font-size: 12px;
  color: #999;
}

/* 标签内容区域 */
.tab-content {
  min-height: calc(100vh - 150px);
  padding: 20px;
}

/* 占位内容样式 */
.placeholder-content {
  text-align: center;
  padding: 40px 20px;
  color: #666;
}

.placeholder-content h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 10px;
}

.placeholder-content p {
  margin-bottom: 30px;
}

/* 骨架屏样式 */
.post-card-skeleton {
  background-color: #f5f5f5;
  border-radius: 12px;
  height: 200px;
  margin: 0 auto 20px;
  width: 80%;
  max-width: 400px;
  position: relative;
  overflow: hidden;
}

.post-card-skeleton::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  animation: loading 1.5s infinite;
}

@keyframes loading {
  100% {
    left: 100%;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .content-list {
    grid-template-columns: repeat(2, 1fr); /* 保持两列显示 */
    gap: 10px;
  }
  
  .tab-content {
    padding: 10px;
  }
}

@media (max-width: 480px) {
  .content-list {
    grid-template-columns: repeat(2, 1fr); /* 手机屏幕也显示两列 */
    padding: 0 8px;
    gap: 8px;
  }
  
  .placeholder-content {
    padding: 20px 10px;
  }
  
  .post-card-skeleton {
    width: 95%;
  }
}
</style>
