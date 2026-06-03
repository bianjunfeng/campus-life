// 帖子相关类型定义

// 用户信息接口
export interface User {
  id: number
  name: string
  avatar: string
  bio?: string
  followers: number
  following: number
}

// 帖子图片接口
export interface PostImage {
  id: number
  url: string
  description?: string
  width: number
  height: number
}

// 评论接口
export interface Comment {
  id: number
  text: string
  user: User
  likes: number
  createdAt: string
  isLiked: boolean
  parentId?: number
  replies?: number
}

// 帖子接口
export interface Post {
  id: number
  text: string
  images: PostImage[]
  user: User
  likes: number
  comments: number
  createdAt: string
  updatedAt?: string
  isLiked: boolean
  collected: boolean
  followed: boolean
  tags?: string[]
  views: number
}
