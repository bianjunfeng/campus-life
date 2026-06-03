package com.campus.campus_life_backend.modules.auth.service.impl;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.auth.service.OAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * QQ OAuth登录服务
 * 注意：需要在application.yml中配置QQ AppID和AppKey
 */
@Service
public class QQOAuthService implements OAuthService {

    @Value("${oauth.qq.app-id:}")
    private String appId;

    @Value("${oauth.qq.app-key:}")
    private String appKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String QQ_AUTH_URL = "https://graph.qq.com/oauth2.0/authorize";
    private static final String QQ_TOKEN_URL = "https://graph.qq.com/oauth2.0/token";
    private static final String QQ_OPENID_URL = "https://graph.qq.com/oauth2.0/me";
    private static final String QQ_USERINFO_URL = "https://graph.qq.com/user/get_user_info";

    @Override
    public String getAuthorizationUrl(String redirectUri, String state) {
        if (appId == null || appId.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.OAUTH_CONFIG_MISSING, "QQ AppID未配置，请在application.yml中配置oauth.qq.app-id");
        }
        if (state == null || state.isBlank()) {
            throw new BusinessException(BusinessErrorCode.OAUTH_STATE_REQUIRED, "QQ OAuth state 不能为空");
        }

        try {
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
            return String.format("%s?response_type=code&client_id=%s&redirect_uri=%s&scope=get_user_info&state=%s",
                    QQ_AUTH_URL, appId, encodedRedirectUri, state);
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.OAUTH_AUTHORIZE_URL_FAILED, "生成QQ授权URL失败", e);
        }
    }

    @Override
    public String getAccessToken(String code, String redirectUri) {
        if (appId == null || appId.isEmpty() || appKey == null || appKey.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.OAUTH_CONFIG_MISSING, "QQ AppID或AppKey未配置");
        }

        String url = String.format("%s?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s",
                QQ_TOKEN_URL, appId, appKey, code, redirectUri);

        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.contains("error")) {
                throw new BusinessException(BusinessErrorCode.OAUTH_ACCESS_TOKEN_FAILED, "获取QQ AccessToken失败");
            }
            // QQ返回格式: access_token=xxx&expires_in=7776000&refresh_token=xxx
            String[] params = response.split("&");
            for (String param : params) {
                if (param.startsWith("access_token=")) {
                    return param.substring("access_token=".length());
                }
            }
            throw new BusinessException(BusinessErrorCode.OAUTH_ACCESS_TOKEN_FAILED, "解析QQ AccessToken失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.OAUTH_ACCESS_TOKEN_FAILED, "获取QQ AccessToken失败", e);
        }
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        // 先获取openid
        String openidUrl = String.format("%s?access_token=%s", QQ_OPENID_URL, accessToken);
        String openidResponse = restTemplate.getForObject(openidUrl, String.class);

        if (openidResponse == null || openidResponse.contains("error")) {
            throw new BusinessException(BusinessErrorCode.OAUTH_ACCESS_TOKEN_FAILED, "获取QQ OpenID失败");
        }

        // 解析openid (格式: callback({"client_id":"xxx","openid":"xxx"});)
        String openid = extractOpenid(openidResponse);

        // 获取用户信息
        String userInfoUrl = String.format("%s?access_token=%s&oauth_consumer_key=%s&openid=%s",
                QQ_USERINFO_URL, accessToken, appId, openid);

        try {
            Map<String, Object> response = restTemplate.getForObject(userInfoUrl, Map.class);
            if (response == null || (Integer) response.get("ret") != 0) {
                throw new BusinessException(BusinessErrorCode.OAUTH_USERINFO_FAILED, "获取QQ用户信息失败");
            }

            String nickname = (String) response.get("nickname");
            String figureurl = (String) response.get("figureurl_qq_2"); // QQ头像
            String gender = (String) response.get("gender");

            return new OAuthUserInfo(openid, nickname, figureurl, gender);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.OAUTH_USERINFO_FAILED, "获取QQ用户信息失败", e);
        }
    }

    private String extractOpenid(String response) {
        // 解析 callback({"client_id":"xxx","openid":"xxx"});
        int start = response.indexOf("\"openid\":\"") + 10;
        int end = response.indexOf("\"", start);
        return response.substring(start, end);
    }
}
