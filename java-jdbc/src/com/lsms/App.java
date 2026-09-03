package com.lsms;

import com.lsms.config.DBConnection;
import com.lsms.dao.CustomerDAO;
import com.lsms.dao.OrderDAO;
import com.lsms.dao.ReportDAO;
import com.lsms.dao.UserDAO;
import com.lsms.models.Customer;
import com.lsms.models.Order;
import com.lsms.models.ServiceType;
import com.lsms.models.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

/**
 * App — Main Interactive Console Application demonstrating JDBC for Laundry Service Management System.
 */
public class App {

    private static final UserDAO userDAO = new UserDAO();
    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final OrderDAO orderDAO = new OrderDAO();
    private static final ReportDAO reportDAO = new ReportDAO();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("    🧺 LSMS — Java Database Connectivity (JDBC) Console App    ");
        System.out.println("===============================================================");

        if (!DBConnection.testConnection()) {
            System.err.println("❌ Could not connect to MySQL database 'lsms'.");
            System.err.println("Please check DBConnection.java credentials and ensure MySQL is running.");
            return;
        }
        System.out.println("✅ Successfully connected to MySQL database via JDBC Driver!\n");

        boolean exit = false;
        while (!exit) {
            System.out.println("------------------------- MAIN MENU ---------------------------");
            System.out.println("1. View Dashboard & Revenue Stats (JDBC Statement)");
            System.out.println("2. View All Orders (JDBC JOIN Query)");
            System.out.println("3. View All Customers (JDBC PreparedStatement)");
            System.out.println("4. View All Registered Users (Role & Auth Check)");
            System.out.println("5. View Active Laundry Services");
            System.out.println("6. Place New Order (JDBC Transaction with Rollback Protection)");
            System.out.println("7. Update Order Status (JDBC PreparedStatement)");
            System.out.println("8. View Monthly Revenue Report");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    reportDAO.printDashboardSummary();
                    break;
                case "2":
                    viewAllOrders();
                    break;
                case "3":
                    viewAllCustomers();
                    break;
                case "4":
                    viewAllUsers();
                    break;
                case "5":
                    viewAllServices();
                    break;
                case "6":
                    placeNewOrder();
                    break;
                case "7":
                    updateOrderStatus();
                    break;
                case "8":
                    reportDAO.printMonthlyRevenueReport();
                    break;
                case "0":
                    exit = true;
                    System.out.println("\nThank you for using LSMS JDBC Console. Goodbye!");
                    break;
                default:
                    System.out.println("⚠️ Invalid choice. Please try again.\n");
            }
        }
    }

    private static void viewAllOrders() {
        System.out.println("\n--- All Laundry Orders (via JDBC) ---");
        List<Order> orders = orderDAO.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("No orders found.");
        } else {
            for (Order o : orders) {
                System.out.println(o);
            }
        }
        System.out.println();
    }

    private static void viewAllCustomers() {
        System.out.println("\n--- All Customers (via JDBC) ---");
        List<Customer> customers = customerDAO.getAllCustomers();
        for (Customer c : customers) {
            System.out.println(c);
        }
        System.out.println();
    }

    private static void viewAllUsers() {
        System.out.println("\n--- Registered Users in Database (via JDBC) ---");
        List<User> users = userDAO.getAllUsers();
        for (User u : users) {
            System.out.println(u);
        }
        System.out.println();
    }

    private static void viewAllServices() {
        System.out.println("\n--- Active Laundry Services ---");
        List<ServiceType> services = orderDAO.getAllServices();
        for (ServiceType s : services) {
            System.out.println(s);
        }
        System.out.println();
    }

    private static void placeNewOrder() {
        System.out.println("\n--- Place New Order (Atomic JDBC Transaction) ---");
        viewAllCustomers();
        System.out.print("Enter Customer ID: ");
        int custId = Integer.parseInt(scanner.nextLine().trim());

        viewAllServices();
        System.out.print("Enter Service ID: ");
        int serviceId = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Enter Garment Type (e.g., Shirts, Suits, Sarees): ");
        String garmentType = scanner.nextLine().trim();

        System.out.print("Enter Quantity: ");
        int qty = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Enter Estimated Weight (in kg, e.g. 2.5): ");
        BigDecimal weight = new BigDecimal(scanner.nextLine().trim());

        System.out.print("Enter Total Service Cost (₹): ");
        BigDecimal cost = new BigDecimal(scanner.nextLine().trim());

        System.out.print("Enter Notes (optional): ");
        String notes = scanner.nextLine().trim();

        int orderId = orderDAO.createOrderWithItem(custId, serviceId, garmentType, qty, weight, cost, notes);
        if (orderId > 0) {
            System.out.printf("✅ Order #%d created successfully with JDBC Transaction commit!\n\n", orderId);
        } else {
            System.err.println("❌ Failed to create order. JDBC Transaction was rolled back.\n");
        }
    }

    private static void updateOrderStatus() {
        System.out.println("\n--- Update Order Status ---");
        System.out.print("Enter Order ID: ");
        int orderId = Integer.parseInt(scanner.nextLine().trim());

        System.out.println("Available Statuses: Pending | Washing | Ironing | Ready for Delivery | Delivered | Cancelled");
        System.out.print("Enter New Status: ");
        String status = scanner.nextLine().trim();

        boolean updated = orderDAO.updateOrderStatus(orderId, status);
        if (updated) {
            System.out.printf("✅ Order #%d status updated to '%s' via PreparedStatement!\n\n", orderId, status);
        } else {
            System.err.println("❌ Failed to update order status. Please verify Order ID.\n");
        }
    }
}
