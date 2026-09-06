# 🧺 Laundry Service Management System (LSMS)

A complete full-stack web application and academic DBMS project for automating commercial laundry operations.

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-blue.svg)](https://www.mysql.com/)
[![JDBC](https://img.shields.io/badge/JDBC-Type--4%20Driver-green.svg)](https://dev.mysql.com/doc/connector-j/en/)
[![Architecture](https://img.shields.io/badge/Architecture-3NF%20%7C%20DAO%20%7C%20REST-purple.svg)](docs/MIGRATION_REPORT.md)

---

## 🏗️ System Architecture

The Laundry Management System implements a multi-tier client-server architecture. The browser communicates with the **Java Web Application Server** via HTTP/REST, and the Java backend communicates with **MySQL** via **Java Database Connectivity (JDBC)** using the Data Access Object (DAO) pattern.

```
┌────────────────────────────────────────────────────────┐
│                   Browser (Frontend)                   │
│   • HTML5, Modern CSS3 (Glassmorphism & Responsive)    │
│   • Vanilla JavaScript (Fetch API / JSON)              │
└───────────────────────────┬────────────────────────────┘
                            │ HTTP Request (Port 3001)
                            ▼
┌────────────────────────────────────────────────────────┐
│             Java Web Server (HttpServerApp)            │
│   • com.sun.net.httpserver.HttpServer                  │
│   • StaticFileHandler: Serves /frontend & /docs        │
│   • ApiDispatcher: Routes 33 REST endpoints & CORS     │
│   • JwtUtil & PasswordUtil: HMAC-SHA256 & BCrypt       │
│   • UserPrincipal: Role-Based Access Control (RBAC)    │
└───────────────────────────┬────────────────────────────┘
                            │ Method Invocations
                            ▼
┌────────────────────────────────────────────────────────┐
│                   DAO Layer (com.lsms.dao)             │
│   • UserDAO, CustomerDAO, OrderDAO, ServiceDAO         │
│   • BillingDAO, PaymentDAO, InventoryDAO               │
│   • DeliveryDAO, DeliveryAgentDAO, FeedbackDAO         │
│   • DashboardDAO, ReportDAO                            │
└───────────────────────────┬────────────────────────────┘
                            │ JDBC Calls
                            ▼
┌────────────────────────────────────────────────────────┐
│        JDBC Layer (MySQL Connector/J 8.3.0 Type-4)     │
│   • DBConnection (DriverManager Connection Factory)    │
│   • PreparedStatement (Parameterized SQL Queries)      │
│   • ResultSet (Typed Object Mapping)                   │
│   • Transactions: setAutoCommit(false) / commit        │
└───────────────────────────┬────────────────────────────┘
                            │ TCP/IP Socket (Port 3306)
                            ▼
┌────────────────────────────────────────────────────────┐
│               MySQL 8.0+ Database (lsms)               │
│   • Third Normal Form (3NF) Normalized Schema          │
│   • SQL Triggers (Order Recalculation, Stock Reduction)│
│   • SQL Views (Analytics, Outstanding Payments)        │
│   • Foreign Keys, Constraints, B-Tree Indexes          │
└────────────────────────────────────────────────────────┘
```

---

## 📂 Project Directory Structure

```
Laundry_Management_System/
├── run_server.sh                  # 1-Click root launcher for Java Backend
├── test_all_endpoints.sh          # Automated test suite for all 33 endpoints
├── README.md                      # Complete system documentation
├── .env                           # Database & server configuration
│
├── frontend/                      # Web user interface
│   ├── index.html                 # Login page
│   ├── register.html              # Customer registration
│   ├── assets/
│   │   ├── css/styles.css         # Glassmorphism design system
│   │   └── js/api.js              # Centralized API client
│   ├── customer/                  # Customer portal
│   │   ├── dashboard.html         # Customer metrics & quick actions
│   │   ├── place-order.html       # Service selection & order creation
│   │   ├── orders.html            # Order tracking & invoice links
│   │   ├── invoice.html           # Bill view & payment processing
│   │   └── feedback.html          # Order review & star rating
│   └── admin/                     # Admin & Staff management portal
│       ├── dashboard.html         # Real-time operational KPI metrics
│       ├── customers.html         # Customer lookup & management
│       ├── orders.html            # Order lifecycle & status transition
│       ├── services.html          # Service pricing & active toggles
│       ├── inventory.html         # Stock tracking & usage logging
│       ├── delivery.html          # Agent assignment & tracking
│       ├── payments.html          # Payment history & ledger
│       └── reports.html           # 7 Analytical revenue & usage reports
│
├── java-jdbc/                     # Java Backend & JDBC Module
│   ├── build_and_run.sh           # Compilation & execution script
│   ├── lib/                       # Bundled JAR dependencies
│   │   ├── mysql-connector-j-8.3.0.jar  # Official MySQL Type-4 JDBC Driver
│   │   ├── gson-2.10.1.jar              # JSON parsing & serialization
│   │   └── jbcrypt-0.4.jar              # BCrypt password hashing
│   └── src/com/lsms/
│       ├── config/
│       │   ├── AppConfig.java           # Dynamic .env configuration
│       │   └── DBConnection.java        # JDBC connection factory
│       ├── security/
│       │   ├── JwtUtil.java             # HMAC-SHA256 token manager
│       │   ├── PasswordUtil.java        # BCrypt verification
│       │   └── UserPrincipal.java       # RBAC context
│       ├── models/                      # Entity POJO classes
│       │   ├── User.java
│       │   ├── Customer.java
│       │   ├── Order.java
│       │   └── ServiceType.java
│       ├── dao/                         # Data Access Objects (JDBC)
│       │   ├── UserDAO.java
│       │   ├── CustomerDAO.java
│       │   ├── OrderDAO.java
│       │   ├── ServiceDAO.java
│       │   ├── BillingDAO.java
│       │   ├── PaymentDAO.java
│       │   ├── InventoryDAO.java
│       │   ├── DeliveryDAO.java
│       │   ├── DeliveryAgentDAO.java
│       │   ├── FeedbackDAO.java
│       │   ├── DashboardDAO.java
│       │   └── ReportDAO.java
│       ├── server/                      # HTTP Web Server Layer
│       │   ├── HttpServerApp.java       # Server entry point
│       │   ├── StaticFileHandler.java   # Static assets & SPA fallback
│       │   └── ApiDispatcher.java       # REST API routing & RBAC
│       ├── App.java                     # Interactive CLI console
│       └── TestJDBC.java                # Standalone JDBC unit tester
│
├── database/                      # MySQL Database Scripts
│   ├── schema.sql                 # Tables, FKs, constraints, triggers, indexes
│   ├── views.sql                  # Analytical SQL views
│   ├── seed.sql                   # Realistic sample seed data
│   └── migrate_accounts.sql       # Schema migration utilities
│
└── docs/                          # Academic Documentation
    ├── MIGRATION_REPORT.md        # Comprehensive technical report & viva guide
    ├── normalization.md           # 1NF -> 2NF -> 3NF formal proof
    └── normalization_report.html  # Visual database report
```

---

## 🗄️ Database Design (Third Normal Form - 3NF)

The database schema is organized into 10 relational tables strictly adhering to **3NF**:

| Table | Primary Key | Foreign Keys | Purpose |
|---|---|---|---|
| `USERS` | `UserID` | — | Authentication credentials, hashed passwords, roles (`admin`, `staff`, `customer`, `driver`). |
| `CUSTOMER` | `CustomerID` | `UserID` → `USERS(UserID)` | Customer domain profiles (name, phone, address). |
| `SERVICE_TYPE` | `ServiceID` | — | Laundry service catalogue and rates per kg. |
| `ORDERS` | `OrderID` | `CustomerID` → `CUSTOMER(CustomerID)` | Master order record, status, date, total weight, and cost. |
| `ORDER_DETAILS` | `DetailID` | `OrderID` → `ORDERS`, `ServiceID` → `SERVICE_TYPE` | Order line items with quantity, weight, and service cost. |
| `BILLING` | `BillID` | `OrderID` → `ORDERS(OrderID)` | Invoice data, due dates, amount, and payment status. |
| `PAYMENT` | `PaymentID` | `BillID` → `BILLING(BillID)` | Payment transactions (Cash, UPI, Card, Net Banking). |
| `INVENTORY` | `ItemID` | — | Consumables stock (detergent, bleach, bags, hangers). |
| `USAGE_LOG` | `UsageID` | `OrderID` → `ORDERS`, `ItemID` → `INVENTORY` | Records items consumed per order. |
| `DELIVERY_AGENT` | `AgentID` | `UserID` → `USERS(UserID)` | Delivery personnel and vehicle details. |
| `DELIVERY` | `DeliveryID` | `OrderID` → `ORDERS`, `AgentID` → `DELIVERY_AGENT` | 1:1 order delivery tracking with timestamps. |
| `FEEDBACK` | `FeedbackID` | `CustomerID` → `CUSTOMER`, `OrderID` → `ORDERS` | Customer ratings (1-5 stars) and reviews. |

### Database Logic (Triggers & Views)
- **Automatic Order Total Recalculation**: SQL triggers (`trg_recalculate_order_after_insert`, `trg_recalculate_order_after_update`, `trg_recalculate_order_after_delete`) automatically update `TotalWeight` and `TotalCost` on `ORDERS`.
- **Automatic Inventory Deduction**: Trigger `trg_reduce_inventory_after_usage` deducts stock from `INVENTORY` upon insertion into `USAGE_LOG`.
- **Analytical Views**: `vw_customer_order_history`, `vw_pending_deliveries`, `vw_outstanding_payments`, `vw_inventory_status`, `vw_service_popularity`, `vw_daily_revenue`.

---

## 🔑 Key JDBC Concepts Implemented

1. **Type-4 Pure Java Driver**: `com.mysql.cj.jdbc.Driver` connects directly over TCP/IP socket to MySQL without native client libraries.
2. **`PreparedStatement`**: All queries use parameterized `?` placeholders, preventing SQL injection attacks.
3. **`ResultSet` Mapping**: Typed data mapping from SQL types (`getInt`, `getString`, `getBigDecimal`, `getDate`) into Java domain objects and JSON.
4. **Multi-Table Atomic Transactions**:
   - `conn.setAutoCommit(false)` initiates an atomic transaction.
   - `conn.commit()` commits multi-table changes simultaneously.
   - `conn.rollback()` restores database consistency on any failure.
5. **Connection & Resource Management**: Try-with-resources (`AutoCloseable`) ensures all connections, statements, and result sets are closed cleanly.

---

## ⚙️ Prerequisites

- **Java Development Kit (JDK)**: Version 17+ or 21+ (`javac` and `java` on PATH)
- **MySQL Server**: Version 8.0+ running on port `3306`

---

## 🚀 Step-by-Step Setup & Execution

### 1. Configure Database Credentials
By default, the server connects to `localhost:3306` with user `root` and blank password for database `lsms`. To customize, edit `.env` in the root directory:
```ini
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=
DB_NAME=lsms
PORT=3001
JWT_SECRET=lsms_super_secret_jwt_key_change_in_production_2024
```

### 2. Initialize MySQL Database
Run the schema, views, and seed data scripts:
```bash
mysql -u root -p < database/schema.sql
mysql -u root -p lsms < database/views.sql
mysql -u root -p lsms < database/seed.sql
```

### 3. Launch the Application
Run the one-click launcher from the project root:
```bash
./run_server.sh
```

Or from the `java-jdbc/` directory:
```bash
cd java-jdbc
./build_and_run.sh server
```

The server will automatically compile all Java files and start listening:
- **Web Application:** [http://localhost:3001](http://localhost:3001)
- **API Base:** [http://localhost:3001/api](http://localhost:3001/api)
- **Documentation:** [http://localhost:3001/docs](http://localhost:3001/docs)

---

## 🧪 Testing & Verification

### Run Full End-to-End API Test Suite
```bash
./test_all_endpoints.sh
```
*Executes all 33 endpoints, verifying authentication, RBAC authorization, order transactions, payment recording, and reports.*

### Run Standalone JDBC Unit Tests
```bash
cd java-jdbc
./build_and_run.sh test
```

### Run Interactive Console CLI
```bash
cd java-jdbc
./build_and_run.sh console
```

---

## 👥 Demo Login Credentials

| Role | Email | Password | Access / Permissions |
|---|---|---|---|
| **Admin** | `uplap.vedant@gmail.com` | `password` | Full access: Analytics dashboard, reports, services, inventory, deliveries, customer accounts |
| **Staff** | `jimmyuplap@gmail.com` | `password` | Operations: Order management, status updates, billing, payments, inventory usage logging |
| **Staff** | `rahul.staff@lsms.com` | `password` | Operations & delivery assignment |
| **Customer** | `aarav.sharma@gmail.com` | `password` | Self-service: Place orders, order tracking, invoices, online payment, feedback |
| **Customer** | `meera.patel@gmail.com` | `password` | Self-service customer portal |
| **Driver** | `raj.driver@lsms.com` | `password` | Logistics: Assigned deliveries, route status updates |

---

## 📡 Complete REST API Catalog (33 Endpoints)

| Module | Method | Endpoint | Allowed Roles | Description |
|---|---|---|---|---|
| **Auth** | `POST` | `/api/auth/register` | Public | Register customer (Atomic JDBC transaction in `USERS` & `CUSTOMER`) |
| | `POST` | `/api/auth/login` | Public | Login with BCrypt verification & HMAC-SHA256 JWT generation |
| | `GET` | `/api/auth/me` | Authenticated | Get current user profile & linked customer/driver identity |
| **Customers** | `GET` | `/api/customers` | Admin, Staff | List all customers with search parameter support |
| | `GET` | `/api/customers/:id` | Admin, Staff, Customer | Get customer details by ID |
| | `PUT` | `/api/customers/:id` | Admin, Staff, Customer | Update customer contact & address details |
| | `DELETE` | `/api/customers/:id` | Admin | Soft-deactivate customer account |
| | `GET` | `/api/customers/:id/orders` | Admin, Staff, Customer | Get order history with billing status |
| **Services** | `GET` | `/api/services` | Public | List all active/inactive laundry services |
| | `GET` | `/api/services/:id` | Public | Get single service details and price per kg |
| | `POST` | `/api/services` | Admin, Staff | Create a new laundry service offering |
| | `PUT` | `/api/services/:id` | Admin, Staff | Update service name, description, rate, or active flag |
| | `DELETE` | `/api/services/:id` | Admin | Soft-delete service |
| **Orders** | `GET` | `/api/orders` | Admin, Staff, Customer, Driver | List orders (filtered by status, customer, date range) |
| | `GET` | `/api/orders/:id` | Admin, Staff, Customer, Driver | Get order details with line items |
| | `POST` | `/api/orders` | Admin, Staff, Customer | Create multi-item order (Atomic JDBC transaction + delivery creation) |
| | `PUT` | `/api/orders/:id/status` | Admin, Staff | Update status (auto-generates bill when `Packed`, syncs delivery) |
| | `DELETE` | `/api/orders/:id` | Admin, Staff, Customer | Cancel pending order |
| **Billing** | `GET` | `/api/billing` | Admin, Staff | List all billing records with total amount paid |
| | `GET` | `/api/billing/order/:orderId` | Admin, Staff, Customer | Get invoice details, payments list, and order breakdown |
| | `POST` | `/api/billing` | Admin, Staff | Manually generate billing record |
| **Payments** | `GET` | `/api/payments` | Admin, Staff | List all recorded payment transactions |
| | `POST` | `/api/payments` | Admin, Staff, Customer | Record payment (Atomic JDBC transaction + recalculates `BillStatus`) |
| **Inventory** | `GET` | `/api/inventory` | Admin, Staff | List stock items with computed `StockStatus` |
| | `GET` | `/api/inventory/:id` | Admin, Staff | Get inventory item by ID |
| | `POST` | `/api/inventory` | Admin, Staff | Add new inventory consumable |
| | `PUT` | `/api/inventory/:id` | Admin, Staff | Update quantity, reorder level, or unit cost |
| | `DELETE` | `/api/inventory/:id` | Admin, Staff | Delete inventory item |
| | `POST` | `/api/inventory/:id/usage` | Admin, Staff | Log usage for an order (Atomic JDBC transaction + trigger deduction) |
| | `GET` | `/api/inventory/usage` | Admin, Staff | List historical inventory usage log |
| **Delivery** | `GET` | `/api/delivery-agents` | Admin, Staff, Driver | List active agents with active delivery count |
| | `POST` | `/api/delivery-agents` | Admin | Register new delivery agent |
| | `PUT` | `/api/delivery-agents/:id` | Admin, Staff | Update agent contact or vehicle |
| | `GET` | `/api/deliveries` | Admin, Staff, Driver | List all deliveries with customer & agent joins |
| | `PUT` | `/api/deliveries/:id` | Admin, Staff, Driver | Assign agent, update route status & timestamps |
| **Feedback** | `GET` | `/api/feedback` | Admin, Staff | View all customer reviews |
| | `GET` | `/api/feedback/my` | Customer | View customer's own submitted reviews |
| | `POST` | `/api/feedback` | Customer | Submit rating (1-5) & review for delivered order |
| **Dashboard** | `GET` | `/api/dashboard/stats` | Admin, Staff | Aggregate operational KPIs (revenue, active orders, low stock) |
| **Reports** | `GET` | `/api/reports/daily-revenue` | Admin, Staff | Daily revenue totals & transaction counts |
| | `GET` | `/api/reports/monthly-revenue` | Admin, Staff | Monthly revenue breakdown (last 12 months) |
| | `GET` | `/api/reports/pending-deliveries` | Admin, Staff | List pending and in-transit deliveries |
| | `GET` | `/api/reports/customer-history/:id` | Admin, Staff | Complete order history and payment ledger for a customer |
| | `GET` | `/api/reports/inventory-usage` | Admin, Staff | Item consumption totals |
| | `GET` | `/api/reports/popular-services` | Admin, Staff | Most ordered services by revenue and weight |
| | `GET` | `/api/reports/outstanding-payments`| Admin, Staff | Unpaid & overdue bills with aging status |

---

