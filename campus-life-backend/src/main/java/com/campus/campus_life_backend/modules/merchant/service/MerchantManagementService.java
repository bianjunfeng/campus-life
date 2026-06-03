package com.campus.campus_life_backend.modules.merchant.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.entity.MerchantType;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class MerchantManagementService {

    private final MerchantMapper merchantMapper;
    private final MerchantTypeMapper merchantTypeMapper;

    public record AdminMerchantCreateResult(Long id, String name, Integer status) {
    }

    public record AdminMerchantUpdateResult(Integer oldStatus, Integer newStatus) {
    }

    public MerchantManagementService(MerchantMapper merchantMapper, MerchantTypeMapper merchantTypeMapper) {
        this.merchantMapper = merchantMapper;
        this.merchantTypeMapper = merchantTypeMapper;
    }

    @Transactional
    public AdminMerchantCreateResult adminCreateMerchant(Map<String, Object> request) {
        String name = trimString(request == null ? null : request.get("name"));
        String contactName = trimString(request == null ? null : request.get("contactName"));
        String contactPhone = trimString(request == null ? null : request.get("contactPhone"));
        String address = trimString(request == null ? null : request.get("address"));
        Long userId = parseLong(request == null ? null : request.get("userId"), null);
        Long typeId = parseLong(request == null ? null : request.get("typeId"), null);
        Integer status = parseInteger(request == null ? null : request.get("status"), 1);

        if (isBlank(name)) {
            throw new BusinessException(BusinessErrorCode.MERCHANT_NAME_REQUIRED);
        }
        if (isBlank(contactPhone)) {
            throw new BusinessException(BusinessErrorCode.MERCHANT_PHONE_REQUIRED);
        }
        typeId = resolveTypeId(typeId);

        Merchant merchant = new Merchant();
        merchant.setUserId(userId);
        merchant.setName(name);
        merchant.setContactName(isBlank(contactName) ? name : contactName);
        merchant.setContactPhone(contactPhone);
        merchant.setAddress(address);
        merchant.setTypeId(typeId);
        merchant.setStatus(status == null ? 1 : status);
        merchantMapper.insertMerchant(merchant);

        return new AdminMerchantCreateResult(merchant.getId(), merchant.getName(), merchant.getStatus());
    }

    @Transactional
    public AdminMerchantUpdateResult adminUpdateMerchant(Long merchantId, Map<String, Object> request) {
        Merchant merchant = merchantMapper.findById(merchantId);
        if (merchant == null) {
            throw new BusinessException(BusinessErrorCode.MERCHANT_NOT_FOUND);
        }

        Integer oldStatus = merchant.getStatus();
        String name = trimString(request == null ? null : request.get("name"));
        String contactName = trimString(request == null ? null : request.get("contactName"));
        String contactPhone = trimString(request == null ? null : request.get("contactPhone"));
        String address = trimString(request == null ? null : request.get("address"));
        Long typeId = parseLong(request == null ? null : request.get("typeId"), null);
        Integer status = parseInteger(request == null ? null : request.get("status"), merchant.getStatus());

        if (!isBlank(name)) {
            merchant.setName(name);
        }
        if (!isBlank(contactName)) {
            merchant.setContactName(contactName);
        }
        if (!isBlank(contactPhone)) {
            merchant.setContactPhone(contactPhone);
        }
        if (!isBlank(address)) {
            merchant.setAddress(address);
        }
        if (typeId != null) {
            if (merchantTypeMapper.findById(typeId) == null) {
                throw new BusinessException(BusinessErrorCode.MERCHANT_CATEGORY_NOT_FOUND);
            }
            merchant.setTypeId(typeId);
        }
        if (status != null) {
            merchant.setStatus(status);
        }

        merchantMapper.updateMerchant(merchant);
        if (!Objects.equals(oldStatus, merchant.getStatus())) {
            merchantMapper.updateStatus(merchantId, merchant.getStatus());
        }

        return new AdminMerchantUpdateResult(oldStatus, merchant.getStatus());
    }

    @Transactional
    public AdminMerchantUpdateResult adminUpdateStatus(Long merchantId, Integer status) {
        Merchant merchant = merchantMapper.findById(merchantId);
        if (merchant == null) {
            throw new BusinessException(BusinessErrorCode.MERCHANT_NOT_FOUND);
        }
        Integer oldStatus = merchant.getStatus();
        merchantMapper.updateStatus(merchantId, status);
        return new AdminMerchantUpdateResult(oldStatus, status);
    }

    private Long resolveTypeId(Long typeId) {
        if (typeId == null) {
            List<MerchantType> typeList = merchantTypeMapper.findAllActive();
            if (typeList == null || typeList.isEmpty()) {
                throw new BusinessException(BusinessErrorCode.MERCHANT_CATEGORY_REQUIRED);
            }
            return typeList.get(0).getId();
        }
        if (merchantTypeMapper.findById(typeId) == null) {
            throw new BusinessException(BusinessErrorCode.MERCHANT_CATEGORY_NOT_FOUND);
        }
        return typeId;
    }

    private String trimString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
}
