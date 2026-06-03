<template>
  <div class="post-detail-container">
    <!-- 页面头部，包含返回按钮和标题 -->
    <div class="page-header">
      <button class="back-button" @click="goBack">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12"></line>
          <polyline points="12 19 5 12 5"></polyline>
        </svg>
      </button>
      <h2 class="page-title">帖子详情</h2>
      <div class="header-right">
        <button v-if="isPostOwner" class="header-action-btn more-btn" @click="toggleMoreMenu" title="更多操作">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="1"></circle>
            <circle cx="12" cy="5" r="1"></circle>
            <circle cx="12" cy="19" r="1"></circle>
          </svg>
        </button>
      </div>
      
      <!-- 更多操作菜单 -->
      <div v-if="showMoreMenu" class="more-menu-overlay" @click="showMoreMenu = false">
        <div class="more-menu" @click.stop>
          <button class="more-menu-item" @click="openEditDialog">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
            </svg>
            <span>编辑</span>
          </button>
          <button class="more-menu-item" @click="showPostSettings">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3"></circle>
              <path d="M12 1v6m0 6v6m9-9h-6m-6 0H3"></path>
            </svg>
            <span>权限设置</span>
          </button>
          <button class="more-menu-item delete-item" @click="handleDeletePost">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
            <span>删除帖子</span>
          </button>
        </div>
      </div>
    </div>
    
    <!-- 编辑帖子弹窗 -->
    <div v-if="showEditDialog" class="modal-overlay" @click="closeEditDialog">
      <div class="modal-content edit-modal" @click.stop>
        <div class="modal-header">
          <h3>编辑帖子</h3>
          <button class="close-btn" @click="closeEditDialog">×</button>
        </div>
        <div class="modal-body edit-modal-body">
          <div class="edit-form">
            <div class="form-item">
              <label class="form-label">标题</label>
              <input 
                v-model="editForm.title" 
                type="text" 
                class="form-input"
                placeholder="请输入标题（必填）"
                maxlength="100"
              />
            </div>
            <div class="form-item">
              <label class="form-label">内容</label>
              <textarea 
                v-model="editForm.content" 
                class="form-textarea"
                placeholder="说点什么或提个问题..."
                rows="8"
              ></textarea>
            </div>
            <div class="form-item">
              <label class="form-label">分类</label>
              <div class="category-select">
                <div 
                  v-for="category in editCategories" 
                  :key="category.id"
                  class="category-option-edit"
                  :class="{ active: editForm.categoryId === category.id }"
                  @click="selectEditCategory(category.id)"
                >
                  <span class="category-name">{{ category.name }}</span>
                  <svg v-if="editForm.categoryId === category.id" class="check-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                </div>
              </div>
            </div>
            <div class="form-actions">
              <button class="cancel-btn" @click="closeEditDialog">取消</button>
              <button class="save-btn" @click="handleSaveEdit" :disabled="!canSaveEdit || isSaving">
                {{ isSaving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 权限设置弹窗 -->
    <div v-if="showSettingsDialog" class="modal-overlay" @click="showSettingsDialog = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>设置帖子权限</h3>
          <button class="close-btn" @click="showSettingsDialog = false">×</button>
        </div>
        <div class="modal-body">
          <div class="permission-option" @click="updatePostPermission(0)">
            <div class="option-info">
              <strong>公开</strong>
              <span>所有人可见</span>
            </div>
            <div class="option-check" v-if="post?.status === 0">✓</div>
          </div>
          <div class="permission-option" @click="updatePostPermission(1)">
            <div class="option-info">
              <strong>仅自己可见</strong>
              <span>只有你可以看到</span>
            </div>
            <div class="option-check" v-if="post?.status === 1">✓</div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="content-wrapper">
      <!-- 帖子详情内容 -->
      <div v-if="post" class="post-detail-content">
        <!-- 用户信息区域 -->
        <div class="user-info-section">
            <div @click="navigateToUserProfile(post.user.id)" class="user-info-clickable">
            <img :src="post.user.avatar || 'https://via.placeholder.com/100?text=U'" :alt="post.user.name" class="user-avatar" />
            <div class="user-details">
              <h3 class="user-name">{{ post.user.name }}</h3>
              <p class="post-time">{{ formatDate(post.createdAt) }}</p>
            </div>
          </div>
          <!-- 只在非自己的帖子时显示关注按钮 -->
          <button 
            v-if="!isPostOwner" 
            class="follow-btn" 
            :class="{ following: isFollowing }" 
            @click="toggleFollow"
          >
            {{ isFollowing ? '已关注' : '关注' }}
          </button>
          <button
            v-if="!isPostOwner"
            class="message-btn"
            @click="sendPrivateMessageToAuthor"
          >
            私信
          </button>
        </div>

        <!-- 帖子标题 -->
        <div class="post-title-section">
          <h2 class="post-title">{{ post.title || '无标题' }}</h2>
        </div>
        
        <!-- 帖子文本内容 -->
        <div class="post-text-section">
          <p class="post-text">{{ post.text }}</p>
        </div>

        <!-- 帖子图片内容 -->
        <div v-if="post.images && post.images.length > 0" class="post-images-section">
          <div 
            v-for="(image, index) in post.images" 
            :key="index" 
            class="post-image-container"
            :class="{ 'single-image': post.images.length === 1 }"
          >
            <img :src="image.url" :alt="image.description || `图片 ${index + 1}`" class="post-image" />
          </div>
        </div>

        <!-- 互动区域 -->
        <div class="post-actions-section">
          <button class="action-btn like-btn" :class="{ liked: post.isLiked }" @click="toggleLike">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
            </svg>
            <span>{{ formatNumber(post.likes) }}</span>
          </button>
          <button class="action-btn comment-btn" @click="toggleCommentSection">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
            </svg>
            <span>{{ formatNumber(post.comments) }}</span>
          </button>
          <button class="action-btn favorite-btn" :class="{ favorited: post.collected }" @click="toggleFavorite">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
            </svg>
            <span>{{ post.collected ? '已收藏' : '收藏' }}</span>
          </button>
          <button class="action-btn share-btn" @click="sharePost">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="18" cy="5" r="3"></circle>
              <circle cx="6" cy="12" r="3"></circle>
              <circle cx="18" cy="19" r="3"></circle>
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line>
              <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
            </svg>
            <span>分享</span>
          </button>
          <button v-if="!isPostOwner" class="action-btn report-btn" @click="reportPost">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 3v18"></path>
              <path d="M3 5h12l-2 4 2 4H3"></path>
            </svg>
            <span>举报</span>
          </button>
        </div>

        <!-- 评论区域 -->
        <div v-if="showComments" class="comments-section">
          <h3 class="section-title">评论 ({{ formatNumber(post.comments) }})</h3>
          
          <!-- 评论列表 -->
          <div class="comments-list">
            <div v-if="comments.length > 0" class="comment-item" v-for="comment in comments" :key="comment.id">
              <img
                :src="comment.user.avatar || 'https://via.placeholder.com/100?text=U'"
                :alt="comment.user.name"
                class="comment-avatar comment-user-clickable"
                @click="navigateToCommentUserProfile(comment.user)"
              />
              <div class="comment-content">
                <div class="comment-header">
                  <span class="comment-user comment-user-clickable" @click="navigateToCommentUserProfile(comment.user)">
                    {{ comment.user.name }}
                  </span>
                  <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
                </div>
                <p class="comment-text">{{ comment.text }}</p>
                <div class="comment-actions">
                  <button class="like-comment-btn" :class="{ liked: comment.isLiked }" @click="toggleCommentLike(comment)">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                    </svg>
                    <span>{{ formatNumber(comment.likes) }}</span>
                  </button>
                  <button class="reply-comment-btn" @click="replyToComment(comment)">
                    回复
                  </button>
                  <button
                    v-if="comment.user.id !== currentUserId"
                    class="report-comment-btn"
                    @click="reportComment(comment)"
                  >
                    举报
                  </button>
                </div>
              </div>
            </div>
            
            <div v-else class="no-comments">
              <p>暂无评论，快来发表第一条评论吧！</p>
            </div>
          </div>

          <!-- 发表评论 -->
          <div class="comment-input-section">
            <img :src="currentUserAvatar" alt="Your avatar" class="your-avatar" />
            <div class="comment-input-wrapper">
              <input 
                v-model="newComment" 
                type="text" 
                placeholder="写下你的评论..." 
                class="comment-input"
                @keyup.enter="submitComment"
              />
              <button 
                class="submit-comment-btn" 
                :disabled="!newComment.trim() || isSubmittingComment"
                @click="submitComment"
              >
                {{ isSubmittingComment ? '发送中...' : '发送' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-else-if="loading" class="loading-section">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <!-- 错误状态 -->
      <div v-else class="error-section">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
        <p>帖子不存在或已被删除</p>
        <button class="back-to-discover" @click="goBackToDiscover">返回发现页</button>
      </div>
    </div>

    <div v-if="reportDialogVisible" class="report-dialog-overlay" @click.self="closeReportDialog">
      <div class="report-dialog">
        <h3>提交举报</h3>
        <p class="report-target-text">{{ reportDialogTarget?.type === 1 ? '举报帖子' : '举报评论' }}</p>
        <textarea
          v-model.trim="reportReason"
          class="report-textarea"
          maxlength="255"
          placeholder="请填写举报原因（必填）"
        ></textarea>
        <div class="report-dialog-actions">
          <button class="report-cancel-btn" :disabled="reportSubmitting" @click="closeReportDialog">取消</button>
          <button class="report-submit-btn" :disabled="reportSubmitting || !reportReason" @click="submitReport">
            {{ reportSubmitting ? '提交中...' : '提交举报' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="reportToastVisible" class="report-toast" @click="hideReportToast">{{ reportToastText }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchPostById, fetchCommentsByPostId, createComment, deletePost, updatePostStatus, updatePost, fetchPostCategories, toggleFavorite as toggleFavoriteAPI, togglePostLike, toggleCommentLike as toggleCommentLikeAPI, submitForumReport, type PostCategory } from '../../api/forum'
import { followUser, unfollowUser } from '../../api/user'
import { getStoredUserInfoObject } from '../../utils/authStorage'
import HistoryManager from '../../utils/historyManager'
import { showConfirm } from '../../utils/dialog'
import { notify } from '@/utils/notify'

/** ==== 本文件内部定义简化类型，避免外部依赖 ==== */
interface SimpleUser {
  id: number
  name: string
  avatar: string
  followers?: number
  following?: number
  bio?: string
}

interface SimpleImage {
  id: number
  url: string
  description?: string
  width?: number
  height?: number
}

interface SimplePost {
  id: number
  title?: string
  text: string
  images: SimpleImage[]
  user: SimpleUser
  likes: number
  comments: number
  createdAt: string
  isLiked: boolean
  collected: boolean
  followed: boolean
  views: number
  status?: number
  categoryId?: number | null // 分类ID
}

interface SimpleComment {
  id: number
  text: string
  user: SimpleUser
  likes: number
  createdAt: string
  isLiked: boolean
}

/** ==== 路由 & 状态 ==== */
const route = useRoute()
const router = useRouter()

const post = ref<SimplePost | null>(null)
const comments = ref<SimpleComment[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const isFollowing = ref(false)
const showComments = ref(true)
const newComment = ref('')
const replyingTo = ref<SimpleComment | null>(null)
const isSubmittingComment = ref(false)
const showMoreMenu = ref(false)
const showSettingsDialog = ref(false)
const showEditDialog = ref(false)
const isSaving = ref(false)
const isTogglingLike = ref(false)
const reportDialogVisible = ref(false)
const reportDialogTarget = ref<{ type: 1 | 2; id: number } | null>(null)
const reportReason = ref('')
const reportSubmitting = ref(false)
const reportToastVisible = ref(false)
const reportToastText = ref('')
let reportToastTimer: number | null = null

// 编辑表单数据
const editForm = ref({
  title: '',
  content: '',
  categoryId: null as number | null
})

// 编辑分类列表
const editCategories = ref<PostCategory[]>([])
const loadingCategories = ref(false)

// 当前登录用户ID
const currentUserId = ref(0)

// 当前登录用户头像
const currentUserAvatar = ref('https://via.placeholder.com/100?text=U')

// 获取当前登录用户信息
const getCurrentUserInfo = () => {
  try {
    const userInfo = getStoredUserInfoObject<any>()
    if (userInfo) {
      currentUserId.value = userInfo.id || 0
      if (userInfo.icon) {
        currentUserAvatar.value = userInfo.icon
      } else if (userInfo.avatarUrl) {
        currentUserAvatar.value = userInfo.avatarUrl
      }
    }
  } catch (e) {
    console.error('获取用户信息失败:', e)
  }
}

// 判断是否为帖子作者
const isPostOwner = computed(() => {
  return post.value && currentUserId.value > 0 && post.value.user.id === currentUserId.value
})

/** ==== 获取路由中的帖子 ID ==== */
const getPostId = (): number => {
  const id = route.params.id
  if (Array.isArray(id)) {
    return parseInt(id[0] || '0')
  }
  return parseInt((id as string) || '0')
}

/** ==== 加载帖子详情（从后端API获取） ==== */
const fetchPostDetail = async () => {
  loading.value = true
  error.value = null
  const postId = getPostId()

  try {
    const postData = await fetchPostById(postId)
    
    if (postData) {
      // 转换后端数据格式为前端格式
      post.value = {
        id: postData.id,
        title: postData.title || '',
        text: postData.content || postData.title || '',
        images: (postData.images || []).map((img: any) => ({
          id: img.id || 0,
          url: img.url,
          description: '',
          width: img.width,
          height: img.height
        })),
        user: {
          id: Number(postData.authorId || postData.userId || postData.user?.id || 0),
          name: postData.authorName || '用户',
          avatar: postData.authorAvatar || 'https://via.placeholder.com/100?text=U'
        },
        likes: postData.likeCount || 0,
        comments: postData.commentCount || 0,
        createdAt: postData.createdAt || new Date().toISOString(),
        isLiked: postData.isLiked || false,
        collected: postData.isCollected || false,
        followed: postData.isFollowed || false,
        views: postData.viewCount || 0,
        categoryId: postData.categoryId || null
      }

      // 写入个人浏览历史（去重，按最近时间排序）
      HistoryManager.addHistory({
        id: postData.id,
        title: postData.title || '无标题',
        authorName: postData.authorName || '用户',
        imageUrls: postData.images && postData.images.length > 0 ? postData.images.map((img: any) => img.url) : (postData.coverImage ? [postData.coverImage] : [])
      })
      
      // 设置关注状态（从后端返回的数据中获取）
      // 确保正确使用后端返回的 isFollowed 字段
      if (postData.isFollowed !== undefined) {
        isFollowing.value = Boolean(postData.isFollowed)
      } else {
        // 如果后端没有返回，默认为 false
        isFollowing.value = false
      }
      
      // 调试信息
      console.log('帖子详情加载完成:', {
        postId: postData.id,
        authorId: postData.authorId,
        isFollowed: postData.isFollowed,
        isFollowing: isFollowing.value
      })
      
      // 加载评论
      try {
        const commentsData = await fetchCommentsByPostId(postId)
        comments.value = commentsData.map((c: any) => ({
          id: c.id,
          text: c.content,
          user: {
            id: c.authorId || 0,
            name: c.authorName || '用户',
            avatar: c.authorAvatar || 'https://via.placeholder.com/100?text=U'
          },
          likes: c.likeCount || 0,
          createdAt: c.createdAt || new Date().toISOString(),
          isLiked: false
        })) || []
      } catch (e) {
        console.warn('加载评论失败:', e)
        comments.value = []
      }
    } else {
      post.value = null
      comments.value = []
      error.value = 'NOT_FOUND'
    }
  } catch (e: any) {
    console.error('加载帖子详情失败:', e)
    post.value = null
    comments.value = []
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

/** ==== 工具方法 ==== */
const formatDate = (dateString: string): string => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN')
}

const formatNumber = (num: number): string => {
  if (num < 1000) return num.toString()
  if (num < 10000) return (num / 1000).toFixed(1) + 'k'
  return (num / 10000).toFixed(1) + 'w'
}

/** ==== 互动逻辑 ==== */
const toggleLike = async () => {
  if (!post.value || isTogglingLike.value) return
  if (currentUserId.value === 0) {
    notify('请先登录')
    return
  }

  const oldLiked = post.value.isLiked
  const oldCount = post.value.likes
  post.value.isLiked = !oldLiked
  post.value.likes = Math.max(0, oldCount + (post.value.isLiked ? 1 : -1))
  isTogglingLike.value = true
  try {
    const result = await togglePostLike(post.value.id)
    post.value.isLiked = Boolean(result.liked)
    post.value.likes = Number(result.likeCount || 0)
  } catch (error: any) {
    post.value.isLiked = oldLiked
    post.value.likes = oldCount
    notify(error?.message || '点赞失败，请稍后重试')
  } finally {
    isTogglingLike.value = false
  }
}

// 收藏/取消收藏
const toggleFavorite = async () => {
  if (!post.value) return
  
  // 检查是否登录
  if (currentUserId.value === 0) {
    notify('请先登录')
    return
  }
  
  const wasFavorited = post.value.collected
  
  // 乐观更新
  post.value.collected = !post.value.collected
  
  try {
    const result = await toggleFavoriteAPI(post.value.id)
    // 使用后端返回的状态
    if (result && result.favorited !== undefined) {
      post.value.collected = result.favorited
    }
  } catch (error: any) {
    // 回滚
    post.value.collected = wasFavorited
    console.error('收藏操作失败:', error)
    notify(error.message || '操作失败，请重试')
  }
}

// 关注/取消关注用户
const toggleFollow = async () => {
  if (!post.value) return
  
  // 如果是自己的帖子，不显示关注按钮（已经在模板中通过 v-if 控制）
  if (post.value.user.id === currentUserId.value) {
    return
  }
  
  const targetUserId = post.value.user.id
  const wasFollowing = isFollowing.value
  
  // 乐观更新：先更新UI
  isFollowing.value = !isFollowing.value
  
  try {
    if (wasFollowing) {
      // 取消关注
      await unfollowUser(targetUserId)
    } else {
      // 关注
      await followUser(targetUserId)
    }
    // 更新帖子中的关注状态
    if (post.value) {
      post.value.followed = isFollowing.value
    }
  } catch (error: any) {
    // 如果失败，回滚状态
    isFollowing.value = wasFollowing
    console.error('关注操作失败:', error)
    notify(error.message || (wasFollowing ? '取消关注失败' : '关注失败'))
  }
}

const toggleCommentSection = () => {
  showComments.value = !showComments.value
}

const openReportDialog = (type: 1 | 2, id: number) => {
  reportDialogTarget.value = { type, id }
  reportReason.value = ''
  reportDialogVisible.value = true
}

const closeReportDialog = (force = false) => {
  if (reportSubmitting.value && !force) return
  reportDialogVisible.value = false
  reportDialogTarget.value = null
  reportReason.value = ''
}

const showReportToast = (text: string) => {
  reportToastText.value = text
  reportToastVisible.value = true
  if (reportToastTimer) {
    window.clearTimeout(reportToastTimer)
  }
  reportToastTimer = window.setTimeout(() => {
    reportToastVisible.value = false
  }, 1800)
}

const hideReportToast = () => {
  reportToastVisible.value = false
  if (reportToastTimer) {
    window.clearTimeout(reportToastTimer)
    reportToastTimer = null
  }
}

const submitReport = async () => {
  if (!reportDialogTarget.value) return
  if (currentUserId.value === 0) {
    showReportToast('请先登录')
    return
  }
  if (!reportReason.value.trim()) {
    showReportToast('请填写举报原因')
    return
  }
  reportSubmitting.value = true
  try {
    await submitForumReport({
      targetType: reportDialogTarget.value.type,
      targetId: reportDialogTarget.value.id,
      reason: reportReason.value.trim()
    })
    closeReportDialog(true)
    showReportToast('举报已提交，等待管理员处理')
  } catch (error: any) {
    showReportToast(error?.message || '举报失败')
  } finally {
    reportSubmitting.value = false
  }
}

const reportPost = async () => {
  if (!post.value) return
  if (currentUserId.value === 0) {
    showReportToast('请先登录')
    return
  }
  openReportDialog(1, post.value.id)
}

const reportComment = async (comment: SimpleComment) => {
  if (currentUserId.value === 0) {
    showReportToast('请先登录')
    return
  }
  openReportDialog(2, comment.id)
}

const submitComment = async () => {
  if (!newComment.value.trim() || !post.value || isSubmittingComment.value) return
  if (currentUserId.value === 0) {
    notify('请先登录后再评论')
    return
  }

  const content = newComment.value.trim()
  isSubmittingComment.value = true
  try {
    const created = await createComment({
      postId: post.value.id,
      content
    })

    comments.value.unshift({
      id: created.id,
      text: created.content,
      user: {
        id: created.authorId || currentUserId.value,
        name: created.authorName || '我',
        avatar: created.authorAvatar || currentUserAvatar.value
      },
      likes: 0,
      createdAt: created.createdAt || new Date().toISOString(),
      isLiked: false
    })
    post.value.comments += 1
    newComment.value = ''
    replyingTo.value = null
  } catch (error: any) {
    console.error('发表评论失败:', error)
    notify(error.message || '发表评论失败，请重试')
  } finally {
    isSubmittingComment.value = false
  }
}

const toggleCommentLike = async (comment: SimpleComment) => {
  if (currentUserId.value === 0) {
    notify('请先登录')
    return
  }
  const oldLiked = comment.isLiked
  const oldCount = comment.likes
  comment.isLiked = !oldLiked
  comment.likes = Math.max(0, oldCount + (comment.isLiked ? 1 : -1))
  try {
    const result = await toggleCommentLikeAPI(comment.id)
    comment.isLiked = Boolean(result.liked)
    comment.likes = Number(result.likeCount || 0)
  } catch (error: any) {
    comment.isLiked = oldLiked
    comment.likes = oldCount
    notify(error?.message || '评论点赞失败，请稍后重试')
  }
}

const replyToComment = (comment: SimpleComment) => {
  replyingTo.value = comment
  newComment.value = `@${comment.user.name} `
  setTimeout(() => {
    const input = document.querySelector('.comment-input') as HTMLInputElement | null
    if (input) input.focus()
  }, 50)
}

/** ==== 路由跳转 ==== */
const goBackToDiscover = () => {
  router.push('/home') // 你的发现页路由是 /home
}

const goBack = () => {
  router.back()
}

// 切换更多菜单
const toggleMoreMenu = () => {
  showMoreMenu.value = !showMoreMenu.value
}

// 显示编辑对话框
const openEditDialog = () => {
  if (!post.value) return
  
  showMoreMenu.value = false
  
  // 初始化编辑表单数据
  editForm.value = {
    title: post.value.title || '',
    content: post.value.text || '',
    categoryId: post.value.categoryId || null
  }
  
  // 加载分类列表
  loadEditCategories()
  
  // 显示编辑对话框
  showEditDialog.value = true
}

// 关闭编辑对话框
const closeEditDialog = () => {
  showEditDialog.value = false
  editForm.value = {
    title: '',
    content: '',
    categoryId: null
  }
}

// 加载分类列表
const loadEditCategories = async () => {
  loadingCategories.value = true
  try {
    const categories = await fetchPostCategories()
    editCategories.value = categories
  } catch (error) {
    console.error('加载分类失败:', error)
  } finally {
    loadingCategories.value = false
  }
}

// 选择编辑分类
const selectEditCategory = (categoryId: number) => {
  editForm.value.categoryId = categoryId
}

// 检查是否可以保存
const canSaveEdit = computed(() => {
  return editForm.value.title.trim().length > 0 && 
         editForm.value.content.trim().length > 0 &&
         !isSaving.value
})

// 保存编辑
const handleSaveEdit = async () => {
  if (!post.value || !canSaveEdit.value) return
  
  isSaving.value = true
  
  try {
    await updatePost(post.value.id, {
      title: editForm.value.title.trim(),
      content: editForm.value.content.trim(),
      categoryId: editForm.value.categoryId || undefined
    })
    
    // 更新本地数据
    if (post.value) {
      post.value.title = editForm.value.title.trim()
      post.value.text = editForm.value.content.trim()
    }
    
    // 关闭对话框
    closeEditDialog()
    
    // 重新加载帖子详情以获取最新数据
    await fetchPostDetail()
    
    notify('帖子已更新')
  } catch (error: any) {
    console.error('保存编辑失败:', error)
    notify(error.message || '保存失败，请重试')
  } finally {
    isSaving.value = false
  }
}

// 显示权限设置
const showPostSettings = () => {
  showMoreMenu.value = false
  showSettingsDialog.value = true
}

// 处理删除帖子
const handleDeletePost = async () => {
  if (!post.value) return
  
  showMoreMenu.value = false
  
  const confirmed = await showConfirm('确定要删除这条帖子吗？删除后无法恢复。')
  if (!confirmed) {
    return
  }
  
  try {
    await deletePost(post.value.id)
    notify('删除成功')
    router.push('/me/posts')
  } catch (error: any) {
    console.error('删除帖子失败:', error)
    notify(error.message || '删除失败，请重试')
  }
}

// 更新帖子权限
const updatePostPermission = async (status: number) => {
  if (!post.value) return
  
  try {
    await updatePostStatus(post.value.id, status)
    const statusText = status === 0 ? '公开' : status === 1 ? '仅自己可见' : '未知'
    notify(`权限已更新为：${statusText}`)
    showSettingsDialog.value = false
    // 更新本地状态
    if (post.value) {
      (post.value as any).status = status
    }
  } catch (error: any) {
    console.error('更新权限失败:', error)
    notify(error.message || '更新权限失败，请重试')
  }
}

const navigateToUserProfile = (userId: number) => {
  if (!userId || userId <= 0 || !post.value) {
    notify('用户信息不完整，无法跳转主页')
    return
  }
  router.push({
    path: `/user/${userId}`,
    query: {
      name: post.value.user.name,
      avatarUrl: post.value.user.avatar
    }
  })
}

const navigateToCommentUserProfile = (user: SimpleUser) => {
  if (!user || !user.id || user.id <= 0) {
    notify('用户信息不完整，无法跳转主页')
    return
  }
  router.push({
    path: `/user/${user.id}`,
    query: {
      name: user.name,
      avatarUrl: user.avatar
    }
  })
}

const sendPrivateMessageToAuthor = () => {
  if (!post.value || !post.value.user.id) {
    notify('用户信息不完整，无法发起私信')
    return
  }
  router.push({
    path: `/messages/${post.value.user.id}`,
    query: {
      name: post.value.user.name,
      avatarUrl: post.value.user.avatar
    }
  })
}

/** ==== 分享 ==== */
const sharePost = () => {
  if (!post.value) return
  if (navigator.share) {
    navigator.share({
      title: post.value.user.name,
      text: post.value.text,
      url: window.location.href
    }).catch(err => console.error('分享失败:', err))
  } else {
    navigator.clipboard.writeText(window.location.href)
      .then(() => notify('链接已复制到剪贴板'))
      .catch(err => console.error('复制失败:', err))
  }
}

/** ==== 生命周期 ==== */
onMounted(() => {
  getCurrentUserInfo() // 获取当前用户头像
  fetchPostDetail()
})

onUnmounted(() => {
  if (reportToastTimer) {
    window.clearTimeout(reportToastTimer)
  }
})

// 监听路由变化，当帖子ID改变时重新加载数据
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    fetchPostDetail()
  }
})
</script>

<style scoped>
/* 保留你原来的样式，这里略 */
.post-detail-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.report-dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2100;
}

.report-dialog {
  width: min(88vw, 360px);
  border-radius: 14px;
  background: #fff;
  padding: 14px 14px 12px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.16);
}

.report-dialog h3 {
  margin: 0;
  font-size: 17px;
  color: #1f1f1f;
}

.report-target-text {
  margin: 6px 0 10px;
  color: #8a8a8a;
  font-size: 12px;
}

.report-textarea {
  width: 100%;
  min-height: 92px;
  resize: none;
  border: 1px solid #e9e9e9;
  border-radius: 10px;
  padding: 10px;
  font-size: 14px;
  outline: none;
}

.report-dialog-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.report-cancel-btn,
.report-submit-btn {
  flex: 1;
  height: 36px;
  border: none;
  border-radius: 18px;
  font-size: 14px;
}

.report-cancel-btn {
  background: #f2f2f2;
  color: #666;
}

.report-submit-btn {
  background: #1677ff;
  color: #fff;
}

.report-submit-btn:disabled,
.report-cancel-btn:disabled {
  opacity: 0.6;
}

.report-toast {
  position: fixed;
  left: 50%;
  bottom: 92px;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: #fff;
  border-radius: 18px;
  padding: 8px 14px;
  font-size: 13px;
  z-index: 2150;
}

/* 页面头部 */
.page-header {
  position: sticky;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: white;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  z-index: 100;
}

/* 返回按钮 */
.back-button {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: transparent;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-button:hover {
  background-color: #f5f5f5;
}

.back-button svg {
  width: 22px;
  height: 22px;
}

/* 页面标题 */
.page-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin: 0;
  flex: 1;
  text-align: center;
}

.header-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.header-action-btn {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.header-action-btn:hover {
  background-color: #f5f5f5;
}

.header-action-btn svg {
  width: 20px;
  height: 20px;
}

.header-action-btn.more-btn svg {
  width: 20px;
  height: 20px;
}

/* 更多操作菜单 */
.more-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.3);
  z-index: 999;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 50px 16px 0;
}

.more-menu {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 160px;
  overflow: hidden;
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.more-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 16px;
  border: none;
  background: none;
  text-align: left;
  font-size: 15px;
  color: #333;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f0f0f0;
}

.more-menu-item:last-child {
  border-bottom: none;
}

.more-menu-item:hover {
  background-color: #f5f5f5;
}

.more-menu-item.delete-item {
  color: #ff4d4f;
}

.more-menu-item.delete-item:hover {
  background-color: #fff1f0;
}

.more-menu-item svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.more-menu-item span {
  flex: 1;
}

/* 权限设置弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-content.edit-modal {
  max-width: 600px;
  max-height: 90vh;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.close-btn:hover {
  background-color: #f5f5f5;
}

.modal-body {
  padding: 20px;
}

.permission-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;
}

.permission-option:hover {
  background-color: #fafafa;
  border-color: #d9d9d9;
}

.option-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.option-info strong {
  font-size: 16px;
  color: #333;
}

.option-info span {
  font-size: 14px;
  color: #666;
}

.option-check {
  color: #1677ff;
  font-size: 20px;
  font-weight: bold;
}

.content-wrapper {
  padding-bottom: 20px;
}

.post-detail-content {
  background-color: white;
  margin-bottom: 10px;
}

.user-info-section {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.user-info-clickable {
  display: flex;
  align-items: center;
  cursor: pointer;
  flex: 1;
}

.user-info-clickable:hover .user-name {
  color: #1890ff;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.user-details {
  flex: 1;
  margin-left: 12px;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px 0;
}

.post-time {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.follow-btn {
  padding: 6px 16px;
  border: 1px solid #1890ff;
  border-radius: 16px;
  background-color: white;
  color: #1890ff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.follow-btn.following {
  border-color: #999;
  color: #999;
  background-color: #f5f5f5;
}

.message-btn {
  margin-left: 8px;
  padding: 6px 16px;
  border: 1px solid #ddd;
  border-radius: 16px;
  background-color: #fff;
  color: #333;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.message-btn:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.post-title-section {
  padding: 16px 16px 0;
}

.post-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 12px 0;
  color: #333;
  line-height: 1.4;
}

.post-text-section {
  padding: 16px;
}

.post-text {
  font-size: 16px;
  line-height: 1.6;
  margin: 0;
  word-wrap: break-word;
}

.post-images-section {
  padding: 0 16px 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.post-image-container {
  flex: 1;
  min-width: calc(50% - 4px);
  aspect-ratio: 1 / 1;
  overflow: hidden;
  border-radius: 8px;
}

.post-image-container.single-image {
  min-width: 100%;
  aspect-ratio: 16 / 9;
}

.post-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.post-actions-section {
  display: flex;
  justify-content: space-around;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border: none;
  background: none;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.action-btn:hover {
  color: #1890ff;
}

.action-btn.liked {
  color: #ff4757;
}

.action-btn.favorited {
  color: #2f3542;
}

.action-btn.favorite-btn {
  color: #666;
}

.action-btn.favorite-btn.favorited {
  color: #2f3542;
  font-weight: 600;
}

.action-btn.report-btn {
  color: #d46b08;
}

.action-btn svg {
  width: 20px;
  height: 20px;
}

.comments-section {
  padding: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 16px 0;
}

.comments-list {
  margin-bottom: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.comment-user-clickable {
  cursor: pointer;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.comment-user.comment-user-clickable:hover {
  color: #1890ff;
}

.comment-user {
  font-weight: 500;
  font-size: 14px;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-text {
  font-size: 14px;
  line-height: 1.5;
  margin: 0 0 8px 0;
  word-wrap: break-word;
}

.comment-actions {
  display: flex;
  gap: 16px;
}

.like-comment-btn, .reply-comment-btn, .report-comment-btn {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px 0;
  border: none;
  background: none;
  font-size: 12px;
  color: #999;
  cursor: pointer;
  transition: all 0.3s;
}

.like-comment-btn:hover, .reply-comment-btn:hover, .report-comment-btn:hover {
  color: #1890ff;
}

.like-comment-btn.liked {
  color: #ff4757;
}

.like-comment-btn svg {
  width: 14px;
  height: 14px;
}

.no-comments {
  text-align: center;
  padding: 24px;
  color: #999;
}

.comment-input-section {
  display: flex;
  gap: 12px;
  align-items: center;
}

.your-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.comment-input-wrapper {
  flex: 1;
  position: relative;
}

.comment-input {
  width: 100%;
  padding: 8px 60px 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}

.comment-input:focus {
  border-color: #1890ff;
}

.submit-comment-btn {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  padding: 4px 12px;
  border: none;
  border-radius: 12px;
  background-color: #1890ff;
  color: white;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-comment-btn:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}

.loading-section, .error-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  text-align: center;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-section svg {
  width: 48px;
  height: 48px;
  color: #ff4757;
  margin-bottom: 16px;
}

.back-to-discover {
  margin-top: 16px;
  padding: 8px 24px;
  border: 1px solid #1890ff;
  border-radius: 20px;
  background-color: white;
  color: #1890ff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.back-to-discover:hover {
  background-color: #1890ff;
  color: white;
}

/* 编辑弹窗样式 */
.edit-modal-body {
  padding: 20px;
  overflow-y: auto;
  max-height: calc(90vh - 120px);
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.3s;
}

.form-input:focus {
  border-color: #1890ff;
}

.form-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.3s;
  resize: vertical;
  min-height: 120px;
  font-family: inherit;
}

.form-textarea:focus {
  border-color: #1890ff;
}

.category-select {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.category-option-edit {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.category-option-edit:hover {
  background-color: #f5f5f5;
  border-color: #d9d9d9;
}

.category-option-edit.active {
  background-color: #e6f7ff;
  border-color: #1890ff;
}

.category-option-edit .category-name {
  font-size: 14px;
  color: #333;
}

.category-option-edit.active .category-name {
  color: #1890ff;
  font-weight: 500;
}

.category-option-edit .check-icon {
  width: 18px;
  height: 18px;
  color: #1890ff;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.cancel-btn {
  padding: 10px 24px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  background-color: white;
  color: #333;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn:hover {
  background-color: #f5f5f5;
  border-color: #bfbfbf;
}

.save-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  background-color: #1890ff;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.save-btn:hover:not(:disabled) {
  background-color: #40a9ff;
}

.save-btn:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
  color: #999;
}
</style>
