<template>
  <div class="follow-page">
    <div class="page-header">
      <h1>关注</h1>
    </div>

    <div class="content">
      <div v-if="loading" class="state-box">正在加载关注动态...</div>
      <div v-else-if="error" class="state-box error">
        {{ error }}
        <button class="mini-btn" @click="loadFollowingFeed">重试</button>
      </div>

      <div v-else-if="posts.length === 0" class="empty-state">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="9" cy="7" r="4"></circle>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
        </svg>
        <h2>暂无关注动态</h2>
        <p>先去关注一些用户，这里会展示他们的最新帖子</p>
        <button class="primary-btn" @click="navigateToDiscover">去发现</button>
      </div>

      <div v-else class="post-list">
        <div
          v-for="post in posts"
          :key="post.id"
          class="post-card"
          @click="goToPost(post.id)"
        >
          <div class="post-header-row">
            <img :src="post.authorAvatar || defaultAvatar" class="avatar" alt="avatar" />
            <div class="meta">
              <div class="name">{{ post.authorName || '用户' }}</div>
              <div class="time">{{ formatTime(post.createdAt) }}</div>
            </div>
          </div>

          <div class="title">{{ post.title || '无标题' }}</div>

          <div class="cover-wrap">
            <img v-if="post.coverImage" :src="post.coverImage" class="cover" alt="cover" />
            <div v-else class="cover empty">{{ (post.title || '帖子').slice(0, 8) }}</div>
          </div>

          <div class="stats">
            <span>❤ {{ formatNumber(post.likeCount || 0) }}</span>
            <span>💬 {{ formatNumber(post.commentCount || 0) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchFollowingPosts, type ForumPost } from '../../api/forum'

const router = useRouter()
const posts = ref<ForumPost[]>([])
const loading = ref(false)
const error = ref('')
const defaultAvatar = 'https://via.placeholder.com/50x50?text=U'

const formatTime = (dateString: string) => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

const formatNumber = (num: number) => {
  if (num >= 10000) return `${(num / 10000).toFixed(1)}w`
  if (num >= 1000) return `${(num / 1000).toFixed(1)}k`
  return String(num)
}

const loadFollowingFeed = async () => {
  loading.value = true
  error.value = ''
  try {
    posts.value = await fetchFollowingPosts(1, 30)
  } catch (e: any) {
    error.value = e?.message || '加载关注动态失败'
    posts.value = []
  } finally {
    loading.value = false
  }
}

const navigateToDiscover = () => {
  router.push('/home')
}

const goToPost = (postId: number) => {
  router.push(`/posts/${postId}`)
}

onMounted(loadFollowingFeed)
</script>

<style scoped>
.follow-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.page-header {
  padding: 16px;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.page-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.content {
  padding: 12px;
}

.state-box {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  color: #666;
  text-align: center;
}

.state-box.error {
  color: #d93025;
}

.mini-btn {
  margin-left: 10px;
  border: none;
  border-radius: 14px;
  padding: 4px 10px;
  background: #1677ff;
  color: #fff;
  cursor: pointer;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 36px 20px;
  background: #fff;
  border-radius: 10px;
}

.icon {
  width: 64px;
  height: 64px;
  color: #ccc;
  margin-bottom: 16px;
}

.empty-state h2 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #333;
}

.empty-state p {
  margin: 0 0 20px;
  font-size: 14px;
  color: #666;
}

.primary-btn {
  border: none;
  border-radius: 18px;
  padding: 8px 20px;
  background: #333;
  color: #fff;
  cursor: pointer;
}

.post-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.post-card {
  background: #fff;
  border-radius: 10px;
  padding: 10px;
  cursor: pointer;
}

.post-header-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
}

.meta {
  min-width: 0;
}

.name {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.time {
  font-size: 12px;
  color: #999;
}

.title {
  margin-top: 8px;
  font-size: 14px;
  color: #333;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cover-wrap {
  margin-top: 8px;
}

.cover {
  width: 100%;
  border-radius: 8px;
  min-height: 130px;
  object-fit: cover;
  display: block;
}

.cover.empty {
  width: 100%;
  min-height: 130px;
  border-radius: 8px;
  background: #f0f0f0;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.stats {
  margin-top: 8px;
  display: flex;
  gap: 12px;
  color: #666;
  font-size: 12px;
}
</style>
