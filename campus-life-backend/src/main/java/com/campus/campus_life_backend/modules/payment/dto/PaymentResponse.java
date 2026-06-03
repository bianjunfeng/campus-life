package com.campus.campus_life_backend.modules.payment.dto;

import lombok.Data;

/**
 * 鏀粯鍝嶅簲 DTO
 */
@Data
public class PaymentResponse {
    /**
     * 鏀粯鏂瑰紡
     */
    private String paymentMethod;
    
    /**
     * 鏀粯璁㈠崟鍙?
     */
    private String paymentOrderNo;
    
    /**
     * 鏀粯URL锛堢敤浜庤烦杞埌鏀粯椤甸潰锛?
     */
    private String payUrl;
    
    /**
     * 鏀粯浜岀淮鐮侊紙鐢ㄤ簬鎵爜鏀粯锛?
     */
    private String qrCode;
    
    /**
     * 鏀粯琛ㄥ崟锛堢敤浜嶱C绔敮浠橈級
     */
    private String payForm;
}



