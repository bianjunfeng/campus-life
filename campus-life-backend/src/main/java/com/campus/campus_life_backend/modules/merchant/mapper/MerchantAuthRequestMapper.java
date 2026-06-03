package com.campus.campus_life_backend.modules.merchant.mapper;

import com.campus.campus_life_backend.modules.merchant.entity.MerchantAuthRequest;
import org.apache.ibatis.annotations.Param;

/**
 * 鍟嗗璁よ瘉鐢宠Mapper
 */
public interface MerchantAuthRequestMapper {

    /**
     * 鏍规嵁鐢ㄦ埛ID鏌ヨ鏈€鏂扮殑璁よ瘉鐢宠
     */
    MerchantAuthRequest findByUserId(@Param("userId") Long userId);

    /**
     * 鎻掑叆璁よ瘉鐢宠
     */
    int insert(MerchantAuthRequest request);

    /**
     * 鏇存柊璁よ瘉鐢宠
     */
    int update(MerchantAuthRequest request);
}



