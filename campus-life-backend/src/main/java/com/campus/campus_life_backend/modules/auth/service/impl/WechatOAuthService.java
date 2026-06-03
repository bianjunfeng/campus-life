package com.campus.campus_life_backend.modules.auth.service.impl;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.auth.service.OAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信OAuth登录服务
 * 注意：需要在application.yml中配置微信AppID和AppSecret
 */
@Service
public class WechatOAuthService implements OAuthService {

    @Value("${oauth.wechat.app-id:}")
    private String appId;

    @Value("${oauth.wechat.app-secret:}")
    private String appSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String WECHAT_AUTH_URL = "https://open.weixin.qq.com/connect/qrconnect";
    private static final String WECHAT_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String WECHAT_USERINFO_URL = "https://api.weixin.qq.com/sns/userinfo";

    @Override
    public String getAuthorizationUrl(String redirectUri, String state) {
        if (appId == null || appId.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.OAUTH_CONFIG_MISSING, "微信AppID未配置，请在application.yml中配置oauth.wechat.app-id");
        }
        if (state == null || state.isBlank()) {
            throw new BusinessException(BusinessErrorCode.OAUTH_STATE_REQUIRED, "微信OAuth state 不能为空");
        }

        try {
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
            return String.format("%s?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_login&state=%s#wechat_redirect",
                    WECHAT_AUTH_URL, appId, encodedRedirectUri, state);
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.OAUTH_AUTHORIZE_URL_FAILED, "生成微信授权URL失败", e);
        }
    }

    @Override
    public String getAccessToken(String code, String redirectUri) {
        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.OAUTH_CONFIG_MISSING, "微信AppID或AppSecret未配置");
        }

        String url = String.format("%s?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                WECHAT_TOKEN_URL, appId, appSecret, code);

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || response.containsKey("errcode")) {
                throw new BusinessException(BusinessErrorCode.OAUTH_ACCESS_TOKEN_FAILED, "获取微信AccessToken失败");
            }

            String accessToken = (String) response.get("access_token");
            String openid = (String) response.get("openid");
            if (accessToken == null || accessToken.isBlank() || openid == null || openid.isBlank()) {
                throw new BusinessException(BusinessErrorCode.OAUTH_ACCESS_TOKEN_MISSING, "微信AccessToken或OpenID缺失");
            }
            return accessToken + "|" + openid;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.OAUTH_ACCESS_TOKEN_FAILED, "获取微信AccessToken失败", e);
        }
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        String[] tokenParts = splitWechatAccessToken(accessToken);
        String actualAccessToken = tokenParts[0];
        String openid = tokenParts[1];

        String url = String.format("%s?access_token=%s&openid=%s",
                WECHAT_USERINFO_URL, actualAccessToken, openid);

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || response.containsKey("errcode")) {
                throw new BusinessException(BusinessErrorCode.OAUTH_USERINFO_FAILED, "获取微信用户信息失败");
            }

            String nickname = (String) response.get("nickname");
            String headimgurl = (String) response.get("headimgurl");
            Integer sex = (Integer) response.get("sex");
            String gender = sex != null && sex == 1 ? "男" : (sex != null && sex == 2 ? "女" : "未知");

            return new OAuthUserInfo(openid, nickname, headimgurl, gender);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.OAUTH_USERINFO_FAILED, "获取微信用户信息失败", e);
        }
    }

    private String[] splitWechatAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank() || !accessToken.contains("|")) {
            throw new BusinessException(BusinessErrorCode.OAUTH_TOKEN_FORMAT_INVALID, "微信AccessToken格式错误");
        }
        String[] tokenParts = accessToken.split("\\|", 2);
        if (tokenParts.length != 2 || tokenParts[0].isBlank() || tokenParts[1].isBlank()) {
            throw new BusinessException(BusinessErrorCode.OAUTH_TOKEN_FORMAT_INVALID, "微信AccessToken格式错误");
        }
        return tokenParts;
    }
}
