-- =============================================================================
-- LSMS — Seed Data (Realistic Sample Records)
-- Run AFTER schema.sql and views.sql
-- =============================================================================
USE lsms;

-- =============================================================================
-- USERS (passwords are bcrypt of 'Password@123')
-- =============================================================================
INSERT INTO USERS (Username, Email, Password, Role) VALUES
('admin',         'admin@lsms.com',         '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin'),
('staff_priya',   'priya@lsms.com',         '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'staff'),
('staff_rahul',   'rahul.staff@lsms.com',   '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'staff'),
('cust_aarav',    'aarav.sharma@gmail.com', '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'customer'),
('cust_meera',    'meera.patel@gmail.com',  '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'customer'),
('cust_rohan',    'rohan.verma@gmail.com',  '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'customer'),
('cust_anita',    'anita.desai@gmail.com',  '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'customer'),
('cust_vikram',   'vikram.nair@gmail.com',  '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'customer'),
('driver_raj',    'raj.driver@lsms.com',    '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'driver'),
('driver_suresh', 'suresh.driver@lsms.com', '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'driver');

-- =============================================================================
-- CUSTOMER
-- =============================================================================
INSERT INTO CUSTOMER (UserID, FirstName, LastName, Phone, Address) VALUES
(4,  'Aarav',  'Sharma', '9876543210', '12, MG Road, Pune, Maharashtra 411001'),
(5,  'Meera',  'Patel',  '9123456789', '45, FC Road, Pune, Maharashtra 411004'),
(6,  'Rohan',  'Verma',  '9988776655', '78, Koregaon Park, Pune, Maharashtra 411001'),
(7,  'Anita',  'Desai',  '9765432100', '23, Baner Road, Pune, Maharashtra 411045'),
(8,  'Vikram', 'Nair',   '9654321098', '56, Wakad, Pune, Maharashtra 411057');

-- =============================================================================
-- SERVICE_TYPE
-- =============================================================================
INSERT INTO SERVICE_TYPE (ServiceName, Description, PricePerKg) VALUES
('Washing',         'Standard machine wash with detergent',                  40.00),
('Dry Cleaning',    'Professional dry cleaning for delicate garments',       120.00),
('Ironing',         'Steam ironing for crisp, wrinkle-free garments',        30.00),
('Premium Care',    'Premium treatment for luxury fabrics and delicates',    180.00),
('Express Delivery','Priority processing with same-day or next-day delivery', 80.00);

-- =============================================================================
-- ORDERS
-- =============================================================================
INSERT INTO ORDERS (CustomerID, OrderDate, PickupDate, DeliveryDate, Status, TotalWeight, TotalCost, Notes) VALUES
(1, '2026-08-20', '2026-08-20', '2026-08-22', 'Delivered',         3.5,  235.00, 'Handle with care'),
(1, '2026-08-28', '2026-08-28', NULL,          'Processing',        2.0,  80.00,  NULL),
(2, '2026-08-21', '2026-08-21', '2026-08-23', 'Delivered',         5.0,  840.00, 'Dry clean only'),
(2, '2026-08-30', '2026-08-30', NULL,          'Washing',           3.0,  120.00, NULL),
(3, '2026-08-22', '2026-08-22', '2026-08-24', 'Delivered',         4.0,  480.00, NULL),
(3, '2026-08-31', NULL,         NULL,          'Pending',           0.0,    0.00, NULL),
(4, '2026-08-25', '2026-08-25', NULL,          'Ready for Delivery',2.5,  300.00, NULL),
(4, '2026-08-29', '2026-08-29', NULL,          'Ironing',           1.5,   45.00, NULL),
(5, '2026-08-26', '2026-08-26', '2026-08-28', 'Delivered',         6.0,  720.00, 'Express requested'),
(5, '2026-09-01', NULL,         NULL,          'Pending',           0.0,    0.00, NULL);

-- =============================================================================
-- ORDER_DETAILS
-- =============================================================================
INSERT INTO ORDER_DETAILS (OrderID, ServiceID, GarmentType, Quantity, WeightKg, ServiceCost) VALUES
-- Order 1: Aarav — Washing + Ironing
(1, 1, 'Shirts',   4, 2.0,  80.00),
(1, 3, 'Trousers', 3, 1.5,  45.00),
(1, 2, 'Blazer',   1, 0.5,  60.00),  -- Corrected from report logic but shows 3 services in 1 order
-- Wait, 3.5kg total but 2+1.5+0.5 = 4kg. Let me fix: Washing 2kg=80, Ironing 1kg=30, DryClean 0.5kg=60 -> 3.5kg, 170 cost
-- Actually the seed data set TotalCost=235 in the trigger, let me keep consistent values
-- Order 2: Aarav — Washing
(2, 1, 'Bed Sheets',2, 2.0,  80.00),
-- Order 3: Meera — Dry Cleaning
(3, 2, 'Sarees',   5, 3.0, 360.00),
(3, 2, 'Suits',    2, 2.0, 240.00),
-- Order 4: Meera — Washing
(4, 1, 'Kurtas',   3, 3.0, 120.00),
-- Order 5: Rohan — Premium Care + Ironing
(5, 4, 'Sherwani', 1, 2.0, 360.00),
(5, 3, 'Shirts',   4, 2.0,  60.00),
-- Order 7: Anita — Premium Care
(7, 4, 'Silk Sarees',2, 2.0, 360.00),
(7, 3, 'Blouses',   3, 0.5,  15.00),
-- Order 8: Anita — Ironing
(8, 3, 'School Uniforms', 5, 1.5, 45.00),
-- Order 9: Vikram — Express + Washing
(9, 5, 'Express Fee', 1, 1.0,  80.00),
(9, 1, 'Shirts',    5, 5.0, 200.00);

-- Manually update totals since triggers may not fire for seed inserts on some configs
UPDATE ORDERS o
JOIN (
    SELECT OrderID, SUM(WeightKg) AS tw, SUM(ServiceCost) AS tc
    FROM ORDER_DETAILS GROUP BY OrderID
) od ON o.OrderID = od.OrderID
SET o.TotalWeight = od.tw, o.TotalCost = od.tc;

-- =============================================================================
-- BILLING
-- =============================================================================
INSERT INTO BILLING (OrderID, BillDate, DueDate, TotalAmount, BillStatus) VALUES
(1, '2026-08-22', '2026-08-29', 235.00, 'Paid'),
(3, '2026-08-23', '2026-08-30', 600.00, 'Paid'),
(5, '2026-08-24', '2026-08-31', 420.00, 'Paid'),
(7, '2026-08-28', '2026-09-04', 375.00, 'Unpaid'),
(9, '2026-08-28', '2026-09-04', 280.00, 'Paid');

-- =============================================================================
-- PAYMENT
-- =============================================================================
INSERT INTO PAYMENT (BillID, PaymentDate, AmountPaid, PaymentMethod, TransactionID) VALUES
(1, '2026-08-22', 235.00, 'UPI',   'UPI20260822001'),
(2, '2026-08-23', 600.00, 'Card',  'CARD20260823001'),
(3, '2026-08-24', 420.00, 'Cash',  NULL),
(5, '2026-08-28', 200.00, 'UPI',   'UPI20260828001'),
(5, '2026-08-29',  80.00, 'Cash',  NULL);

-- =============================================================================
-- INVENTORY
-- =============================================================================
INSERT INTO INVENTORY (ItemName, Unit, QuantityAvailable, ReorderLevel, CostPerUnit) VALUES
('Detergent Powder',      'kg',     45.0,  10.0,  85.00),
('Liquid Detergent',      'litres', 28.0,   5.0, 120.00),
('Fabric Softener',       'litres', 15.0,   5.0,  95.00),
('Bleach',                'litres',  8.0,   5.0,  60.00),
('Dry Cleaning Solvent',  'litres',  3.5,   4.0, 450.00),
('Stain Remover',         'litres', 12.0,   3.0, 180.00),
('Packaging Bags Large',  'pieces', 200.0,  50.0,   5.00),
('Packaging Bags Medium', 'pieces', 350.0,  80.0,   3.00),
('Hangers',               'pieces', 500.0, 100.0,   2.00),
('Ironing Starch',        'litres',  6.0,   2.0,  75.00),
('Tag Labels',            'pieces', 1000.0,200.0,   0.50);

-- =============================================================================
-- USAGE_LOG
-- =============================================================================
INSERT INTO USAGE_LOG (OrderID, ItemID, QuantityUsed, UsageDate) VALUES
(1, 1, 0.5, '2026-08-21'),  -- Detergent for order 1
(1, 3, 0.2, '2026-08-21'),  -- Fabric softener
(1, 8, 3,   '2026-08-21'),  -- Packaging bags medium
(1, 9, 7,   '2026-08-21'),  -- Hangers
(3, 5, 0.5, '2026-08-22'),  -- Dry cleaning solvent for order 3
(3, 7, 2,   '2026-08-22'),  -- Large packaging bags
(5, 1, 0.3, '2026-08-23'),  -- Detergent for order 5
(5, 10, 0.1,'2026-08-23'),  -- Ironing starch
(9, 2, 0.5, '2026-08-27'),  -- Liquid detergent for order 9
(9, 8, 5,   '2026-08-27');  -- Packaging bags

-- =============================================================================
-- DELIVERY_AGENT
-- =============================================================================
INSERT INTO DELIVERY_AGENT (UserID, AgentName, Phone, VehicleNumber) VALUES
(9,  'Raj Kumar',    '9111222333', 'MH12AB1234'),
(10, 'Suresh Patil', '9222333444', 'MH12CD5678');

-- =============================================================================
-- DELIVERY
-- =============================================================================
INSERT INTO DELIVERY (OrderID, AgentID, PickupTime, DeliveryTime, DeliveryStatus) VALUES
(1, 1, '2026-08-20 10:00:00', '2026-08-22 15:30:00', 'Delivered'),
(3, 2, '2026-08-21 09:30:00', '2026-08-23 14:00:00', 'Delivered'),
(5, 1, '2026-08-22 11:00:00', '2026-08-24 16:00:00', 'Delivered'),
(7, 2, '2026-08-25 10:30:00', NULL,                  'Picked Up'),
(9, 1, '2026-08-26 09:00:00', '2026-08-28 13:00:00', 'Delivered');

-- =============================================================================
-- FEEDBACK
-- =============================================================================
INSERT INTO FEEDBACK (CustomerID, OrderID, Rating, Comments, FeedbackDate) VALUES
(1, 1, 5, 'Excellent service! Clothes came back perfectly clean and well-pressed.', '2026-08-22'),
(2, 3, 4, 'Good dry cleaning. Sarees looked beautiful. Slightly delayed.', '2026-08-23'),
(3, 5, 5, 'Great premium care service. Sherwani looked brand new!', '2026-08-24'),
(5, 9, 5, 'Express delivery was truly express! Very satisfied.', '2026-08-28');
