<template>
  <div class="merchant-ai-page">
    <div class="page-header">
      <div>
        <p class="eyebrow">Merchant AI</p>
        <h1>商家 AI 工作台</h1>
        <p class="subtitle">对话 Agent、知识库 Agent、商家运营 Agent 已统一接入。</p>
      </div>
      <button type="button" class="ghost-button" @click="loadAgents" :disabled="loading">
        {{ loading ? '刷新中...' : '刷新入口' }}
      </button>
    </div>

    <div v-if="errorMessage" class="page-state error">{{ errorMessage }}</div>

    <section class="agent-grid">
      <RouterLink
        v-for="entry in displayEntries"
        :key="entry.code"
        class="agent-card"
        :to="entry.frontendPath"
      >
        <span class="agent-code">{{ entry.sceneCode }}</span>
        <h2>{{ entry.name }}</h2>
        <p>{{ entry.description }}</p>
        <strong>{{ entry.backendPath }}</strong>
      </RouterLink>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getMerchantAiAgents, type MerchantAiAgentEntry } from '../../api/merchant'

const loading = ref(false)
const errorMessage = ref('')
const entries = ref<MerchantAiAgentEntry[]>([])

const fallbackEntries: MerchantAiAgentEntry[] = [
  {
    code: 'dialog',
    name: '商家对话 Agent',
    description: '用于商家日常平台问答和操作咨询。',
    frontendPath: '/merchant/ai/agent/chat',
    backendPath: '/api/agent/conversations',
    sceneCode: 'chat.general'
  },
  {
    code: 'knowledge',
    name: '商家知识库 Agent',
    description: '上传商家运营文档，并在对话中引用检索结果。',
    frontendPath: '/merchant/ai/agent/knowledge-bases',
    backendPath: '/api/agent/knowledge-bases',
    sceneCode: 'chat.personal_qa'
  },
  {
    code: 'merchant_ops',
    name: '商家运营 Agent',
    description: '围绕优惠券、退款、门店资料和运营策略给出建议。',
    frontendPath: '/merchant/ai/agent/chat?assistantType=merchant_ops',
    backendPath: '/api/agent/conversations/{sessionId}/messages',
    sceneCode: 'chat.merchant_ops'
  }
]

const displayEntries = computed(() => entries.value.length ? entries.value : fallbackEntries)

const loadAgents = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    entries.value = await getMerchantAiAgents()
  } catch (error: any) {
    errorMessage.value = error?.message || '加载商家 AI 入口失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadAgents)
</script>

<style scoped>
.merchant-ai-page {
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef6ff 100%);
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #0f172a;
}

.subtitle {
  margin: 8px 0 0;
  color: #64748b;
}

.ghost-button {
  height: 40px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  color: #0f172a;
  font-weight: 700;
}

.page-state {
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fee2e2;
  color: #991b1b;
}

.agent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}

.agent-card {
  min-height: 180px;
  padding: 18px;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #fff;
  color: inherit;
  text-decoration: none;
}

.agent-card:hover {
  border-color: #0f766e;
  box-shadow: 0 12px 24px rgba(15, 118, 110, .08);
}

.agent-code {
  display: inline-flex;
  padding: 4px 8px;
  border-radius: 999px;
  background: #ecfdf5;
  color: #047857;
  font-size: 12px;
  font-weight: 700;
}

.agent-card h2 {
  margin: 14px 0 8px;
  color: #0f172a;
  font-size: 20px;
}

.agent-card p {
  min-height: 44px;
  margin: 0 0 16px;
  color: #475569;
  line-height: 1.6;
}

.agent-card strong {
  color: #334155;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
}
</style>
