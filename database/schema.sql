-- =============================================================================
-- LAUNDRY SERVICE MANAGEMENT SYSTEM (LSMS)
-- Database Schema — Third Normal Form (3NF)
-- MySQL 8.0+
-- =============================================================================

CREATE DATABASE IF NOT EXISTS lsms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lsms;

-- =============================================================================
-- TABLE: USERS
-- Stores authentication credentials for all system users.
-- Separated from domain tables to avoid transitive dependencies (3NF).
-- =============================================================================
CREATE TABLE IF NOT EXISTS USERS (
    UserID      INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    Username    VARCHAR(100)    NOT NULL,
    Email       VARCHAR(255)    NOT NULL,
    Password    VARCHAR(255)    NOT NULL,
    Role        ENUM('admin','staff','customer','driver') NOT NULL DEFAULT 'customer',
    IsActive    TINYINT(1)      NOT NULL DEFAULT 1,
    CreatedAt   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (UserID),
    CONSTRAINT uq_users_email UNIQUE (Email),
    CONSTRAINT uq_users_username UNIQUE (Username)
);

-- =============================================================================
-- TABLE: CUSTOMER
-- Stores customer profile/domain data. Authentication lives in USERS.
-- =============================================================================
CREATE TABLE IF NOT EXISTS CUSTOMER (
    CustomerID  INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    UserID      INT UNSIGNED    NOT NULL,
    FirstName   VARCHAR(100)    NOT NULL,
    LastName    VARCHAR(100)    NOT NULL,
    Phone       VARCHAR(20)     NOT NULL,
    Address     TEXT            NOT NULL,
    CONSTRAINT pk_customer PRIMARY KEY (CustomerID),
    CONSTRAINT fk_customer_user FOREIGN KEY (UserID)
        REFERENCES USERS(UserID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT uq_customer_user UNIQUE (UserID)
);

-- =============================================================================
-- TABLE: SERVICE_TYPE
-- Defines laundry service offerings and pricing.
-- =============================================================================
CREATE TABLE IF NOT EXISTS SERVICE_TYPE (
    ServiceID       INT UNSIGNED        NOT NULL AUTO_INCREMENT,
    ServiceName     VARCHAR(150)        NOT NULL,
    Description     TEXT,
    PricePerKg      DECIMAL(10,2)       NOT NULL,
    IsActive        TINYINT(1)          NOT NULL DEFAULT 1,
    CONSTRAINT pk_service_type PRIMARY KEY (ServiceID),
    CONSTRAINT uq_service_name UNIQUE (ServiceName),
    CONSTRAINT chk_service_price CHECK (PricePerKg >= 0)
);

-- =============================================================================
-- TABLE: ORDERS
-- Main order record. Links customer to their laundry orders.
-- =============================================================================
CREATE TABLE IF NOT EXISTS ORDERS (
    OrderID         INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    CustomerID      INT UNSIGNED    NOT NULL,
    OrderDate       DATE            NOT NULL DEFAULT (CURRENT_DATE),
    PickupDate      DATE,
    DeliveryDate    DATE,
    Status          ENUM(
                        'Pending',
                        'Picked Up',
                        'Received',
                        'Processing',
                        'Washing',
                        'Drying',
                        'Ironing',
                        'Packed',
                        'Ready for Delivery',
                        'Out for Delivery',
                        'Delivered',
                        'Cancelled'
                    ) NOT NULL DEFAULT 'Pending',
    TotalWeight     DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    TotalCost       DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    Notes           TEXT,
    CONSTRAINT pk_orders PRIMARY KEY (OrderID),
    CONSTRAINT fk_orders_customer FOREIGN KEY (CustomerID)
        REFERENCES CUSTOMER(CustomerID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_orders_weight CHECK (TotalWeight >= 0),
    CONSTRAINT chk_orders_cost CHECK (TotalCost >= 0)
);

-- =============================================================================
-- TABLE: ORDER_DETAILS
-- Line items for each order. Multiple services per order.
-- =============================================================================
CREATE TABLE IF NOT EXISTS ORDER_DETAILS (
    DetailID        INT UNSIGNED        NOT NULL AUTO_INCREMENT,
    OrderID         INT UNSIGNED        NOT NULL,
    ServiceID       INT UNSIGNED        NOT NULL,
    GarmentType     VARCHAR(150)        NOT NULL,
    Quantity        INT UNSIGNED        NOT NULL DEFAULT 1,
    WeightKg        DECIMAL(10,2)       NOT NULL,
    ServiceCost     DECIMAL(10,2)       NOT NULL DEFAULT 0.00,
    CONSTRAINT pk_order_details PRIMARY KEY (DetailID),
    CONSTRAINT fk_od_order FOREIGN KEY (OrderID)
        REFERENCES ORDERS(OrderID)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_od_service FOREIGN KEY (ServiceID)
        REFERENCES SERVICE_TYPE(ServiceID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_od_quantity CHECK (Quantity > 0),
    CONSTRAINT chk_od_weight CHECK (WeightKg > 0),
    CONSTRAINT chk_od_cost CHECK (ServiceCost >= 0)
);

-- =============================================================================
-- TABLE: BILLING
-- One billing record per order. Stores invoice information.
-- =============================================================================
CREATE TABLE IF NOT EXISTS BILLING (
    BillID          INT UNSIGNED        NOT NULL AUTO_INCREMENT,
    OrderID         INT UNSIGNED        NOT NULL,
    BillDate        DATE                NOT NULL DEFAULT (CURRENT_DATE),
    DueDate         DATE                NOT NULL,
    TotalAmount     DECIMAL(10,2)       NOT NULL,
    BillStatus      ENUM('Unpaid','Partially Paid','Paid','Overdue') NOT NULL DEFAULT 'Unpaid',
    CONSTRAINT pk_billing PRIMARY KEY (BillID),
    CONSTRAINT fk_billing_order FOREIGN KEY (OrderID)
        REFERENCES ORDERS(OrderID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT uq_billing_order UNIQUE (OrderID),
    CONSTRAINT chk_billing_amount CHECK (TotalAmount >= 0)
);

-- =============================================================================
-- TABLE: PAYMENT
-- Records individual payments against a bill. Supports partial payments.
-- =============================================================================
CREATE TABLE IF NOT EXISTS PAYMENT (
    PaymentID       INT UNSIGNED        NOT NULL AUTO_INCREMENT,
    BillID          INT UNSIGNED        NOT NULL,
    PaymentDate     DATE                NOT NULL DEFAULT (CURRENT_DATE),
    AmountPaid      DECIMAL(10,2)       NOT NULL,
    PaymentMethod   ENUM('Cash','UPI','Card','Net Banking') NOT NULL,
    TransactionID   VARCHAR(100),
    CONSTRAINT pk_payment PRIMARY KEY (PaymentID),
    CONSTRAINT fk_payment_bill FOREIGN KEY (BillID)
        REFERENCES BILLING(BillID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_payment_amount CHECK (AmountPaid > 0)
);

-- =============================================================================
-- TABLE: INVENTORY
-- Consumable items used in laundry operations.
-- =============================================================================
CREATE TABLE IF NOT EXISTS INVENTORY (
    ItemID              INT UNSIGNED        NOT NULL AUTO_INCREMENT,
    ItemName            VARCHAR(150)        NOT NULL,
    Unit                VARCHAR(50)         NOT NULL,
    QuantityAvailable   DECIMAL(10,2)       NOT NULL DEFAULT 0.00,
    ReorderLevel        DECIMAL(10,2)       NOT NULL DEFAULT 0.00,
    CostPerUnit         DECIMAL(10,2)       NOT NULL DEFAULT 0.00,
    CONSTRAINT pk_inventory PRIMARY KEY (ItemID),
    CONSTRAINT uq_inventory_name UNIQUE (ItemName),
    CONSTRAINT chk_inv_qty CHECK (QuantityAvailable >= 0),
    CONSTRAINT chk_inv_reorder CHECK (ReorderLevel >= 0),
    CONSTRAINT chk_inv_cost CHECK (CostPerUnit >= 0)
);

-- =============================================================================
-- TABLE: USAGE_LOG
-- Records which inventory items were consumed by which order.
-- =============================================================================
CREATE TABLE IF NOT EXISTS USAGE_LOG (
    UsageID         INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    OrderID         INT UNSIGNED    NOT NULL,
    ItemID          INT UNSIGNED    NOT NULL,
    QuantityUsed    DECIMAL(10,2)   NOT NULL,
    UsageDate       DATE            NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT pk_usage_log PRIMARY KEY (UsageID),
    CONSTRAINT fk_usage_order FOREIGN KEY (OrderID)
        REFERENCES ORDERS(OrderID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_usage_item FOREIGN KEY (ItemID)
        REFERENCES INVENTORY(ItemID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_usage_qty CHECK (QuantityUsed > 0)
);

-- =============================================================================
-- TABLE: DELIVERY_AGENT
-- Delivery personnel. Authentication lives in USERS.
-- =============================================================================
CREATE TABLE IF NOT EXISTS DELIVERY_AGENT (
    AgentID         INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    UserID          INT UNSIGNED,
    AgentName       VARCHAR(200)    NOT NULL,
    Phone           VARCHAR(20)     NOT NULL,
    VehicleNumber   VARCHAR(50),
    IsActive        TINYINT(1)      NOT NULL DEFAULT 1,
    CONSTRAINT pk_delivery_agent PRIMARY KEY (AgentID),
    CONSTRAINT fk_agent_user FOREIGN KEY (UserID)
        REFERENCES USERS(UserID)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

-- =============================================================================
-- TABLE: DELIVERY
-- One delivery record per order (1:1 with ORDERS).
-- =============================================================================
CREATE TABLE IF NOT EXISTS DELIVERY (
    DeliveryID      INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    OrderID         INT UNSIGNED    NOT NULL,
    AgentID         INT UNSIGNED,
    PickupTime      DATETIME,
    DeliveryTime    DATETIME,
    DeliveryStatus  ENUM('Pending','Assigned','Picked Up','In Transit','Delivered','Failed') NOT NULL DEFAULT 'Pending',
    CONSTRAINT pk_delivery PRIMARY KEY (DeliveryID),
    CONSTRAINT fk_delivery_order FOREIGN KEY (OrderID)
        REFERENCES ORDERS(OrderID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_agent FOREIGN KEY (AgentID)
        REFERENCES DELIVERY_AGENT(AgentID)
        ON UPDATE CASCADE
        ON DELETE SET NULL,
    CONSTRAINT uq_delivery_order UNIQUE (OrderID)
);

-- =============================================================================
-- TABLE: FEEDBACK
-- Customer feedback after order delivery. One per order.
-- =============================================================================
CREATE TABLE IF NOT EXISTS FEEDBACK (
    FeedbackID      INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    CustomerID      INT UNSIGNED    NOT NULL,
    OrderID         INT UNSIGNED    NOT NULL,
    Rating          TINYINT UNSIGNED NOT NULL,
    Comments        TEXT,
    FeedbackDate    DATE            NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT pk_feedback PRIMARY KEY (FeedbackID),
    CONSTRAINT fk_feedback_customer FOREIGN KEY (CustomerID)
        REFERENCES CUSTOMER(CustomerID)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_feedback_order FOREIGN KEY (OrderID)
        REFERENCES ORDERS(OrderID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT uq_feedback_order UNIQUE (OrderID),
    CONSTRAINT chk_feedback_rating CHECK (Rating BETWEEN 1 AND 5)
);

-- =============================================================================
-- INDEXES (performance optimization)
-- MySQL has no native "CREATE INDEX IF NOT EXISTS" syntax, and we cannot drop
-- indexes that MySQL internally uses to enforce FK constraints. The solution:
-- use a helper procedure that creates each index ONLY if it does not already
-- exist in information_schema. This makes the script safe on both a fresh
-- database and one where the schema has been applied before.
-- =============================================================================
DROP PROCEDURE IF EXISTS lsms_create_index_if_missing;

DELIMITER $$
CREATE PROCEDURE lsms_create_index_if_missing(
    IN p_tbl  VARCHAR(64),
    IN p_idx  VARCHAR(64),
    IN p_cols TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_tbl
          AND INDEX_NAME   = p_idx
    ) THEN
        SET @create_sql = CONCAT(
            'CREATE INDEX `', p_idx, '` ON `', p_tbl, '` (', p_cols, ')'
        );
        PREPARE create_stmt FROM @create_sql;
        EXECUTE create_stmt;
        DEALLOCATE PREPARE create_stmt;
    END IF;
END$$
DELIMITER ;

CALL lsms_create_index_if_missing('ORDERS',        'idx_orders_customer',   'CustomerID');
CALL lsms_create_index_if_missing('ORDERS',        'idx_orders_status',     'Status');
CALL lsms_create_index_if_missing('ORDERS',        'idx_orders_date',       'OrderDate');
CALL lsms_create_index_if_missing('ORDER_DETAILS', 'idx_od_order',          'OrderID');
CALL lsms_create_index_if_missing('ORDER_DETAILS', 'idx_od_service',        'ServiceID');
CALL lsms_create_index_if_missing('BILLING',       'idx_billing_status',    'BillStatus');
CALL lsms_create_index_if_missing('PAYMENT',       'idx_payment_bill',      'BillID');
CALL lsms_create_index_if_missing('PAYMENT',       'idx_payment_date',      'PaymentDate');
CALL lsms_create_index_if_missing('USAGE_LOG',     'idx_usage_order',       'OrderID');
CALL lsms_create_index_if_missing('USAGE_LOG',     'idx_usage_item',        'ItemID');
CALL lsms_create_index_if_missing('DELIVERY',      'idx_delivery_status',   'DeliveryStatus');
CALL lsms_create_index_if_missing('DELIVERY',      'idx_delivery_agent',    'AgentID');
CALL lsms_create_index_if_missing('FEEDBACK',      'idx_feedback_customer', 'CustomerID');

DROP PROCEDURE IF EXISTS lsms_create_index_if_missing;

-- =============================================================================
-- TRIGGERS
-- =============================================================================

DELIMITER $$

-- Drop triggers first so the script is safe to re-run
DROP TRIGGER IF EXISTS trg_recalculate_order_after_insert$$
DROP TRIGGER IF EXISTS trg_recalculate_order_after_update$$
DROP TRIGGER IF EXISTS trg_recalculate_order_after_delete$$
DROP TRIGGER IF EXISTS trg_reduce_inventory_after_usage$$

-- Recalculate order totals when an order detail row is inserted
CREATE TRIGGER trg_recalculate_order_after_insert
AFTER INSERT ON ORDER_DETAILS
FOR EACH ROW
BEGIN
    UPDATE ORDERS
    SET
        TotalWeight = (SELECT COALESCE(SUM(WeightKg), 0)    FROM ORDER_DETAILS WHERE OrderID = NEW.OrderID),
        TotalCost   = (SELECT COALESCE(SUM(ServiceCost), 0) FROM ORDER_DETAILS WHERE OrderID = NEW.OrderID)
    WHERE OrderID = NEW.OrderID;
END$$

-- Recalculate order totals when an order detail row is updated
CREATE TRIGGER trg_recalculate_order_after_update
AFTER UPDATE ON ORDER_DETAILS
FOR EACH ROW
BEGIN
    UPDATE ORDERS
    SET
        TotalWeight = (SELECT COALESCE(SUM(WeightKg), 0)    FROM ORDER_DETAILS WHERE OrderID = NEW.OrderID),
        TotalCost   = (SELECT COALESCE(SUM(ServiceCost), 0) FROM ORDER_DETAILS WHERE OrderID = NEW.OrderID)
    WHERE OrderID = NEW.OrderID;
END$$

-- Recalculate order totals when an order detail row is deleted
CREATE TRIGGER trg_recalculate_order_after_delete
AFTER DELETE ON ORDER_DETAILS
FOR EACH ROW
BEGIN
    UPDATE ORDERS
    SET
        TotalWeight = (SELECT COALESCE(SUM(WeightKg), 0)    FROM ORDER_DETAILS WHERE OrderID = OLD.OrderID),
        TotalCost   = (SELECT COALESCE(SUM(ServiceCost), 0) FROM ORDER_DETAILS WHERE OrderID = OLD.OrderID)
    WHERE OrderID = OLD.OrderID;
END$$

-- Reduce inventory stock when usage is logged
CREATE TRIGGER trg_reduce_inventory_after_usage
AFTER INSERT ON USAGE_LOG
FOR EACH ROW
BEGIN
    UPDATE INVENTORY
    SET QuantityAvailable = QuantityAvailable - NEW.QuantityUsed
    WHERE ItemID = NEW.ItemID;
END$$

DELIMITER ;
