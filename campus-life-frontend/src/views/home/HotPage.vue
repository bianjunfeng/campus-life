<template>
  <div class="hot-page">
    <div class="page-header">
      <h1>热点</h1>
    </div>
    <div class="content">
      <div class="hot-list">
        <div v-if="loading" class="status-text">加载中...</div>
        <div v-else-if="error" class="status-text error">{{ error }}</div>
        <div v-else-if="hotPosts.length === 0" class="status-text">暂无热点内容</div>
        <!-- 热点帖子列表 -->
        <div v-else v-for="(post, index) in hotPosts" :key="post.id" class="hot-post-item" @click="goToPost(post.id)">
          <div class="rank">{{ index + 1 }}</div>
          <div class="post-content">
            <div class="post-title">{{ post.title }}</div>
            <div class="post-meta">
              <span class="author">{{ post.authorName }}</span>
              <span class="stats">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                  <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
                {{ post.likeCount }}
              </span>
              <span class="stats">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon">
                  <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
                </svg>
                {{ post.commentCount }}
              </span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!loading && !error && hotPosts.length > 0" class="load-more-wrap">
        <button class="load-more-btn" :disabled="loadingMore || !hasMore" @click="loadMoreHotPosts">
          {{ loadingMore ? '加载中...' : (hasMore ? '加载更多' : '没有更多了') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchPosts, type ForumPost } from '../../api/forum'

const router = useRouter()
const hotPosts = ref<ForumPost[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const page = ref(1)
const PAGE_SIZE = 12
const error = ref('')

const loadHotPosts = async (append = false) => {
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
    const list = await fetchPosts({ order: 'hot', page: targetPage, size: PAGE_SIZE })
    hotPosts.value = append ? [...hotPosts.value, ...list] : list
    page.value = targetPage
    hasMore.value = list.length === PAGE_SIZE
  } catch (e: any) {
    error.value = e?.message || '加载热点失败'
  } finally {
    if (append) {
      loadingMore.value = false
    } else {
      loading.value = false
    }
  }
}

const loadMoreHotPosts = async () => {
  await loadHotPosts(true)
}

const goToPost = (postId: number) => {
  router.push(`/posts/${postId}`)
}

onMounted(loadHotPosts)
</script>

<style scoped>
.hot-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-header {
  padding: 16px;
  background-color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.page-header h1 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.content {
  padding: 20px;
}

.hot-list {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  min-height: 160px;
}

.hot-post-item {
  display: flex;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s ease;
}

.hot-post-item:last-child {
  border-bottom: none;
}

.hot-post-item:hover {
  background-color: #fafafa;
}

.status-text {
  padding: 20px;
  text-align: center;
  color: #666;
}

.status-text.error {
  color: #d14343;
}

.load-more-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
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

.rank {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #ff4757;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  margin-right: 16px;
  flex-shrink: 0;
}

.post-content {
  flex: 1;
}

.post-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
  line-height: 1.4;
}

.post-meta {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #999;
}

.author {
  margin-right: 16px;
}

.stats {
  display: flex;
  align-items: center;
  margin-right: 16px;
}

.icon {
  width: 14px;
  height: 14px;
  margin-right: 4px;
}
</style>
