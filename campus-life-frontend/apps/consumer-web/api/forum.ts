// apps/consumer-web/api/forum.ts
import http from './http'

export interface ForumPost {
  id: number
  authorId: number
  authorName: string
  authorAvatar?: string
  title: string
  content: string
  coverImage?: string
  likeCount: number
  commentCount: number
  createdAt: string
  category: string
  isLiked?: boolean
}

export interface ForumComment {
  id: number
  postId: number
  authorId: number
  authorName: string
  authorAvatar?: string
  content: string
  createdAt: string
}

export interface SearchPostItem {
  id: number
  title: string
  content: string
  highlightTitle?: string
  highlightContent?: string
  authorName?: string
  coverImage?: string
  createTime?: string
  likeCount?: number
  commentCount?: number
}

export interface SearchUserItem {
  id: number
  username: string
  avatarUrl?: string
  bio?: string
  role?: number
  postCount?: number
  followerCount?: number
  createTime?: string
}

export interface ForumReportPayload {
  targetType: 1 | 2
  targetId: number
  reason: string
}

// ---- API调用函数 ----

// 获取帖子列表
export async function fetchPosts(
  options?: string | number | {
    categoryId?: string | number
    order?: 'latest' | 'hot' | 'follow'
    page?: number
    size?: number
  }
): Promise<ForumPost[]> {
  try {
    let categoryId: string | number | undefined
    let order: 'latest' | 'hot' | 'follow' = 'latest'
    let page = 1
    let size = 12

    if (typeof options === 'object' && options !== null) {
      categoryId = options.categoryId
      order = options.order || 'latest'
      page = options.page || 1
      size = options.size || 12
    } else {
      categoryId = options
    }

    const params: any = { order, page, size }
    if (categoryId) params.categoryId = categoryId
    const { data } = await http.get('/forum/posts', { params })
    
    if (data.code === 200 && data.data && data.data.list) {
      // 转换后端数据格式为前端格式
      const posts = data.data.list.map((post: any) => ({
        id: post.id,
        authorId: post.authorId || post.userId || 0,
        authorName: post.authorName || post.username || '用户',
        authorAvatar: post.authorAvatar || post.avatarUrl || '',
        title: post.title || '',
        content: post.content || '',
        coverImage: post.coverImage || '', // 从 post_image 表获取的第一张图片
        likeCount: post.likeCount || 0,
        commentCount: post.commentCount || 0,
        createdAt: post.createTime || post.createdAt || new Date().toISOString(),
        category: post.categoryCode || post.category || 'general',
        categoryId: post.categoryId || null,  // 分类ID
        isLiked: post.isLiked !== undefined ? Boolean(post.isLiked) : false
      }))
      
      return posts
    }
    return []
  } catch (error) {
    console.error('获取帖子列表失败:', error)
    throw error
  }
}

// 获取关注用户的帖子流（需登录）
export async function fetchFollowingPosts(page: number = 1, size: number = 12): Promise<ForumPost[]> {
  const { data } = await http.get('/forum/posts/following', { params: { page, size } })
  if (data.code === 200 && data.data && data.data.list) {
    return data.data.list.map((post: any) => ({
      id: post.id,
      authorId: post.authorId || post.userId || 0,
      authorName: post.authorName || post.username || '用户',
      authorAvatar: post.authorAvatar || post.avatarUrl || '',
      title: post.title || '',
      content: post.content || '',
      coverImage: post.coverImage || '',
      likeCount: post.likeCount || 0,
      commentCount: post.commentCount || 0,
      createdAt: post.createTime || post.createdAt || new Date().toISOString(),
      category: post.categoryCode || post.category || 'general',
      categoryId: post.categoryId || null,
      isLiked: post.isLiked !== undefined ? Boolean(post.isLiked) : false
    }))
  }
  return []
}

// 获取帖子详情
export async function fetchPostById(id: number): Promise<any | null> {
  try {
    const { data } = await http.get(`/forum/posts/${id}`)
    
    if (data.code === 200 && data.data) {
      const post = data.data
      return {
        id: post.id,
        authorId: post.authorId || post.userId || 0,
        authorName: post.authorName || post.username || '用户',
        authorAvatar: post.authorAvatar || post.avatarUrl || '',
        title: post.title || '',
        content: post.content || '',
        coverImage: post.coverImage || '',
        images: post.images || [], // 图片列表
        likeCount: post.likeCount || 0,
        commentCount: post.commentCount || 0,
        viewCount: post.viewCount || 0,
        createdAt: post.createTime || post.createdAt || new Date().toISOString(),
        category: post.categoryCode || post.category || 'general',
        categoryId: post.categoryId || null, // 分类ID
        isFollowed: post.isFollowed !== undefined ? post.isFollowed : false, // 是否已关注作者
        isLiked: post.isLiked !== undefined ? post.isLiked : false, // 是否已点赞
        isCollected: post.isCollected !== undefined ? post.isCollected : false // 是否已收藏
      }
    }
    return null
  } catch (error) {
    console.error('获取帖子详情失败:', error)
    return null
  }
}

// 获取评论列表
export async function fetchCommentsByPostId(postId: number): Promise<ForumComment[]> {
  try {
    const { data } = await http.get(`/forum/posts/${postId}/comments`)
    
    if (data.code === 200 && data.data && data.data.list) {
      return data.data.list.map((comment: any) => ({
        id: comment.id,
        postId: comment.postId || postId,
        authorId: comment.authorId || comment.userId || 0,
        authorName: comment.authorName || comment.author_name || comment.username || comment.userName || '用户',
        authorAvatar: comment.authorAvatar || comment.author_avatar || comment.avatarUrl || comment.userAvatar || '',
        content: comment.content || '',
        createdAt: comment.createTime || comment.createdAt || new Date().toISOString()
      }))
    }
    return []
  } catch (error) {
    console.error('获取评论列表失败:', error)
    return []
  }
}

// 创建帖子
export async function createPost(payload: {
  title?: string
  content: string
  images?: Array<{ url: string; width?: number; height?: number }>
  category?: string
  categoryId?: number
}): Promise<ForumPost> {
  try {
    const { data } = await http.post('/forum/posts', {
      title: payload.title || '',
      content: payload.content,
      images: payload.images || [],
      category: payload.category,
      categoryId: payload.categoryId
    })
    
    if (data.code === 200 && data.data) {
      const post = data.data
      return {
        id: post.id,
        authorId: post.authorId || post.userId || 0,
        authorName: post.authorName || post.username || '用户',
        authorAvatar: post.authorAvatar || post.avatarUrl || '',
        title: post.title || '',
        content: post.content || '',
        coverImage: post.coverImage || '',
        likeCount: post.likeCount || 0,
        commentCount: post.commentCount || 0,
        createdAt: post.createTime || post.createdAt || new Date().toISOString(),
        category: post.category || post.categoryCode || payload.category
      }
    }
    throw new Error('创建帖子失败')
  } catch (error) {
    console.error('创建帖子失败:', error)
    throw error
  }
}

// 创建评论
export async function createComment(payload: {
  postId: number
  content: string
}): Promise<ForumComment> {
  try {
    const { data } = await http.post(`/forum/posts/${payload.postId}/comments`, {
      content: payload.content
    })
    
    if (data.code === 200 && data.data) {
      const comment = data.data
      return {
        id: comment.id,
        postId: comment.postId || payload.postId,
        authorId: comment.authorId || comment.userId || 0,
        authorName: comment.authorName || comment.username || '用户',
        authorAvatar: comment.authorAvatar || comment.avatarUrl || '',
        content: comment.content || '',
        createdAt: comment.createTime || comment.createdAt || new Date().toISOString()
      }
    }
    throw new Error('创建评论失败')
  } catch (error) {
    console.error('创建评论失败:', error)
    throw error
  }
}

// 获取我的帖子列表
export async function fetchMyPosts(page: number = 1, size: number = 20): Promise<ForumPost[]> {
  try {
    const { data } = await http.get('/forum/posts/me', { params: { page, size } })
    if (data.code === 200 && data.data && data.data.list) {
      return data.data.list.map((post: any) => ({
        id: post.id,
        authorId: post.authorId || post.userId || 0,
        authorName: post.authorName || post.username || '用户',
        authorAvatar: post.authorAvatar || post.avatarUrl || '',
        title: post.title || '',
        content: post.content || '',
        coverImage: post.coverImage || '',
        images: post.images || [],
        likeCount: post.likeCount || 0,
        commentCount: post.commentCount || 0,
        viewCount: post.viewCount || 0,
        status: post.status || 0,
        createdAt: post.createTime || post.createdAt || new Date().toISOString(),
        category: post.categoryCode || post.category || 'general'
      }))
    }
    return []
  } catch (error) {
    console.error('获取我的帖子列表失败:', error)
    throw error
  }
}

// 获取指定用户帖子列表（公开）
export async function fetchPostsByUserId(userId: number, page: number = 1, size: number = 20): Promise<any[]> {
  try {
    const { data } = await http.get(`/forum/posts/user/${userId}`, { params: { page, size } })
    if (data.code === 200 && data.data && data.data.list) {
      return data.data.list.map((post: any) => ({
        id: post.id,
        authorId: post.authorId || post.userId || userId,
        authorName: post.authorName || post.username || '用户',
        authorAvatar: post.authorAvatar || post.avatarUrl || '',
        title: post.title || '',
        content: post.content || '',
        coverImage: post.coverImage || '',
        images: post.images || [],
        likeCount: post.likeCount || 0,
        commentCount: post.commentCount || 0,
        viewCount: post.viewCount || 0,
        createdAt: post.createTime || post.createdAt || new Date().toISOString(),
        isFollowed: post.isFollowed !== undefined ? Boolean(post.isFollowed) : false
      }))
    }
    return []
  } catch (error) {
    console.error('获取用户帖子列表失败:', error)
    throw error
  }
}

export async function searchPosts(
  q: string,
  page: number = 1,
  size: number = 10
): Promise<{ list: SearchPostItem[]; total: number; page: number; size: number }> {
  const { data } = await http.get('/search/posts', { params: { q, page, size } })
  if (data.code === 200 && data.data) {
    return {
      list: data.data.list || [],
      total: Number(data.data.total || 0),
      page: Number(data.data.page || page),
      size: Number(data.data.size || size)
    }
  }
  throw new Error(data.message || '搜索失败')
}

export async function searchUsers(
  q: string,
  page: number = 1,
  size: number = 8
): Promise<{ list: SearchUserItem[]; total: number; page: number; size: number }> {
  const { data } = await http.get('/search/users', { params: { q, page, size } })
  if (data.code === 200 && data.data) {
    return {
      list: data.data.list || [],
      total: Number(data.data.total || 0),
      page: Number(data.data.page || page),
      size: Number(data.data.size || size)
    }
  }
  throw new Error(data.message || '搜索用户失败')
}

// 删除帖子
export async function deletePost(postId: number): Promise<void> {
  try {
    const { data } = await http.delete(`/forum/posts/${postId}`)
    if (data.code !== 200) {
      throw new Error(data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除帖子失败:', error)
    throw error
  }
}

// 更新帖子内容
export async function updatePost(postId: number, payload: {
  title?: string
  content?: string
  categoryId?: number
}): Promise<ForumPost> {
  try {
    const { data } = await http.put(`/forum/posts/${postId}`, payload)
    if (data.code === 200 && data.data) {
      const post = data.data
      return {
        id: post.id,
        authorId: post.userId || post.authorId || 0,
        authorName: post.authorName || '用户',
        authorAvatar: post.authorAvatar || '',
        title: post.title || '',
        content: post.content || '',
        coverImage: post.coverImage || '',
        likeCount: post.likeCount || 0,
        commentCount: post.commentCount || 0,
        createdAt: post.createTime || post.createdAt || new Date().toISOString(),
        category: post.categoryCode || post.category || 'general'
      }
    }
    throw new Error(data.message || '更新帖子失败')
  } catch (error) {
    console.error('更新帖子失败:', error)
    throw error
  }
}

// 更新帖子权限（status: 0-正常, 1-仅自己可见, 2-已删除, 3-屏蔽）
export async function updatePostStatus(postId: number, status: number): Promise<void> {
  try {
    const { data } = await http.put(`/forum/posts/${postId}/status`, { status })
    if (data.code !== 200) {
      throw new Error(data.message || '更新权限失败')
    }
  } catch (error) {
    console.error('更新帖子权限失败:', error)
    throw error
  }
}

// 获取帖子分类列表
export interface PostCategory {
  id: number
  name: string
  code: string
  description?: string
}

// 收藏/取消收藏帖子
export async function toggleFavorite(postId: number): Promise<{ favorited: boolean; favoriteCount: number }> {
  try {
    const { data } = await http.post(`/forum/posts/${postId}/favorite`)
    if (data.code === 200 && data.data) {
      return data.data as { favorited: boolean; favoriteCount: number }
    }
    throw new Error(data.message || '操作失败')
  } catch (error) {
    console.error('收藏操作失败:', error)
    throw error
  }
}

// 点赞/取消点赞帖子
export async function togglePostLike(postId: number): Promise<{ liked: boolean; likeCount: number }> {
  const { data } = await http.post(`/forum/posts/${postId}/like`)
  if (data.code === 200 && data.data) {
    return data.data as { liked: boolean; likeCount: number }
  }
  throw new Error(data.message || '点赞失败')
}

// 点赞/取消点赞评论
export async function toggleCommentLike(commentId: number): Promise<{ liked: boolean; likeCount: number }> {
  const { data } = await http.post(`/forum/comments/${commentId}/like`)
  if (data.code === 200 && data.data) {
    return data.data as { liked: boolean; likeCount: number }
  }
  throw new Error(data.message || '评论点赞失败')
}

// 获取我的收藏列表
export async function fetchMyFavorites(page: number = 1, size: number = 20): Promise<ForumPost[]> {
  try {
    const { data } = await http.get('/forum/posts/me/favorites', { params: { page, size } })
    if (data.code === 200 && data.data && data.data.list) {
      return data.data.list.map((post: any) => ({
        id: post.id,
        authorId: post.authorId || post.userId || 0,
        authorName: post.authorName || post.username || '用户',
        authorAvatar: post.authorAvatar || post.avatarUrl || '',
        title: post.title || '',
        content: post.content || '',
        coverImage: post.coverImage || '',
        images: post.images || [],
        likeCount: post.likeCount || 0,
        commentCount: post.commentCount || 0,
        createdAt: post.createTime || post.createdAt || new Date().toISOString(),
        category: post.category || post.categoryCode || 'general'
      }))
    }
    return []
  } catch (error) {
    console.error('获取收藏列表失败:', error)
    return []
  }
}

export async function fetchUserFavorites(userId: number, page: number = 1, size: number = 20): Promise<ForumPost[]> {
  try {
    const { data } = await http.get(`/forum/posts/user/${userId}/favorites`, { params: { page, size } })
    if (data.code === 200 && data.data && data.data.list) {
      return data.data.list.map((post: any) => ({
        id: post.id,
        authorId: post.authorId || post.userId || userId,
        authorName: post.authorName || post.username || '用户',
        authorAvatar: post.authorAvatar || post.avatarUrl || '',
        title: post.title || '',
        content: post.content || '',
        coverImage: post.coverImage || '',
        images: post.images || [],
        likeCount: post.likeCount || 0,
        commentCount: post.commentCount || 0,
        createdAt: post.createTime || post.createdAt || new Date().toISOString(),
        category: post.category || post.categoryCode || 'general'
      }))
    }
    return []
  } catch (error) {
    console.error('获取用户收藏列表失败:', error)
    return []
  }
}

export async function fetchPostCategories(): Promise<PostCategory[]> {
  try {
    const { data } = await http.get('/categories/posts')
    if (data.code === 200 && data.data) {
      // 过滤掉"推荐"选项，只返回真实分类
      return data.data
        .filter((cat: any) => cat.type === 'category' && cat.id !== 'recommend')
        .map((cat: any) => ({
          id: parseInt(cat.id),
          name: cat.name,
          code: cat.code,
          description: cat.description
        }))
    }
    return []
  } catch (error) {
    console.error('获取分类列表失败:', error)
    return []
  }
}

export async function submitForumReport(payload: ForumReportPayload): Promise<{ id: number; status: number }> {
  const { data } = await http.post('/forum/reports', payload)
  if (data.code === 200 && data.data) {
    return data.data as { id: number; status: number }
  }
  throw new Error(data.message || '举报失败')
}
