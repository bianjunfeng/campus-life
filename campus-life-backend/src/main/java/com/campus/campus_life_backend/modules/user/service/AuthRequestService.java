package com.campus.campus_life_backend.modules.user.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.user.entity.StudentAuthRequest;
import com.campus.campus_life_backend.modules.user.entity.StudentProfile;
import com.campus.campus_life_backend.modules.user.mapper.StudentAuthRequestMapper;
import com.campus.campus_life_backend.modules.user.mapper.StudentProfileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证申请服务
 */
@Service
public class AuthRequestService {

    private static final Logger logger = LoggerFactory.getLogger(AuthRequestService.class);

    private final StudentAuthRequestMapper studentAuthRequestMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserService userService;

    public record AdminStudentAuthReviewResult(Integer oldStatus, Integer newStatus) {
    }

    public AuthRequestService(StudentAuthRequestMapper studentAuthRequestMapper,
                              StudentProfileMapper studentProfileMapper,
                              StringRedisTemplate stringRedisTemplate,
                              UserService userService) {
        this.studentAuthRequestMapper = studentAuthRequestMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.userService = userService;
    }

    /**
     * 提交学生认证申请
     */
    @Transactional
    public StudentAuthRequest submitStudentAuthRequest(Long userId, Map<String, Object> requestData) {
        String lockKey = "auth:student:submit:" + userId;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 8, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(BusinessErrorCode.TOO_MANY_REQUESTS, "提交过于频繁，请稍后重试");
        }

        try {
            if (userService.hasAnyAuthVerified(userId)) {
                throw new BusinessException(BusinessErrorCode.STUDENT_AUTH_ALREADY_APPROVED_OTHER);
            }

            // 检查是否已有待审核的申请
            StudentAuthRequest existing = studentAuthRequestMapper.findByUserId(userId);

            StudentAuthRequest request = new StudentAuthRequest();
            request.setUserId(userId);
            request.setRealName(toStr(requestData.get("realName")));
            request.setSchool(toStr(requestData.get("school")));
            request.setCollege(toStr(requestData.get("college")));
            request.setMajor(toStr(requestData.get("major")));
            request.setGrade(toStr(requestData.get("grade")));
            request.setClassName(toStr(requestData.get("className")));
            request.setStudentNo(toStr(requestData.get("studentNo")));
            request.setStudentCardImg(toStr(requestData.get("studentCardImg")));
            request.setStatus(0); // 待审核

            if (existing != null) {
                Integer status = existing.getStatus();
                if (status != null && status == 0) {
                    throw new BusinessException(BusinessErrorCode.STUDENT_AUTH_PENDING_DUPLICATE);
                }
                if (status != null && status == 1) {
                    throw new BusinessException(BusinessErrorCode.STUDENT_AUTH_ALREADY_APPROVED);
                }
                // 仅当上一次被驳回(status=2)时，允许再次提交新申请
                if (status != null && status != 2) {
                    throw new BusinessException(BusinessErrorCode.STUDENT_AUTH_STATUS_NOT_ALLOWED);
                }
                studentAuthRequestMapper.insert(request);
                logger.info("创建新的学生认证申请(上次已驳回): userId={}", userId);
            } else {
                // 创建新申请
                studentAuthRequestMapper.insert(request);
                logger.info("创建学生认证申请: userId={}", userId);
            }

            return studentAuthRequestMapper.findByUserId(userId);
        } finally {
            String current = stringRedisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(current)) {
                stringRedisTemplate.delete(lockKey);
            }
        }
    }

    private String toStr(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * 获取用户的学生认证申请状态
     */
    public StudentAuthRequest getStudentAuthRequest(Long userId) {
        return studentAuthRequestMapper.findByUserId(userId);
    }

    @Transactional
    public AdminStudentAuthReviewResult adminApproveStudentAuth(Long requestId, Long reviewerId, String reason) {
        StudentAuthRequest request = studentAuthRequestMapper.findById(requestId);
        if (request == null) {
            throw new BusinessException(BusinessErrorCode.STUDENT_AUTH_REQUEST_NOT_FOUND);
        }
        Integer oldStatus = request.getStatus();
        if (oldStatus == null || oldStatus != 0) {
            throw new BusinessException(BusinessErrorCode.STUDENT_AUTH_ALREADY_PASSED);
        }

        StudentProfile duplicated = studentProfileMapper.findBySchoolAndStudentNo(request.getSchool(), request.getStudentNo());
        if (duplicated != null && !request.getUserId().equals(duplicated.getUserId())) {
            throw new BusinessException(BusinessErrorCode.STUDENT_NO_USED_BY_OTHER);
        }

        StudentProfile profile = studentProfileMapper.findByUserIdAllStatus(request.getUserId());
        if (profile == null) {
            profile = new StudentProfile();
            profile.setUserId(request.getUserId());
            applyProfile(profile, request);
            profile.setStatus(1);
            studentProfileMapper.insertStudentProfile(profile);
        } else {
            applyProfile(profile, request);
            studentProfileMapper.updateStudentProfile(profile);
            studentProfileMapper.updateStatus(request.getUserId(), 1);
        }

        studentAuthRequestMapper.review(requestId, 1, reason, reviewerId);
        return new AdminStudentAuthReviewResult(oldStatus, 1);
    }

    @Transactional
    public AdminStudentAuthReviewResult adminRejectStudentAuth(Long requestId, Long reviewerId, String reason) {
        StudentAuthRequest request = studentAuthRequestMapper.findById(requestId);
        if (request == null) {
            throw new BusinessException(BusinessErrorCode.STUDENT_AUTH_REQUEST_NOT_FOUND);
        }
        Integer oldStatus = request.getStatus();
        if (oldStatus == null || oldStatus != 0) {
            throw new BusinessException(BusinessErrorCode.STUDENT_AUTH_ALREADY_REJECTED);
        }
        studentAuthRequestMapper.review(requestId, 2, reason, reviewerId);
        return new AdminStudentAuthReviewResult(oldStatus, 2);
    }

    private void applyProfile(StudentProfile profile, StudentAuthRequest request) {
        profile.setRealName(request.getRealName());
        profile.setSchool(request.getSchool());
        profile.setCollege(request.getCollege());
        profile.setMajor(request.getMajor());
        profile.setGrade(request.getGrade());
        profile.setClassName(request.getClassName());
        profile.setStudentNo(request.getStudentNo());
    }
}
