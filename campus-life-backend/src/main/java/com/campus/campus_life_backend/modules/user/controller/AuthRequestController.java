package com.campus.campus_life_backend.modules.user.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireLogin;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.merchant.entity.MerchantAuthRequest;
import com.campus.campus_life_backend.modules.merchant.service.MerchantAuthService;
import com.campus.campus_life_backend.modules.user.entity.StudentAuthRequest;
import com.campus.campus_life_backend.modules.user.service.AuthRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth-request")
@RequireLogin
public class AuthRequestController {

    private static final Logger logger = LoggerFactory.getLogger(AuthRequestController.class);

    private final CurrentUserAccessor currentUserAccessor;
    private final AuthRequestService authRequestService;
    private final MerchantAuthService merchantAuthService;

    public AuthRequestController(CurrentUserAccessor currentUserAccessor,
                                 AuthRequestService authRequestService,
                                 MerchantAuthService merchantAuthService) {
        this.currentUserAccessor = currentUserAccessor;
        this.authRequestService = authRequestService;
        this.merchantAuthService = merchantAuthService;
    }

    @PostMapping("/student")
    public ApiResponse<Map<String, Object>> submitStudentAuthRequest(@RequestBody Map<String, Object> requestData) {
        Long userId = currentUserAccessor.requireUserId();

        String realName = readString(requestData.get("realName"));
        String school = readString(requestData.get("school"));
        String studentNo = readString(requestData.get("studentNo"));
        String studentCardImg = readString(requestData.get("studentCardImg"));
        if (realName == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "真实姓名不能为空");
        }
        if (school == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "学校名称不能为空");
        }
        if (studentNo == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "学号不能为空");
        }
        if (studentCardImg == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "学生证照片不能为空");
        }

        try {
            StudentAuthRequest request = authRequestService.submitStudentAuthRequest(userId, requestData);

            Map<String, Object> result = new HashMap<>();
            result.put("id", request.getId());
            result.put("status", request.getStatus());
            result.put("statusText", getStatusText(request.getStatus()));
            result.put("reason", request.getReason());
            return ApiResponse.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("提交学生认证申请失败", e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "提交认证申请失败，请稍后重试", e);
        }
    }

    @GetMapping("/student")
    public ApiResponse<Map<String, Object>> getStudentAuthRequest() {
        Long userId = currentUserAccessor.requireUserId();
        StudentAuthRequest request = authRequestService.getStudentAuthRequest(userId);

        if (request == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("hasRequest", false);
            return ApiResponse.success(result);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("hasRequest", true);
        result.put("id", request.getId());
        result.put("status", request.getStatus());
        result.put("statusText", getStatusText(request.getStatus()));
        result.put("reason", request.getReason());
        result.put("createTime", request.getCreateTime());
        result.put("reviewTime", request.getReviewTime());
        result.put("realName", request.getRealName());
        result.put("school", request.getSchool());
        result.put("college", request.getCollege());
        result.put("major", request.getMajor());
        result.put("grade", request.getGrade());
        result.put("className", request.getClassName());
        result.put("studentNo", request.getStudentNo());
        result.put("studentCardImg", request.getStudentCardImg());
        return ApiResponse.success(result);
    }

    @PostMapping("/merchant")
    public ApiResponse<Map<String, Object>> submitMerchantAuthRequest(@RequestBody Map<String, Object> requestData) {
        Long userId = currentUserAccessor.requireUserId();

        String merchantName = readString(requestData.get("merchantName"));
        String licenseImg = readString(requestData.get("licenseImg"));
        if (merchantName == null) {
            throw new BusinessException(BusinessErrorCode.MERCHANT_NAME_REQUIRED);
        }
        if (licenseImg == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "营业执照照片不能为空");
        }

        try {
            MerchantAuthRequest request = merchantAuthService.submitMerchantAuthRequest(userId, requestData);

            Map<String, Object> result = new HashMap<>();
            result.put("id", request.getId());
            result.put("status", request.getStatus());
            result.put("statusText", getStatusText(request.getStatus()));
            result.put("reason", request.getReason());
            return ApiResponse.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("提交商家认证申请失败", e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "提交认证申请失败，请稍后重试", e);
        }
    }

    @GetMapping("/merchant")
    public ApiResponse<Map<String, Object>> getMerchantAuthRequest() {
        Long userId = currentUserAccessor.requireUserId();
        MerchantAuthRequest request = merchantAuthService.getMerchantAuthRequest(userId);

        if (request == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("hasRequest", false);
            return ApiResponse.success(result);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("hasRequest", true);
        result.put("id", request.getId());
        result.put("status", request.getStatus());
        result.put("statusText", getStatusText(request.getStatus()));
        result.put("reason", request.getReason());
        result.put("createTime", request.getCreateTime());
        result.put("reviewTime", request.getReviewTime());
        result.put("merchantName", request.getMerchantName());
        result.put("licenseNo", request.getLicenseNo());
        result.put("address", request.getAddress());
        result.put("licenseImg", request.getLicenseImg());
        return ApiResponse.success(result);
    }

    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "待审核";
            case 1:
                return "已通过";
            case 2:
                return "已驳回";
            default:
                return "未知";
        }
    }

    private String readString(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }
}
