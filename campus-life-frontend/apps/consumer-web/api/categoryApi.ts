// src/api/categoryApi.ts
import http from './http'

// 帖子分类接口
export interface PostCategory {
  id: string
  name: string
  code: string
  type: string
}

// 商家分类接口
export interface MerchantCategory {
  id: number
  code: string
  name: string
  description?: string
}

/**
 * 获取帖子分类列表（用于TabBar）
 */
export async function getPostCategories(): Promise<PostCategory[]> {
  try {
    const { data } = await http.get('/categories/posts')
    if (data.code === 200 && data.data) {
      return data.data
    }
    return []
  } catch (error) {
    console.error('获取帖子分类失败:', error)
    return []
  }
}

/**
 * 获取商家分类列表（用于WelfarePage）
 */
export async function getMerchantCategories(): Promise<MerchantCategory[]> {
  try {
    const { data } = await http.get('/shops/types')
    if (data.code === 200 && data.data) {
      return data.data
    }
    return []
  } catch (error) {
    console.error('获取商家分类失败:', error)
    return []
  }
}

/**
 * 获取所有分类（通用）
 */
export async function getAllCategories(): Promise<Record<string, any[]>> {
  try {
    const { data } = await http.get('/categories')
    return data.data || {}
  } catch (error) {
    console.error('获取分类失败:', error)
    return {}
  }
}

