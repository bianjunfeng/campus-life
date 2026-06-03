create table if not exists merchant_type
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    code        varchar(50)                        not null comment '类型编码，如 drink, food, study',
    name        varchar(50)                        not null comment '类型名称，如 奶茶饮品、正餐餐饮',
    description varchar(255)                       null comment '类型说明',
    sort_order  int      default 0                 null comment '排序优先级，越大越靠前',
    status      tinyint  default 1                 null comment '状态：1-启用 0-禁用',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint code
        unique (code)
)
    comment '商家类型表' charset = utf8mb4;

create table if not exists post_category
(
    id          bigint unsigned auto_increment
        primary key,
    name        varchar(50)                        not null comment '分类名称，如：树洞、学习、二手、活动',
    code        varchar(50)                        not null comment '英文编码，便于前端路由或内部使用',
    description varchar(255)                       null comment '分类描述',
    sort_order  int      default 0                 not null comment '排序，越大越靠前',
    status      tinyint  default 1                 not null comment '1-启用;0-停用',
    create_time datetime default CURRENT_TIMESTAMP not null,
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_code
        unique (code)
)
    comment '帖子分类表' charset = utf8mb4;

create table if not exists user
(
    id                  bigint unsigned auto_increment comment '用户ID'
        primary key,
    phone               varchar(20)                        not null comment '手机号',
    email               varchar(100)                       null comment '邮箱',
    username            varchar(50)                        not null comment '昵称/用户名',
    password_hash       varchar(255)                       not null comment '密码哈希',
    salt                varchar(64)                        null comment '密码盐',
    role                tinyint  default 0                 not null comment '0-学生;1-商家;2-管理员',
    status              tinyint  default 1                 not null comment '0-禁用;1-正常',
    avatar_url          varchar(255)                       null comment '头像地址',
    bio                 varchar(255)                       null comment '个人简介',
    is_student_verified tinyint  default 0                 not null comment '学生认证状态：0-未认证;1-已认证',
    is_merchant         tinyint  default 0                 not null comment '是否为商家：0-否;1-是',
    create_time         datetime default CURRENT_TIMESTAMP not null comment '注册时间',
    update_time         datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_email
        unique (email),
    constraint uk_phone
        unique (phone)
)
    comment '用户表' charset = utf8mb4;

create table if not exists login_token
(
    id          bigint unsigned auto_increment
        primary key,
    user_id     bigint unsigned                    not null,
    token       varchar(128)                       not null comment '登录凭证',
    device      varchar(100)                       null comment '设备信息',
    ip          varchar(50)                        null comment '登录IP',
    status      tinyint  default 1                 not null comment '1-有效;0-已失效',
    expire_at   datetime                           not null comment '过期时间',
    create_time datetime default CURRENT_TIMESTAMP not null,
    constraint uk_token
        unique (token),
    constraint fk_login_token_user
        foreign key (user_id) references user (id)
)
    comment '登录 Token 表' charset = utf8mb4;

create index idx_user_id
    on login_token (user_id);

create table if not exists auth_session
(
    id                 bigint auto_increment
        primary key,
    session_id         varchar(64)                        not null,
    user_id            bigint                             not null,
    access_jti         varchar(64)                        null,
    refresh_jti        varchar(64)                        null,
    access_token_hash  varchar(128)                       null,
    refresh_token_hash varchar(128)                       null,
    device             varchar(255)                       null,
    ip                 varchar(64)                        null,
    user_agent         varchar(512)                       null,
    status             tinyint  default 1                 not null comment '1-在线/有效;0-已退出;2-强制下线;3-已过期',
    login_time         datetime                           not null,
    last_seen_time     datetime                           null,
    logout_time        datetime                           null,
    expire_at          datetime                           not null,
    create_time        datetime default CURRENT_TIMESTAMP not null,
    update_time        datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_auth_session_session_id
        unique (session_id)
)
    comment '认证会话表' charset = utf8mb4;

create index idx_auth_session_status_expire
    on auth_session (status, expire_at);

create index idx_auth_session_user_status_seen
    on auth_session (user_id, status, last_seen_time);

create table if not exists login_audit_log
(
    id             bigint auto_increment
        primary key,
    user_id        bigint                             null,
    account        varchar(100)                       null,
    role           tinyint                            null,
    login_type     varchar(32)                        not null comment 'password/code/wechat/qq',
    success        tinyint                            not null,
    failure_reason varchar(255)                       null,
    ip             varchar(64)                        null,
    user_agent     varchar(512)                       null,
    session_id     varchar(64)                        null,
    create_time    datetime default CURRENT_TIMESTAMP not null
)
    comment '登录审计日志表' charset = utf8mb4;

create index idx_login_audit_role_time
    on login_audit_log (role, create_time);

create index idx_login_audit_success_time
    on login_audit_log (success, create_time);

create index idx_login_audit_user_time
    on login_audit_log (user_id, create_time);

create table if not exists merchant
(
    id            bigint unsigned auto_increment
        primary key,
    user_id       bigint unsigned                    not null comment '关联用户ID（商家账号）',
    name          varchar(100)                       not null comment '商家名称',
    contact_name  varchar(50)                        null comment '联系人姓名',
    contact_phone varchar(20)                        null comment '联系电话',
    address       varchar(255)                       null comment '详细地址',
    business_hours varchar(80)                        null comment '营业时间',
    status        tinyint  default 0                 not null comment '0-待审核;1-正常;2-冻结;3-关闭',
    create_time   datetime default CURRENT_TIMESTAMP not null,
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    type_id       bigint                             null comment '商家类型ID，关联 merchant_type(id)',
    constraint uk_merchant_user
        unique (user_id),
    constraint fk_merchant_type
        foreign key (type_id) references merchant_type (id),
    constraint fk_merchant_user
        foreign key (user_id) references user (id)
)
    comment '商家表' charset = utf8mb4;

create table if not exists merchant_auth_request
(
    id            bigint unsigned auto_increment
        primary key,
    user_id       bigint unsigned                    not null comment '申请用户ID',
    merchant_name varchar(100)                       not null comment '商家名称',
    license_no    varchar(100)                       null comment '营业执照号/统一社会信用代码',
    license_img   varchar(255)                       null comment '证照图片URL',
    address       varchar(255)                       null comment '地址',
    status        tinyint  default 0                 not null comment '0-待审核;1-通过;2-驳回',
    reason        varchar(255)                       null comment '驳回原因',
    reviewer_id   bigint unsigned                    null comment '审核管理员ID',
    create_time   datetime default CURRENT_TIMESTAMP not null,
    review_time   datetime                           null,
    constraint fk_merchant_auth_user
        foreign key (user_id) references user (id)
)
    comment '商家认证申请表' charset = utf8mb4;

create index idx_user_id
    on merchant_auth_request (user_id);

create table if not exists message
(
    id              bigint unsigned auto_increment
        primary key,
    conversation_id varchar(64)                        not null comment '会话ID，可按小ID_大ID生成',
    from_user_id    bigint unsigned                    not null comment '发送者',
    to_user_id      bigint unsigned                    not null comment '接收者',
    content         text                               not null comment '内容',
    status          tinyint  default 0                 not null comment '0-未读;1-已读;2-撤回/删除',
    create_time     datetime default CURRENT_TIMESTAMP not null,
    constraint fk_message_from_user
        foreign key (from_user_id) references user (id),
    constraint fk_message_to_user
        foreign key (to_user_id) references user (id)
)
    comment '私信消息表' charset = utf8mb4;

create index idx_conversation_id
    on message (conversation_id);

create index idx_to_user_id
    on message (to_user_id);

create table if not exists post
(
    id                bigint unsigned auto_increment
        primary key,
    user_id           bigint unsigned                    not null comment '作者ID',
    category_id       bigint unsigned                    null comment '分类ID，允许为空表示未分类',
    title             varchar(255)                       null comment '标题，可为空（纯想法类）',
    content           text                               not null comment '正文内容',
    status            tinyint  default 0                 not null comment '0-正常;1-仅自己可见;2-已删除;3-屏蔽',
    is_pinned         tinyint  default 0                 not null comment '是否置顶',
    is_hot            tinyint  default 0                 not null comment '是否热门',
    like_count        int      default 0                 not null comment '点赞数',
    comment_count     int      default 0                 not null comment '评论数',
    favorite_count    int      default 0                 not null comment '收藏数',
    view_count        int      default 0                 not null comment '浏览次数',
    last_comment_time datetime                           null comment '最后评论时间',
    create_time       datetime default CURRENT_TIMESTAMP not null,
    update_time       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_post_category
        foreign key (category_id) references post_category (id),
    constraint fk_post_user
        foreign key (user_id) references user (id)
)
    comment '帖子表' charset = utf8mb4;

create table if not exists browse_history
(
    id              bigint unsigned auto_increment
        primary key,
    user_id         bigint unsigned                    not null comment '用户ID',
    post_id         bigint unsigned                    not null comment '帖子ID',
    first_view_time datetime default CURRENT_TIMESTAMP not null comment '首次浏览时间',
    last_view_time  datetime default CURRENT_TIMESTAMP not null comment '最近浏览时间',
    view_count      int      default 1                 not null comment '浏览次数',
    constraint uk_user_post
        unique (user_id, post_id),
    constraint fk_browse_history_post
        foreign key (post_id) references post (id)
            on delete cascade,
    constraint fk_browse_history_user
        foreign key (user_id) references user (id)
)
    comment '帖子浏览历史表' charset = utf8mb4;

create index idx_last_view_time
    on browse_history (last_view_time);

create table if not exists comment
(
    id               bigint unsigned auto_increment
        primary key,
    post_id          bigint unsigned                    not null comment '帖子ID',
    user_id          bigint unsigned                    not null comment '评论用户ID',
    parent_id        bigint unsigned                    null comment '父级评论ID，一级评论为空',
    reply_to_user_id bigint unsigned                    null comment '被回复的用户ID',
    content          text                               not null comment '评论内容',
    status           tinyint  default 0                 not null comment '0-正常;1-已删除;2-屏蔽',
    like_count       int      default 0                 not null comment '点赞数',
    create_time      datetime default CURRENT_TIMESTAMP not null,
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_comment_post
        foreign key (post_id) references post (id)
            on delete cascade,
    constraint fk_comment_user
        foreign key (user_id) references user (id)
)
    comment '评论表' charset = utf8mb4;

create index idx_parent_id
    on comment (parent_id);

create index idx_post_id
    on comment (post_id);

create index idx_user_id
    on comment (user_id);

create table if not exists comment_like
(
    id          bigint unsigned auto_increment
        primary key,
    comment_id  bigint unsigned                    not null,
    user_id     bigint unsigned                    not null,
    create_time datetime default CURRENT_TIMESTAMP not null,
    constraint uk_comment_user
        unique (comment_id, user_id),
    constraint fk_comment_like_comment
        foreign key (comment_id) references comment (id)
            on delete cascade,
    constraint fk_comment_like_user
        foreign key (user_id) references user (id)
)
    comment '评论点赞表' charset = utf8mb4;

create index idx_user_id
    on comment_like (user_id);

create index idx_category_id
    on post (category_id);

create index idx_create_time
    on post (create_time);

create index idx_hot
    on post (is_hot);

create index idx_status
    on post (status);

create index idx_user_id
    on post (user_id);

create table if not exists post_favorite
(
    id          bigint unsigned auto_increment
        primary key,
    post_id     bigint unsigned                    not null comment '帖子ID',
    user_id     bigint unsigned                    not null comment '用户ID',
    create_time datetime default CURRENT_TIMESTAMP not null,
    constraint uk_post_user
        unique (post_id, user_id),
    constraint fk_post_favorite_post
        foreign key (post_id) references post (id)
            on delete cascade,
    constraint fk_post_favorite_user
        foreign key (user_id) references user (id)
)
    comment '帖子收藏表' charset = utf8mb4;

create index idx_user_id
    on post_favorite (user_id);

create table if not exists post_image
(
    id          bigint unsigned auto_increment
        primary key,
    post_id     bigint unsigned                    not null comment '帖子ID',
    url         varchar(255)                       not null comment '图片地址',
    width       int                                null comment '原始宽度',
    height      int                                null comment '原始高度',
    sort_order  int      default 0                 not null comment '排序，越大越靠前/越后',
    create_time datetime default CURRENT_TIMESTAMP not null,
    constraint fk_post_image_post
        foreign key (post_id) references post (id)
            on delete cascade
)
    comment '帖子图片表' charset = utf8mb4;

create index idx_post_id
    on post_image (post_id);

create table if not exists post_like
(
    id          bigint unsigned auto_increment
        primary key,
    post_id     bigint unsigned                    not null,
    user_id     bigint unsigned                    not null,
    create_time datetime default CURRENT_TIMESTAMP not null,
    constraint uk_post_user
        unique (post_id, user_id),
    constraint fk_post_like_post
        foreign key (post_id) references post (id)
            on delete cascade,
    constraint fk_post_like_user
        foreign key (user_id) references user (id)
)
    comment '帖子点赞表' charset = utf8mb4;

create index idx_user_id
    on post_like (user_id);

create table if not exists report
(
    id            bigint unsigned auto_increment
        primary key,
    reporter_id   bigint unsigned                    not null comment '举报人ID',
    target_type   tinyint                            not null comment '1-帖子;2-评论;3-用户',
    target_id     bigint unsigned                    not null comment '目标ID',
    reason        varchar(255)                       not null comment '举报原因',
    status        tinyint  default 0                 not null comment '0-待处理;1-已处理;2-忽略',
    handler_id    bigint unsigned                    null comment '处理人（管理员）',
    handle_result varchar(255)                       null comment '处理结果说明',
    create_time   datetime default CURRENT_TIMESTAMP not null,
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_report_reporter
        foreign key (reporter_id) references user (id)
)
    comment '举报表' charset = utf8mb4;

create index idx_reporter_id
    on report (reporter_id);

create index idx_target
    on report (target_type, target_id);

create table if not exists student_auth_request
(
    id               bigint unsigned auto_increment
        primary key,
    user_id          bigint unsigned                    not null comment '申请人ID',
    real_name        varchar(50)                        not null,
    school           varchar(100)                       not null,
    college          varchar(100)                       null,
    major            varchar(100)                       null,
    grade            varchar(20)                        null,
    class_name       varchar(50)                        null,
    student_no       varchar(50)                        not null,
    student_card_img varchar(255)                       null comment '学生证照片URL',
    status           tinyint  default 0                 not null comment '0-待审核;1-通过;2-驳回',
    reason           varchar(255)                       null comment '驳回原因',
    reviewer_id      bigint unsigned                    null comment '审核管理员ID',
    create_time      datetime default CURRENT_TIMESTAMP not null,
    review_time      datetime                           null,
    constraint fk_student_auth_user
        foreign key (user_id) references user (id)
)
    comment '学生认证申请表' charset = utf8mb4;

create index idx_user_id
    on student_auth_request (user_id);

create table if not exists student_profile
(
    user_id     bigint unsigned                    not null comment '关联用户ID'
        primary key,
    real_name   varchar(50)                        not null comment '真实姓名',
    school      varchar(100)                       not null comment '学校',
    college     varchar(100)                       null comment '学院',
    major       varchar(100)                       null comment '专业',
    grade       varchar(20)                        null comment '年级，如2022',
    class_name  varchar(50)                        null comment '班级',
    student_no  varchar(50)                        not null comment '学号',
    status      tinyint  default 1                 not null comment '0-无效;1-有效',
    create_time datetime default CURRENT_TIMESTAMP not null,
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_student_no
        unique (school, student_no),
    constraint fk_student_profile_user
        foreign key (user_id) references user (id)
)
    comment '学生档案表' charset = utf8mb4;

create index idx_role
    on user (role);

create table if not exists user_follow
(
    id          bigint unsigned auto_increment
        primary key,
    follower_id bigint unsigned                    not null comment '关注发起者',
    followee_id bigint unsigned                    not null comment '被关注的人',
    create_time datetime default CURRENT_TIMESTAMP not null,
    constraint uk_follow_pair
        unique (follower_id, followee_id),
    constraint fk_user_follow_followee
        foreign key (followee_id) references user (id),
    constraint fk_user_follow_follower
        foreign key (follower_id) references user (id)
)
    comment '用户关注表' charset = utf8mb4;

create index idx_followee_id
    on user_follow (followee_id);

create table if not exists voucher
(
    id          bigint unsigned auto_increment
        primary key,
    merchant_id bigint unsigned                    not null comment '商家ID',
    title       varchar(100)                       not null comment '券标题',
    sub_title   varchar(255)                       null comment '副标题/描述',
    image_url   varchar(255)                       null comment '图片URL',
    stock       int      default 0                 not null comment '库存',
    sold_count  int      default 0                 not null comment '已售数量（支付成功）',
    amount      decimal(10, 2)                     not null comment '面值/优惠金额',
    pay_value   decimal(10, 2)                     null comment '需要支付的金额（如团购券）',
    status      tinyint  default 1                 not null comment '1-上架;0-下架',
    begin_time  datetime                           null comment '可使用起始时间',
    end_time    datetime                           null comment '可使用结束时间',
    create_time datetime default CURRENT_TIMESTAMP not null,
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_voucher_merchant
        foreign key (merchant_id) references merchant (id)
)
    comment '优惠券基础表' charset = utf8mb4;

create table if not exists seckill_voucher
(
    id          bigint unsigned auto_increment
        primary key,
    voucher_id  bigint unsigned                    not null comment '关联 voucher',
    stock       int      default 0                 not null comment '秒杀库存',
    start_time  datetime                           not null comment '秒杀开始时间',
    end_time    datetime                           not null comment '秒杀结束时间',
    create_time datetime default CURRENT_TIMESTAMP not null,
    constraint uk_voucher_id
        unique (voucher_id),
    constraint fk_seckill_voucher_voucher
        foreign key (voucher_id) references voucher (id)
            on delete cascade
)
    comment '秒杀券表' charset = utf8mb4;

create index idx_merchant_id
    on voucher (merchant_id);

create table if not exists voucher_order
(
    id          bigint unsigned auto_increment
        primary key,
    user_id     bigint unsigned                    not null comment '用户ID',
    voucher_id  bigint unsigned                    not null comment '券ID',
    status      tinyint  default 0                 not null comment '0-待支付;1-待使用;2-已使用;3-已过期;4-已退款;5-已取消',
    order_no    varchar(64)                        not null comment '订单号',
    pay_amount  decimal(10, 2)                     null comment '支付金额',
    payment_method varchar(20)                     null comment '支付方式：alipay/wechat',
    payment_idempotency_key varchar(80)            null comment '支付请求幂等键',
    payment_status tinyint default 0               not null comment '0-待支付;1-支付成功;2-支付失败;3-已退款',
    order_source varchar(20) default 'app'         not null comment '订单来源：app/web/admin/seckill',
    pay_deadline datetime                          null comment '支付截止时间',
    cancel_reason varchar(255)                     null comment '取消原因',
    pay_time    datetime                           null comment '支付时间',
    create_time datetime default CURRENT_TIMESTAMP not null,
    use_time    datetime                           null comment '使用时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_order_no
        unique (order_no),
    constraint fk_voucher_order_user
        foreign key (user_id) references user (id),
    constraint fk_voucher_order_voucher
        foreign key (voucher_id) references voucher (id)
)
    comment '优惠券订单表' charset = utf8mb4;

create index idx_user_id
    on voucher_order (user_id);

create index idx_voucher_id
    on voucher_order (voucher_id);

create unique index uk_voucher_order_payment_idempotency_key
    on voucher_order (payment_idempotency_key);

create index idx_user_status_time
    on voucher_order (user_id, status, create_time);

create table if not exists shopping_cart
(
    id          bigint unsigned auto_increment
        primary key,
    user_id     bigint unsigned                    not null comment '用户ID',
    voucher_id  bigint unsigned                    not null comment '优惠券ID',
    quantity    int      default 1                 not null comment '购买数量',
    selected    tinyint  default 1                 not null comment '是否选中:1是;0否',
    create_time datetime default CURRENT_TIMESTAMP not null,
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_cart_user_voucher
        unique (user_id, voucher_id),
    constraint fk_cart_user
        foreign key (user_id) references user (id)
            on delete cascade,
    constraint fk_cart_voucher
        foreign key (voucher_id) references voucher (id)
            on delete cascade
)
    comment '购物车表' charset = utf8mb4;

create index idx_cart_user_update_time
    on shopping_cart (user_id, update_time);

create table if not exists user_wallet
(
    user_id       bigint unsigned                    not null comment '用户ID'
        primary key,
    balance       decimal(12, 2) default 1000.00    not null comment '可用余额',
    frozen_amount decimal(12, 2) default 0.00       not null comment '冻结金额',
    total_recharge decimal(12, 2) default 1000.00   not null comment '累计入账',
    total_spent   decimal(12, 2) default 0.00       not null comment '累计消费',
    status        tinyint  default 1                 not null comment '1-正常;0-冻结',
    create_time   datetime default CURRENT_TIMESTAMP not null,
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_user_wallet_user
        foreign key (user_id) references user (id)
            on delete cascade
)
    comment '用户钱包表' charset = utf8mb4;

create table if not exists wallet_transaction
(
    id            bigint unsigned auto_increment
        primary key,
    user_id       bigint unsigned                    not null comment '用户ID',
    biz_type      tinyint                            not null comment '1-充值;2-消费;3-退款;4-调整',
    amount        decimal(12, 2)                     not null comment '变动金额',
    balance_after decimal(12, 2)                     not null comment '变动后余额',
    order_no      varchar(64)                        null comment '关联订单号',
    remark        varchar(255)                       null comment '备注',
    create_time   datetime default CURRENT_TIMESTAMP not null,
    constraint fk_wallet_transaction_user
        foreign key (user_id) references user (id)
            on delete cascade
)
    comment '钱包交易流水' charset = utf8mb4;

create index idx_wallet_tx_order_no
    on wallet_transaction (order_no);

create index idx_wallet_tx_user_time
    on wallet_transaction (user_id, create_time);

create table if not exists admin_operation_log
(
    id              bigint unsigned auto_increment
        primary key,
    admin_id        bigint unsigned                    not null comment '操作管理员ID',
    operation_type  varchar(50)                        not null comment '操作类型',
    target_type     varchar(50)                        not null comment '目标类型',
    target_id       bigint unsigned                    not null comment '目标ID',
    action          varchar(50)                        not null comment '操作动作',
    old_status      tinyint                            null comment '操作前状态',
    new_status      tinyint                            null comment '操作后状态',
    reason          varchar(500)                       null comment '操作原因/备注',
    ip_address      varchar(50)                        null comment '操作IP地址',
    create_time     datetime default CURRENT_TIMESTAMP not null,
    constraint fk_admin_operation_log_admin
        foreign key (admin_id) references user (id)
            on delete cascade
)
    comment '管理员操作日志表' charset = utf8mb4;

create index idx_admin_operation_log_admin_id
    on admin_operation_log (admin_id);

create index idx_admin_operation_log_target
    on admin_operation_log (target_type, target_id);

create index idx_admin_operation_log_operation_type
    on admin_operation_log (operation_type);

create index idx_admin_operation_log_create_time
    on admin_operation_log (create_time);
