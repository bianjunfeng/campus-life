package com.campus.campus_life_backend.modules.order.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 浼樻儬鍒歌鍗曞疄浣撶被
 */
@Data
public class VoucherOrder {

    private Long id;

    private Long userId;

    private Long voucherId;

    private Integer status;  // 0-寰呮敮浠?1-寰呬娇鐢?2-宸蹭娇鐢?3-宸茶繃鏈?4-宸查€€娆?5-宸插彇娑?

    private String orderNo;

    private BigDecimal payAmount;
    
    /**
     * 鏀粯鏂瑰紡锛歛lipay(鏀粯瀹?銆亀echat(寰俊鏀粯)
     */
    private String paymentMethod;

    private String paymentIdempotencyKey;
    
    /**
     * 鏀粯鐘舵€侊細0-寰呮敮浠?1-鏀粯鎴愬姛;2-鏀粯澶辫触;3-宸查€€娆?
     */
    private Integer paymentStatus;
    
    /**
     * 鏀粯鏃堕棿
     */
    private LocalDateTime payTime;

    private String orderSource;

    private LocalDateTime payDeadline;

    private LocalDateTime useDeadline;

    private String cancelReason;

    private LocalDateTime createTime;

    private LocalDateTime useTime;

    private LocalDateTime updateTime;
}

