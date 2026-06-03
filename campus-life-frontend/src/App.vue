<script setup lang="ts">
// App.vue 简化为只包含路由视图
</script>

<template>
  <div class="app-container">
    <router-view />
  </div>
</template>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',
    sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background-color: #f5f5f5;
  color: #333;
  line-height: 1.6;
  font-size: 14px;
}

/* 应用容器 */
.app-container {
  min-height: 100vh;
}

/* 通用样式 */
button {
  cursor: pointer;
}

img {
  max-width: 100%;
  height: auto;
}

/* 响应式调整 */
@media (min-width: 769px) {
  .app-container {
    max-width: 768px;
    margin: 0 auto;
    border-left: 1px solid #e0e0e0;
    border-right: 1px solid #e0e0e0;
  }
}
</style>