<template>
  <nav class="merchant-console-nav" aria-label="商家管理导航">
    <RouterLink
      class="nav-item"
      :class="{ active: active === 'home' }"
      to="/merchant/home"
    >
      管理首页
    </RouterLink>
    <RouterLink
      class="nav-item"
      :class="{ active: active === 'vouchers' }"
      to="/merchant/vouchers"
    >
      优惠券管理
    </RouterLink>
    <RouterLink
      class="nav-item"
      :class="{ active: active === 'refunds' }"
      to="/merchant/refunds"
    >
      退款审核
    </RouterLink>
    <RouterLink
      class="nav-item"
      :class="{ active: active === 'ai' }"
      to="/merchant/ai"
    >
      AI 工作台
    </RouterLink>
  </nav>
</template>

<script setup lang="ts">
defineProps<{
  active: 'home' | 'vouchers' | 'refunds' | 'ai'
}>()
</script>

<style scoped>
.merchant-console-nav {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  border-bottom: 1px solid #dbe4f0;
}

.nav-item {
  display: inline-flex;
  align-items: center;
  height: 42px;
  padding: 0 14px;
  border: 1px solid transparent;
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  color: #475569;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
}

.nav-item:hover {
  color: #0f766e;
}

.nav-item.active {
  color: #0f766e;
  background: #fff;
  border-color: #dbe4f0;
}

@media (max-width: 640px) {
  .merchant-console-nav {
    overflow-x: auto;
  }

  .nav-item {
    flex: 0 0 auto;
  }
}
</style>
