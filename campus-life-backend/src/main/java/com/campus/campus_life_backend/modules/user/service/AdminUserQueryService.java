package com.campus.campus_life_backend.modules.user.service;

import com.campus.campus_life_backend.modules.user.entity.StudentAuthRequest;
import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.user.mapper.StudentAuthRequestMapper;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import com.campus.campus_life_backend.modules.user.vo.UserTrendVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminUserQueryService {

    private final UserMapper userMapper;
    private final StudentAuthRequestMapper studentAuthRequestMapper;

    public AdminUserQueryService(UserMapper userMapper, StudentAuthRequestMapper studentAuthRequestMapper) {
        this.userMapper = userMapper;
        this.studentAuthRequestMapper = studentAuthRequestMapper;
    }

    public Map<String, Object> getStudentAuthList(Integer page, Integer size, Integer status, String keyword) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        int offset = (p - 1) * s;
        String safeKeyword = isBlank(keyword) ? null : keyword.trim();

        List<StudentAuthRequest> list = studentAuthRequestMapper.findAdminList(status, safeKeyword, offset, s);
        long total = studentAuthRequestMapper.countAdminList(status, safeKeyword);

        List<Map<String, Object>> rows = (list == null ? List.<StudentAuthRequest>of() : list).stream()
                .map(this::toStudentAuthItem)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", rows);
        result.put("page", p);
        result.put("size", s);
        result.put("total", total);
        return result;
    }

    public Map<String, Object> getUserList(Integer page, Integer size, Integer role, Integer status, String keyword) {
        String keywordLower = toLower(keyword);
        List<User> filtered = findAllUsersForAdmin().stream()
                .filter(u -> role == null || Objects.equals(u.getRole(), role))
                .filter(u -> status == null || Objects.equals(u.getStatus(), status))
                .filter(u -> isBlank(keyword)
                        || containsIgnoreCase(u.getUsername(), keywordLower)
                        || containsIgnoreCase(u.getPhone(), keywordLower)
                        || containsIgnoreCase(u.getEmail(), keywordLower))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", paginate(filtered, page, size));
        result.put("page", normalizePage(page));
        result.put("size", normalizeSize(size));
        result.put("total", filtered.size());
        return result;
    }

    public Map<String, Object> getUserListAdvanced(Integer page, Integer size, String keyword, String username, String phone,
                                                   String email, Integer role, Integer status,
                                                   String startDate, String endDate) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        String keywordLower = toLower(keyword);
        Long keywordUserId = tryParseLong(keyword);

        List<User> filtered = findAllUsersForAdmin().stream()
                .filter(u -> role == null || Objects.equals(u.getRole(), role))
                .filter(u -> status == null || Objects.equals(u.getStatus(), status))
                .filter(u -> isBlank(keyword)
                        || Objects.equals(u.getId(), keywordUserId)
                        || containsIgnoreCase(u.getUsername(), keywordLower)
                        || containsIgnoreCase(u.getPhone(), keywordLower)
                        || containsIgnoreCase(u.getEmail(), keywordLower))
                .filter(u -> isBlank(username) || containsIgnoreCase(u.getUsername(), toLower(username)))
                .filter(u -> isBlank(phone) || containsIgnoreCase(u.getPhone(), toLower(phone)))
                .filter(u -> isBlank(email) || containsIgnoreCase(u.getEmail(), toLower(email)))
                .filter(u -> withinDateRange(u.getCreateTime(), start, end))
                .sorted(Comparator.comparing(User::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", paginate(filtered, page, size));
        result.put("page", normalizePage(page));
        result.put("size", normalizeSize(size));
        result.put("total", filtered.size());
        return result;
    }

    public Map<String, Object> getMemberListPage(Integer page, Integer size, String keyword, String username, String phone,
                                                 String email, Integer role, Integer status,
                                                 String startDate, String endDate) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        int offset = (p - 1) * s;

        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        LocalDateTime startTime = start == null ? null : start.atStartOfDay();
        LocalDateTime endTime = end == null ? null : end.atTime(23, 59, 59);
        String safeKeyword = isBlank(keyword) ? null : keyword.trim();
        Long keywordId = tryParseLong(safeKeyword);

        List<Map<String, Object>> list = userMapper.findAdminMembers(
                safeKeyword,
                keywordId,
                isBlank(username) ? null : username.trim(),
                isBlank(phone) ? null : phone.trim(),
                isBlank(email) ? null : email.trim(),
                role,
                status,
                startTime,
                endTime,
                offset,
                s
        );
        long total = userMapper.countAdminMembers(
                safeKeyword,
                keywordId,
                isBlank(username) ? null : username.trim(),
                isBlank(phone) ? null : phone.trim(),
                isBlank(email) ? null : email.trim(),
                role,
                status,
                startTime,
                endTime
        );

        List<Map<String, Object>> normalized = (list == null ? List.<Map<String, Object>>of() : list).stream()
                .map(this::normalizeMemberRow)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", normalized);
        result.put("page", p);
        result.put("size", s);
        result.put("total", total);
        result.put("enabledTotal", userMapper.countAdminMembersByStatus(1));
        result.put("disabledTotal", userMapper.countAdminMembersByStatus(0));
        return result;
    }

    public Map<String, Object> getUserStats() {
        List<User> users = findAllUsersForAdmin();
        long student = users.stream().filter(u -> u.getRole() != null && u.getRole() == 0).count();
        long merchant = users.stream().filter(u -> u.getRole() != null && u.getRole() == 1).count();
        long admin = users.stream().filter(u -> u.getRole() != null && u.getRole() == 2).count();
        long verifiedStudents = users.stream()
                .filter(u -> u.getRole() != null && u.getRole() == 0)
                .filter(u -> (u.getIsStudentVerified() != null && u.getIsStudentVerified() == 1)
                        || (u.getStatus() != null && u.getStatus() == 1))
                .count();
        long verifiedMerchants = users.stream()
                .filter(u -> u.getRole() != null && u.getRole() == 1)
                .filter(u -> (u.getIsMerchant() != null && u.getIsMerchant() == 1)
                        || (u.getStatus() != null && u.getStatus() == 1))
                .count();
        YearMonth currentMonth = YearMonth.now();
        long monthlyNew = users.stream()
                .map(User::getCreateTime)
                .filter(Objects::nonNull)
                .filter(t -> YearMonth.from(t).equals(currentMonth))
                .count();
        long totalSchools = users.stream()
                .map(User::getRegion)
                .filter(r -> !isBlank(r))
                .distinct()
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("student", student);
        stats.put("merchant", merchant);
        stats.put("admin", admin);
        stats.put("total", users.size());
        stats.put("totalStudents", student);
        stats.put("totalMerchants", merchant);
        stats.put("totalSchools", totalSchools);
        stats.put("verifiedStudents", verifiedStudents);
        stats.put("verifiedMerchants", verifiedMerchants);
        stats.put("monthlyNew", monthlyNew);
        stats.put("genderData", buildGenderStats(users));
        stats.put("schoolData", buildSchoolStats(users));
        stats.put("collegeData", buildCollegeStats(users));
        stats.put("gradeData", List.of());
        return stats;
    }

    public List<User> findAllUsersForAdmin() {
        List<User> users = userMapper.findAll();
        return users == null ? List.of() : users;
    }

    public long countUsers() {
        return userMapper.countAllForAdmin();
    }

    public long countPendingStudentAuth() {
        return studentAuthRequestMapper.countByStatus(0);
    }

    public long countUsersSince(LocalDateTime since) {
        return userMapper.countUsersSince(since);
    }

    public List<UserTrendVO> countUsersTrend(int days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(Math.max(days, 1) - 1L).toLocalDate().atStartOfDay();
        return mapTrendRows(userMapper.countUsersTrend(start, end), UserTrendVO::new);
    }

    public long countUsersByRoleAndStatus(Integer role, Integer status) {
        return userMapper.countByRoleAndStatusForAdmin(role, status);
    }

    public long countVerifiedStudents() {
        return userMapper.countVerifiedStudentsForAdmin();
    }

    public long countVerifiedMerchants() {
        return userMapper.countVerifiedMerchantsForAdmin();
    }

    public long countDistinctRegions() {
        return userMapper.countDistinctRegionsForAdmin();
    }

    public List<Map<String, Object>> countGenderData() {
        List<Map<String, Object>> rows = userMapper.countByGenderForAdmin();
        return rows == null ? List.of() : rows;
    }

    public List<Map<String, Object>> countSchoolData(int limit) {
        List<Map<String, Object>> rows = userMapper.countByRegionForAdmin(limit);
        return rows == null ? List.of() : rows;
    }

    public List<Map<String, Object>> countCollegeData(int limit) {
        List<Map<String, Object>> rows = userMapper.countByOccupationForAdmin(limit);
        return rows == null ? List.of() : rows;
    }

    private List<Map<String, Object>> buildGenderStats(List<User> users) {
        long male = users.stream().filter(u -> u.getGender() != null && u.getGender() == 1).count();
        long female = users.stream().filter(u -> u.getGender() != null && u.getGender() == 2).count();
        long unknown = Math.max(users.size() - male - female, 0);
        long total = Math.max(users.size(), 1);

        List<Map<String, Object>> list = new ArrayList<>();
        list.add(buildGenderItem("男", male, total, "#5470c6"));
        list.add(buildGenderItem("女", female, total, "#ee6666"));
        list.add(buildGenderItem("未知", unknown, total, "#91cc75"));
        return list;
    }

    private Map<String, Object> buildGenderItem(String name, long value, long total, String color) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value);
        item.put("percent", Math.round(value * 10000.0 / total) / 100.0);
        item.put("color", color);
        return item;
    }

    private List<Map<String, Object>> buildSchoolStats(List<User> users) {
        Map<String, Integer> totalMap = new LinkedHashMap<>();
        Map<String, Integer> verifiedMap = new LinkedHashMap<>();
        for (User user : users) {
            String region = isBlank(user.getRegion()) ? "未知地区" : user.getRegion().trim();
            totalMap.put(region, totalMap.getOrDefault(region, 0) + 1);
            boolean verified = user.getRole() != null && user.getRole() == 0
                    && (user.getIsStudentVerified() != null && user.getIsStudentVerified() == 1);
            if (verified) {
                verifiedMap.put(region, verifiedMap.getOrDefault(region, 0) + 1);
            }
        }
        return totalMap.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(entry -> {
                    int value = entry.getValue();
                    int verified = verifiedMap.getOrDefault(entry.getKey(), 0);
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", entry.getKey());
                    item.put("value", value);
                    item.put("verified", verified);
                    item.put("unverified", Math.max(value - verified, 0));
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> buildCollegeStats(List<User> users) {
        Map<String, Integer> counter = new LinkedHashMap<>();
        for (User user : users) {
            String occupation = isBlank(user.getOccupation()) ? "未填写" : user.getOccupation().trim();
            counter.put(occupation, counter.getOrDefault(occupation, 0) + 1);
        }
        return counter.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    return item;
                })
                .toList();
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size < 1 ? 20 : size;
    }

    private <T> List<T> paginate(List<T> source, Integer page, Integer size) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        int from = (p - 1) * s;
        if (from >= source.size()) {
            return List.of();
        }
        int to = Math.min(from + s, source.size());
        return new ArrayList<>(source.subList(from, to));
    }

    private boolean containsIgnoreCase(String value, String expectedLower) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(expectedLower);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String toLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Long tryParseLong(String value) {
        if (isBlank(value)) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> normalizeMemberRow(Map<String, Object> row) {
        Map<String, Object> result = new HashMap<>();
        Long id = parseLong(row.get("id"), 0L);
        result.put("id", id);
        result.put("xiaolanshuId", id == null ? "" : String.valueOf(id));
        result.put("username", row.getOrDefault("username", ""));
        result.put("avatarUrl", row.getOrDefault("avatarUrl", ""));
        result.put("gender", parseInteger(row.get("gender"), 0));
        result.put("phone", row.getOrDefault("phone", ""));
        result.put("email", row.getOrDefault("email", ""));
        result.put("role", parseInteger(row.get("role"), 0));
        result.put("status", parseInteger(row.get("status"), 1));
        result.put("region", row.getOrDefault("region", ""));
        result.put("postCount", parseInteger(row.get("postCount"), 0));
        result.put("voucherCount", parseInteger(row.get("voucherCount"), 0));
        result.put("createTime", row.get("createTime"));
        return result;
    }

    private Map<String, Object> toStudentAuthItem(StudentAuthRequest request) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", request.getId());
        row.put("userId", request.getUserId());
        row.put("realName", request.getRealName());
        row.put("school", request.getSchool());
        row.put("college", request.getCollege());
        row.put("major", request.getMajor());
        row.put("grade", request.getGrade());
        row.put("className", request.getClassName());
        row.put("studentNo", request.getStudentNo());
        row.put("studentCardImg", request.getStudentCardImg());
        row.put("status", request.getStatus());
        row.put("reason", request.getReason());
        row.put("reviewerId", request.getReviewerId());
        row.put("createTime", request.getCreateTime());
        row.put("reviewTime", request.getReviewTime());
        return row;
    }

    private boolean withinDateRange(LocalDateTime dateTime, LocalDate start, LocalDate end) {
        if (start == null && end == null) return true;
        if (dateTime == null) return false;
        LocalDate date = dateTime.toLocalDate();
        if (start != null && date.isBefore(start)) return false;
        if (end != null && date.isAfter(end)) return false;
        return true;
    }

    private Long parseLong(Object value, Long defaultValue) {
        if (value == null) return defaultValue;
        try {
            if (value instanceof Number num) return num.longValue();
            String str = String.valueOf(value).trim();
            if (str.isEmpty()) return defaultValue;
            return Long.parseLong(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Integer parseInteger(Object value, Integer defaultValue) {
        if (value == null) return defaultValue;
        try {
            if (value instanceof Number num) return num.intValue();
            String str = String.valueOf(value).trim();
            if (str.isEmpty()) return defaultValue;
            return Integer.parseInt(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private <T> List<T> mapTrendRows(List<Map<String, Object>> rows, java.util.function.BiFunction<String, Long, T> mapper) {
        return (rows == null ? List.<Map<String, Object>>of() : rows).stream()
                .map(row -> mapper.apply(String.valueOf(row.get("day")), parseLong(row.get("value"), 0L)))
                .collect(Collectors.toList());
    }
}
