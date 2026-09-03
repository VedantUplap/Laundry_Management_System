-- =============================================================================
-- LSMS — Account Migration
-- Updates Admin and Staff accounts to personalized emails.
-- Run this against your live `lsms` database ONCE.
-- Existing customer data and all orders remain untouched.
-- =============================================================================
USE lsms;

-- ---------------------------------------------------------------------------
-- 1. Update Admin account
--    Old email : admin@lsms.com
--    New email : uplap.vedant@gmail.com
--    New username: vedant_admin
-- ---------------------------------------------------------------------------
UPDATE USERS
SET
    Email    = 'uplap.vedant@gmail.com',
    Username = 'vedant_admin'
WHERE Email = 'admin@lsms.com'
  AND Role  = 'admin';

-- ---------------------------------------------------------------------------
-- 2. Update Staff account (Priya → Jimmy)
--    Old email : priya@lsms.com
--    New email : jimmyuplap@gmail.com
--    New username: jimmy_staff
-- ---------------------------------------------------------------------------
UPDATE USERS
SET
    Email    = 'jimmyuplap@gmail.com',
    Username = 'jimmy_staff'
WHERE Email = 'priya@lsms.com'
  AND Role  = 'staff';

-- ---------------------------------------------------------------------------
-- 3. Verify the changes
-- ---------------------------------------------------------------------------
SELECT UserID, Username, Email, Role, IsActive
FROM USERS
WHERE Role IN ('admin', 'staff')
ORDER BY Role, UserID;
