// apps/consumer-web/api/user.ts
import http from './http'

export interface UserFollowInfo {
  id: number
  userId: number
  name: string
  avatar?: string
  bio?: string
  followeeId?: number
  followerId?: number
  isFollowing?: boolean
  isMutual?: boolean
}

export interface FollowListResponse {
  list: UserFollowInfo[]
  page: number
  size: number
}

export interface PublicUserProfile {
  id: number
  nickName: string
  icon?: string
  bio?: string
  region?: string
  occupation?: string
  postCount: number
  followingCount: number
  followerCount: number
  isFollowing?: boolean
}

/**
 * 获取用户的粉丝列表
 */
export async function getFollowers(userId: number, page: number = 1, size: number = 20): Promise<FollowListResponse> {
  const { data } = await http.get(`/users/${userId}/followers`, {
    params: { page, size }
  })
  if (data.code === 200 && data.data) {
    return data.data as FollowListResponse
  }
  throw new Error(data.message || '获取粉丝列表失败')
}

/**
 * 获取用户的关注列表
 */
export async function getFollowings(userId: number, page: number = 1, size: number = 20): Promise<FollowListResponse> {
  const { data } = await http.get(`/users/${userId}/followees`, {
    params: { page, size }
  })
  if (data.code === 200 && data.data) {
    return data.data as FollowListResponse
  }
  throw new Error(data.message || '获取关注列表失败')
}

/**
 * 获取用户的互相关注列表
 */
export async function getMutualFollows(userId: number, page: number = 1, size: number = 20): Promise<FollowListResponse> {
  const { data } = await http.get(`/users/${userId}/mutual-follows`, {
    params: { page, size }
  })
  if (data.code === 200 && data.data) {
    return data.data as FollowListResponse
  }
  throw new Error(data.message || '获取互相关注列表失败')
}

/**
 * 关注用户
 */
export async function followUser(userId: number): Promise<{ followed: boolean }> {
  const { data } = await http.post(`/users/${userId}/follow`)
  if (data.code === 200 && data.data) {
    return data.data as { followed: boolean }
  }
  throw new Error(data.message || '关注失败')
}

/**
 * 取消关注用户
 */
export async function unfollowUser(userId: number): Promise<{ followed: boolean }> {
  const { data } = await http.delete(`/users/${userId}/follow`)
  if (data.code === 200 && data.data) {
    return data.data as { followed: boolean }
  }
  throw new Error(data.message || '取消关注失败')
}

export async function getPublicUserProfile(userId: number): Promise<PublicUserProfile> {
  const { data } = await http.get(`/users/${userId}`)
  if (data.code === 200 && data.data) {
    return data.data as PublicUserProfile
  }
  throw new Error(data.message || '获取用户主页失败')
}

