-- Initial schema for a fresh NikyFarm database.
-- Derived from the supplied MySQL 8.0.46 structure-only exports.
-- Intentionally excludes DROP TABLE statements and existing AUTO_INCREMENT counters.
-- Do not run this migration manually against an existing schema; baseline existing DBs
-- at version 1 only after a backup and a schema comparison (see FLYWAY_MIGRATION_GUIDE.md).

-- Table: categories
CREATE TABLE `categories` (
                              `is_active` bit(1) DEFAULT NULL,
                              `sort_order` int DEFAULT NULL,
                              `created_at` datetime(6) NOT NULL,
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `parent_id` bigint DEFAULT NULL,
                              `updated_at` datetime(6) NOT NULL,
                              `name` varchar(150) NOT NULL,
                              `slug` varchar(160) NOT NULL,
                              `description` varchar(500) DEFAULT NULL,
                              `image_public_id` varchar(255) DEFAULT NULL,
                              `image_url` varchar(255) DEFAULT NULL,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `UKoul14ho7bctbefv8jywp5v3i2` (`slug`),
                              KEY `idx_categories_parent` (`parent_id`),
                              CONSTRAINT `FKsaok720gsu4u2wrgbk10b5n8d` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: users
CREATE TABLE `users` (
                         `is_active` bit(1) DEFAULT NULL,
                         `created_at` datetime(6) NOT NULL,
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `updated_at` datetime(6) NOT NULL,
                         `email` varchar(255) DEFAULT NULL,
                         `name` varchar(255) DEFAULT NULL,
                         `password_hash` varchar(255) DEFAULT NULL,
                         `role` enum('ADMIN','CUSTOMER') DEFAULT NULL,
                         `google_id` varchar(255) DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `UKovh8xmu9ac27t18m56gri58i1` (`google_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: coupons
CREATE TABLE `coupons` (
                           `discount_value` decimal(10,2) NOT NULL,
                           `is_active` bit(1) NOT NULL,
                           `max_uses` int DEFAULT NULL,
                           `min_order_value` decimal(10,2) DEFAULT NULL,
                           `times_used` int NOT NULL,
                           `id` bigint NOT NULL AUTO_INCREMENT,
                           `valid_from` datetime(6) DEFAULT NULL,
                           `valid_until` datetime(6) DEFAULT NULL,
                           `code` varchar(50) NOT NULL,
                           `description` varchar(255) DEFAULT NULL,
                           `discount_type` enum('FLAT','PERCENTAGE') NOT NULL,
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `UKeplt0kkm9yf2of2lnx6c1oy9b` (`code`),
                           KEY `idx_coupons_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: products
CREATE TABLE `products` (
                            `avg_rating` float DEFAULT NULL,
                            `base_price` decimal(19,2) NOT NULL,
                            `discounted_price` decimal(19,2) DEFAULT NULL,
                            `is_active` bit(1) NOT NULL,
                            `review_count` int NOT NULL,
                            `category_id` bigint NOT NULL,
                            `created_at` datetime(6) NOT NULL,
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `updated_at` datetime(6) NOT NULL,
                            `slug` varchar(160) NOT NULL,
                            `name` varchar(200) NOT NULL,
                            `short_description` varchar(500) DEFAULT NULL,
                            `description` text,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `UKostq1ec3toafnjok09y9l7dox` (`slug`),
                            KEY `idx_products_category` (`category_id`),
                            KEY `idx_products_active` (`is_active`),
                            CONSTRAINT `FKog2rp4qthbtt2lfyhfo32lsw9` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: attributes
CREATE TABLE `attributes` (
                              `sort_order` int DEFAULT NULL,
                              `category_id` bigint DEFAULT NULL,
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `name` varchar(255) DEFAULT NULL,
                              `unit` varchar(255) DEFAULT NULL,
                              `input_type` enum('BOOLEAN','NUMBER','SELECT','TEXT') DEFAULT NULL,
                              PRIMARY KEY (`id`),
                              KEY `FK8kxsgf8yatov9ub1i7mycsn6y` (`category_id`),
                              CONSTRAINT `FK8kxsgf8yatov9ub1i7mycsn6y` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: attribute_options
CREATE TABLE `attribute_options` (
                                     `attribute_id` bigint DEFAULT NULL,
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `value` varchar(255) DEFAULT NULL,
                                     PRIMARY KEY (`id`),
                                     KEY `FKo2hpoh278o091gr6uvd9l8iyb` (`attribute_id`),
                                     CONSTRAINT `FKo2hpoh278o091gr6uvd9l8iyb` FOREIGN KEY (`attribute_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: product_variants
CREATE TABLE `product_variants` (
                                    `discounted_price` decimal(19,2) DEFAULT NULL,
                                    `is_active` bit(1) NOT NULL,
                                    `is_default` bit(1) NOT NULL,
                                    `price` decimal(19,2) NOT NULL,
                                    `stock_quantity` int NOT NULL,
                                    `id` bigint NOT NULL AUTO_INCREMENT,
                                    `product_id` bigint NOT NULL,
                                    `sku` varchar(80) NOT NULL,
                                    `variant_label` varchar(150) NOT NULL,
                                    `colour` varchar(255) DEFAULT NULL,
                                    `size` varchar(255) DEFAULT NULL,
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `UKq935p2d1pbjm39n0063ghnfgn` (`sku`),
                                    KEY `idx_variants_product` (`product_id`),
                                    CONSTRAINT `FKosqitn4s405cynmhb87lkvuau` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: addresses
CREATE TABLE `addresses` (
                             `is_default` bit(1) DEFAULT NULL,
                             `created_at` datetime(6) NOT NULL,
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `user_id` bigint DEFAULT NULL,
                             `city` varchar(255) DEFAULT NULL,
                             `full_name` varchar(255) DEFAULT NULL,
                             `line_1` varchar(255) DEFAULT NULL,
                             `line_2` varchar(255) DEFAULT NULL,
                             `phone` varchar(255) DEFAULT NULL,
                             `postal_code` varchar(255) DEFAULT NULL,
                             `state` varchar(255) DEFAULT NULL,
                             PRIMARY KEY (`id`),
                             KEY `idx_addresses_user` (`user_id`),
                             CONSTRAINT `FK1fa36y2oqhao3wgg2rw1pi459` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: carts
CREATE TABLE `carts` (
                         `created_at` datetime(6) NOT NULL,
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `updated_at` datetime(6) NOT NULL,
                         `user_id` bigint NOT NULL,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `UK64t7ox312pqal3p7fg9o503c2` (`user_id`),
                         CONSTRAINT `FKb5o626f86h46m4s7ms6ginnop` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: product_attribute_value_seq
CREATE TABLE `product_attribute_value_seq` (
                                               `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Hibernate's table-backed sequence needs an initial row or the first insert fails.
INSERT INTO `product_attribute_value_seq` (`next_val`) VALUES (1);

-- Table: product_attribute_value
CREATE TABLE `product_attribute_value` (
                                           `attribute_id` bigint DEFAULT NULL,
                                           `attribute_options_id` bigint DEFAULT NULL,
                                           `id` bigint NOT NULL,
                                           `product_id` bigint DEFAULT NULL,
                                           `value_text` varchar(255) DEFAULT NULL,
                                           PRIMARY KEY (`id`),
                                           KEY `idx_pav_product` (`product_id`),
                                           KEY `FK3accdpkc5cmlk7nionpyg2wpx` (`attribute_id`),
                                           KEY `FKpu2y73923arneet189nyglhlo` (`attribute_options_id`),
                                           CONSTRAINT `FK3accdpkc5cmlk7nionpyg2wpx` FOREIGN KEY (`attribute_id`) REFERENCES `attributes` (`id`),
                                           CONSTRAINT `FK8xow8ax3k0w2by7efnlutws1a` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
                                           CONSTRAINT `FKpu2y73923arneet189nyglhlo` FOREIGN KEY (`attribute_options_id`) REFERENCES `attribute_options` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: product_images
CREATE TABLE `product_images` (
                                  `is_primary` bit(1) DEFAULT NULL,
                                  `sort_order` int DEFAULT NULL,
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `product_id` bigint NOT NULL,
                                  `product_variant_id` bigint DEFAULT NULL,
                                  `alt_text` varchar(255) DEFAULT NULL,
                                  `image_url` text NOT NULL,
                                  `public_id` varchar(255) DEFAULT NULL,
                                  PRIMARY KEY (`id`),
                                  KEY `idx_product_images_product` (`product_id`),
                                  KEY `idx_product_images_variant` (`product_variant_id`),
                                  CONSTRAINT `FK7duksvvesmxhelrq7ugqkaq8b` FOREIGN KEY (`product_variant_id`) REFERENCES `product_variants` (`id`),
                                  CONSTRAINT `FKqnq71xsohugpqwf3c9gxmsuy` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: cart_items
CREATE TABLE `cart_items` (
                              `quantity` int NOT NULL,
                              `added_at` datetime(6) NOT NULL,
                              `cart_id` bigint NOT NULL,
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `product_variant_id` bigint NOT NULL,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_cart_item_cart_variant` (`cart_id`,`product_variant_id`),
                              KEY `FKn1s4l7h0vm4o259wpu7ft0y2y` (`product_variant_id`),
                              CONSTRAINT `FKn1s4l7h0vm4o259wpu7ft0y2y` FOREIGN KEY (`product_variant_id`) REFERENCES `product_variants` (`id`),
                              CONSTRAINT `FKpcttvuq4mxppo8sxggjtn5i2c` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: wishlist_items
CREATE TABLE `wishlist_items` (
                                  `created_at` datetime(6) NOT NULL,
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `product_id` bigint DEFAULT NULL,
                                  `user_id` bigint DEFAULT NULL,
                                  PRIMARY KEY (`id`),
                                  KEY `FKqxj7lncd242b59fb78rqegyxj` (`product_id`),
                                  KEY `FKmmj2k1i459yu449k3h1vx5abp` (`user_id`),
                                  CONSTRAINT `FKmmj2k1i459yu449k3h1vx5abp` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
                                  CONSTRAINT `FKqxj7lncd242b59fb78rqegyxj` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: review_images
CREATE TABLE `review_images` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `image_url` varchar(255) DEFAULT NULL,
                                 `public_id` varchar(255) DEFAULT NULL,
                                 `product_id` bigint DEFAULT NULL,
                                 `user_id` bigint DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 KEY `FKbfppg7or4lpoehvmi8ppqf3cq` (`product_id`),
                                 KEY `FKbwln3sc1pdq39p6n0u7uo1mg9` (`user_id`),
                                 CONSTRAINT `FKbfppg7or4lpoehvmi8ppqf3cq` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
                                 CONSTRAINT `FKbwln3sc1pdq39p6n0u7uo1mg9` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: reviews
CREATE TABLE `reviews` (
                           `is_verified_purchase` bit(1) DEFAULT NULL,
                           `rating` smallint NOT NULL,
                           `created_at` datetime(6) NOT NULL,
                           `id` bigint NOT NULL AUTO_INCREMENT,
                           `product_id` bigint DEFAULT NULL,
                           `review_image_id` bigint DEFAULT NULL,
                           `updated_at` datetime(6) NOT NULL,
                           `user_id` bigint DEFAULT NULL,
                           `comment` varchar(255) DEFAULT NULL,
                           `title` varchar(255) DEFAULT NULL,
                           PRIMARY KEY (`id`),
                           KEY `idx_reviews_product` (`product_id`),
                           KEY `FKddehx801im6e6kl6tty40dnv5` (`review_image_id`),
                           KEY `FKcgy7qjc1r99dp117y9en6lxye` (`user_id`),
                           CONSTRAINT `FKcgy7qjc1r99dp117y9en6lxye` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
                           CONSTRAINT `FKddehx801im6e6kl6tty40dnv5` FOREIGN KEY (`review_image_id`) REFERENCES `review_images` (`id`),
                           CONSTRAINT `FKpl51cejpw4gy5swfar8br9ngi` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: review_media
CREATE TABLE `review_media` (
                                `id` bigint NOT NULL AUTO_INCREMENT,
                                `media_type` enum('IMAGE','VIDEO') NOT NULL,
                                `media_url` text NOT NULL,
                                `public_id` varchar(500) NOT NULL,
                                `sort_order` int NOT NULL,
                                `thumbnail_url` text,
                                `review_id` bigint NOT NULL,
                                PRIMARY KEY (`id`),
                                KEY `idx_review_media_review` (`review_id`),
                                KEY `idx_review_media_type` (`media_type`),
                                KEY `idx_review_media_sort` (`review_id`,`sort_order`),
                                CONSTRAINT `FK5eeii8rke42oss6yq63k2hrte` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: orders
CREATE TABLE `orders` (
                          `discount` decimal(10,2) NOT NULL,
                          `shipping_fee` decimal(10,2) NOT NULL,
                          `subtotal` decimal(10,2) NOT NULL,
                          `total_amount` decimal(10,2) NOT NULL,
                          `billing_address_id` bigint DEFAULT NULL,
                          `coupon_id` bigint DEFAULT NULL,
                          `created_at` datetime(6) NOT NULL,
                          `id` bigint NOT NULL AUTO_INCREMENT,
                          `shipping_address_id` bigint DEFAULT NULL,
                          `updated_at` datetime(6) DEFAULT NULL,
                          `user_id` bigint NOT NULL,
                          `order_number` varchar(64) NOT NULL,
                          `payment_status` enum('FAILED','PAID','REFUNDED','REFUND_PENDING','UNPAID') NOT NULL,
                          `status` enum('CANCELLED','DELIVERED','PENDING','PROCESSING','SHIPPED') NOT NULL,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `UKnthkiu7pgmnqnu86i2jyoe2v7` (`order_number`),
                          KEY `idx_orders_user` (`user_id`),
                          KEY `idx_orders_status` (`status`),
                          KEY `FK66jolu65brloux12yi37qy3ky` (`billing_address_id`),
                          KEY `FKn1d1gkxckw648m2n2d5gx0yx5` (`coupon_id`),
                          KEY `FKmk6q95x8ffidq82wlqjaq7sqc` (`shipping_address_id`),
                          CONSTRAINT `FK32ql8ubntj5uh44ph9659tiih` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
                          CONSTRAINT `FK66jolu65brloux12yi37qy3ky` FOREIGN KEY (`billing_address_id`) REFERENCES `addresses` (`id`),
                          CONSTRAINT `FKmk6q95x8ffidq82wlqjaq7sqc` FOREIGN KEY (`shipping_address_id`) REFERENCES `addresses` (`id`),
                          CONSTRAINT `FKn1d1gkxckw648m2n2d5gx0yx5` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: order_items
CREATE TABLE `order_items` (
                               `line_total` decimal(10,2) NOT NULL,
                               `quantity` int NOT NULL,
                               `unit_price` decimal(10,2) NOT NULL,
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `order_id` bigint NOT NULL,
                               `product_id` bigint DEFAULT NULL,
                               `product_variant_id` bigint DEFAULT NULL,
                               `product_name_snapshot` varchar(255) NOT NULL,
                               `variant_label_snpashot` varchar(255) DEFAULT NULL,
                               PRIMARY KEY (`id`),
                               KEY `idx_order_items_order` (`order_id`),
                               KEY `FKocimc7dtr037rh4ls4l95nlfi` (`product_id`),
                               KEY `FKltmtlue0wixrg1cf0xo7x0l4d` (`product_variant_id`),
                               CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
                               CONSTRAINT `FKltmtlue0wixrg1cf0xo7x0l4d` FOREIGN KEY (`product_variant_id`) REFERENCES `product_variants` (`id`),
                               CONSTRAINT `FKocimc7dtr037rh4ls4l95nlfi` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: payments
CREATE TABLE `payments` (
                            `amount` decimal(19,2) NOT NULL,
                            `refund_amount` decimal(19,2) DEFAULT NULL,
                            `created_at` datetime(6) NOT NULL,
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `order_id` bigint NOT NULL,
                            `paid_at` datetime(6) DEFAULT NULL,
                            `refunded_at` datetime(6) DEFAULT NULL,
                            `updated_at` datetime(6) DEFAULT NULL,
                            `failure_reason` varchar(1000) DEFAULT NULL,
                            `currency` varchar(255) NOT NULL,
                            `failure_code` varchar(255) DEFAULT NULL,
                            `payment_method` varchar(255) DEFAULT NULL,
                            `provider` varchar(255) NOT NULL,
                            `provider_order_id` varchar(255) DEFAULT NULL,
                            `provider_payment_id` varchar(255) DEFAULT NULL,
                            `provider_refund_id` varchar(255) DEFAULT NULL,
                            `status` enum('CREATED','PENDING','SUCCESS','FAILED','CAPTURED','REFUND_PENDING','REFUNDED') NOT NULL,
                            PRIMARY KEY (`id`),
                            KEY `idx_payments_order` (`order_id`),
                            KEY `idx_payments_provider_order` (`provider_order_id`),
                            KEY `idx_payments_provider_payment` (`provider_payment_id`),
                            CONSTRAINT `FK81gagumt0r8y3rmudcgpbk42l` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: spring_session
CREATE TABLE `spring_session` (
                                  `PRIMARY_ID` char(36) NOT NULL,
                                  `SESSION_ID` char(36) NOT NULL,
                                  `CREATION_TIME` bigint NOT NULL,
                                  `LAST_ACCESS_TIME` bigint NOT NULL,
                                  `MAX_INACTIVE_INTERVAL` int NOT NULL,
                                  `EXPIRY_TIME` bigint NOT NULL,
                                  `PRINCIPAL_NAME` varchar(100) DEFAULT NULL,
                                  PRIMARY KEY (`PRIMARY_ID`),
                                  UNIQUE KEY `SPRING_SESSION_IX1` (`SESSION_ID`),
                                  KEY `SPRING_SESSION_IX2` (`EXPIRY_TIME`),
                                  KEY `SPRING_SESSION_IX3` (`PRINCIPAL_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- Table: spring_session_attributes
CREATE TABLE `spring_session_attributes` (
                                             `SESSION_PRIMARY_ID` char(36) NOT NULL,
                                             `ATTRIBUTE_NAME` varchar(200) NOT NULL,
                                             `ATTRIBUTE_BYTES` blob NOT NULL,
                                             PRIMARY KEY (`SESSION_PRIMARY_ID`,`ATTRIBUTE_NAME`),
                                             CONSTRAINT `SPRING_SESSION_ATTRIBUTES_FK` FOREIGN KEY (`SESSION_PRIMARY_ID`) REFERENCES `spring_session` (`PRIMARY_ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;