<template>
  <div style="padding: 20px;">
    <h2>测试后端健康接口</h2>
    <button @click="checkHealth">点击测试</button>

    <p v-if="loading">请求中...</p>

    <div v-if="result">
      <p>状态：{{ result.status }}</p>
      <p>时间：{{ result.time }}</p>
    </div>

    <p v-if="error" style="color: red;">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import http from './api/http'

const loading = ref(false)
const result = ref<any>(null)
const error = ref<string | null>(null)

const checkHealth = async () => {
  loading.value = true
  result.value = null
  error.value = null
  try {
    const res = await http.get('/api/health')
    result.value = res.data
  } catch (e: any) {
    error.value = e.message || '请求失败'
  } finally {
    loading.value = false
  }
}
</script>