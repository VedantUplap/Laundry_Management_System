# LSMS — Java Database Connectivity (JDBC) Module

This module provides a complete, standalone **Java Database Connectivity (JDBC)** implementation for the **Laundry Service Management System (LSMS)**. It connects directly to the MySQL database (`lsms`) and demonstrates all key JDBC concepts required for academic DBMS / Java lab submissions and viva presentations.

---

## 🏗️ Architecture & JDBC Components

```
java-jdbc/
├── lib/
│   └── mysql-connector-j-8.3.0.jar      # Official MySQL Type-4 JDBC Driver
├── src/
│   └── com/lsms/
│       ├── config/
│       │   └── DBConnection.java        # JDBC Connection Manager (DriverManager)
│       ├── models/
│       │   ├── User.java                # USERS Entity Model
│       │   ├── Customer.java            # CUSTOMER Entity Model
│       │   ├── Order.java               # ORDERS Entity Model
│       │   └── ServiceType.java         # SERVICE_TYPE Entity Model
│       ├── dao/
│       │   ├── UserDAO.java             # PreparedStatement Auth & User Retrieval
│       │   ├── CustomerDAO.java         # CRUD Operations with PreparedStatement
│       │   ├── OrderDAO.java            # JDBC Transactions (commit, rollback)
│       │   └── ReportDAO.java           # Aggregates, GROUP BY & Analytics
│       ├── App.java                     # Interactive CLI Console Application
│       └── TestJDBC.java                # Automated Test Runner
└── build_and_run.sh                     # 1-Click Build & Execution Script
```

---

## 🔑 Key JDBC Concepts Demonstrated

### 1. JDBC Driver Loading & Connection Management (`DBConnection.java`)
- Loads the Type-4 pure Java driver `com.mysql.cj.jdbc.Driver`
- Connects using `DriverManager.getConnection(url, user, password)`
- Uses try-with-resources (`AutoCloseable`) to prevent connection leaks

### 2. SQL Injection Prevention via `PreparedStatement` (`UserDAO.java`, `CustomerDAO.java`)
- Parameterized queries using `pstmt.setString(1, email)` and `pstmt.setInt(...)`

### 3. JDBC Transaction Management with Commit & Rollback (`OrderDAO.java`)
- `conn.setAutoCommit(false)` — Initiates atomic multi-table transaction
- `conn.commit()` — Commits order header + order details simultaneously
- `conn.rollback()` — Automatically rolls back if any step fails

### 4. Reading Results via `ResultSet` and Data Mapping
- Mapping SQL column types (`getInt`, `getString`, `getBigDecimal`, `getDate`, `getTimestamp`) to Java POJOs

### 5. Database Metadata Inspection (`TestJDBC.java`)
- Using `DatabaseMetaData` to inspect database product version, driver details, and URL

---

## 🚀 How to Run the JDBC Application

### 1. Automated Verification Test (Quick Check)
```bash
cd java-jdbc
./build_and_run.sh test
```

### 2. Interactive Console Application
```bash
cd java-jdbc
./build_and_run.sh
```

---

## 📋 Viva / Defense Q&A Quick Reference

**Q: What Type of JDBC Driver is used?**  
**A:** MySQL Connector/J is a **Type 4 JDBC Driver** (Native-protocol, pure Java driver). It converts JDBC calls directly into the MySQL network protocol without requiring any native client libraries.

**Q: Where is transaction management handled in your JDBC code?**  
**A:** In `OrderDAO.java` (`createOrderWithItem`). We disable auto-commit with `conn.setAutoCommit(false)`, insert the order, insert the line items, and call `conn.commit()`. In case of an exception, `conn.rollback()` is triggered.

**Q: How are SQL injection attacks prevented?**  
**A:** All dynamic user inputs are passed using `java.sql.PreparedStatement` with placeholder `?` parameters, ensuring inputs are pre-compiled and treated strictly as literal data.
