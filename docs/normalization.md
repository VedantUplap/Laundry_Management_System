# Normalization of Laundry Service Management System (LSMS)

## 1. Introduction

This document explains the complete normalization process applied to the LSMS database, progressing from an Unnormalized Form (UNF) through 1NF, 2NF, and finally 3NF. The final schema implemented in MySQL exactly matches what is described here.

The goal of normalization is to eliminate data redundancy, prevent update/insert/delete anomalies, and ensure data integrity.

---

## 2. Unnormalized Form (UNF)

In the initial conceptual model, all laundry business data could be stored in a single flat table:

```
LAUNDRY_ORDER(
  OrderID, OrderDate, PickupDate, DeliveryDate, OrderStatus,
  CustomerID, CustomerFirstName, CustomerLastName, CustomerPhone, CustomerEmail, CustomerAddress,
  [ServiceID, ServiceName, ServiceDescription, PricePerKg, GarmentType, Quantity, WeightKg, ServiceCost],  ← repeating group
  TotalWeight, TotalCost,
  BillID, BillDate, DueDate, BillAmount, BillStatus,
  [PaymentID, PaymentDate, AmountPaid, PaymentMethod, TransactionID],  ← repeating group
  AgentID, AgentName, AgentPhone, VehicleNumber,
  DeliveryID, PickupTime, DeliveryTime, DeliveryStatus,
  FeedbackID, Rating, Comments, FeedbackDate,
  [ItemID, ItemName, Unit, QuantityUsed, QuantityAvailable, ReorderLevel]  ← repeating group
)
```

**Problems:**
- Repeating groups (multiple services, payments, inventory items per order)
- Massive redundancy (customer info repeated for every order)
- No atomic values (multiple services in one cell)
- Update anomaly: changing a customer's phone requires updating every row

---

## 3. First Normal Form (1NF)

**Rules Applied:**
- Eliminate repeating groups
- Ensure atomic (single-valued) attributes
- Each row must be uniquely identifiable

**Steps:**
1. Separate repeating service items → `ORDER_DETAILS` table
2. Separate repeating payments → `PAYMENT` table
3. Separate repeating inventory usage → `USAGE_LOG` table
4. Assign primary keys to each table

**Resulting 1NF Tables:**

```
LAUNDRY_ORDER(OrderID PK, OrderDate, PickupDate, DeliveryDate, Status,
              CustomerID, CustomerFirstName, CustomerLastName, CustomerPhone,
              CustomerEmail, CustomerAddress, TotalWeight, TotalCost,
              BillID, BillDate, DueDate, BillAmount, BillStatus,
              AgentID, AgentName, AgentPhone, VehicleNumber,
              DeliveryStatus, PickupTime, DeliveryTime,
              FeedbackRating, FeedbackComments, FeedbackDate)

ORDER_DETAIL(DetailID PK, OrderID, ServiceID, ServiceName, ServiceDescription,
             PricePerKg, GarmentType, Quantity, WeightKg, ServiceCost)

PAYMENT(PaymentID PK, OrderID, BillID, PaymentDate, AmountPaid,
        PaymentMethod, TransactionID)

USAGE_LOG(UsageID PK, OrderID, ItemID, ItemName, Unit,
          QuantityUsed, QuantityAvailable, ReorderLevel, UsageDate)
```

**Status:** No repeating groups. All values are atomic. ✅

---

## 4. Second Normal Form (2NF)

**Rule Applied:**
- Remove **partial dependencies** — every non-key attribute must depend on the **entire** primary key (applies to tables with composite keys).

**Partial Dependencies Found:**

### In `ORDER_DETAIL(DetailID, OrderID, ServiceID, ...)`
- `ServiceName`, `ServiceDescription`, `PricePerKg` depend only on `ServiceID` (not on `DetailID`)
- **Fix:** Extract `SERVICE_TYPE(ServiceID, ServiceName, Description, PricePerKg)`

### In `USAGE_LOG(UsageID, OrderID, ItemID, ...)`
- `ItemName`, `Unit`, `QuantityAvailable`, `ReorderLevel` depend only on `ItemID` (not on `UsageID`)
- **Fix:** Extract `INVENTORY(ItemID, ItemName, Unit, QuantityAvailable, ReorderLevel, CostPerUnit)`

### In `LAUNDRY_ORDER(...)`
- Customer attributes (`FirstName`, `LastName`, `Phone`, `Email`, `Address`) depend only on `CustomerID`
- Agent attributes (`AgentName`, `AgentPhone`, `VehicleNumber`) depend only on `AgentID`
- Bill attributes (`BillDate`, `DueDate`, `BillAmount`, `BillStatus`) depend only on `BillID`
- **Fix:** Extract each into its own table

**Resulting 2NF Tables:**

```
CUSTOMER(CustomerID PK, FirstName, LastName, Phone, Email, Address)
SERVICE_TYPE(ServiceID PK, ServiceName, Description, PricePerKg)
INVENTORY(ItemID PK, ItemName, Unit, QuantityAvailable, ReorderLevel, CostPerUnit)
DELIVERY_AGENT(AgentID PK, AgentName, Phone, VehicleNumber)
BILLING(BillID PK, OrderID FK, BillDate, DueDate, TotalAmount, BillStatus)

ORDERS(OrderID PK, CustomerID FK, OrderDate, PickupDate, DeliveryDate, Status, TotalWeight, TotalCost)
ORDER_DETAILS(DetailID PK, OrderID FK, ServiceID FK, GarmentType, Quantity, WeightKg, ServiceCost)
PAYMENT(PaymentID PK, BillID FK, PaymentDate, AmountPaid, PaymentMethod, TransactionID)
USAGE_LOG(UsageID PK, OrderID FK, ItemID FK, QuantityUsed, UsageDate)
DELIVERY(DeliveryID PK, OrderID FK, AgentID FK, PickupTime, DeliveryTime, DeliveryStatus)
FEEDBACK(FeedbackID PK, CustomerID FK, OrderID FK, Rating, Comments, FeedbackDate)
```

**Status:** No partial dependencies remain. ✅

---

## 5. Third Normal Form (3NF)

**Rule Applied:**
- Remove **transitive dependencies** — non-key attributes must not depend on other non-key attributes.

**Transitive Dependencies Found:**

### In `CUSTOMER` table
- `Email` could be used for authentication, but authentication data (password, role) would transitively depend on `Email`, not `CustomerID`.
- **Fix:** Extract `USERS(UserID PK, Email, Password, Role)` and link `CUSTOMER.UserID → USERS.UserID`

### In `DELIVERY_AGENT` table
- Same issue: agent login credentials would create transitive dependencies.
- **Fix:** `DELIVERY_AGENT.UserID → USERS.UserID` (optional link)

### Checking remaining tables:
- `ORDER_DETAILS`: `ServiceCost = WeightKg × PricePerKg` — this is a derived/calculated attribute stored for historical accuracy (price may change). No transitive dependency since `PricePerKg` is in `SERVICE_TYPE`, not `ORDER_DETAILS`.
- `BILLING`: `TotalAmount` is derived from `ORDERS.TotalCost` at bill generation time. Stored for historical immutability. Acceptable in normalized design.
- All other non-key attributes depend **only** on the primary key of their table. ✅

**Final 3NF Schema:**

```
USERS(UserID PK, Username UNIQUE, Email UNIQUE, Password, Role, IsActive)

CUSTOMER(CustomerID PK, UserID FK→USERS, FirstName, LastName, Phone, Address)

SERVICE_TYPE(ServiceID PK, ServiceName UNIQUE, Description, PricePerKg, IsActive)

ORDERS(OrderID PK, CustomerID FK→CUSTOMER, OrderDate, PickupDate, DeliveryDate,
       Status, TotalWeight, TotalCost, Notes)

ORDER_DETAILS(DetailID PK, OrderID FK→ORDERS, ServiceID FK→SERVICE_TYPE,
              GarmentType, Quantity, WeightKg, ServiceCost)

BILLING(BillID PK, OrderID FK→ORDERS UNIQUE, BillDate, DueDate,
        TotalAmount, BillStatus)

PAYMENT(PaymentID PK, BillID FK→BILLING, PaymentDate, AmountPaid,
        PaymentMethod, TransactionID)

INVENTORY(ItemID PK, ItemName UNIQUE, Unit, QuantityAvailable,
          ReorderLevel, CostPerUnit)

USAGE_LOG(UsageID PK, OrderID FK→ORDERS, ItemID FK→INVENTORY,
          QuantityUsed, UsageDate)

DELIVERY_AGENT(AgentID PK, UserID FK→USERS, AgentName, Phone,
               VehicleNumber, IsActive)

DELIVERY(DeliveryID PK, OrderID FK→ORDERS UNIQUE, AgentID FK→DELIVERY_AGENT,
         PickupTime, DeliveryTime, DeliveryStatus)

FEEDBACK(FeedbackID PK, CustomerID FK→CUSTOMER, OrderID FK→ORDERS UNIQUE,
         Rating CHECK(1-5), Comments, FeedbackDate)
```

---

## 6. Final Normalized Relations

| Relation | Primary Key | Foreign Keys | Notes |
|---|---|---|---|
| USERS | UserID | — | Central auth table |
| CUSTOMER | CustomerID | UserID | 1:1 with USERS |
| SERVICE_TYPE | ServiceID | — | Lookup table |
| ORDERS | OrderID | CustomerID | Core transaction |
| ORDER_DETAILS | DetailID | OrderID, ServiceID | Line items |
| BILLING | BillID | OrderID (UNIQUE) | 1:1 with ORDERS |
| PAYMENT | PaymentID | BillID | Many payments per bill |
| INVENTORY | ItemID | — | Consumables stock |
| USAGE_LOG | UsageID | OrderID, ItemID | M:N resolution |
| DELIVERY_AGENT | AgentID | UserID | Delivery personnel |
| DELIVERY | DeliveryID | OrderID (UNIQUE), AgentID | 1:1 with ORDERS |
| FEEDBACK | FeedbackID | CustomerID, OrderID (UNIQUE) | 1:1 with ORDERS |

---

## 7. Primary Keys

| Table | Primary Key | Type |
|---|---|---|
| USERS | UserID | INT UNSIGNED AUTO_INCREMENT |
| CUSTOMER | CustomerID | INT UNSIGNED AUTO_INCREMENT |
| SERVICE_TYPE | ServiceID | INT UNSIGNED AUTO_INCREMENT |
| ORDERS | OrderID | INT UNSIGNED AUTO_INCREMENT |
| ORDER_DETAILS | DetailID | INT UNSIGNED AUTO_INCREMENT |
| BILLING | BillID | INT UNSIGNED AUTO_INCREMENT |
| PAYMENT | PaymentID | INT UNSIGNED AUTO_INCREMENT |
| INVENTORY | ItemID | INT UNSIGNED AUTO_INCREMENT |
| USAGE_LOG | UsageID | INT UNSIGNED AUTO_INCREMENT |
| DELIVERY_AGENT | AgentID | INT UNSIGNED AUTO_INCREMENT |
| DELIVERY | DeliveryID | INT UNSIGNED AUTO_INCREMENT |
| FEEDBACK | FeedbackID | INT UNSIGNED AUTO_INCREMENT |

---

## 8. Foreign Keys

| Table | Foreign Key | References | On Update | On Delete |
|---|---|---|---|---|
| CUSTOMER | UserID | USERS(UserID) | CASCADE | RESTRICT |
| ORDERS | CustomerID | CUSTOMER(CustomerID) | CASCADE | RESTRICT |
| ORDER_DETAILS | OrderID | ORDERS(OrderID) | CASCADE | CASCADE |
| ORDER_DETAILS | ServiceID | SERVICE_TYPE(ServiceID) | CASCADE | RESTRICT |
| BILLING | OrderID | ORDERS(OrderID) | CASCADE | RESTRICT |
| PAYMENT | BillID | BILLING(BillID) | CASCADE | RESTRICT |
| USAGE_LOG | OrderID | ORDERS(OrderID) | CASCADE | RESTRICT |
| USAGE_LOG | ItemID | INVENTORY(ItemID) | CASCADE | RESTRICT |
| DELIVERY_AGENT | UserID | USERS(UserID) | CASCADE | SET NULL |
| DELIVERY | OrderID | ORDERS(OrderID) | CASCADE | RESTRICT |
| DELIVERY | AgentID | DELIVERY_AGENT(AgentID) | CASCADE | SET NULL |
| FEEDBACK | CustomerID | CUSTOMER(CustomerID) | CASCADE | CASCADE |
| FEEDBACK | OrderID | ORDERS(OrderID) | CASCADE | RESTRICT |

---

## 9. Functional Dependencies

### USERS
- `UserID → Username, Email, Password, Role, IsActive`

### CUSTOMER
- `CustomerID → UserID, FirstName, LastName, Phone, Address`
- `UserID → CustomerID` (by UNIQUE constraint)

### SERVICE_TYPE
- `ServiceID → ServiceName, Description, PricePerKg, IsActive`
- `ServiceName → ServiceID` (by UNIQUE constraint)

### ORDERS
- `OrderID → CustomerID, OrderDate, PickupDate, DeliveryDate, Status, TotalWeight, TotalCost, Notes`

### ORDER_DETAILS
- `DetailID → OrderID, ServiceID, GarmentType, Quantity, WeightKg, ServiceCost`
- `ServiceCost` is a derived value (WeightKg × PricePerKg at time of order — stored for historical accuracy)

### BILLING
- `BillID → OrderID, BillDate, DueDate, TotalAmount, BillStatus`
- `OrderID → BillID` (by UNIQUE constraint — 1:1 relationship)

### PAYMENT
- `PaymentID → BillID, PaymentDate, AmountPaid, PaymentMethod, TransactionID`

### INVENTORY
- `ItemID → ItemName, Unit, QuantityAvailable, ReorderLevel, CostPerUnit`
- `ItemName → ItemID` (by UNIQUE constraint)

### USAGE_LOG
- `UsageID → OrderID, ItemID, QuantityUsed, UsageDate`

### DELIVERY_AGENT
- `AgentID → UserID, AgentName, Phone, VehicleNumber, IsActive`

### DELIVERY
- `DeliveryID → OrderID, AgentID, PickupTime, DeliveryTime, DeliveryStatus`
- `OrderID → DeliveryID` (by UNIQUE constraint — 1:1 relationship)

### FEEDBACK
- `FeedbackID → CustomerID, OrderID, Rating, Comments, FeedbackDate`
- `OrderID → FeedbackID` (by UNIQUE constraint — 1:1 per order)

---

## 10. Referential Integrity

The schema enforces referential integrity through:

1. **Foreign Key Constraints** — MySQL enforces all FK relationships
2. **CASCADE UPDATE** — If a parent PK changes, child FK updates automatically
3. **RESTRICT DELETE** — Cannot delete a customer who has orders; cannot delete an order that has billing
4. **SET NULL** — If a delivery agent is deleted, existing deliveries retain their record but AgentID becomes NULL
5. **UNIQUE on OrderID in BILLING/DELIVERY/FEEDBACK** — Enforces 1:1 cardinality at the database level

---

## 11. Constraints Implemented in MySQL

```sql
-- Rating must be 1–5
CONSTRAINT chk_feedback_rating CHECK (Rating BETWEEN 1 AND 5)

-- Prices cannot be negative
CONSTRAINT chk_service_price CHECK (PricePerKg >= 0)

-- Stock quantities cannot be negative
CONSTRAINT chk_inv_qty CHECK (QuantityAvailable >= 0)

-- Order quantities must be positive
CONSTRAINT chk_od_quantity CHECK (Quantity > 0)
CONSTRAINT chk_od_weight CHECK (WeightKg > 0)

-- Payment amount must be positive
CONSTRAINT chk_payment_amount CHECK (AmountPaid > 0)

-- Bill amount cannot be negative
CONSTRAINT chk_billing_amount CHECK (TotalAmount >= 0)

-- One bill per order (1:1)
CONSTRAINT uq_billing_order UNIQUE (OrderID)

-- One delivery per order (1:1)
CONSTRAINT uq_delivery_order UNIQUE (OrderID)

-- One feedback per order (1:1)
CONSTRAINT uq_feedback_order UNIQUE (OrderID)
```

---

## 12. Connection Between Normalized Database and Application

The normalized schema is the **single source of truth**:

```
Frontend (HTML/JS)
       ↓  fetch('/api/...')
Express.js Backend (REST API)
       ↓  mysql2 connection pool
MySQL Database (3NF Normalized)
```

- The **frontend never stores business data** in localStorage or hardcoded arrays
- All **cost calculations** happen in the backend using `PricePerKg` from `SERVICE_TYPE`
- **Transactions** ensure atomic multi-table operations (e.g., order creation inserts into ORDERS, ORDER_DETAILS, and DELIVERY in one transaction — if any step fails, all changes are rolled back)
- **Database triggers** maintain derived attributes: `TotalWeight` and `TotalCost` in ORDERS are automatically recalculated when ORDER_DETAILS rows are inserted/updated/deleted
- **SQL Views** (6 views) simplify complex multi-table queries for reports without denormalizing the schema

---

## 13. Summary

| Normal Form | Achieved | Key Change |
|---|---|---|
| UNF | ✅ Starting point | Flat table with repeating groups |
| 1NF | ✅ | Eliminated repeating groups; atomic values |
| 2NF | ✅ | Removed partial dependencies; extracted lookup tables |
| 3NF | ✅ | Removed transitive dependencies; separated auth from domain |

The LSMS MySQL database is in **Third Normal Form (3NF)** as implemented in `schema.sql`.
