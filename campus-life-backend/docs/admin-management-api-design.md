# 管理员管理功能 API 设计

## 1. 帖子管理

### 1.1 获取帖子列表（管理后台）
```
GET /admin/posts
参数：
  - page: 页码
  - size: 每页数量
  - status: 状态筛选 (0-正常, 1-仅自己可见, 2-已删除, 3-屏蔽)
  - keyword: 搜索关键词（标题/内容）
  - startTime: 开始时间
  - endTime: 结束时间
  - userId: 用户ID筛选
  - isPinned: 是否置顶
  - isHot: 是否热门

返回：
{
  "code": 200,
  "data": {
    "list": [...],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

### 1.2 删除帖子
```
POST /admin/posts/{postId}/delete
Body: {
  "reason": "违规内容"
}

返回：
{
  "code": 200,
  "msg": "删除成功"
}
```

### 1.3 屏蔽/解除屏蔽帖子
```
POST /admin/posts/{postId}/ban
Body: {
  "reason": "违规内容"
}

POST /admin/posts/{postId}/unban
Body: {
  "reason": "已整改"
}
```

### 1.4 置顶/取消置顶
```
POST /admin/posts/{postId}/pin
POST /admin/posts/{postId}/unpin
```

### 1.5 设为热门/取消热门
```
POST /admin/posts/{postId}/set-hot
POST /admin/posts/{postId}/remove-hot
```

### 1.6 批量操作
```
POST /admin/posts/batch
Body: {
  "postIds": [1, 2, 3],
  "action": "delete|ban|unban|pin|unpin|set-hot|remove-hot",
  "reason": "批量操作原因"
}
```

## 2. 商家管理

### 2.1 获取商家列表（管理后台）
```
GET /admin/merchants
参数：
  - page: 页码
  - size: 每页数量
  - status: 状态筛选 (0-待审核, 1-正常, 2-冻结, 3-关闭)
  - keyword: 搜索关键词（商家名称/联系人）
  - typeId: 商家类型ID

返回：
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "userId": 123,
        "name": "XX奶茶店",
        "contactName": "张三",
        "contactPhone": "13800138000",
        "address": "XX路XX号",
        "typeId": 1,
        "typeName": "奶茶饮品",
        "status": 1,
        "createTime": "2024-01-01 10:00:00",
        "user": {
          "id": 123,
          "username": "merchant_user"
        }
      }
    ],
    "total": 50,
    "page": 1,
    "size": 20
  }
}
```

### 2.2 审核商家（通过/拒绝）
```
POST /admin/merchants/{merchantId}/approve
Body: {
  "reason": "审核通过"
}

POST /admin/merchants/{merchantId}/reject
Body: {
  "reason": "营业执照不清晰"
}
```

### 2.3 冻结/解冻商家
```
POST /admin/merchants/{merchantId}/freeze
Body: {
  "reason": "多次违规"
}

POST /admin/merchants/{merchantId}/unfreeze
Body: {
  "reason": "已整改"
}
```

### 2.4 关闭商家
```
POST /admin/merchants/{merchantId}/close
Body: {
  "reason": "严重违规"
}
```

### 2.5 查看商家详情
```
GET /admin/merchants/{merchantId}
返回：商家详细信息，包括认证申请记录
```

## 3. 券管理

### 3.1 获取券列表（管理后台）
```
GET /admin/vouchers
参数：
  - page: 页码
  - size: 每页数量
  - status: 状态筛选 (0-下架, 1-上架, 2-已下架(管理员), 3-违规下架)
  - keyword: 搜索关键词（标题）
  - merchantId: 商家ID筛选
  - startTime: 开始时间
  - endTime: 结束时间

返回：
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "merchantId": 1,
        "merchantName": "XX奶茶店",
        "title": "新用户优惠券",
        "amount": 10.00,
        "stock": 100,
        "status": 1,
        "beginTime": "2024-01-01 00:00:00",
        "endTime": "2024-12-31 23:59:59",
        "createTime": "2024-01-01 10:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

### 3.2 下架券（管理员）
```
POST /admin/vouchers/{voucherId}/offline
Body: {
  "reason": "违规内容"
}
```

### 3.3 违规下架
```
POST /admin/vouchers/{voucherId}/violation-offline
Body: {
  "reason": "虚假宣传"
}
```

### 3.4 恢复上架
```
POST /admin/vouchers/{voucherId}/online
Body: {
  "reason": "已整改"
}
```

### 3.5 批量操作
```
POST /admin/vouchers/batch
Body: {
  "voucherIds": [1, 2, 3],
  "action": "offline|violation-offline|online",
  "reason": "批量操作原因"
}
```

## 4. 评论管理

### 4.1 获取评论列表（管理后台）
```
GET /admin/comments
参数：
  - page: 页码
  - size: 每页数量
  - status: 状态筛选 (0-正常, 1-已删除, 2-屏蔽)
  - keyword: 搜索关键词（评论内容）
  - postId: 帖子ID筛选
  - userId: 用户ID筛选
  - startTime: 开始时间
  - endTime: 结束时间

返回：
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "postId": 123,
        "postTitle": "帖子标题",
        "userId": 456,
        "username": "user123",
        "content": "评论内容",
        "status": 0,
        "likeCount": 10,
        "createTime": "2024-01-01 10:00:00"
      }
    ],
    "total": 200,
    "page": 1,
    "size": 20
  }
}
```

### 4.2 删除评论
```
POST /admin/comments/{commentId}/delete
Body: {
  "reason": "违规内容"
}
```

### 4.3 屏蔽/解除屏蔽评论
```
POST /admin/comments/{commentId}/ban
Body: {
  "reason": "违规内容"
}

POST /admin/comments/{commentId}/unban
Body: {
  "reason": "已整改"
}
```

### 4.4 批量操作
```
POST /admin/comments/batch
Body: {
  "commentIds": [1, 2, 3],
  "action": "delete|ban|unban",
  "reason": "批量操作原因"
}
```

## 5. 管理操作日志

### 5.1 获取操作日志
```
GET /admin/operation-logs
参数：
  - page: 页码
  - size: 每页数量
  - adminId: 管理员ID筛选
  - operationType: 操作类型筛选
  - targetType: 目标类型筛选
  - action: 操作动作筛选
  - startTime: 开始时间
  - endTime: 结束时间

返回：
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "adminId": 1,
        "adminName": "admin",
        "operationType": "post_manage",
        "targetType": "post",
        "targetId": 123,
        "action": "ban",
        "oldStatus": 0,
        "newStatus": 3,
        "reason": "违规内容",
        "ipAddress": "192.168.1.1",
        "createTime": "2024-01-01 10:00:00"
      }
    ],
    "total": 1000,
    "page": 1,
    "size": 20
  }
}
```

## 6. 管理统计

### 6.1 获取管理统计
```
GET /admin/statistics
返回：
{
  "code": 200,
  "data": {
    "post": {
      "total": 1000,
      "normal": 950,
      "banned": 50,
      "pinned": 10,
      "hot": 20
    },
    "merchant": {
      "total": 100,
      "pending": 5,
      "normal": 80,
      "frozen": 10,
      "closed": 5
    },
    "voucher": {
      "total": 500,
      "online": 450,
      "offline": 50
    },
    "comment": {
      "total": 5000,
      "normal": 4800,
      "banned": 200
    }
  }
}
```

## 7. 权限验证

所有管理接口都需要：
1. 验证用户是否登录
2. 验证用户角色是否为管理员（role = 2）
3. 记录操作日志

## 8. 状态流转图

### 帖子状态流转
```
正常(0) <---> 屏蔽(3)
正常(0) --> 已删除(2)
```

### 商家状态流转
```
待审核(0) --> 正常(1) 或 关闭(3)
正常(1) <---> 冻结(2)
正常(1) --> 关闭(3)
```

### 券状态流转
```
下架(0) <---> 上架(1)
上架(1) --> 已下架(管理员)(2)
上架(1) --> 违规下架(3)
```

### 评论状态流转
```
正常(0) <---> 屏蔽(2)
正常(0) --> 已删除(1)
```

