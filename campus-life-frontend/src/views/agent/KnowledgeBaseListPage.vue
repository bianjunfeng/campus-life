<template>
  <div class="kb-page">
    <header class="page-header">
      <button class="icon-btn" @click="router.back()" aria-label="返回">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 18l-6-6 6-6"></path>
        </svg>
      </button>
      <h1>我的知识库</h1>
      <button class="text-btn" @click="showCreate = !showCreate">新建</button>
    </header>

    <section v-if="showCreate" class="create-panel">
      <input v-model="form.name" class="field" placeholder="知识库名称" maxlength="128" />
      <textarea v-model="form.description" class="field textarea" placeholder="描述，可不填" maxlength="512"></textarea>
      <div class="actions">
        <button class="ghost-btn" @click="showCreate = false">取消</button>
        <button class="primary-btn" :disabled="!form.name.trim() || saving" @click="submitCreate">保存</button>
      </div>
    </section>

    <main class="content">
      <div v-if="loading" class="state">加载中...</div>
      <div v-else-if="knowledgeBases.length === 0" class="empty">
        <h2>还没有知识库</h2>
        <p>新建知识库后即可上传 txt 或 md 文档，并在对话中使用。</p>
      </div>
      <div v-else class="kb-list">
        <article v-for="item in knowledgeBases" :key="item.id" class="kb-item" @click="openDetail(item.id)">
          <div class="kb-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 19.5V5a2 2 0 0 1 2-2h10l4 4v12.5A1.5 1.5 0 0 1 18.5 21h-13A1.5 1.5 0 0 1 4 19.5z"></path>
              <path d="M14 3v5h5"></path>
            </svg>
          </div>
          <div class="kb-info">
            <h3>{{ item.name }}</h3>
            <p>{{ item.description || '未填写描述' }}</p>
            <span>{{ item.documentCount || 0 }} 个文档 · {{ formatSize(item.totalSize || 0) }}</span>
          </div>
          <button class="delete-btn" @click.stop="removeBase(item.id)" aria-label="删除">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 6h18"></path>
              <path d="M8 6V4h8v2"></path>
              <path d="M19 6l-1 14H6L5 6"></path>
            </svg>
          </button>
        </article>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createKnowledgeBase, deleteKnowledgeBase, listKnowledgeBases, type KnowledgeBase } from '../../api/knowledge'
import { showAlert, showConfirm } from '../../utils/dialog'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const showCreate = ref(false)
const knowledgeBases = ref<KnowledgeBase[]>([])
const form = reactive({ name: '', description: '' })

const agentBasePath = computed(() => {
  if (route.path.startsWith('/admin/ai/agent')) return '/admin/ai/agent'
  if (route.path.startsWith('/merchant/ai')) return '/merchant/ai'
  return '/agent'
})

const loadData = async () => {
  loading.value = true
  try {
    knowledgeBases.value = await listKnowledgeBases()
  } catch (error: any) {
    await showAlert(error.message || '加载知识库失败')
  } finally {
    loading.value = false
  }
}

const submitCreate = async () => {
  if (!form.name.trim() || saving.value) return
  saving.value = true
  try {
    const created = await createKnowledgeBase({
      name: form.name.trim(),
      description: form.description.trim() || undefined
    })
    form.name = ''
    form.description = ''
    showCreate.value = false
    knowledgeBases.value.unshift(created)
  } catch (error: any) {
    await showAlert(error.message || '创建知识库失败')
  } finally {
    saving.value = false
  }
}

const removeBase = async (id: number) => {
  if (!(await showConfirm('确定删除这个知识库吗？'))) return
  try {
    await deleteKnowledgeBase(id)
    knowledgeBases.value = knowledgeBases.value.filter(item => item.id !== id)
  } catch (error: any) {
    await showAlert(error.message || '删除知识库失败')
  }
}

const openDetail = (id: number) => {
  router.push(`${agentBasePath.value}/knowledge-bases/${id}`)
}

const formatSize = (size: number) => {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

onMounted(loadData)
</script>

<style scoped>
.kb-page {
  min-height: 100vh;
  background: #f5f6f8;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 12px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.page-header h1 {
  margin: 0;
  font-size: 16px;
  color: #222;
}

.icon-btn,
.delete-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  color: #333;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-btn svg,
.delete-btn svg {
  width: 20px;
  height: 20px;
}

.text-btn,
.primary-btn,
.ghost-btn {
  border: none;
  border-radius: 8px;
  padding: 8px 14px;
  font-size: 14px;
}

.text-btn,
.primary-btn {
  background: #3478f6;
  color: white;
}

.primary-btn:disabled {
  background: #b9c9e8;
}

.ghost-btn {
  background: #eef1f5;
  color: #333;
}

.create-panel {
  margin: 12px;
  padding: 12px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
}

.field {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
}

.textarea {
  min-height: 72px;
  resize: vertical;
  margin-top: 10px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.content {
  padding: 12px;
}

.state,
.empty {
  text-align: center;
  color: #666;
  padding: 48px 20px;
}

.empty h2 {
  color: #222;
  font-size: 20px;
  margin: 0 0 8px;
}

.kb-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kb-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
}

.kb-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: #eaf2ff;
  color: #3478f6;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kb-icon svg {
  width: 22px;
  height: 22px;
}

.kb-info {
  flex: 1;
  min-width: 0;
}

.kb-info h3 {
  margin: 0 0 4px;
  font-size: 15px;
  color: #222;
}

.kb-info p,
.kb-info span {
  display: block;
  margin: 0;
  font-size: 13px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-btn {
  color: #999;
}
</style>
