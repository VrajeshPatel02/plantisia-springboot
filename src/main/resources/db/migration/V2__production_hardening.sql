-- V2__production_hardening.sql
-- Production hardening: unique constraints and missing query indexes

-- 1. Prevent duplicate registrations and full-table scans during login/JWT lookups
ALTER TABLE `users`
    ADD CONSTRAINT `uk_users_email` UNIQUE (`email`);

-- 2. Prevent full-table gap locks during pessimistic write locking on refunds
CREATE INDEX `idx_payments_provider_refund`
    ON `payments` (`provider_refund_id`);

-- 3. Optimize periodic background expiration sweep for unpaid orders
CREATE INDEX `idx_orders_status_payment_created`
    ON `orders` (`status`, `payment_status`, `created_at`);

