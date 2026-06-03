ALTER TABLE `post`
    ADD INDEX `idx_post_status_create_time` (`status`, `create_time` DESC),
    ADD INDEX `idx_post_category_status_pin_time` (`category_id`, `status`, `is_pinned`, `create_time` DESC),
    ADD INDEX `idx_post_user_status_time` (`user_id`, `status`, `create_time` DESC);

ALTER TABLE `post_image`
    ADD INDEX `idx_post_image_post_sort_id` (`post_id`, `sort_order` DESC, `id`);

ALTER TABLE `comment`
    ADD INDEX `idx_comment_post_status_time` (`post_id`, `status`, `create_time`);

ALTER TABLE `post_favorite`
    ADD INDEX `idx_post_favorite_user_time_post` (`user_id`, `create_time` DESC, `post_id`);

ALTER TABLE `message`
    ADD INDEX `idx_message_conversation_status_time` (`conversation_id`, `status`, `create_time`, `id`),
    ADD INDEX `idx_message_to_status_conversation` (`to_user_id`, `status`, `conversation_id`, `create_time`),
    ADD INDEX `idx_message_from_status_conversation` (`from_user_id`, `status`, `conversation_id`, `create_time`);

ALTER TABLE `user_follow`
    ADD INDEX `idx_user_follow_follower_time` (`follower_id`, `create_time` DESC, `followee_id`),
    ADD INDEX `idx_user_follow_followee_time` (`followee_id`, `create_time` DESC, `follower_id`);

ALTER TABLE `voucher_order`
    ADD INDEX `idx_voucher_order_status_pay_deadline` (`status`, `pay_deadline`),
    ADD INDEX `idx_voucher_order_status_payment_use_deadline` (`status`, `payment_status`, `use_deadline`);

ALTER TABLE `user`
    ADD INDEX `idx_user_status_create_time` (`status`, `create_time` DESC);
