# 前端首页图片显示指南

## 📋 概述

前端首页已经支持显示帖子图片。图片数据来自后端的 `post_image` 表，通过 `PostDTO.coverImage` 字段传递给前端。

## 🔍 数据流程

```
后端 PostDTO.coverImage
    ↓
JSON API 响应
    ↓
前端 forum.ts (fetchPosts)
    ↓
DiscoverPage.vue (v-for 渲染)
    ↓
<img :src="post.coverImage">
```

## ✅ 前端代码位置

### 1. API 调用层

**文件：** `src/api/forum.ts`

```typescript
export async function fetchPosts(): Promise<ForumPost[]> {
  const { data } = await http.get('/forum/posts', {
    params: { order: 'latest', page: 1, size: 100 }
  })
  
  if (data.code === 200 && data.data && data.data.list) {
    return data.data.list.map((post: any) => ({
      // ... 其他字段
      coverImage: post.coverImage || '', // 从 post_image 表获取的第一张图片
      // ...
    }))
  }
}
```

### 2. 页面组件

**文件：** `src/views/DiscoverPage.vue`

```vue
<template>
  <div class="post-content">
    <div class="post-images single-image">
      <!-- 如果有图片，显示图片 -->
      <img 
        v-if="post.coverImage"
        :src="post.coverImage" 
        alt="封面图"
        class="post-image"
      >
      <!-- 如果没有图片，显示标题占位符 -->
      <div v-else class="post-image post-image-placeholder">
        {{ post.title.slice(0, 6) }}
      </div>
    </div>
  </div>
</template>
```

## 🎨 显示效果

### 有图片的帖子
- 显示图片（`post.coverImage` 不为空）
- 图片使用 `post-image` 样式类
- 图片尺寸：100% 宽度，1:1 比例（padding-bottom: 100%）

### 没有图片的帖子
- 显示标题占位符（前6个字符）
- 使用 `post-image-placeholder` 样式类
- 灰色背景，居中显示文字

## 🔧 如何验证图片显示

### 方法1：浏览器控制台

1. 打开浏览器开发者工具（F12）
2. 切换到 Console 标签
3. 刷新首页
4. 查看日志输出：
   ```
   ✅ 成功加载 8 条帖子，其中 5 条包含图片
   ```

### 方法2：网络请求检查

1. 打开浏览器开发者工具（F12）
2. 切换到 Network 标签
3. 刷新首页
4. 找到 `GET /api/forum/posts` 请求
5. 查看响应数据，确认 `coverImage` 字段是否有值

**示例响应：**
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "title": "帖子标题",
        "coverImage": "https://picsum.photos/400/400?random=1",
        // ... 其他字段
      }
    ]
  }
}
```

### 方法3：页面元素检查

1. 在首页找到帖子卡片
2. 右键点击图片区域 → 检查元素
3. 查看 `<img>` 标签的 `src` 属性是否有值

## 🐛 常见问题

### 问题1：图片不显示

**可能原因：**
- 后端没有返回 `coverImage` 字段
- `post_image` 表中没有数据
- 图片URL无效或无法访问

**解决方法：**
1. 检查后端日志，确认 SQL 查询是否包含 `cover_image`
2. 检查数据库 `post_image` 表是否有数据
3. 执行 `init-test-data.sql` 脚本添加测试图片

### 问题2：显示占位符而不是图片

**原因：** 帖子没有关联的图片数据

**解决方法：**
1. 在 `post_image` 表中为该帖子添加图片记录
2. 或者等待用户上传图片

### 问题3：图片加载失败（显示破损图标）

**可能原因：**
- 图片URL无效
- 网络问题
- 跨域问题

**解决方法：**
1. 检查图片URL是否可访问
2. 检查后端CORS配置
3. 使用有效的图片URL（如云存储地址）

## 📝 添加测试图片

如果数据库中没有图片数据，可以执行以下SQL：

```sql
-- 为帖子添加测试图片
INSERT INTO `post_image` (post_id, url, sort_order, create_time)
SELECT 
    p.id,
    CONCAT('https://picsum.photos/400/400?random=', p.id) as url,
    0 as sort_order,
    NOW() as create_time
FROM `post` p
WHERE NOT EXISTS (
    SELECT 1 FROM `post_image` pi WHERE pi.post_id = p.id
)
LIMIT 5;
```

## 🎯 下一步优化

1. **图片懒加载**：使用 `v-lazy` 或 `IntersectionObserver` 实现图片懒加载
2. **图片预览**：点击图片可以放大查看
3. **多图支持**：支持显示多张图片（目前只显示第一张）
4. **图片压缩**：后端返回缩略图URL，提升加载速度
5. **占位图优化**：使用更美观的占位图

## 📚 相关文件

- `src/api/forum.ts` - API调用
- `src/views/DiscoverPage.vue` - 首页组件
- `src/main/java/.../dto/PostDTO.java` - 后端DTO
- `src/main/resources/mapper/PostMapper.xml` - SQL查询

