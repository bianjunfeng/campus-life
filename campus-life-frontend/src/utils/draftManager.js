import { getStoredUserInfoObject } from './authStorage'

const DRAFT_STORAGE_KEY_PREFIX = 'postDrafts'
const MAX_DRAFT_ITEMS = 50

function getUserScopedKey() {
  try {
    const userInfo = getStoredUserInfoObject()
    const userId = userInfo && userInfo.id ? String(userInfo.id) : 'guest'
    return `${DRAFT_STORAGE_KEY_PREFIX}:${userId}`
  } catch (e) {
    return `${DRAFT_STORAGE_KEY_PREFIX}:guest`
  }
}

function normalizeImage(image) {
  if (!image || !image.url || typeof image.url !== 'string') {
    return null
  }
  if (image.url.startsWith('data:')) {
    return null
  }
  return {
    url: image.url,
    width: image.width,
    height: image.height,
    uploaded: image.uploaded !== false,
    ossUrl: image.ossUrl || image.url
  }
}

class DraftManager {
  static getDrafts() {
    try {
      const raw = localStorage.getItem(getUserScopedKey())
      const parsed = raw ? JSON.parse(raw) : []
      if (!Array.isArray(parsed)) return []
      return parsed
        .filter(item => item && item.id)
        .sort((a, b) => new Date(b.updatedAt || 0).getTime() - new Date(a.updatedAt || 0).getTime())
    } catch (error) {
      console.error('读取草稿失败:', error)
      return []
    }
  }

  static getDraftById(draftId) {
    const id = String(draftId || '')
    if (!id) return null
    return this.getDrafts().find(item => String(item.id) === id) || null
  }

  static saveDraft(payload, existingId = null) {
    try {
      const drafts = this.getDrafts()
      const now = new Date().toISOString()
      const normalizedImages = Array.isArray(payload.images)
        ? payload.images.map(normalizeImage).filter(Boolean)
        : []

      const targetId = existingId || payload.id || `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
      const newDraft = {
        id: String(targetId),
        title: payload.title || '',
        content: payload.content || '',
        categoryId: payload.categoryId ?? null,
        images: normalizedImages,
        createdAt: payload.createdAt || now,
        updatedAt: now
      }

      const index = drafts.findIndex(item => String(item.id) === String(targetId))
      if (index >= 0) {
        newDraft.createdAt = drafts[index].createdAt || newDraft.createdAt
        drafts[index] = newDraft
      } else {
        drafts.unshift(newDraft)
      }

      if (drafts.length > MAX_DRAFT_ITEMS) {
        drafts.splice(MAX_DRAFT_ITEMS)
      }

      localStorage.setItem(getUserScopedKey(), JSON.stringify(drafts))
      return newDraft
    } catch (error) {
      console.error('保存草稿失败:', error)
      return null
    }
  }

  static removeDraft(draftId) {
    try {
      const id = String(draftId || '')
      const drafts = this.getDrafts()
      const filtered = drafts.filter(item => String(item.id) !== id)
      localStorage.setItem(getUserScopedKey(), JSON.stringify(filtered))
      return filtered.length !== drafts.length
    } catch (error) {
      console.error('删除草稿失败:', error)
      return false
    }
  }
}

export default DraftManager
