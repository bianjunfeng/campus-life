/**
 * 浏览记录管理器
 * 用于管理用户的笔记浏览记录
 */
import { getStoredUserInfoObject } from './authStorage'

const HISTORY_STORAGE_KEY_PREFIX = 'noteHistory';
const MAX_HISTORY_ITEMS = 50; // 最大记录条数，防止存储空间过大

class HistoryManager {
  static getStorageKey() {
    try {
      const userInfo = getStoredUserInfoObject()
      const userId = userInfo && userInfo.id ? String(userInfo.id) : 'guest'
      return `${HISTORY_STORAGE_KEY_PREFIX}:${userId}`
    } catch (e) {
      return `${HISTORY_STORAGE_KEY_PREFIX}:guest`
    }
  }

  /**
   * 添加浏览记录
   * @param {Object} noteInfo - 笔记信息对象
   * @param {number} noteInfo.id - 笔记ID
   * @param {string} noteInfo.title - 笔记标题
   * @param {string} noteInfo.authorName - 作者名称
   * @param {Array} noteInfo.imageUrls - 笔记图片URL列表
   */
  static addHistory(noteInfo) {
    try {
      if (!noteInfo || !noteInfo.id) {
        return false
      }
      // 获取现有浏览记录
      const history = this.getHistory()
      
      // 创建新的浏览记录项
      const newItem = {
        id: noteInfo.id,
        title: noteInfo.title || '无标题',
        authorName: noteInfo.authorName || '用户',
        imageUrls: noteInfo.imageUrls || [],
        viewTime: new Date().toISOString() // 使用ISO格式时间，便于排序和格式化
      }
      
      // 检查是否已存在相同笔记的记录
      const existingIndex = history.findIndex(item => Number(item.id) === Number(noteInfo.id))
      
      if (existingIndex !== -1) {
        // 如果已存在，移除旧记录
        history.splice(existingIndex, 1)
      }
      
      // 将新记录添加到开头（最新的记录在前面）
      history.unshift(newItem)
      
      // 限制记录数量
      if (history.length > MAX_HISTORY_ITEMS) {
        history.splice(MAX_HISTORY_ITEMS)
      }
      
      // 保存到localStorage
      localStorage.setItem(this.getStorageKey(), JSON.stringify(history))
      
      return true
    } catch (error) {
      console.error('添加浏览记录失败:', error)
      return false
    }
  }
  
  /**
   * 获取所有浏览记录
   * @returns {Array} 浏览记录数组
   */
  static getHistory() {
    try {
      const currentKey = this.getStorageKey()
      const historyData = localStorage.getItem(currentKey)
      let parsed = historyData ? JSON.parse(historyData) : []
      if (!Array.isArray(parsed)) {
        parsed = []
      }

      // 兼容旧版共用key数据，迁移到当前账号
      const legacyRaw = localStorage.getItem(HISTORY_STORAGE_KEY_PREFIX)
      if ((!parsed || parsed.length === 0) && legacyRaw) {
        try {
          const legacy = JSON.parse(legacyRaw)
          if (Array.isArray(legacy) && legacy.length > 0) {
            parsed = legacy
            localStorage.setItem(currentKey, JSON.stringify(parsed))
          }
        } catch (e) {
          // ignore
        }
      }

      return parsed
        .filter(item => item && item.id)
        .sort((a, b) => new Date(b.viewTime || 0).getTime() - new Date(a.viewTime || 0).getTime())
    } catch (error) {
      console.error('获取浏览记录失败:', error)
      return []
    }
  }
  
  /**
   * 清除所有浏览记录
   * @returns {boolean} 是否清除成功
   */
  static clearHistory() {
    try {
      localStorage.removeItem(this.getStorageKey())
      return true
    } catch (error) {
      console.error('清除浏览记录失败:', error)
      return false
    }
  }
  
  /**
   * 删除单条浏览记录
   * @param {number} noteId - 笔记ID
   * @returns {boolean} 是否删除成功
   */
  static removeHistoryItem(noteId) {
    try {
      const history = this.getHistory()
      const newHistory = history.filter(item => Number(item.id) !== Number(noteId))
      
      if (newHistory.length !== history.length) {
        localStorage.setItem(this.getStorageKey(), JSON.stringify(newHistory))
        return true
      }
      
      return false // 没有找到要删除的记录
    } catch (error) {
      console.error('删除浏览记录失败:', error)
      return false
    }
  }
  
  /**
   * 获取指定数量的最近浏览记录
   * @param {number} limit - 要获取的记录数量
   * @returns {Array} 浏览记录数组
   */
  static getRecentHistory(limit = 10) {
    const history = this.getHistory();
    return history.slice(0, limit);
  }
  
  /**
   * 检查是否已经浏览过某个笔记
   * @param {number} noteId - 笔记ID
   * @returns {boolean} 是否已浏览
   */
  static hasViewed(noteId) {
    const history = this.getHistory();
    return history.some(item => item.id === noteId);
  }
  
  /**
   * 获取浏览记录数量
   * @returns {number} 记录数量
   */
  static getHistoryCount() {
    const history = this.getHistory();
    return history.length;
  }
}

export default HistoryManager;
