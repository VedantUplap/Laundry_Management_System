# 🧺 Laundry Service Management System (LSMS)

A complete full-stack web application for automating commercial laundry operations.

**Tech Stack:** HTML5/CSS3/JS → Express.js → MySQL 8+ (3NF Normalized)

---

## 📋 Project Overview

LSMS automates the management of a commercial laundry business covering:
- Customer management and digital orders
- Automated billing and payment recording
- Inventory tracking with usage logging
- Delivery agent assignment and tracking
- Customer feedback collection
- Real-time reporting dashboard

---

## ⚙️ Prerequisites

- **Node.js** v18+ (tested on v24)
- **MySQL** 8.0+
- **npm** v8+

---

## 🚀 Setup Instructions

### 1. Clone / Navigate to Project

```bash
cd /Users/vedantuplap/Desktop/Laundry_Management_System
```

### 2. Install Backend Dependencies

```bash
cd backend
npm install
```

### 3. Configure Environment Variables

```bash
# backend/.env already configured with defaults:
# DB_HOST=localhost
# DB_PORT=3306
# DB_USER=root
# DB_PASSWORD=
# DB_NAME=lsms
# PORT=3001
```

Edit `backend/.env` if your MySQL credentials differ.

### 4. Create the Database

```bash
mysql -u root < database/schema.sql
```

### 5. Create SQL Views

```bash
mysql -u root lsms < database/views.sql
```

### 6. Load Sample Data

```bash
mysql -u root lsms < database/seed.sql
```

### 7. Update Passwords (required after seed)

```bash
# Run this once to set bcrypt hashed passwords for all seed users
node -e "
const b=require('./backend/node_modules/bcryptjs');
const mysql=require('./backend/node_modules/mysql2/promise');
async function run() {
  const hash = await b.hash('password', 10);
  const conn = await mysql.createConnection({host:'localhost',user:'root',database:'lsms'});
  await conn.query('UPDATE USERS SET Password=?', [hash]);
  console.log('Passwords updated!');
  conn.end();
}
run();
"
```

### 8. Start the Backend

```bash
cd backend
npm run dev       # Development (with nodemon auto-reload)
# OR
npm start         # Production
```

The server starts at: **http://localhost:3001**

### 9. Open the Application

Open your browser and go to: **http://localhost:3001/index.html**

---

## 🔐 Test Credentials

All seed users share the password: **`password`**

| Role     | Email                      | Access |
|----------|----------------------------|--------|
| Admin    | admin@lsms.com             | Full access to all modules |
| Staff    | priya@lsms.com             | Orders, inventory, billing, delivery |
| Customer | aarav.sharma@gmail.com     | Own orders, billing, feedback |
| Customer | meera.patel@gmail.com      | Own orders, billing, feedback |
| Driver   | raj.driver@lsms.com        | Delivery tracking |

---

## 📁 Project Structure

```
Laundry_Management_System/
├── backend/
│   ├── config/db.js              # MySQL connection pool
│   ├── controllers/              # Business logic (10 controllers)
│   ├── routes/                   # REST API routes (10 route files)
│   ├── middleware/               # JWT auth + RBAC
│   ├── .env                      # Environment config
│   ├── app.js                    # Express app
│   └── server.js                 # Entry point
├── frontend/
│   ├── assets/css/styles.css     # Global design system
│   ├── assets/js/api.js          # Centralized API client
│   ├── index.html                # Login page
│   ├── register.html             # Customer registration
│   ├── admin/                    # Admin portal (8 pages)
│   └── customer/                 # Customer portal (5 pages)
├── database/
│   ├── schema.sql                # 12 tables + triggers + indexes
│   ├── views.sql                 # 6 SQL views
│   └── seed.sql                  # Realistic sample data
├── docs/
│   └── normalization.md          # 3NF normalization documentation
└── README.md
```

---

## 🗄️ Database Schema (12 Tables)

| Table            | Description |
|------------------|-------------|
| `USERS`          | Auth credentials for all users |
| `CUSTOMER`       | Customer profile data |
| `SERVICE_TYPE`   | Laundry services and pricing |
| `ORDERS`         | Main order records |
| `ORDER_DETAILS`  | Line items per order |
| `BILLING`        | Invoice records (1:1 with Orders) |
| `PAYMENT`        | Payment transactions |
| `INVENTORY`      | Consumable stock items |
| `USAGE_LOG`      | Inventory usage per order |
| `DELIVERY_AGENT` | Delivery personnel |
| `DELIVERY`       | Delivery assignments (1:1 with Orders) |
| `FEEDBACK`       | Customer ratings and reviews |

---

## 📡 API Endpoints

```
POST /api/auth/register       Register new customer
POST /api/auth/login          Login (returns JWT)
GET  /api/auth/me             Get current user

GET  /api/customers           List all customers [admin/staff]
GET  /api/customers/:id       Get customer [admin/staff/self]
PUT  /api/customers/:id       Update customer
DELETE /api/customers/:id     Deactivate customer [admin]
GET  /api/customers/:id/orders  Customer order history

GET  /api/services            List services (public)
POST /api/services            Create service [admin/staff]
PUT  /api/services/:id        Update service [admin/staff]
DELETE /api/services/:id      Deactivate service [admin]

GET  /api/orders              List orders
GET  /api/orders/:id          Get order with details
POST /api/orders              Create order (DB transaction)
PUT  /api/orders/:id/status   Update status [admin/staff]
DELETE /api/orders/:id        Cancel order

GET  /api/billing             All bills [admin/staff]
GET  /api/billing/order/:id   Bill for order
POST /api/billing             Generate bill [admin/staff]

GET  /api/payments            All payments [admin/staff]
POST /api/payments            Record payment (updates bill status)

GET  /api/inventory           Stock list [admin/staff]
GET  /api/inventory/usage     Usage log
POST /api/inventory           Add item
PUT  /api/inventory/:id       Update item
DELETE /api/inventory/:id     Delete item
POST /api/inventory/:id/usage Log usage (reduces stock via trigger)

GET  /api/delivery-agents     List agents [admin/staff]
POST /api/delivery-agents     Add agent [admin]
PUT  /api/delivery-agents/:id Update agent

GET  /api/deliveries          List deliveries
PUT  /api/deliveries/:id      Assign agent / update status

GET  /api/feedback            All feedback [admin/staff]
GET  /api/feedback/my         My feedback [customer]
POST /api/feedback            Submit feedback [customer]

GET  /api/dashboard/stats     Live dashboard numbers

GET  /api/reports/daily-revenue
GET  /api/reports/monthly-revenue
GET  /api/reports/pending-deliveries
GET  /api/reports/customer-history/:id
GET  /api/reports/inventory-usage
GET  /api/reports/popular-services
GET  /api/reports/outstanding-payments
```

---

## 🎓 DBMS Concepts Demonstrated

| Concept | Where Used |
|---------|-----------|
| Relational Model | 12 normalized tables with FK relationships |
| 3NF Normalization | All tables — no repeating groups, no transitive deps |
| Primary Keys | All tables |
| Foreign Keys | CUSTOMER→USERS, ORDERS→CUSTOMER, ORDER_DETAILS→{ORDERS,SERVICE_TYPE}, BILLING→ORDERS, PAYMENT→BILLING, USAGE_LOG→{ORDERS,INVENTORY}, DELIVERY→{ORDERS,DELIVERY_AGENT}, FEEDBACK→{CUSTOMER,ORDERS} |
| CHECK Constraints | Rating (1-5), Price ≥ 0, Quantity > 0, Weight > 0, AmountPaid > 0 |
| UNIQUE Constraints | Email, Username, OrderID→Billing (1:1), OrderID→Delivery (1:1), OrderID→Feedback (1:1) |
| Transactions | Order creation (Order + Details + Delivery atomically), Payment recording |
| Triggers | Auto-recalculate TotalWeight/TotalCost on ORDER_DETAILS insert/update/delete; auto-reduce INVENTORY on USAGE_LOG insert |
| SQL Views | 6 views: customer_order_history, pending_deliveries, outstanding_payments, inventory_status, service_popularity, daily_revenue |
| JOINs | All report and list queries use multi-table JOINs |
| Aggregate Functions | COUNT(), SUM(), AVG(), GROUP BY in all report endpoints |
| Referential Integrity | ON UPDATE CASCADE / ON DELETE RESTRICT where appropriate |
| Indexes | 13 indexes on FK and frequently-filtered columns |

---

## 📱 Frontend Pages

**Admin Portal:**
- `admin/dashboard.html` — Stats, charts, recent orders, low stock
- `admin/customers.html` — Customer list with search, edit, order history
- `admin/orders.html` — Order management with status updates
- `admin/services.html` — Service pricing configuration
- `admin/inventory.html` — Stock management + usage logging
- `admin/delivery.html` — Delivery tracking + agent management
- `admin/payments.html` — Billing overview + payment recording
- `admin/reports.html` — 6 SQL-powered reports

**Customer Portal:**
- `customer/dashboard.html` — Personal overview
- `customer/place-order.html` — Multi-item order builder
- `customer/orders.html` — Order history with status tracking
- `customer/invoice.html` — Printable invoice
- `customer/feedback.html` — Star rating feedback

---

## 🔄 Order Workflow

```
Customer Places Order → Pending
      ↓
Staff Picks Up → Picked Up → Received → Processing
      ↓
Washing → Drying → Ironing → Packed  ← Bill auto-generated here
      ↓
Ready for Delivery → Out for Delivery → Delivered
      ↓
Customer can submit Feedback
```

---

## 📞 Support

This is a college DBMS project. For issues:
1. Check MySQL is running: `mysql.server start`
2. Check backend logs in terminal
3. Verify `.env` credentials match your MySQL setup
