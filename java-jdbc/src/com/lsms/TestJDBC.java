package com.lsms;

import com.lsms.config.DBConnection;
import com.lsms.dao.CustomerDAO;
import com.lsms.dao.OrderDAO;
import com.lsms.dao.ReportDAO;
import com.lsms.dao.UserDAO;
import com.lsms.models.Customer;
import com.lsms.models.Order;
import com.lsms.models.User;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;

/**
 * TestJDBC — Automated test suite to verify JDBC driver, connection pool, and DAO queries.
 */
public class TestJDBC {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("          LSMS — Automated JDBC Verification Test              ");
        System.out.println("===============================================================");

        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("✅ JDBC Connection Successful!");

            DatabaseMetaData metaData = conn.getMetaData();
            System.out.println("   Driver Name    : " + metaData.getDriverName());
            System.out.println("   Driver Version : " + metaData.getDriverVersion());
            System.out.println("   Database Name  : " + metaData.getDatabaseProductName());
            System.out.println("   Database Version: " + metaData.getDatabaseProductVersion());
            System.out.println("   URL            : " + metaData.getURL());

            System.out.println("\n1. Testing UserDAO (JDBC PreparedStatement)...");
            UserDAO userDAO = new UserDAO();
            List<User> users = userDAO.getAllUsers();
            System.out.printf("   Found %d users.\n", users.size());
            for (User u : users) {
                System.out.println("   " + u);
            }

            System.out.println("\n2. Testing CustomerDAO (JDBC Statement)...");
            CustomerDAO customerDAO = new CustomerDAO();
            List<Customer> customers = customerDAO.getAllCustomers();
            System.out.printf("   Found %d customers.\n", customers.size());
            for (Customer c : customers) {
                System.out.println("   " + c);
            }

            System.out.println("\n3. Testing OrderDAO (JDBC JOIN Query)...");
            OrderDAO orderDAO = new OrderDAO();
            List<Order> orders = orderDAO.getAllOrders();
            System.out.printf("   Found %d orders.\n", orders.size());
            for (Order o : orders) {
                System.out.println("   " + o);
            }

            System.out.println("\n4. Testing ReportDAO (JDBC Aggregates)...");
            ReportDAO reportDAO = new ReportDAO();
            reportDAO.printDashboardSummary();
            reportDAO.printMonthlyRevenueReport();

            System.out.println("🎉 All JDBC tests executed successfully without errors!\n");

        } catch (Exception e) {
            System.err.println("❌ JDBC Test Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
