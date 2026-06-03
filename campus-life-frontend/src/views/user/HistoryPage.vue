<template>
  <div class="history-page">
    <!-- 页面标题栏 -->
    <div class="header-bar">
      <button class="back-button" @click="goBack" aria-label="返回">
        <svg class="back-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 18-6-6 6-6"/>
        </svg>
      </button>
      <div class="page-title">浏览记录</div>
      <div class="right-button">
        <button v-if="history.length > 0" class="clear-button" @click="clearHistory">清除</button>
      </div>
    </div>

    <!-- 浏览记录列表 -->
    <div class="history-content">
      <div v-if="history.length === 0" class="empty-history">
        <svg class="empty-icon" viewBox="0 0 24 24" fill="currentColor">
          <path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/>
        </svg>
        <p class="empty-text">暂无浏览记录</p>
      </div>

      <div v-else class="history-list">
        <div v-for="item in history" :key="item.id" class="history-item" @click="handleItemClick(item)">
          <!-- 笔记缩略图 -->
          <div v-if="item.imageUrls && item.imageUrls.length > 0" class="note-thumbnail">
            <img :src="item.imageUrls[0]" alt="笔记缩略图">
          </div>
          <div v-else class="note-thumbnail empty-thumbnail">
            <svg class="image-placeholder" viewBox="0 0 24 24" fill="currentColor">
              <path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/>
            </svg>
          </div>

          <!-- 笔记信息 -->
          <div class="note-info">
            <div class="note-title">{{ item.title }}</div>
            <div class="note-meta">
              <span class="note-author">{{ item.authorName }}</span>
              <span class="note-time">{{ formatTime(item.viewTime) }}</span>
            </div>
          </div>

          <!-- 箭头图标 -->
          <div class="arrow-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/>
            </svg>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import HistoryManager from '../../utils/historyManager'
import { fetchPostById } from '../../api/forum'
import { showAlert, showConfirm } from '../../utils/dialog'

export default {
  name: 'HistoryPage',
  data() {
    return {
      history: []
    }
  },
  mounted() {
    this.loadHistory()
  },
  activated() {
    this.loadHistory()
  },
  methods: {
    goBack() {
      this.$router.back()
    },
    async loadHistory() {
      try {
        const rawHistory = HistoryManager.getHistory()
        if (!rawHistory || rawHistory.length === 0) {
          this.history = []
          return
        }

        // 仅保留“正常可访问”帖子，过滤已删除/屏蔽/异常状态帖子
        const checks = await Promise.allSettled(
          rawHistory.map((item) => fetchPostById(Number(item.id)))
        )

        const validHistory = []
        for (let i = 0; i < rawHistory.length; i += 1) {
          const result = checks[i]
          const item = rawHistory[i]
          if (result.status !== 'fulfilled' || !result.value) {
            continue
          }
          validHistory.push(item)
        }

        this.history = validHistory

        // 同步清理本地历史中的无效项，保持后续读取稳定
        if (validHistory.length !== rawHistory.length) {
          HistoryManager.clearHistory()
          validHistory
            .slice()
            .reverse()
            .forEach((item) => {
              HistoryManager.addHistory(item)
            })
        }
      } catch (error) {
        console.error('加载浏览记录失败:', error)
        this.history = []
      }
    },
    formatTime(timestamp) {
      const now = new Date()
      const viewTime = new Date(timestamp)
      const diff = now - viewTime
      if (diff < 60000) return '刚刚'
      if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
      if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
      if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
      const year = viewTime.getFullYear()
      const month = String(viewTime.getMonth() + 1).padStart(2, '0')
      const day = String(viewTime.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    handleItemClick(item) {
      HistoryManager.addHistory({
        id: item.id,
        title: item.title,
        authorName: item.authorName,
        imageUrls: item.imageUrls
      })
      this.$router.push(`/posts/${item.id}`)
    },
    async clearHistory() {
      const confirmed = await showConfirm('确定要清除所有浏览记录吗？')
      if (!confirmed) return
      try {
        const success = HistoryManager.clearHistory()
        if (success) {
          this.history = []
        } else {
          throw new Error('清除失败')
        }
      } catch (error) {
        console.error('清除浏览记录失败:', error)
        showAlert('清除失败，请稍后重试')
      }
    }
  }
}
</script>

<style scoped>
.history-page {
  width: 100%;
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 页面标题栏 */
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  background-color: #ffffff;
  padding: 0 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-button {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
}

.back-button:hover {
  background: #f5f5f5;
}

.back-icon {
  width: 20px;
  height: 20px;
  color: #333333;
}

.page-title {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
  flex: 1;
  text-align: center;
}

.right-button {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.clear-button {
  font-size: 14px;
  color: #ff6b6b;
  background: none;
  border: none;
  padding: 6px 12px;
  cursor: pointer;
  border-radius: 4px;
}

.clear-button:hover {
  background-color: #fff5f5;
}

/* 内容区域 */
.history-content {
  flex: 1;
  padding: 12px;
}

/* 空状态 */
.empty-history {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  width: 64px;
  height: 64px;
  color: #cccccc;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #999999;
}

/* 历史记录列表 */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  display: flex;
  align-items: center;
  background-color: #ffffff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.1s ease, box-shadow 0.1s ease;
}

.history-item:active {
  transform: scale(0.98);
}

/* 笔记缩略图 */
.note-thumbnail {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  background-color: #f0f0f0;
  flex-shrink: 0;
}

.note-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.empty-thumbnail {
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-placeholder {
  width: 32px;
  height: 32px;
  color: #cccccc;
}

/* 笔记信息 */
.note-info {
  flex: 1;
  margin-left: 12px;
  margin-right: 8px;
  min-width: 0;
}

.note-title {
  font-size: 16px;
  font-weight: 500;
  color: #333333;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.note-meta {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #999999;
}

.note-author {
  margin-right: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100px;
}

.note-time {
  flex-shrink: 0;
}

/* 箭头图标 */
.arrow-icon {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #cccccc;
}

.arrow-icon svg {
  width: 20px;
  height: 20px;
}
</style>
