<template>
  <div class="kb-detail-page">
    <header class="page-header">
      <button class="icon-btn" @click="router.back()" aria-label="返回">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 18l-6-6 6-6"></path>
        </svg>
      </button>
      <div class="title-area">
        <h1>{{ knowledgeBase?.name || '知识库详情' }}</h1>
        <span v-if="knowledgeBase">{{ knowledgeBase.documentCount || 0 }} 个文档 · {{ formatSize(knowledgeBase.totalSize || 0) }}</span>
      </div>
      <button class="text-btn" @click="triggerUpload">上传</button>
      <input ref="fileInput" type="file" class="hidden-input" accept=".txt,.md" @change="handleFileChange" />
    </header>

    <main class="content">
      <div v-if="uploading" class="uploading">正在上传并索引...</div>
      <div v-if="loading" class="state">加载中...</div>
      <div v-else-if="documents.length === 0" class="empty">
        <h2>暂无文档</h2>
        <p>支持上传 txt、md 文件，索引完成后可在对话中选择使用。</p>
      </div>
      <div v-else class="doc-list">
        <article v-for="doc in documents" :key="doc.id" class="doc-item">
          <div class="doc-main">
            <div class="doc-title-row">
              <h3>{{ doc.title }}</h3>
              <span :class="['status', statusClass(doc.parseStatusText)]">{{ statusText(doc.parseStatusText) }}</span>
            </div>
            <p>{{ doc.originalFilename }} · {{ formatSize(doc.fileSize || 0) }} · {{ doc.chunkCount || 0 }} 个分片</p>
            <p v-if="doc.errorMessage" class="error">{{ doc.errorMessage }}</p>
          </div>
          <div class="doc-actions">
            <button class="ghost-btn" @click="reindex(doc.id)">重建</button>
            <button class="danger-btn" @click="removeDocument(doc.id)">删除</button>
          </div>
        </article>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  deleteKnowledgeDocument,
  getKnowledgeBase,
  listKnowledgeDocuments,
  reindexKnowledgeDocument,
  uploadKnowledgeDocument,
  type KnowledgeBase,
  type KnowledgeDocument
} from '../../api/knowledge'
import { showAlert, showConfirm } from '../../utils/dialog'

const route = useRoute()
const router = useRouter()
const kbId = computed(() => Number(route.params.id))
const fileInput = ref<HTMLInputElement | null>(null)
const knowledgeBase = ref<KnowledgeBase | null>(null)
const documents = ref<KnowledgeDocument[]>([])
const loading = ref(false)
const uploading = ref(false)

const loadData = async () => {
  if (!kbId.value) return
  loading.value = true
  try {
    const [base, docs] = await Promise.all([
      getKnowledgeBase(kbId.value),
      listKnowledgeDocuments(kbId.value)
    ])
    knowledgeBase.value = base
    documents.value = docs
  } catch (error: any) {
    await showAlert(error.message || '加载知识库详情失败')
  } finally {
    loading.value = false
  }
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !kbId.value) return
  uploading.value = true
  try {
    const uploaded = await uploadKnowledgeDocument(kbId.value, file)
    documents.value.unshift(uploaded)
    knowledgeBase.value = await getKnowledgeBase(kbId.value)
  } catch (error: any) {
    await showAlert(error.message || '上传文档失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

const reindex = async (documentId: number) => {
  try {
    const updated = await reindexKnowledgeDocument(kbId.value, documentId)
    documents.value = documents.value.map(item => item.id === documentId ? updated : item)
  } catch (error: any) {
    await showAlert(error.message || '重建索引失败')
  }
}

const removeDocument = async (documentId: number) => {
  if (!(await showConfirm('确定删除这个文档吗？'))) return
  try {
    await deleteKnowledgeDocument(kbId.value, documentId)
    documents.value = documents.value.filter(item => item.id !== documentId)
    knowledgeBase.value = await getKnowledgeBase(kbId.value)
  } catch (error: any) {
    await showAlert(error.message || '删除文档失败')
  }
}

const statusText = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '等待中',
    INDEXING: '索引中',
    INDEXED: '已完成',
    FAILED: '失败',
    UNKNOWN: '未知'
  }
  return map[status] || '未知'
}

const statusClass = (status: string) => {
  if (status === 'INDEXED') return 'ok'
  if (status === 'FAILED') return 'failed'
  return 'pending'
}

const formatSize = (size: number) => {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

onMounted(loadData)
</script>

<style scoped>
.kb-detail-page {
  min-height: 100vh;
  background: #f5f6f8;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 52px;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.title-area {
  flex: 1;
  min-width: 0;
}

.title-area h1 {
  margin: 0;
  font-size: 16px;
  color: #222;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.title-area span {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: #777;
}

.icon-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  color: #333;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-btn svg {
  width: 20px;
  height: 20px;
}

.text-btn,
.ghost-btn,
.danger-btn {
  border: none;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 14px;
}

.text-btn {
  background: #3478f6;
  color: #fff;
}

.ghost-btn {
  background: #eef1f5;
  color: #333;
}

.danger-btn {
  background: #ffecec;
  color: #d93025;
}

.hidden-input {
  display: none;
}

.content {
  padding: 12px;
}

.uploading,
.state,
.empty {
  text-align: center;
  color: #666;
  padding: 36px 20px;
}

.empty h2 {
  color: #222;
  margin: 0 0 8px;
  font-size: 20px;
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
}

.doc-main {
  flex: 1;
  min-width: 0;
}

.doc-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.doc-title-row h3 {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-size: 15px;
  color: #222;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-main p {
  margin: 5px 0 0;
  font-size: 13px;
  color: #666;
}

.doc-main .error {
  color: #d93025;
}

.status {
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 12px;
  flex-shrink: 0;
}

.status.ok {
  background: #e7f6ed;
  color: #188038;
}

.status.pending {
  background: #fff6df;
  color: #9a6700;
}

.status.failed {
  background: #ffecec;
  color: #d93025;
}

.doc-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

@media (max-width: 560px) {
  .doc-item {
    align-items: stretch;
    flex-direction: column;
  }

  .doc-actions {
    justify-content: flex-end;
  }
}
</style>
