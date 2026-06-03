CREATE TABLE IF NOT EXISTS shopping_cart (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    voucher_id BIGINT UNSIGNED NOT NULL COMMENT '优惠券ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    selected TINYINT NOT NULL DEFAULT 1 COMMENT '是否选中: 1是 0否',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_cart_user_voucher UNIQUE (user_id, voucher_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_voucher FOREIGN KEY (voucher_id) REFERENCES voucher(id) ON DELETE CASCADE
) COMMENT='购物车表' CHARSET=utf8mb4;

CREATE INDEX idx_cart_user_update_time ON shopping_cart(user_id, update_time);
