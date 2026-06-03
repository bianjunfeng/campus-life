package com.campus.campus_life_backend.modules.auth.service;

/**
 * OAuth第三方登录服务接口
 */
public interface OAuthService {

    /**
     * 获取授权URL
     * @param redirectUri 回调地址
     * @param state 状态参数
     * @return 授权URL
     */
    String getAuthorizationUrl(String redirectUri, String state);

    /**
     * 通过授权码获取AccessToken
     * @param code 授权码
     * @param redirectUri 回调地址
     * @return AccessToken
     */
    String getAccessToken(String code, String redirectUri);

    /**
     * 通过AccessToken获取用户信息
     * @param accessToken AccessToken
     * @return 用户OpenID和基本信息
     */
    OAuthUserInfo getUserInfo(String accessToken);

    /**
     * OAuth用户信息
     */
    class OAuthUserInfo {
        private String openid;
        private String nickname;
        private String avatar;
        private String gender;

        public OAuthUserInfo(String openid, String nickname, String avatar, String gender) {
            this.openid = openid;
            this.nickname = nickname;
            this.avatar = avatar;
            this.gender = gender;
        }

        public String getOpenid() { return openid; }
        public String getNickname() { return nickname; }
        public String getAvatar() { return avatar; }
        public String getGender() { return gender; }
    }
}
