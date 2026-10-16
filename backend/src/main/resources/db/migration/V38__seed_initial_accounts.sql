-- Migration: V38__seed_initial_accounts.sql
-- Purpose: Seed default accounts (ADMIN, VENDOR, CUSTOMER) for local development and testing
-- Default password for all 3 accounts: Password123!

INSERT INTO users (
    id,
    email,
    password_hash,
    full_name,
    phone,
    role,
    avatar_url,
    locale,
    is_email_verified,
    is_locked,
    session_version,
    created_at,
    updated_at
) VALUES 
(
    '11111111-1111-1111-1111-111111111111',
    'admin@danasea.vn',
    '$2b$10$8pHoge4D/5s3b83qeGllTea6.U0HHHWWprzT0dHTWPGOAblVPfZZu',
    'Quản Trị Viên Danasea',
    '0905000001',
    'ADMIN',
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
    'vi',
    true,
    false,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '22222222-2222-2222-2222-222222222222',
    'vendor@danasea.vn',
    '$2b$10$8pHoge4D/5s3b83qeGllTea6.U0HHHWWprzT0dHTWPGOAblVPfZZu',
    'Công Ty Biển Xanh Danasea',
    '0905000002',
    'VENDOR',
    'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150',
    'vi',
    true,
    false,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '33333333-3333-3333-3333-333333333333',
    'customer@danasea.vn',
    '$2b$10$8pHoge4D/5s3b83qeGllTea6.U0HHHWWprzT0dHTWPGOAblVPfZZu',
    'Nguyễn Văn Khách',
    '0905000003',
    'CUSTOMER',
    'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150',
    'vi',
    true,
    false,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- Seed linked Vendor Profile for vendor@danasea.vn
INSERT INTO vendors (
    id,
    user_id,
    business_name,
    tax_code,
    address,
    bank_name,
    bank_account_number,
    bank_account_holder,
    verification_status,
    badge_tier,
    rating_avg,
    rating_count,
    verified_at,
    created_at,
    updated_at
) VALUES (
    '22222222-2222-2222-2222-222222222221',
    '22222222-2222-2222-2222-222222222222',
    'Danasea Water Sports JSC',
    '0401234567',
    'Bán đảo Sơn Trà, TP. Đà Nẵng',
    'Vietcombank',
    '9876543210',
    'DANASEA WATER SPORTS',
    'APPROVED',
    'VERIFIED',
    4.90,
    128,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;
