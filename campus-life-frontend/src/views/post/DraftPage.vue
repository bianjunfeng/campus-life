<template>
  <div class="draft-page">
    <div class="header-bar">
      <button class="back-button" @click="goBack" aria-label="返回">
        <svg class="back-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 18-6-6 6-6" />
        </svg>
      </button>
      <div class="page-title">我的草稿</div>
      <div class="right-placeholder"></div>
    </div>

    <div class="warning-message">
      草稿仅保存在本设备当前账号下，卸载或清缓存后会丢失，请及时发布。
    </div>

    <div class="content-wrap">
      <div v-if="drafts.length === 0" class="empty-state">
        <svg class="empty-icon" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14z" />
          <path d="M7 7h10v2H7zm0 4h10v2H7zm0 4h6v2H7z" />
        </svg>
        <p>暂无草稿</p>
      </div>

      <div v-else class="draft-list">
        <div v-for="item in drafts" :key="item.id" class="draft-item" @click="goEdit(item.id)">
          <div class="thumb-wrap">
            <img v-if="firstImage(item)" :src="firstImage(item)" alt="草稿图片" class="thumb" />
            <div v-else class="thumb-placeholder">草稿</div>
          </div>

          <div class="draft-main">
            <div class="draft-title">{{ item.title || '无标题草稿' }}</div>
            <div class="draft-preview">{{ previewText(item.content) }}</div>
            <div class="draft-meta">
              <span>{{ formatDate(item.updatedAt) }}</span>
              <span>{{ (item.images || []).length }} 张图</span>
            </div>
          </div>

          <button class="delete-button" type="button" @click.stop="remove(item.id)">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import DraftManager from '../../utils/draftManager'
import { showAlert, showConfirm } from '../../utils/dialog'

const router = useRouter()
const drafts = ref([])

const loadDrafts = () => {
  drafts.value = DraftManager.getDrafts()
}

const goBack = () => {
  router.back()
}

const goEdit = (draftId) => {
  router.push(`/posts/new?draftId=${draftId}`)
}

const remove = async (draftId) => {
  if (!(await showConfirm('确定删除该草稿吗？'))) {
    return
  }
  const ok = DraftManager.removeDraft(draftId)
  if (ok) {
    loadDrafts()
  } else {
    await showAlert('删除失败，请稍后重试')
  }
}

const previewText = (text) => {
  const value = (text || '').trim()
  if (!value) return '暂无正文内容'
  return value.length > 44 ? `${value.slice(0, 44)}...` : value
}

const formatDate = (value) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '未知时间'
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

const firstImage = (item) => {
  const images = Array.isArray(item.images) ? item.images : []
  if (images.length === 0) return ''
  return images[0].ossUrl || images[0].url || ''
}

onMounted(loadDrafts)
onActivated(loadDrafts)
</script>

<style scoped>
.draft-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 12px;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon {
  width: 20px;
  height: 20px;
  color: #333;
}

.page-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.right-placeholder {
  width: 40px;
}

.warning-message {
  margin: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff8e8;
  color: #8a5a00;
  font-size: 13px;
  line-height: 1.5;
}

.content-wrap {
  padding: 0 12px 16px;
}

.empty-state {
  margin-top: 36px;
  text-align: center;
  color: #999;
}

.empty-icon {
  width: 64px;
  height: 64px;
  color: #c5c5c5;
}

.draft-list {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}

.draft-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-bottom: 1px solid #f2f2f2;
}

.draft-item:last-child {
  border-bottom: none;
}

.thumb-wrap {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f1f1f1;
}

.thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 12px;
}

.draft-main {
  flex: 1;
  min-width: 0;
}

.draft-title {
  font-size: 15px;
  color: #333;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.draft-preview {
  margin-top: 6px;
  color: #666;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.draft-meta {
  margin-top: 8px;
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #999;
}

.delete-button {
  border: none;
  background: #fff0f0;
  color: #de3c3c;
  border-radius: 14px;
  font-size: 12px;
  padding: 6px 10px;
}
</style>
