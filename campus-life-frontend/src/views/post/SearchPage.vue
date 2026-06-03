<template>
  <div class="search-container">
    <div class="search-header">
      <div class="search-bar-wrapper">
        <div class="search-bar">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            type="text"
            class="search-input"
            v-model="searchKeyword"
            placeholder="搜索帖子内容"
            ref="searchInput"
            @focus="handleFocus"
            @input="handleInput"
            @keyup.enter="handleSearch"
          />
          <button v-if="searchKeyword" class="clear-btn" @click="clearSearch">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      </div>
      <button class="cancel-btn" @click="handleCancel">取消</button>
    </div>

    <div class="search-results" v-if="showResults">
      <div class="search-stats">
        <span>共 {{ totalResults }} 条结果</span>
      </div>
      <div v-if="searchError" class="search-error">{{ searchError }}</div>

      <div class="results-list">
        <div v-if="userResults.length > 0" class="result-section">
          <h3 class="section-title">用户</h3>
          <div class="user-results">
            <div
              v-for="user in userResults"
              :key="user.id"
              class="user-result-item"
              @click="goUser(user.id)"
            >
              <img :src="user.avatarUrl || fallbackAvatar" alt="用户头像" class="user-avatar">
              <div class="user-info">
                <div class="user-name">{{ user.username || '用户' }}</div>
                <div class="user-bio">{{ user.bio || '这个用户很懒，还没有留下简介。' }}</div>
                <div class="user-stats">
                  <span>帖子 {{ user.postCount || 0 }}</span>
                  <span>粉丝 {{ user.followerCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="postResults.length > 0" class="result-section">
          <h3 class="section-title">帖子</h3>
          <div class="notes-grid">
            <div
              v-for="post in postResults"
              :key="post.id"
              class="note-result-item"
              @click="goPost(post.id)"
            >
              <div class="note-image">
                <img :src="post.coverImage || fallbackCover" alt="帖子封面">
                <div class="note-likes">❤ {{ post.likeCount || 0 }}</div>
              </div>
              <div class="note-content">
                <div class="note-title">
                  <template
                    v-for="(segment, segmentIndex) in toHighlightSegments(post.highlightTitle || post.title)"
                    :key="`title-${post.id}-${segmentIndex}`"
                  >
                    <span :class="{ 'search-hit': segment.highlighted }">{{ segment.text }}</span>
                  </template>
                </div>
                <div class="note-excerpt">
                  <template
                    v-for="(segment, segmentIndex) in toHighlightSegments(post.highlightContent || post.content)"
                    :key="`content-${post.id}-${segmentIndex}`"
                  >
                    <span :class="{ 'search-hit': segment.highlighted }">{{ segment.text }}</span>
                  </template>
                </div>
                <div class="note-meta">
                  <span class="note-author">{{ post.authorName || '用户' }}</span>
                  <span class="note-date">评 {{ post.commentCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="userResults.length === 0 && postResults.length === 0" class="no-results">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <h3>暂无搜索结果</h3>
          <p>请尝试调整关键词</p>
        </div>
      </div>

      <div class="load-more" v-if="postResults.length > 0 && !loading && hasMorePosts">
        <button class="load-more-btn" @click="loadMore">加载更多</button>
      </div>
      <div class="load-more" v-if="loading">
        <button class="load-more-btn" disabled>加载中...</button>
      </div>
    </div>

    <div class="search-suggestions" v-else>
      <div class="history-section" v-if="searchHistory.length > 0">
        <div class="section-header">
          <h3 class="section-title">历史记录</h3>
          <button class="clear-history" @click="clearHistory">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              <line x1="10" y1="11" x2="10" y2="17"></line>
              <line x1="14" y1="11" x2="14" y2="17"></line>
            </svg>
          </button>
        </div>
        <div class="history-tags">
          <div
            v-for="item in searchHistory"
            :key="item.id"
            class="history-tag"
            @click="handleHistoryTagClick(item.keyword)"
          >
            {{ item.keyword }}
          </div>
        </div>
      </div>

      <div class="suggestions-section">
        <div class="section-header">
          <h3 class="section-title">猜你想搜</h3>
        </div>
        <div class="suggestion-tags">
          <div
            v-for="suggestion in suggestions"
            :key="suggestion.id"
            class="suggestion-tag"
            @click="handleSuggestionClick(suggestion.keyword)"
          >
            {{ suggestion.keyword }}
          </div>
        </div>
      </div>

      <div class="hot-topics-section" v-if="hotTopics.length > 0">
        <div class="section-header">
          <h3 class="section-title">
            <span class="hot-icon">🔥</span>
            热门搜索
          </h3>
        </div>
        <div class="hot-topics-list">
          <div
            v-for="topic in hotTopics"
            :key="topic.id"
            class="hot-topic-item"
            @click="handleHotTopicClick(topic.title)"
          >
            <div class="topic-rank">{{ topic.rank }}</div>
            <div class="topic-content">
              <div class="topic-title">{{ topic.title }}</div>
              <div class="topic-stats">{{ topic.viewsText }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchPosts, searchUsers, type SearchPostItem, type SearchUserItem } from '@/api/forum'

interface SearchRecord {
  keyword: string
  count: number
  lastAt: number
}

interface SearchTagItem {
  id: string
  keyword: string
}

interface HotTopicItem {
  id: string
  rank: number
  title: string
  viewsText: string
}

const SEARCH_RECORDS_KEY = 'post-search-records'

const router = useRouter()
const route = useRoute()
const searchInput = ref<HTMLInputElement>()
const searchKeyword = ref('')
const showResults = ref(false)
const loading = ref(false)
const searchError = ref('')
const postPage = ref(1)
const postSize = ref(10)
const postTotal = ref(0)
const hasMorePosts = ref(false)
const postResults = ref<SearchPostItem[]>([])
const userResults = ref<SearchUserItem[]>([])
const userTotal = ref(0)
let searchTimer: number | null = null
const fallbackCover = 'https://picsum.photos/seed/post/400/300'
const fallbackAvatar = 'https://picsum.photos/seed/user-avatar/120/120'
const searchRecords = ref<SearchRecord[]>([])
const totalResults = computed(() => userTotal.value + postTotal.value)
const searchHistory = computed<SearchTagItem[]>(() =>
  searchRecords.value.slice(0, 10).map((item, index) => ({
    id: `history-${index}`,
    keyword: item.keyword
  }))
)
const suggestions = computed<SearchTagItem[]>(() =>
  searchRecords.value
    .filter(item => item.keyword !== searchKeyword.value.trim())
    .sort((a, b) => b.lastAt - a.lastAt)
    .slice(0, 6)
    .map((item, index) => ({
      id: `suggest-${index}`,
      keyword: item.keyword
    }))
)
const hotTopics = computed<HotTopicItem[]>(() =>
  [...searchRecords.value]
    .sort((a, b) => b.count - a.count || b.lastAt - a.lastAt)
    .slice(0, 8)
    .map((item, index) => ({
      id: `hot-${index}`,
      rank: index + 1,
      title: item.keyword,
      viewsText: `${item.count}次搜索`
    }))
)

interface HighlightSegment {
  text: string
  highlighted: boolean
}

const decodeHtmlEntities = (value: string) => {
  if (!value) {
    return ''
  }
  const textarea = document.createElement('textarea')
  textarea.innerHTML = value
  return textarea.value
}

const toHighlightSegments = (value: string | undefined): HighlightSegment[] => {
  const source = value || ''
  const segments: HighlightSegment[] = []
  const regex = /<em class='search-hit'>(.*?)<\/em>/g
  let lastIndex = 0
  let match: RegExpExecArray | null

  while ((match = regex.exec(source)) !== null) {
    if (match.index > lastIndex) {
      segments.push({
        text: decodeHtmlEntities(source.slice(lastIndex, match.index)),
        highlighted: false
      })
    }
    segments.push({
      text: decodeHtmlEntities(match[1] || ''),
      highlighted: true
    })
    lastIndex = regex.lastIndex
  }

  if (lastIndex < source.length) {
    segments.push({
      text: decodeHtmlEntities(source.slice(lastIndex)),
      highlighted: false
    })
  }

  if (segments.length === 0) {
    return [{ text: decodeHtmlEntities(source), highlighted: false }]
  }

  return segments
}

onMounted(() => {
  searchRecords.value = loadSearchRecords()
  nextTick(() => searchInput.value?.focus())
  const initialQ = String(route.query.q || '').trim()
  if (initialQ) {
    searchKeyword.value = initialQ
    handleSearch()
  }
})

onUnmounted(() => {
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
})

watch(() => route.query.q, (q) => {
  const nextQ = String(q || '').trim()
  if (!nextQ || nextQ === searchKeyword.value.trim()) {
    return
  }
  searchKeyword.value = nextQ
  handleSearch()
})

const handleCancel = () => router.back()

const handleFocus = () => {
  if (!searchKeyword.value.trim()) {
    showResults.value = false
  }
}

const handleInput = () => {
  if (!searchKeyword.value.trim()) {
    showResults.value = false
    postResults.value = []
    userResults.value = []
    postTotal.value = 0
    userTotal.value = 0
    searchError.value = ''
    return
  }
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = window.setTimeout(() => {
    handleSearch()
  }, 350)
}

const clearSearch = () => {
  searchKeyword.value = ''
  showResults.value = false
  postResults.value = []
  userResults.value = []
  postTotal.value = 0
  userTotal.value = 0
  searchError.value = ''
  router.replace({ path: '/search' })
  nextTick(() => searchInput.value?.focus())
}

const loadSearchRecords = (): SearchRecord[] => {
  try {
    const raw = localStorage.getItem(SEARCH_RECORDS_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed)
      ? parsed.filter(item => item && typeof item.keyword === 'string')
      : []
  } catch {
    return []
  }
}

const persistSearchRecords = () => {
  localStorage.setItem(SEARCH_RECORDS_KEY, JSON.stringify(searchRecords.value))
}

const addToHistory = (keyword: string) => {
  const normalizedKeyword = keyword.trim()
  if (!normalizedKeyword) return
  const record = searchRecords.value.find(item => item.keyword === normalizedKeyword)
  if (record) {
    record.count += 1
    record.lastAt = Date.now()
  } else {
    searchRecords.value.unshift({
      keyword: normalizedKeyword,
      count: 1,
      lastAt: Date.now()
    })
  }
  searchRecords.value = [...searchRecords.value]
    .sort((a, b) => b.lastAt - a.lastAt)
    .slice(0, 20)
  persistSearchRecords()
}

const executeSearch = async (append = false) => {
  const keyword = searchKeyword.value.trim()
  if (!keyword || loading.value) {
    return
  }

  searchError.value = ''
  loading.value = true
  try {
    if (append) {
      const postResult = await searchPosts(keyword, postPage.value, postSize.value)
      postResults.value = [...postResults.value, ...postResult.list]
      postTotal.value = postResult.total
    } else {
      const [userResult, postResult] = await Promise.all([
        searchUsers(keyword, 1, 8),
        searchPosts(keyword, postPage.value, postSize.value)
      ])
      userResults.value = userResult.list
      userTotal.value = userResult.total
      postResults.value = postResult.list
      postTotal.value = postResult.total
      showResults.value = true
    }
    hasMorePosts.value = postResults.value.length < postTotal.value
  } catch (e) {
    console.error('搜索失败:', e)
    searchError.value = '搜索服务暂不可用，请稍后重试'
    if (!append) {
      postResults.value = []
      userResults.value = []
      postTotal.value = 0
      userTotal.value = 0
      showResults.value = true
    } else if (postPage.value > 1) {
      postPage.value -= 1
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    return
  }
  addToHistory(keyword)
  postPage.value = 1
  await executeSearch(false)
  router.replace({ path: '/search', query: { q: keyword } })
}

const loadMore = async () => {
  if (!hasMorePosts.value || loading.value) {
    return
  }
  postPage.value += 1
  await executeSearch(true)
}

const clearHistory = () => {
  searchRecords.value = []
  persistSearchRecords()
}

const handleHistoryTagClick = async (keyword: string) => {
  searchKeyword.value = keyword
  await handleSearch()
}

const handleSuggestionClick = async (keyword: string) => {
  searchKeyword.value = keyword
  await handleSearch()
}

const handleHotTopicClick = async (keyword: string) => {
  searchKeyword.value = keyword
  await handleSearch()
}

const goPost = (id: number) => {
  router.push(`/posts/${id}`)
}

const goUser = (id: number) => {
  router.push(`/user/${id}`)
}
</script>

<style scoped>
.search-container {
  min-height: 100vh;
  background-color: #fafafa;
}

.search-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background-color: #ffffff;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.search-bar-wrapper {
  flex: 1;
  margin-right: 12px;
}

.search-bar {
  display: flex;
  align-items: center;
  background-color: #f5f5f5;
  border-radius: 8px;
  padding: 6px 12px;
}

.search-bar svg {
  width: 16px;
  height: 16px;
  color: #999;
  margin-right: 8px;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  background-color: transparent;
  font-size: 15px;
  color: #333;
  padding: 4px 0;
}

.clear-btn {
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-btn svg {
  width: 16px;
  height: 16px;
  color: #999;
  margin-right: 0;
}

.cancel-btn {
  background: none;
  border: none;
  font-size: 15px;
  color: #333;
  padding: 6px 0;
  cursor: pointer;
}

.history-section,
.suggestions-section,
.hot-topics-section {
  padding: 16px;
  background-color: #ffffff;
  margin-bottom: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.hot-icon {
  margin-right: 4px;
}

.clear-history {
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
}

.clear-history svg {
  width: 16px;
  height: 16px;
  color: #999;
}

.history-tags,
.suggestion-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag,
.suggestion-tag {
  padding: 6px 12px;
  background-color: #f5f5f5;
  border-radius: 16px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
}

.hot-topics-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-topic-item {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 0;
}

.topic-rank {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background-color: #ff4757;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  margin-right: 12px;
}

.topic-content {
  flex: 1;
}

.topic-title {
  font-size: 15px;
  color: #333;
  line-height: 1.4;
}

.topic-stats {
  font-size: 12px;
  color: #999;
}

.search-results {
  background-color: #ffffff;
}

.search-stats {
  padding: 12px 16px;
  font-size: 14px;
  color: #999;
  border-bottom: 1px solid #f0f0f0;
}

.search-error {
  padding: 10px 16px;
  color: #ff4d4f;
  font-size: 13px;
}

.results-list {
  padding: 0 16px;
}

.result-section {
  margin-bottom: 24px;
}

.result-section .section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 16px 0;
}

.user-results {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.user-result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  cursor: pointer;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.user-bio {
  margin-top: 2px;
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-stats {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
  display: flex;
  gap: 10px;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.note-result-item {
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s, box-shadow 0.2s;
}

.note-image {
  position: relative;
  width: 100%;
  height: 150px;
  overflow: hidden;
}

.note-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.note-likes {
  position: absolute;
  bottom: 8px;
  left: 8px;
  background-color: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.note-content {
  padding: 12px;
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.note-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.note-excerpt {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.note-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.no-results {
  text-align: center;
  padding: 64px 16px;
  color: #999;
}

.no-results svg {
  width: 48px;
  height: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.no-results h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #333;
}

.no-results p {
  font-size: 14px;
  margin: 0;
}

.load-more {
  text-align: center;
  padding: 24px 0;
}

.load-more-btn {
  padding: 10px 24px;
  background-color: #f5f5f5;
  color: #666;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
}

:deep(.search-hit) {
  color: #ff4d4f;
  font-style: normal;
  font-weight: 600;
}

@media (min-width: 768px) {
  .search-container {
    max-width: 768px;
    margin: 0 auto;
  }

  .notes-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 767px) {
  .notes-grid {
    grid-template-columns: 1fr;
  }
}
</style>
