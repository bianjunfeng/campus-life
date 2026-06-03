<template>
  <div class="post-write-page">
    <!-- 顶部导航栏 -->
    <div class="header">
      <div class="cancel-button" @click="handleCancel">取消</div>
      <h1>写想法</h1>
      <div class="header-actions">
        <button class="save-draft-button" type="button" @click="handleSaveDraft">存草稿</button>
        <div class="publish-button" @click="handlePublish" :class="{ disabled: !canPublish }">发布</div>
      </div>
    </div>
    
    <!-- 内容输入区域 -->
    <div class="content-container">
      <div class="quote-icon">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
        </svg>
      </div>
      
      <!-- 标题输入框 -->
      <input 
        v-model="title" 
        type="text" 
        placeholder="请输入标题（必填）" 
        class="title-input"
        ref="titleInput"
        @input="checkContent"
        maxlength="100"
      />
      
      <textarea 
        v-model="content" 
        placeholder="说点什么或提个问题..." 
        class="content-input"
        ref="contentInput"
        @input="checkContent"
        rows="10"
      ></textarea>
      
      <!-- 分类选择区域（引导选择，不强制） -->
      <div class="category-section">
        <div class="category-label">
          <span class="label-text">选择分类</span>
          <span class="label-hint">（可选，未选择将自动归类为"校园生活"）</span>
        </div>
        <div class="category-options">
          <div 
            v-for="category in categories" 
            :key="category.id"
            class="category-option"
            :class="{ active: selectedCategoryId === category.id }"
            @click="selectCategory(category.id)"
          >
            <span class="category-name">{{ category.name }}</span>
            <span v-if="category.description" class="category-desc">{{ category.description }}</span>
            <svg v-if="selectedCategoryId === category.id" class="check-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          </div>
          <div 
            v-if="loadingCategories"
            class="category-loading"
          >
            加载分类中...
          </div>
          <div 
            v-if="!loadingCategories && categories.length === 0"
            class="category-empty"
          >
            暂无分类
          </div>
        </div>
      </div>
      
      <!-- 图片上传区域 -->
      <div class="image-upload-section">
        <div class="image-list">
          <div 
            v-for="(image, index) in images" 
            :key="index" 
            class="image-item"
            :class="{ 'uploading': image.uploading }"
          >
            <img :src="image.url" :alt="`图片${index + 1}`" />
            <div class="image-overlay">
              <div class="image-badge" v-if="index === 0">首图</div>
              <div class="uploading-badge" v-if="image.uploading">上传中...</div>
              <button class="delete-btn" @click="removeImage(index)" type="button" :disabled="image.uploading">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>
          <div 
            v-if="images.length < 9" 
            class="image-upload-btn" 
            @click="selectImages"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            <span class="upload-text">添加图片</span>
            <span class="upload-count">{{ images.length }}/9</span>
          </div>
        </div>
        <input 
          ref="fileInput" 
          type="file" 
          accept="image/*" 
          multiple 
          style="display: none"
          @change="handleFileSelect"
        />
      </div>
      
      <!-- 写长文选项 -->
      <div class="long-text-option" @click="toggleLongTextMode">
        <div class="long-text-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
            <polyline points="10 9 9 9 8 9"></polyline>
          </svg>
        </div>
        <span>写长文</span>
        <span class="description">支持千字以上，全屏阅读体验</span>
        <div class="arrow-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick, watch, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { createPost, fetchPostCategories, type PostCategory } from '../../api/forum';
import http from '../../api/http';
import DraftManager from '../../utils/draftManager';
import { showConfirm } from '../../utils/dialog';
import { notify } from '@/utils/notify'

// 路由实例
const router = useRouter();
const route = useRoute();

// 标题输入
const title = ref('');
const titleInput = ref<HTMLInputElement | null>(null);

// 内容输入
const content = ref('');
const contentInput = ref<HTMLTextAreaElement | null>(null);
const isLongTextMode = ref(false);

// 分类相关
const categories = ref<PostCategory[]>([]);
const selectedCategoryId = ref<number | null>(null);
const loadingCategories = ref(false);

// 图片相关
interface ImageItem {
  url: string;
  width?: number;
  height?: number;
  file?: File;
  uploading?: boolean; // 是否正在上传
  uploaded?: boolean; // 是否已上传到OSS
  ossUrl?: string; // OSS URL（上传后）
}

const images = ref<ImageItem[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);
const isPublishing = ref(false);
const currentDraftId = ref<string | null>(null);
let autoSaveTimer: number | null = null;

// 计算是否可以发布（标题必填，且有内容或图片）
const canPublish = computed(() => {
  return title.value.trim().length > 0 && (content.value.trim().length > 0 || images.value.length > 0) && !isPublishing.value;
});

// 检查内容
const checkContent = () => {
  // 这里可以添加内容验证逻辑
};

const hasDraftContent = computed(() => {
  return title.value.trim().length > 0 || content.value.trim().length > 0 || images.value.length > 0;
});

const getDraftPayload = () => ({
  title: title.value.trim(),
  content: content.value.trim(),
  categoryId: selectedCategoryId.value,
  images: images.value
});

const saveDraftSilently = () => {
  if (!hasDraftContent.value) {
    return null;
  }
  const saved = DraftManager.saveDraft(getDraftPayload(), currentDraftId.value);
  if (saved?.id) {
    currentDraftId.value = String(saved.id);
  }
  return saved;
};

const scheduleAutoSave = () => {
  if (autoSaveTimer) {
    window.clearTimeout(autoSaveTimer);
  }
  autoSaveTimer = window.setTimeout(() => {
    saveDraftSilently();
  }, 1200);
};

// 选择分类
const selectCategory = (categoryId: number) => {
  if (selectedCategoryId.value === categoryId) {
    // 如果点击已选中的分类，则取消选择
    selectedCategoryId.value = null;
  } else {
    selectedCategoryId.value = categoryId;
  }
};

// 加载分类列表
const loadCategories = async () => {
  loadingCategories.value = true;
  try {
    const categoryList = await fetchPostCategories();
    categories.value = categoryList;
  } catch (error) {
    console.error('加载分类失败:', error);
  } finally {
    loadingCategories.value = false;
  }
};

// 切换长文模式
const toggleLongTextMode = () => {
  isLongTextMode.value = !isLongTextMode.value;
  notify('长文模式功能即将上线');
};

// 选择图片
const selectImages = () => {
  if (images.value.length >= 9) {
    notify('最多只能上传9张图片');
    return;
  }
  fileInput.value?.click();
};

// 上传图片到OSS
const uploadImageToOSS = async (file: File, retryCount = 0): Promise<string> => {
  const formData = new FormData();
  formData.append('file', file);
  
  try {
    const { data } = await http.post('/upload/post-image', formData, {
      timeout: 30000 // 30秒超时，大文件上传可能需要更长时间
    });
    
    if (data.code === 200 && data.data?.url) {
      return data.data.url;
    } else {
      throw new Error(data.message || '图片上传失败');
    }
  } catch (error: any) {
    // 网络错误时，如果是第一次尝试且错误是网络相关，可以重试一次
    if (retryCount === 0 && (!error.response || error.code === 'ECONNABORTED' || error.code === 'NETWORK_ERROR')) {
      console.log('图片上传失败，尝试重试...', file.name);
      // 等待1秒后重试
      await new Promise(resolve => setTimeout(resolve, 1000));
      return uploadImageToOSS(file, 1);
    }
    
    // 处理错误信息
    let errorMessage = '图片上传失败';
    if (error.response) {
      // 服务器返回了错误响应
      errorMessage = error.response.data?.message || error.message || '服务器错误';
    } else if (error.request) {
      // 请求已发出但没有收到响应
      errorMessage = '网络错误，请检查网络连接';
    } else {
      // 其他错误
      errorMessage = error.message || '未知错误';
    }
    
    throw new Error(errorMessage);
  }
};

// 处理文件选择
const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const files = target.files;
  if (!files) return;
  
  const remainingSlots = 9 - images.value.length;
  const filesToAdd = Array.from(files).slice(0, remainingSlots);
  
  for (const file of filesToAdd) {
    if (!file.type.startsWith('image/')) {
      notify(`${file.name} 不是图片文件`);
      continue;
    }
    
    // 先显示预览（base64）
    const reader = new FileReader();
    reader.onload = async (e) => {
      const img = new Image();
      img.onload = async () => {
        const imageItem: ImageItem = {
          url: e.target?.result as string,
          width: img.width,
          height: img.height,
          file: file,
          uploading: true,
          uploaded: false
        };
        
        images.value.push(imageItem);
        
        // 立即上传到OSS
        try {
          const ossUrl = await uploadImageToOSS(file);
          imageItem.ossUrl = ossUrl;
          imageItem.url = ossUrl; // 更新为OSS URL
          imageItem.uploaded = true;
          imageItem.uploading = false;
        } catch (error: any) {
          console.error('图片上传失败:', error);
          // 显示更友好的错误提示
          const errorMsg = error.message || '未知错误';
          notify(`图片 ${file.name} 上传失败: ${errorMsg}`);
          // 上传失败，移除该图片
          const index = images.value.indexOf(imageItem);
          if (index > -1) {
            images.value.splice(index, 1);
          }
        }
      };
      img.src = e.target?.result as string;
    };
    reader.readAsDataURL(file);
  }
  
  // 清空input，以便可以重复选择同一文件
  target.value = '';
};

// 删除图片
const removeImage = (index: number) => {
  images.value.splice(index, 1);
};

// 处理图片，确保所有图片都已上传到OSS
const processImages = async (): Promise<Array<{ url: string; width?: number; height?: number }>> => {
  const processedImages: Array<{ url: string; width?: number; height?: number }> = [];
  
  for (const img of images.value) {
    // 如果图片还没有上传，先上传
    if (!img.uploaded && img.file) {
      try {
        img.uploading = true;
        const ossUrl = await uploadImageToOSS(img.file);
        img.ossUrl = ossUrl;
        img.url = ossUrl;
        img.uploaded = true;
        img.uploading = false;
      } catch (error: any) {
        console.error('图片上传失败:', error);
        throw new Error(`图片上传失败: ${error.message || '未知错误'}`);
      }
    }
    
    // 使用OSS URL（如果已上传）或当前URL
    processedImages.push({
      url: img.ossUrl || img.url,
      width: img.width,
      height: img.height
    });
  }
  
  return processedImages;
};

// 处理取消
const handleCancel = async () => {
  if (!hasDraftContent.value) {
    router.back();
    return;
  }
  const shouldSave = await showConfirm('是否保存到草稿箱后再离开？');
  if (shouldSave) {
    const saved = saveDraftSilently();
    if (saved) {
      notify('已保存到草稿箱');
      router.push('/drafts');
      return;
    }
    notify('保存草稿失败，请重试');
    return;
  }
  const shouldDiscard = await showConfirm('确定不保存并离开吗？');
  if (shouldDiscard) {
    router.back();
  }
};

// 处理发布
const handlePublish = async () => {
  if (!canPublish.value) return;
  
  // 验证标题
  if (!title.value.trim()) {
    notify('请输入标题');
    return;
  }
  
  if (!content.value.trim() && images.value.length === 0) {
    notify('请输入内容或添加图片');
    return;
  }
  
  isPublishing.value = true;
  
  try {
    // 处理图片
    const processedImages = await processImages();
    
    // 调用API创建帖子
    await createPost({
      title: title.value.trim(), // 标题必填
      content: content.value.trim(),
      images: processedImages,
      categoryId: selectedCategoryId.value || undefined, // 分类可选
      category: 'general' // 默认分类（如果未选择分类）
    });

    if (currentDraftId.value) {
      DraftManager.removeDraft(currentDraftId.value);
      currentDraftId.value = null;
    }
    
    // 发布成功，返回首页
    router.push('/home');
  } catch (error: any) {
    console.error('发布失败:', error);
    notify(error.response?.data?.message || error.message || '发布失败，请重试');
  } finally {
    isPublishing.value = false;
  }
};

const handleSaveDraft = () => {
  if (!hasDraftContent.value) {
    notify('当前没有可保存的内容');
    return;
  }
  const saved = saveDraftSilently();
  if (!saved) {
    notify('保存草稿失败，请重试');
    return;
  }
  notify('草稿已保存');
};

const loadDraftIfNeeded = () => {
  const draftId = String(route.query.draftId || '');
  if (!draftId) return;
  const draft = DraftManager.getDraftById(draftId);
  if (!draft) {
    notify('草稿不存在或已删除');
    return;
  }
  currentDraftId.value = String(draft.id);
  title.value = draft.title || '';
  content.value = draft.content || '';
  selectedCategoryId.value = draft.categoryId ?? null;
  images.value = Array.isArray(draft.images)
    ? draft.images.map((img: any) => ({
        url: img.ossUrl || img.url,
        width: img.width,
        height: img.height,
        uploaded: true,
        uploading: false,
        ossUrl: img.ossUrl || img.url
      }))
    : [];
};

watch(
  [title, content, selectedCategoryId, images],
  () => {
    if (!isPublishing.value) {
      scheduleAutoSave();
    }
  },
  { deep: true }
);

// 组件挂载后自动聚焦到标题输入框并加载分类
onMounted(() => {
  // 加载分类列表
  loadCategories();
  
  // 聚焦到标题输入框
  nextTick(() => {
    if (titleInput.value) {
      titleInput.value.focus();
    }
  });

  loadDraftIfNeeded();
});

onBeforeUnmount(() => {
  if (autoSaveTimer) {
    window.clearTimeout(autoSaveTimer);
  }
});
</script>

<style scoped>
.post-write-page {
  position: relative;
  min-height: 100vh;
  background-color: #f8f8f8;
  padding-bottom: 20px;
}

/* 顶部导航栏 */
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px;
  padding: 0 16px;
  background-color: white;
  border-bottom: 1px solid #e0e0e0;
  z-index: 100;
}

.cancel-button,
.publish-button {
  font-size: 16px;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.save-draft-button {
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #666;
  border-radius: 4px;
  padding: 6px 10px;
  font-size: 14px;
  cursor: pointer;
}

.cancel-button {
  color: #666;
}

.publish-button {
  color: #1677ff;
  font-weight: 500;
}

.publish-button.disabled {
  color: #ccc;
  cursor: not-allowed;
}

.header h1 {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

/* 内容容器 */
.content-container {
  margin-top: 50px;
  padding: 20px;
  background-color: white;
  min-height: calc(100vh - 50px);
}

.quote-icon {
  width: 40px;
  height: 40px;
  margin-bottom: 20px;
  color: #d9d9d9;
}

.quote-icon svg {
  width: 100%;
  height: 100%;
}

/* 标题输入框 */
.title-input {
  width: 100%;
  border: none;
  border-bottom: 1px solid #e0e0e0;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.5;
  color: #333;
  outline: none;
  padding: 12px 0;
  margin-bottom: 16px;
  background-color: transparent;
}

.title-input::placeholder {
  color: #bfbfbf;
  font-weight: 400;
}

.title-input:focus {
  border-bottom-color: #1677ff;
}

/* 内容输入区域 */
.content-input {
  width: 100%;
  border: none;
  resize: none;
  font-size: 18px;
  line-height: 1.5;
  color: #333;
  outline: none;
  min-height: 200px;
}

.content-input::placeholder {
  color: #bfbfbf;
}

/* 分类选择区域 */
.category-section {
  margin-top: 24px;
  margin-bottom: 20px;
  padding: 16px;
  background-color: #fafafa;
  border-radius: 8px;
}

.category-label {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.label-text {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.label-hint {
  font-size: 13px;
  color: #999;
  margin-left: 8px;
}

.category-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-option {
  position: relative;
  padding: 10px 16px;
  background-color: white;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 80px;
}

.category-option:hover {
  border-color: #1677ff;
  background-color: #f0f7ff;
}

.category-option.active {
  border-color: #1677ff;
  background-color: #e6f4ff;
  color: #1677ff;
}

.category-name {
  font-size: 14px;
  font-weight: 500;
}

.category-desc {
  font-size: 12px;
  color: #999;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-option.active .category-desc {
  color: #1677ff;
}

.check-icon {
  width: 16px;
  height: 16px;
  margin-left: auto;
  flex-shrink: 0;
}

.category-loading,
.category-empty {
  padding: 10px 16px;
  color: #999;
  font-size: 14px;
  text-align: center;
  width: 100%;
}

/* 图片上传区域 */
.image-upload-section {
  margin-top: 20px;
}

.image-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.image-item {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background-color: #f5f5f5;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to bottom, rgba(0,0,0,0.3) 0%, transparent 30%);
  opacity: 0;
  transition: opacity 0.2s;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 8px;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.image-item.uploading .image-overlay {
  opacity: 1;
  background: linear-gradient(to bottom, rgba(0,0,0,0.5) 0%, rgba(0,0,0,0.3) 100%);
}

.image-badge {
  background-color: #1677ff;
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}

.uploading-badge {
  background-color: rgba(255, 193, 7, 0.9);
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

.delete-btn {
  background-color: rgba(255, 255, 255, 0.9);
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #ff4d4f;
  padding: 0;
}

.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.delete-btn svg {
  width: 16px;
  height: 16px;
}

.image-upload-btn {
  aspect-ratio: 1;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s;
  background-color: #fafafa;
}

.image-upload-btn:hover {
  border-color: #1677ff;
  background-color: #f0f7ff;
}

.image-upload-btn svg {
  width: 32px;
  height: 32px;
  color: #999;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.upload-count {
  font-size: 12px;
  color: #999;
}

/* 写长文选项 */
.long-text-option {
  margin-top: 30px;
  padding: 16px;
  background-color: #fafafa;
  border-radius: 8px;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.2s;
}

.long-text-option:active {
  background-color: #f0f0f0;
}

.long-text-icon {
  width: 32px;
  height: 32px;
  margin-right: 12px;
  color: #1677ff;
}

.long-text-icon svg {
  width: 100%;
  height: 100%;
}

.long-text-option span {
  font-size: 16px;
  color: #333;
}

.long-text-option .description {
  font-size: 14px;
  color: #999;
  margin-left: 8px;
}

.arrow-icon {
  margin-left: auto;
  color: #999;
}

.arrow-icon svg {
  width: 20px;
  height: 20px;
}
</style>
