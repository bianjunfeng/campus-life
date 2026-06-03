<template>
  <div class="admin-agent-hub">
    <div class="page-header">
      <div>
        <h1>AI Agent 中心</h1>
        <p>管理端对话 Agent、知识库 Agent、运维 Agent 的统一入口。</p>
      </div>
      <button class="btn" :disabled="loading" @click="loadAgents">
        {{ loading ? '刷新中...' : '刷新入口' }}
      </button>
    </div>

    <div v-if="errorMessage" class="state error">{{ errorMessage }}</div>

    <section class="agent-grid">
      <RouterLink
        v-for="entry in displayEntries"
        :key="entry.code"
        class="agent-card"
        :to="entry.frontendPath"
      >
        <span>{{ entry.sceneCode }}</span>
        <h2>{{ entry.name }}</h2>
        <p>{{ entry.description }}</p>
        <strong>{{ entry.backendPath }}</strong>
      </RouterLink>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getAdminAiAgents, type AdminAiAgentEntry } from '../../api/admin'

const loading = ref(false)
const errorMessage = ref('')
const entries = ref<AdminAiAgentEntry[]>([])

const fallbackEntries: AdminAiAgentEntry[] = [
  {
    code: 'dialog',
    name: '管理端对话 Agent',
    description: '用于管理员平台问答、配置说明和审核辅助。',
    frontendPath: '/admin/ai/agent/chat',
    backendPath: '/api/agent/conversations',
    sceneCode: 'chat.general'
  },
  {
    code: 'knowledge',
    name: '管理端知识库 Agent',
    description: '上传管理侧文档，在对话中引用知识库内容。',
    frontendPath: '/admin/ai/agent/knowledge-bases',
    backendPath: '/api/agent/knowledge-bases',
    sceneCode: 'chat.personal_qa'
  },
  {
    code: 'ops',
    name: '运维 Agent',
    description: '检查服务健康、日志、告警和 AI 调用状态，生成诊断报告。',
    frontendPath: '/admin/ai?tab=ops',
    backendPath: '/api/admin/ai/ops',
    sceneCode: 'ops.diagnosis'
  }
]

const displayEntries = computed(() => entries.value.length ? entries.value : fallbackEntries)

const loadAgents = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    entries.value = await getAdminAiAgents()
  } catch (error: any) {
    errorMessage.value = error?.message || '加载管理端 Agent 入口失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadAgents)
</script>

<style scoped>
.admin-agent-hub {
  color: #1f2937;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  color: #111827;
  font-size: 28px;
}

.page-header p {
  margin: 8px 0 0;
  color: #6b7280;
}

.btn {
  min-width: 96px;
  height: 38px;
  border: 1px solid #2563eb;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  font-weight: 700;
}

.state {
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 8px;
}

.state.error {
  background: #fee2e2;
  color: #991b1b;
}

.agent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.agent-card {
  min-height: 190px;
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: inherit;
  text-decoration: none;
  box-shadow: 0 1px 2px rgba(15, 23, 42, .04);
}

.agent-card:hover {
  border-color: #2563eb;
  box-shadow: 0 14px 28px rgba(37, 99, 235, .1);
}

.agent-card span {
  display: inline-flex;
  padding: 4px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.agent-card h2 {
  margin: 16px 0 8px;
  color: #111827;
  font-size: 20px;
}

.agent-card p {
  min-height: 48px;
  margin: 0 0 18px;
  color: #4b5563;
  line-height: 1.6;
}

.agent-card strong {
  color: #374151;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
}
</style>
