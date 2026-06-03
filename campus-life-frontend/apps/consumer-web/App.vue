<template>
  <div class="app-container">
    <router-view />
  </div>
</template>

<style>
.app-container {
  min-height: 100vh;
}

@media (min-width: 769px) {
  .app-container {
    max-width: 768px;
    margin: 0 auto;
    border-left: 1px solid #e0e0e0;
    border-right: 1px solid #e0e0e0;
  }
}
</style>

