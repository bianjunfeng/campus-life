<template>
  <div class="markdown-message" v-html="safeHtml"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify, { type Config } from 'dompurify'

const props = defineProps<{
  content?: string
}>()

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: false
})

const defaultLinkOpen = markdown.renderer.rules.link_open
markdown.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  token.attrSet('target', '_blank')
  token.attrSet('rel', 'noopener noreferrer')
  return defaultLinkOpen
    ? defaultLinkOpen(tokens, idx, options, env, self)
    : self.renderToken(tokens, idx, options)
}

markdown.validateLink = (url: string): boolean => {
  const normalized = String(url || '').trim().replace(/[\u0000-\u001F\u007F\s]+/g, '')
  if (!normalized) return false
  if (normalized.startsWith('#') || normalized.startsWith('/')) return true

  const colonIndex = normalized.indexOf(':')
  if (colonIndex < 0) return false

  const protocol = normalized.slice(0, colonIndex).toLowerCase()
  return protocol === 'http' || protocol === 'https' || protocol === 'mailto'
}

const sanitizeConfig: Config = {
  ALLOWED_TAGS: [
    'p', 'br', 'strong', 'em', 'blockquote',
    'ul', 'ol', 'li',
    'h1', 'h2', 'h3', 'h4',
    'code', 'pre',
    'a', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
    'hr'
  ],
  ALLOWED_ATTR: ['href', 'title', 'target', 'rel'],
  ALLOW_DATA_ATTR: false,
  ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto):|[#/])/i,
  FORBID_TAGS: [
    'script', 'style', 'iframe', 'object', 'embed',
    'form', 'input', 'button', 'textarea', 'select', 'option',
    'img', 'video', 'audio', 'svg', 'math'
  ]
}

const safeHtml = computed(() => {
  const raw = props.content || ''
  if (!raw.trim()) return ''
  return DOMPurify.sanitize(markdown.render(raw), sanitizeConfig)
})
</script>

<style scoped>
.markdown-message {
  color: inherit;
  line-height: 1.6;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.markdown-message :deep(*) {
  box-sizing: border-box;
}

.markdown-message :deep(:first-child) {
  margin-top: 0;
}

.markdown-message :deep(:last-child) {
  margin-bottom: 0;
}

.markdown-message :deep(p) {
  margin: 0 0 8px;
  white-space: pre-wrap;
}

.markdown-message :deep(h1),
.markdown-message :deep(h2),
.markdown-message :deep(h3),
.markdown-message :deep(h4) {
  margin: 12px 0 8px;
  color: inherit;
  font-weight: 700;
  line-height: 1.35;
}

.markdown-message :deep(h1) {
  font-size: 18px;
}

.markdown-message :deep(h2) {
  font-size: 17px;
}

.markdown-message :deep(h3) {
  font-size: 16px;
}

.markdown-message :deep(h4) {
  font-size: 15px;
}

.markdown-message :deep(ul),
.markdown-message :deep(ol) {
  margin: 6px 0 10px;
  padding-left: 22px;
}

.markdown-message :deep(li) {
  margin: 3px 0;
}

.markdown-message :deep(blockquote) {
  margin: 8px 0;
  padding: 6px 10px;
  border-left: 3px solid #d6ddff;
  background: rgba(102, 126, 234, 0.08);
  color: #4b5563;
}

.markdown-message :deep(pre) {
  max-width: 100%;
  margin: 8px 0;
  padding: 10px 12px;
  overflow-x: auto;
  border-radius: 8px;
  background: #f6f8fa;
  color: #1f2937;
  font-size: 13px;
  line-height: 1.5;
}

.markdown-message :deep(code) {
  border-radius: 4px;
  background: #f1f5f9;
  color: #1f2937;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 0.92em;
}

.markdown-message :deep(p code),
.markdown-message :deep(li code),
.markdown-message :deep(td code),
.markdown-message :deep(th code) {
  padding: 1px 4px;
}

.markdown-message :deep(pre code) {
  padding: 0;
  background: transparent;
}

.markdown-message :deep(a) {
  color: #4f6df5;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.markdown-message :deep(table) {
  display: block;
  max-width: 100%;
  margin: 8px 0;
  overflow-x: auto;
  border-collapse: collapse;
  font-size: 13px;
}

.markdown-message :deep(th),
.markdown-message :deep(td) {
  padding: 6px 8px;
  border: 1px solid #e5e7eb;
  text-align: left;
}

.markdown-message :deep(th) {
  background: #f8fafc;
  font-weight: 700;
}

.markdown-message :deep(hr) {
  height: 1px;
  margin: 10px 0;
  border: 0;
  background: #e5e7eb;
}
</style>
