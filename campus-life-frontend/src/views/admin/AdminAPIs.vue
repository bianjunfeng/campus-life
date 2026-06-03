<template>
  <div class="admin-apis">
    <!-- 筛选条件 -->
    <div class="filter-section">
      <input 
        type="text" 
        v-model="searchKeyword" 
        placeholder="搜索接口路径或描述"
        @keyup.enter="filterApis"
      />
      <select v-model="filterMethod" @change="filterApis">
        <option value="">全部方法</option>
        <option value="ALL">ALL</option>
        <option value="GET">GET</option>
        <option value="POST">POST</option>
        <option value="PUT">PUT</option>
        <option value="DELETE">DELETE</option>
        <option value="PATCH">PATCH</option>
      </select>
      <select v-model="filterModule" @change="filterApis">
        <option value="">全部模块</option>
        <option v-for="module in modules" :key="module" :value="module">{{ module }}</option>
      </select>
      <button @click="filterApis">查询</button>
      <button @click="resetFilters" class="btn-secondary">重置</button>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">接口总数</div>
          <div class="stat-value">{{ filteredApiList.length }}</div>
        </div>
      </div>
    </div>
    
    <!-- 接口列表 -->
    <div class="api-list">
      <div v-for="module in filteredModules" :key="module" class="api-module">
        <h3 class="module-title">{{ module }}</h3>
        <div class="api-items">
          <div 
            v-for="api in getApisByModule(module)" 
            :key="getApiKey(api)" 
            class="api-item"
            :class="{ expanded: expandedApis.has(getApiKey(api)) }"
          >
            <div class="api-header" @click="toggleApi(getApiKey(api))">
              <div class="api-method" :class="`method-${api.method.toLowerCase()}`">
                {{ api.method }}
              </div>
              <div class="api-path">{{ api.path }}</div>
              <div class="api-description">{{ api.description }}</div>
              <svg 
                class="expand-icon"
                :class="{ expanded: expandedApis.has(getApiKey(api)) }"
                xmlns="http://www.w3.org/2000/svg" 
                viewBox="0 0 24 24" 
                fill="none" 
                stroke="currentColor" 
                stroke-width="2"
              >
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
            <div v-if="expandedApis.has(getApiKey(api))" class="api-details">
              <div class="detail-row">
                <span class="detail-label">接口路径：</span>
                <span class="detail-value code">{{ api.path }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">请求方法：</span>
                <span class="detail-value">
                  <span class="method-badge" :class="`method-${api.method.toLowerCase()}`">
                    {{ api.method }}
                  </span>
                </span>
              </div>
              <div class="detail-row" v-if="api.description">
                <span class="detail-label">接口描述：</span>
                <span class="detail-value">{{ api.description }}</span>
              </div>
              <div class="detail-row" v-if="api.params && api.params.length > 0">
                <span class="detail-label">请求参数：</span>
                <div class="detail-value">
                  <div v-for="param in api.params" :key="param.name" class="param-item">
                    <span class="param-name">{{ param.name }}</span>
                    <span class="param-type">{{ param.type }}</span>
                    <span class="param-required" v-if="param.required">必填</span>
                    <span class="param-desc" v-if="param.description">- {{ param.description }}</span>
                  </div>
                </div>
              </div>
              <div class="detail-row" v-if="api.response">
                <span class="detail-label">返回示例：</span>
                <pre class="detail-value-pre">{{ formatResponse(api.response) }}</pre>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getApiList, type ApiInfo } from '../../api/admin'
import { showAlert } from '../../utils/dialog'

const apiList = ref<ApiInfo[]>([])
const searchKeyword = ref('')
const filterMethod = ref('')
const filterModule = ref('')
const expandedApis = ref<Set<string>>(new Set())

const modules = computed(() => {
  const moduleSet = new Set<string>()
  apiList.value.forEach(api => {
    if (api.module) {
      moduleSet.add(api.module)
    }
  })
  return Array.from(moduleSet).sort()
})

const filteredApiList = computed(() => {
  let filtered = apiList.value
  
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(api => 
      api.path.toLowerCase().includes(keyword) || 
      (api.description && api.description.toLowerCase().includes(keyword))
    )
  }
  
  if (filterMethod.value) {
    filtered = filtered.filter(api => api.method === filterMethod.value)
  }
  
  if (filterModule.value) {
    filtered = filtered.filter(api => api.module === filterModule.value)
  }
  
  return filtered
})

const filteredModules = computed(() => {
  if (filterModule.value) {
    return [filterModule.value]
  }
  const moduleSet = new Set<string>()
  filteredApiList.value.forEach(api => {
    if (api.module) {
      moduleSet.add(api.module)
    }
  })
  return Array.from(moduleSet).sort()
})

const getApisByModule = (module: string) => {
  return filteredApiList.value.filter(api => api.module === module)
}

const getApiKey = (api: ApiInfo) => `${api.method}:${api.path}`

const toggleApi = (apiKey: string) => {
  if (expandedApis.value.has(apiKey)) {
    expandedApis.value.delete(apiKey)
  } else {
    expandedApis.value.add(apiKey)
  }
}

const filterApis = () => {
  // 过滤逻辑已在 computed 中实现
}

const resetFilters = () => {
  searchKeyword.value = ''
  filterMethod.value = ''
  filterModule.value = ''
}

const formatResponse = (response: any) => {
  if (typeof response === 'string') return response
  return JSON.stringify(response, null, 2)
}

const loadApis = async () => {
  try {
    const data = await getApiList()
    apiList.value = data.list || []
    if (!apiList.value.length) {
      await showAlert('暂无接口数据')
    }
  } catch (error: any) {
    console.error('加载接口列表失败:', error)
    apiList.value = []
    await showAlert(error?.message || '加载接口列表失败')
  }
}

onMounted(() => {
  loadApis()
})
</script>

<style scoped>
.admin-apis {
  width: 100%;
}

.filter-section {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-section input,
.filter-section select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.filter-section input {
  flex: 1;
  min-width: 200px;
}

.filter-section button {
  padding: 8px 16px;
  background: #1677ff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.filter-section button.btn-secondary {
  background: #f0f0f0;
  color: #333;
}

.filter-section button:hover {
  opacity: 0.9;
}

.stats-section {
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: #e6f4ff;
  color: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon svg {
  width: 28px;
  height: 28px;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #333;
}

.api-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.api-module {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.module-title {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.api-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.api-item {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  overflow: hidden;
  transition: all 0.3s;
}

.api-item:hover {
  border-color: #1677ff;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.1);
}

.api-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  cursor: pointer;
  background: #fafafa;
  transition: background 0.3s;
}

.api-header:hover {
  background: #f0f7ff;
}

.api-method {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  min-width: 60px;
  text-align: center;
}

.method-get {
  background: #e6f4ff;
  color: #1677ff;
}

.method-post {
  background: #f6ffed;
  color: #52c41a;
}

.method-put {
  background: #fff7e6;
  color: #fa8c16;
}

.method-delete {
  background: #fff2f0;
  color: #ff4d4f;
}

.api-path {
  font-family: monospace;
  font-size: 14px;
  color: #333;
  flex: 1;
}

.api-description {
  color: #666;
  font-size: 14px;
  flex: 2;
}

.expand-icon {
  width: 16px;
  height: 16px;
  color: #999;
  transition: transform 0.3s;
  flex-shrink: 0;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.api-details {
  padding: 16px;
  background: white;
  border-top: 1px solid #f0f0f0;
}

.detail-row {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  width: 120px;
  color: #666;
  font-weight: 500;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  color: #333;
  word-break: break-all;
}

.detail-value.code {
  font-family: monospace;
  background: #f5f5f5;
  padding: 4px 8px;
  border-radius: 4px;
}

.method-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 600;
}

.param-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
}

.param-name {
  font-family: monospace;
  color: #1677ff;
  font-weight: 500;
}

.param-type {
  color: #666;
  font-size: 12px;
}

.param-required {
  background: #fff2f0;
  color: #ff4d4f;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 11px;
}

.param-desc {
  color: #999;
  font-size: 12px;
}

.detail-value-pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  font-family: monospace;
  font-size: 12px;
  margin: 0;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .filter-section {
    flex-direction: column;
  }
  
  .filter-section input,
  .filter-section select,
  .filter-section button {
    width: 100%;
  }
  
  .api-header {
    flex-wrap: wrap;
  }
  
  .api-description {
    width: 100%;
    order: 3;
  }
  
  .detail-row {
    flex-direction: column;
    gap: 8px;
  }
  
  .detail-label {
    width: 100%;
  }
}
</style>
