package com.campus.campus_life_backend.modules.user.mapper;

import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.search.dto.SearchSyncCandidateDTO;
import com.campus.campus_life_backend.modules.search.dto.SearchUserItemDTO;
import com.campus.campus_life_backend.modules.search.dto.UserSearchSourceDTO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ClassName: UserMapper
 * Description:
 *
 * @Author Junfeng Bian
 * @Create 2025/11/29 20:20
 * @Version 1.0
 */
public interface UserMapper {

    User findByPhone(@Param("phone") String phone);

    User findByEmail(@Param("email") String email);

    User findByUsername(@Param("username") String username);

    User findById(@Param("id") Long id);

    List<User> findByIds(@Param("ids") List<Long> ids);

    User findByWechatOpenid(@Param("wechatOpenid") String wechatOpenid);

    User findByQqOpenid(@Param("qqOpenid") String qqOpenid);

    List<User> findAll();

    int insertUser(User user);

    int updateUser(User user);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    List<SearchUserItemDTO> searchUsers(
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    long countSearchUsers(@Param("keyword") String keyword);

    UserSearchSourceDTO findSearchUserById(@Param("id") Long id);

    List<Long> findIndexableUserIds(@Param("offset") Integer offset, @Param("limit") Integer limit);

    List<SearchSyncCandidateDTO> findUpdatedUserCandidates(
            @Param("since") LocalDateTime since,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    List<Map<String, Object>> findAdminMembers(
            @Param("keyword") String keyword,
            @Param("keywordId") Long keywordId,
            @Param("username") String username,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("role") Integer role,
            @Param("status") Integer status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    long countAdminMembers(
            @Param("keyword") String keyword,
            @Param("keywordId") Long keywordId,
            @Param("username") String username,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("role") Integer role,
            @Param("status") Integer status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    long countAdminMembersByStatus(@Param("status") Integer status);

    List<Long> findUserIdsByRole(@Param("role") Integer role);

    long countAllForAdmin();

    long countByRoleAndStatusForAdmin(@Param("role") Integer role, @Param("status") Integer status);

    long countVerifiedStudentsForAdmin();

    long countVerifiedMerchantsForAdmin();

    long countUsersSince(@Param("since") LocalDateTime since);

    long countDistinctRegionsForAdmin();

    List<Map<String, Object>> countByGenderForAdmin();

    List<Map<String, Object>> countByRegionForAdmin(@Param("limit") Integer limit);

    List<Map<String, Object>> countByOccupationForAdmin(@Param("limit") Integer limit);

    List<Map<String, Object>> countUsersTrend(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
