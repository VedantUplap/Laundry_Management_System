# 🧺 Laundry Service Management System (LSMS)

A complete full-stack web application and academic DBMS project for automating commercial laundry operations.

**Tech Stack:**
- **Frontend:** HTML5, Modern CSS3 (Glassmorphism & Responsive Design System), Vanilla JavaScript (Fetch API)
- **Backend:** Node.js, Express.js (REST API, JWT Authentication, RBAC)
- **Database:** MySQL 8.0+ (Third Normal Form - 3NF, Triggers, Views, Foreign Keys, Indexes)
- **Database Connectivity (Academic):** **Java Database Connectivity (JDBC)** Module (`java-jdbc/` with Type-4 Driver, PreparedStatements, Transactions) + `mysql2/promise` for Node.js.

---

## 📋 Project Overview

LSMS automates end-to-end commercial laundry operations covering:
1. **Role-Based Authentication & Account Management**: Admin, Staff, Customer, and Driver roles with secure server-side authorization.
2. **Customer Management**: Profile tracking, address records, and complete historical laundry records.
3. **Order Lifecycle Tracking**: From pickup, sorting, washing, drying, ironing, packing, to delivery with automatic weight/cost recalculation via SQL Triggers.
4. **Billing & Payment Management**: Automatic invoice generation, partial and full payment recording (Cash, UPI, Card, Net Banking).
5. **Inventory Control & Auto-Deduction**: Real-time stock tracking with automatic inventory reduction on usage logging.
6. **Delivery Logistics**: Delivery agent assignment, route status tracking (Pending → Assigned → Picked Up → In Transit → Delivered).
7. **Customer Feedback**: Verified customer rating (1-5 stars) and review submission.
8. **Real-Time Analytics & SQL Reports**: Interactive revenue, pending deliveries, inventory consumption, and service popularity charts.
9. **Academic Java JDBC Module**: Standalone Java console application and DAO layer demonstrating pure JDBC connectivity, prepared statements, and atomic transactions.

---

## ⚙️ Prerequisites

- **Node.js** v18+ (tested on v20, v22, v24)
- **Java Development Kit (JDK)** 17+ or 21+ (`javac` and `java`)
- **MySQL Server** 8.0+
- **npm** v8+

---

## 🚀 Complete Step-by-Step Setup Instructions

### 1. Navigate to Project Root
```bash
cd /Users/vedantuplap/Desktop/Laundry_Management_System
```

---

### 2. Configure Backend Environment
Edit `backend/.env` to match your local MySQL configuration:
```ini
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=
DB_NAME=lsms

JWT_SECRET=lsms_super_secret_jwt_key_change_in_production_2024
JWT_EXPIRES_IN=24h

PORT=3001
NODE_ENV=development
```

---

### 3. Initialize MySQL Database & Tables

Execute the schema, views, and seed data in your MySQL server:

```bash
# 1. Create database schema (tables, foreign keys, triggers, constraints, indexes)
mysql -u root -p < database/schema.sql

# 2. Create optimized SQL views
mysql -u root -p lsms < database/views.sql

# 3. Load realistic seed data
mysql -u root -p lsms < database/seed.sql
```

> 💡 *If you are migrating an existing database, run the one-time migration script:*
> ```bash
> mysql -u root -p lsms < database/migrate_accounts.sql
> ```

---

### 4. Install Dependencies & Start the Web Application

```bash
# Install backend dependencies
cd backend
npm install

# Start development server (with nodemon auto-reload)
npm run dev

# OR start standard production server
npm start
```

The web server will start at: **http://localhost:3001**  
Open your browser and navigate to: **[http://localhost:3001/index.html](http://localhost:3001/index.html)**

---

## ☕ Java Database Connectivity (JDBC) Module

A complete, standalone Java JDBC application is included in [`java-jdbc/`](./java-jdbc/) to fulfill academic and course requirements.

### Architecture of JDBC Module
```
java-jdbc/
├── lib/
│   └── mysql-connector-j-8.3.0.jar      # Official Type-4 Pure Java JDBC Driver
├── src/com/lsms/
│   ├── config/
│   │   └── DBConnection.java            # JDBC DriverManager Connection Factory
│   ├── models/
│   │   ├── User.java                    # Entity models
│   │   ├── Customer.java
│   │   ├── Order.java
│   │   └── ServiceType.java
│   ├── dao/
│   │   ├── UserDAO.java                 # PreparedStatement auth & user retrieval
│   │   ├── CustomerDAO.java             # PreparedStatement CRUD operations
│   │   ├── OrderDAO.java                # Multi-table atomic JDBC Transactions (commit/rollback)
│   │   └── ReportDAO.java               # Aggregate reports & analytics
│   ├── App.java                         # Interactive CLI console application
│   └── TestJDBC.java                    # Automated test verification runner
├── build_and_run.sh                     # 1-click compile & execution script
└── README.md                            # Complete JDBC documentation for submission & viva
```

### How to Run the JDBC Module:

#### Option A: Quick Automated Verification Test
Compiles the code, verifies driver registration, tests database metadata, and executes sample queries against the live MySQL `lsms` database:
```bash
cd java-jdbc
./build_and_run.sh test
```

#### Option B: Interactive JDBC Console Menu
Launches an interactive menu to view dashboard stats, inspect orders, place new orders using atomic JDBC transactions, and generate revenue reports:
```bash
cd java-jdbc
./build_and_run.sh
```

### Key Academic JDBC Concepts Implemented:
1. **Type-4 Driver Loading**: Explicit registration of `com.mysql.cj.jdbc.Driver`
2. **Connection Factory**: `DriverManager.getConnection("jdbc:mysql://localhost:3306/lsms", user, pass)`
3. **SQL Injection Prevention**: Dynamic queries parameterized using `java.sql.PreparedStatement`
4. **Transaction Management**: `conn.setAutoCommit(false)`, `conn.commit()`, and `conn.rollback()` in `OrderDAO.java`
5. **Data Mapping**: Reading `ResultSet` rows and constructing typed model objects (`rs.getInt()`, `rs.getString()`, `rs.getBigDecimal()`, etc.)
6. **Metadata Inspection**: Using `DatabaseMetaData` to inspect database product version, driver details, and URL.

---

## 🔐 Default Credentials

All seed accounts use the default password: **`password`**

| Role     | Email                      | Username       | Access Scope |
|----------|----------------------------|----------------|--------------|
| **Admin**    | `uplap.vedant@gmail.com`   | `vedant_admin` | Full system control: Analytics, Inventory, Services, Delivery, Customers, Billing |
| **Staff**    | `jimmyuplap@gmail.com`     | `jimmy_staff`  | Operations: Orders, Inventory, Billing, Deliveries |
| **Customer** | `aarav.sharma@gmail.com`   | `cust_aarav`   | Customer portal: Place orders, View order status, Download invoice, Feedback |
| **Customer** | `meera.patel@gmail.com`    | `cust_meera`   | Customer portal |
| **Driver**   | `raj.driver@lsms.com`      | `driver_raj`   | Logistics: Delivery tracking and status updates |

> 🔄 **Switch Account Feature**: A dedicated **Switch Account** button is available at the bottom-left of every page in both the Admin and Customer dashboards for instant switching between roles.

---

## 📁 Project Directory Structure

```
Laundry_Management_System/
├── backend/
│   ├── config/db.js              # MySQL connection pool & configuration
│   ├── controllers/              # REST API controllers (11 controllers)
│   ├── routes/                   # Express routes with role middleware (12 files)
│   ├── middleware/               # JWT authentication & role-based access control (RBAC)
│   ├── .env                      # Database & server configuration
│   ├── app.js                    # Express app & middleware setup
│   └── server.js                 # HTTP server bootstrap
├── frontend/
│   ├── assets/css/styles.css     # Global CSS design system with CSS custom properties
│   ├── assets/js/api.js          # Centralized API fetch helper with JWT management
│   ├── index.html                # Login page with demo credentials
│   ├── register.html             # Customer registration page
│   ├── admin/                    # Admin portal (8 pages)
│   │   ├── dashboard.html        # Analytics, summary stats, live charts
│   │   ├── customers.html        # Customer search, detail, order history
│   │   ├── orders.html           # Order lifecycle management
│   │   ├── services.html         # Laundry pricing and service catalog
│   │   ├── inventory.html        # Stock management & usage logging
│   │   ├── delivery.html         # Delivery assignment & fleet tracking
│   │   ├── payments.html         # Invoice & payment ledger
│   │   └── reports.html          # 6 analytical SQL reports
│   └── customer/                 # Customer portal (5 pages)
│       ├── dashboard.html        # Overview & active laundry tracking
│       ├── place-order.html      # Interactive order submission
│       ├── orders.html           # Full order history
│       ├── invoice.html          # Printable invoice & receipt
│       └── feedback.html         # Star ratings & review submission
├── java-jdbc/                    # Java Database Connectivity (JDBC) module
│   ├── src/com/lsms/             # Java source code (config, models, dao, app)
│   ├── lib/mysql-connector-j.jar # MySQL Type-4 JDBC driver
│   ├── build_and_run.sh          # Build & run script
│   └── README.md                 # Detailed JDBC documentation
├── database/
│   ├── schema.sql                # 12 tables in 3NF + triggers + indexes
│   ├── views.sql                 # 6 analytical SQL views
│   ├── seed.sql                  # Realistic sample dataset
│   └── migrate_accounts.sql      # Live database migration script
├── docs/
│   └── normalization.md          # 3NF normalization proof & ER explanation
└── README.md                     # Main documentation
```

---

## 🗄️ Database Schema (12 Tables in 3NF)

| Table | Purpose | Key Relationships |
|---|---|---|
| `USERS` | Authentication credentials & roles | Primary entity for all roles |
| `CUSTOMER` | Customer profiles & addresses | FK → `USERS(UserID)` |
| `SERVICE_TYPE` | Laundry offerings & price per kg | Independent master catalog |
| `ORDERS` | Master laundry order record | FK → `CUSTOMER(CustomerID)` |
| `ORDER_DETAILS` | Line items per order (services & garments) | FK → `ORDERS(OrderID)`, `SERVICE_TYPE(ServiceID)` |
| `BILLING` | Invoice summary (1:1 with Orders) | FK → `ORDERS(OrderID)` |
| `PAYMENT` | Transactions & payment modes | FK → `BILLING(BillID)` |
| `INVENTORY` | Raw materials (detergent, bleach, bags, etc.) | Stock master |
| `USAGE_LOG` | Inventory consumption per order | FK → `ORDERS(OrderID)`, `INVENTORY(ItemID)` |
| `DELIVERY_AGENT` | Delivery personnel directory | FK → `USERS(UserID)` |
| `DELIVERY` | Logistics status (1:1 with Orders) | FK → `ORDERS(OrderID)`, `DELIVERY_AGENT(AgentID)` |
| `FEEDBACK` | Customer reviews & ratings (1:1 with Orders)| FK → `CUSTOMER(CustomerID)`, `ORDERS(OrderID)` |

---

## 🎓 Academic DBMS Concepts Checklist

- [x] **Relational Schema Design**: 12 normalized tables adhering to Third Normal Form (3NF).
- [x] **Primary & Foreign Keys**: Enforced referential integrity (`ON UPDATE CASCADE`, `ON DELETE RESTRICT`).
- [x] **Triggers**:
  - `trg_recalculate_order_after_insert/update/delete`: Automatically updates order weight & cost on order detail modification.
  - `trg_reduce_inventory_after_usage`: Automatically deducts inventory stock when material usage is logged.
- [x] **Database Views**: 6 analytical SQL views (`customer_order_history`, `pending_deliveries`, `outstanding_payments`, `inventory_status`, `service_popularity`, `daily_revenue`).
- [x] **Transactions**: ACID compliance during order creation and payment handling (both in Node.js and Java JDBC).
- [x] **Check & Unique Constraints**: Validates ratings (1-5), positive quantities, positive costs, and unique email addresses.
- [x] **JDBC Connectivity**: Complete Java module implementing Type-4 driver loading, prepared statements, metadata inspection, and transactions.

---

## 🔄 Order Lifecycle Flow

```
Customer Places Order ──────► [Pending]
                                  │
Staff Assigns Pickup ────────► [Picked Up] ──► [Received]
                                  │
Laundry Processing ──────────► [Washing] ──► [Drying] ──► [Ironing] ──► [Packed] (Invoice Generated)
                                  │
Logistics Delivery ──────────► [Ready for Delivery] ──► [Out for Delivery] ──► [Delivered]
                                  │
Feedback & Rating ───────────► [Customer Review & Star Rating]
```

---

## 📞 Troubleshooting & FAQs

1. **MySQL Connection Error (`Access denied for user 'root'@'localhost'`):**
   - Check the `DB_PASSWORD` setting inside [`backend/.env`](file:///Users/vedantuplap/Desktop/Laundry_Management_System/backend/.env) and [`java-jdbc/src/com/lsms/config/DBConnection.java`](file:///Users/vedantuplap/Desktop/Laundry_Management_System/java-jdbc/src/com/lsms/config/DBConnection.java).
2. **Port 3001 Already in Use:**
   - Change `PORT=3001` to `PORT=3002` in `backend/.env` and restart the backend.
3. **Running the Java JDBC App in Terminal:**
   - Make sure you are in the `java-jdbc` directory: `cd java-jdbc && ./build_and_run.sh`
