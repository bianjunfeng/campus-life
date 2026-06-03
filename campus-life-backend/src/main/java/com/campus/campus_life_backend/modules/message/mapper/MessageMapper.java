package com.campus.campus_life_backend.modules.message.mapper;

import com.campus.campus_life_backend.modules.message.entity.Message;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface MessageMapper {

    /**
     * 鎻掑叆娑堟伅
     */
    int insertMessage(Message message);

    /**
     * 鏍规嵁ID鏌ヨ娑堟伅
     */
    Message findById(@Param("id") Long id);

    /**
     * 鑾峰彇浼氳瘽鐨勬秷鎭垪琛?
     */
    List<Message> findByConversationId(@Param("conversationId") String conversationId, 
                                       @Param("offset") Integer offset, 
                                       @Param("limit") Integer limit);

    /**
     * 鑾峰彇鐢ㄦ埛鐨勬墍鏈変細璇濆垪琛紙姣忎釜浼氳瘽杩斿洖鏈€鏂颁竴鏉℃秷鎭級
     */
    List<Message> findConversationsByUserId(@Param("userId") Long userId);

    /**
     * 鑾峰彇涓や釜鐢ㄦ埛涔嬮棿鐨勪細璇滻D
     */
    String findConversationId(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 缁熻浼氳瘽涓殑娑堟伅鏁伴噺
     */
    int countByConversationId(@Param("conversationId") String conversationId);

    /**
     * 缁熻鐢ㄦ埛鏈娑堟伅鏁伴噺
     */
    int countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 缁熻浼氳瘽涓湭璇绘秷鎭暟閲?
     */
    int countUnreadByConversationId(@Param("conversationId") String conversationId, @Param("userId") Long userId);

    List<Map<String, Object>> countUnreadByConversationIds(
            @Param("userId") Long userId,
            @Param("conversationIds") List<String> conversationIds
    );

    /**
     * 鏍囪娑堟伅涓哄凡璇?
     */
    int markAsRead(@Param("conversationId") String conversationId, @Param("userId") Long userId);

    /**
     * 鍒犻櫎娑堟伅锛堣蒋鍒犻櫎锛?
     */
    int deleteMessage(@Param("id") Long id);

    /**
     * 检查会话是否属于用户
     */
    int existsConversationForUser(@Param("conversationId") String conversationId, @Param("userId") Long userId);
}


