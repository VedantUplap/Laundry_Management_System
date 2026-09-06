# 🧺 Laundry Service Management System (LSMS)

A complete full-stack web application and academic DBMS project for automating commercial laundry operations.

**Tech Stack:**
- **Frontend:** HTML5, Modern CSS3 (Glassmorphism & Responsive Design System), Vanilla JavaScript (Fetch API)
- **Backend / Web Server:** **Pure Java Web Server (`com.sun.net.httpserver.HttpServer`)** with REST API, HMAC-SHA256 JWT Authentication, and RBAC
- **Database Connectivity:** **Java Database Connectivity (JDBC)** with MySQL Connector/J 8.3.0 Type-4 Driver, PreparedStatements, Transactions (`commit`/`rollback`), and DAOs
- **Database:** MySQL 8.0+ (Third Normal Form - 3NF, Triggers, Views, Foreign Keys, Indexes)

---

## 🏗️ Architecture

```
[Browser Frontend (HTML/CSS/JS)]
             │ HTTP (Fetch API / JSON) — Port 3001
             ▼
[Java Web Application Server] (com.sun.net.httpserver.HttpServer)
   ├── StaticFileHandler (Serves /frontend & /docs)
   ├── ApiDispatcher (Routes 33 REST endpoints, CORS)
   ├── JwtUtil (HMAC-SHA256 Authentication)
   └── UserPrincipal (Role-Based Access Control)
             │
             ▼
[DAO Layer] (com.lsms.dao.*)
   ├── UserDAO, CustomerDAO, OrderDAO, ServiceDAO
   ├── BillingDAO, PaymentDAO, InventoryDAO
   ├── DeliveryDAO, DeliveryAgentDAO, FeedbackDAO
   └── DashboardDAO, ReportDAO
             │
             ▼
[JDBC Layer] (MySQL Connector/J 8.3.0 Type-4 Driver)
   ├── DBConnection (DriverManager Connection Factory)
   ├── PreparedStatement (Parameterized Queries)
   ├── ResultSet (Typed Object Mapping)
   └── Transactions (setAutoCommit(false), commit(), rollback())
             │ TCP/IP (Port 3306)
             ▼
[MySQL 8.0+ Database (lsms)]
   ├── 10 Relational Tables (3NF Normalized)
   ├── SQL Triggers (Order Recalculation, Stock Deduction)
   └── SQL Views (Analytical Reports, Outstanding Balances)
```

---

## 📋 Project Features

1. **Role-Based Authentication & Account Management**: Admin, Staff, Customer, and Driver roles with secure server-side authorization.
2. **Customer Management**: Profile tracking, address records, and complete historical laundry records.
3. **Order Lifecycle Tracking**: From pickup, sorting, washing, drying, ironing, packing, to delivery with automatic weight/cost recalculation via SQL Triggers.
4. **Billing & Payment Management**: Automatic invoice generation upon packing, partial and full payment recording (Cash, UPI, Card, Net Banking).
5. **Inventory Control & Auto-Deduction**: Real-time stock tracking with automatic inventory reduction on usage logging.
6. **Delivery Logistics**: Delivery agent assignment, route status tracking (Pending → Assigned → Picked Up → In Transit → Delivered).
7. **Customer Feedback**: Verified customer rating (1-5 stars) and review submission for delivered orders.
8. **Real-Time Analytics & SQL Reports**: Interactive revenue, pending deliveries, inventory consumption, and service popularity charts.
9. **Academic Java JDBC Architecture**: Complete replacement of Node.js with native Java DAOs, multi-table transactions, and PreparedStatement security.

---

## ⚙️ Prerequisites

- **Java Development Kit (JDK)** 17+ or 21+ (`javac` and `java` on PATH)
- **MySQL Server** 8.0+ (running on port 3306)

---

## 🚀 Quick Start (Running the Application)

### 1. Configure Database Credentials (Optional)
By default, the server connects to `localhost:3306` with user `root` and blank password for database `lsms`. To override, edit `.env` in the project root:
```ini
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=
DB_NAME=lsms
PORT=3001
```

### 2. Initialize Database (If Not Already Initialized)
```bash
mysql -u root -p < database/schema.sql
mysql -u root -p lsms < database/views.sql
mysql -u root -p lsms < database/seed.sql
```

### 3. Start the Java Web Application Server
From the project root:
```bash
./run_server.sh
```

Or from the `java-jdbc/` directory:
```bash
cd java-jdbc
./build_and_run.sh server
```

The server will compile all Java sources and start listening at:
- **Web Application:** [http://localhost:3001](http://localhost:3001)
- **API Base URL:** [http://localhost:3001/api](http://localhost:3001/api)
- **Documentation:** [http://localhost:3001/docs](http://localhost:3001/docs)

---

## 🧪 Testing & Verification

Run the automated end-to-end verification test suite:
```bash
./test_all_endpoints.sh
```

Run standalone JDBC unit tests:
```bash
cd java-jdbc
./build_and_run.sh test
```

---

## 👥 Demo Login Credentials

| Role | Email | Password | Access / Features |
|---|---|---|---|
| **Admin** | `uplap.vedant@gmail.com` | `password` | Full system control, analytics dashboard, reports, services, inventory, delivery management |
| **Staff** | `jimmyuplap@gmail.com` | `password` | Operations, order status updates, billing, payments, inventory usage logging |
| **Staff** | `rahul.staff@lsms.com` | `password` | Order processing, billing, deliveries |
| **Customer** | `aarav.sharma@gmail.com` | `password` | Self-service order placement, order tracking, invoice view, payment, feedback |
| **Customer** | `meera.patel@gmail.com` | `password` | Order history, placing orders |
| **Driver** | `raj.driver@lsms.com` | `password` | Assigned deliveries, route status updates |

---

## 🎓 Academic Viva & DBMS Defense Guide

For comprehensive defense preparation, see [`docs/MIGRATION_REPORT.md`](file:///Users/vedantuplap/Desktop/Laundry_Management_System/docs/MIGRATION_REPORT.md).

### Key Questions & Answers:
- **Q: Why is a Java backend needed between the browser and MySQL?**  
  **A:** Web browsers operate in a sandboxed JavaScript runtime that can only make HTTP network requests. JDBC is a binary TCP/IP protocol meant for Java applications. The Java Web Server receives HTTP requests, executes JDBC queries via DAOs, and returns JSON responses.
- **Q: How does JDBC prevent SQL Injection?**  
  **A:** All queries utilize `java.sql.PreparedStatement` with `?` parameter placeholders. The SQL structure is pre-compiled by the database engine, ensuring user input cannot alter the query logic.
- **Q: How are transactions implemented?**  
  **A:** Using `conn.setAutoCommit(false)`, executing multiple related operations, calling `conn.commit()` on success, and invoking `conn.rollback()` in the `catch (SQLException e)` block.
