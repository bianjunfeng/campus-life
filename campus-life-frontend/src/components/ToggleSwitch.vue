
<!-- 切换开关组件 -->
<template>
  <label class="toggle-switch">
    <input 
      type="checkbox" 
      :checked="modelValue" 
      @change="onChange"
      :disabled="disabled"
    >
    <span class="toggle-slider" :class="{ disabled: disabled }"></span>
  </label>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue'

const props = defineProps<{
  modelValue: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const onChange = (event: Event) => {
  if (props.disabled) return
  const target = event.target as HTMLInputElement
  emit('update:modelValue', target.checked)
}
</script>

<style scoped>
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 52px;
  height: 28px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 28px;
}

.toggle-slider:before {
  position: absolute;
  content: "";
  height: 24px;
  width: 24px;
  left: 2px;
  bottom: 2px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .toggle-slider {
  background-color: #ff4d4f;
}

input:checked + .toggle-slider:before {
  transform: translateX(24px);
}

/* 禁用状态 */
.toggle-slider.disabled {
  background-color: #e0e0e0;
  cursor: not-allowed;
}

input:checked + .toggle-slider.disabled {
  background-color: #ffb4b4;
}

/* 暗色模式样式 */
:global(.dark-mode) .toggle-slider {
  background-color: #555;
}

:global(.dark-mode) input:checked + .toggle-slider {
  background-color: #ff7875;
}

:global(.dark-mode) .toggle-slider.disabled {
  background-color: #444;
}

:global(.dark-mode) input:checked + .toggle-slider.disabled {
  background-color: #885858;
}

/* 过渡动画增强 */
.toggle-slider {
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.2);
}

input:checked + .toggle-slider {
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.3);
}

.toggle-slider:before {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}
</style>