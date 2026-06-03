package com.campus.campus_life_backend.modules.merchant.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.entity.MerchantType;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantTypeMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MerchantQueryService {

    private final MerchantMapper merchantMapper;
    private final MerchantTypeMapper merchantTypeMapper;

    public MerchantQueryService(MerchantMapper merchantMapper, MerchantTypeMapper merchantTypeMapper) {
        this.merchantMapper = merchantMapper;
        this.merchantTypeMapper = merchantTypeMapper;
    }

    public List<Map<String, Object>> getMerchantTypes() {
        return merchantTypeMapper.findAllActive().stream().map(type -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", type.getId());
            item.put("code", type.getCode());
            item.put("name", type.getName());
            item.put("description", type.getDescription());
            return item;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getMerchants(Long typeId, Integer page, Integer size) {
        List<Merchant> merchants = typeId != null ? merchantMapper.findByTypeId(typeId) : merchantMapper.findAllActive();
        Map<String, Object> result = new HashMap<>();
        result.put("list", merchants);
        result.put("page", page);
        result.put("size", size);
        result.put("total", merchants.size());
        return result;
    }

    public Map<String, Object> getAdminMerchantList(Integer page, Integer size, Integer status, String keyword) {
        String keywordLower = toLower(keyword);
        List<Merchant> filtered = findAllMerchantsForAdmin().stream()
                .filter(m -> status == null || Objects.equals(m.getStatus(), status))
                .filter(m -> isBlank(keyword)
                        || containsIgnoreCase(m.getName(), keywordLower)
                        || containsIgnoreCase(m.getContactName(), keywordLower)
                        || containsIgnoreCase(m.getContactPhone(), keywordLower))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", paginate(filtered, page, size));
        result.put("page", normalizePage(page));
        result.put("size", normalizeSize(size));
        result.put("total", filtered.size());
        return result;
    }

    public List<Merchant> findAllMerchantsForAdmin() {
        List<Merchant> merchants = merchantMapper.findAll();
        return merchants == null ? List.of() : merchants;
    }

    public List<MerchantType> findActiveMerchantTypesForAdmin() {
        List<MerchantType> types = merchantTypeMapper.findAllActive();
        return types == null ? List.of() : types;
    }

    public long countMerchants() {
        return merchantMapper.countAllForAdmin();
    }

    public long countMerchantsByStatus(Integer status) {
        return merchantMapper.countByStatusForAdmin(status);
    }

    public long countPendingMerchantAuth() {
        return merchantMapper.countByStatusForAdmin(0);
    }

    public long countMerchantsSince(java.time.LocalDateTime since) {
        return merchantMapper.countMerchantsSince(since);
    }

    public List<Map<String, Object>> countMerchantTypeData() {
        List<Map<String, Object>> rows = merchantMapper.countByTypeForAdmin();
        return rows == null ? List.of() : rows;
    }

    public Merchant getMerchantById(Long id) {
        return merchantMapper.findById(id);
    }

    public Merchant requireActiveMerchantForLocation(Long id) {
        Merchant merchant = merchantMapper.findById(id);
        if (merchant == null) {
            throw new MerchantNotFoundException("商家不存在");
        }
        if (merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "商家状态不可更新定位");
        }
        return merchant;
    }

    public static class MerchantNotFoundException extends BusinessException {
        public MerchantNotFoundException(String message) {
            super(BusinessErrorCode.MERCHANT_NOT_FOUND, message);
        }
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
}
