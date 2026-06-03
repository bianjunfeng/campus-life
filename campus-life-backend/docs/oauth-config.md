# 第三方登录配置说明

## 概述

系统已支持微信和QQ的第三方登录功能。使用前需要在配置文件中添加相应的AppID和AppSecret。

## 配置步骤

### 1. 微信登录配置

1. 访问 [微信开放平台](https://open.weixin.qq.com/)
2. 注册并创建网站应用
3. 获取 AppID 和 AppSecret
4. 在 `application-dev.yml` 或 `application.yml` 中添加配置：

```yaml
oauth:
  wechat:
    app-id: 你的微信AppID
    app-secret: 你的微信AppSecret
```

5. 在微信开放平台设置授权回调域名（如：`yourdomain.com`）

### 2. QQ登录配置

1. 访问 [QQ互联](https://connect.qq.com/)
2. 注册并创建网站应用
3. 获取 AppID 和 AppKey
4. 在 `application-dev.yml` 或 `application.yml` 中添加配置：

```yaml
oauth:
  qq:
    app-id: 你的QQ AppID
    app-key: 你的QQ AppKey
```

5. 在QQ互联设置授权回调域名（如：`yourdomain.com`）

## 数据库迁移

在首次使用前，需要执行数据库迁移脚本：

```sql
-- 执行 docs/add-oauth-fields.sql
ALTER TABLE `user` 
ADD COLUMN `wechat_openid` VARCHAR(100) NULL COMMENT '微信OpenID' AFTER `phone`,
ADD COLUMN `qq_openid` VARCHAR(100) NULL COMMENT 'QQ OpenID' AFTER `wechat_openid`,
ADD UNIQUE KEY `uk_wechat_openid` (`wechat_openid`),
ADD UNIQUE KEY `uk_qq_openid` (`qq_openid`);
```

## 使用说明

1. 用户点击登录页面的微信或QQ登录按钮
2. 系统跳转到对应的授权页面
3. 用户授权后，回调到 `/oauth/wechat/callback` 或 `/oauth/qq/callback`
4. 系统自动创建用户（如果不存在）并登录
5. 跳转到首页或之前访问的页面

## 注意事项

- 如果未配置AppID和AppSecret，点击登录按钮会提示配置错误
- 第三方登录的用户手机号可能为空，需要后续绑定
- 首次登录会自动创建账号，使用第三方平台的昵称和头像

