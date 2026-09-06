package com.lsms.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lsms.dao.*;
import com.lsms.security.JwtUtil;
import com.lsms.security.PasswordUtil;
import com.lsms.security.UserPrincipal;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

/**
 * ApiDispatcher — Central REST API router for LSMS.
 * Dispatches all frontend /api/* calls to Java DAOs, enforces RBAC, and returns JSON.
 */
public class ApiDispatcher implements HttpHandler {

    private final Gson gson = new Gson();

    private final UserDAO userDAO = new UserDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final BillingDAO billingDAO = new BillingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final DeliveryAgentDAO deliveryAgentDAO = new DeliveryAgentDAO();
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final ReportDAO reportDAO = new ReportDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Add CORS headers to all API responses
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String method = exchange.getRequestMethod().toUpperCase();
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());

        try {
            dispatch(exchange, method, path, queryParams);
        } catch (Exception e) {
            System.err.println("API Error on " + method + " " + path + ": " + e.getMessage());
            e.printStackTrace();
            sendJson(exchange, 500, errorResponse(e.getMessage() != null ? e.getMessage() : "Internal server error."));
        }
    }

    private void dispatch(HttpExchange exchange, String method, String path, Map<String, String> q) throws Exception {
        // Remove trailing slash if needed
        if (path.length() > 5 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // =========================================================================
        // 1. AUTH ROUTES (/api/auth/...)
        // =========================================================================
        if (path.startsWith("/api/auth")) {
            if ("/api/auth/register".equals(path) && "POST".equals(method)) {
                handleRegister(exchange);
                return;
            }
            if ("/api/auth/login".equals(path) && "POST".equals(method)) {
                handleLogin(exchange);
                return;
            }
            if ("/api/auth/me".equals(path) && "GET".equals(method)) {
                UserPrincipal user = requireAuth(exchange);
                if (user == null) return;
                Map<String, Object> profile = userDAO.getUserProfile(user.getUserId());
                if (profile == null) {
                    sendJson(exchange, 404, errorResponse("User not found."));
                } else {
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    res.put("user", profile);
                    sendJson(exchange, 200, res);
                }
                return;
            }
        }

        // =========================================================================
        // 2. SERVICE ROUTES (/api/services/...)
        // =========================================================================
        if (path.equals("/api/services") || path.startsWith("/api/services/")) {
            if ("/api/services".equals(path)) {
                if ("GET".equals(method)) {
                    boolean activeOnly = "true".equalsIgnoreCase(q.get("active"));
                    sendJson(exchange, 200, successData(serviceDAO.getAllServices(activeOnly)));
                    return;
                }
                if ("POST".equals(method)) {
                    UserPrincipal user = requireRole(exchange, "admin", "staff");
                    if (user == null) return;
                    JsonObject body = parseBody(exchange);
                    if (!body.has("serviceName") || !body.has("pricePerKg")) {
                        sendJson(exchange, 400, errorResponse("serviceName and pricePerKg are required."));
                        return;
                    }
                    String name = body.get("serviceName").getAsString();
                    String desc = body.has("description") && !body.get("description").isJsonNull() ? body.get("description").getAsString() : null;
                    BigDecimal price = body.get("pricePerKg").getAsBigDecimal();
                    try {
                        int id = serviceDAO.createService(name, desc, price);
                        Map<String, Object> res = new HashMap<>();
                        res.put("success", true);
                        res.put("message", "Service created.");
                        res.put("serviceID", id);
                        sendJson(exchange, 201, res);
                    } catch (SQLException ex) {
                        if (ex.getErrorCode() == 1062) {
                            sendJson(exchange, 409, errorResponse("Service name already exists."));
                        } else throw ex;
                    }
                    return;
                }
            }

            // /api/services/:id
            String[] segs = path.split("/");
            if (segs.length == 4) {
                int serviceId = Integer.parseInt(segs[3]);
                if ("GET".equals(method)) {
                    Map<String, Object> svc = serviceDAO.getServiceById(serviceId);
                    if (svc == null) sendJson(exchange, 404, errorResponse("Service not found."));
                    else sendJson(exchange, 200, successData(svc));
                    return;
                }
                if ("PUT".equals(method)) {
                    UserPrincipal user = requireRole(exchange, "admin", "staff");
                    if (user == null) return;
                    JsonObject body = parseBody(exchange);
                    String name = body.has("serviceName") && !body.get("serviceName").isJsonNull() ? body.get("serviceName").getAsString() : null;
                    String desc = body.has("description") && !body.get("description").isJsonNull() ? body.get("description").getAsString() : null;
                    BigDecimal price = body.has("pricePerKg") && !body.get("pricePerKg").isJsonNull() ? body.get("pricePerKg").getAsBigDecimal() : null;
                    Integer isActive = body.has("isActive") && !body.get("isActive").isJsonNull() ? body.get("isActive").getAsInt() : null;

                    boolean ok = serviceDAO.updateService(serviceId, name, desc, price, isActive);
                    if (!ok) sendJson(exchange, 404, errorResponse("Service not found."));
                    else sendJson(exchange, 200, successMessage("Service updated."));
                    return;
                }
                if ("DELETE".equals(method)) {
                    UserPrincipal user = requireRole(exchange, "admin");
                    if (user == null) return;
                    boolean ok = serviceDAO.deleteService(serviceId);
                    if (!ok) sendJson(exchange, 404, errorResponse("Service not found."));
                    else sendJson(exchange, 200, successMessage("Service deactivated."));
                    return;
                }
            }
        }

        // =========================================================================
        // 3. CUSTOMER ROUTES (/api/customers/...)
        // =========================================================================
        if (path.equals("/api/customers") || path.startsWith("/api/customers/")) {
            UserPrincipal user = requireAuth(exchange);
            if (user == null) return;

            if ("/api/customers".equals(path) && "GET".equals(method)) {
                if (!user.hasRole("admin", "staff")) {
                    sendForbidden(exchange, "admin or staff");
                    return;
                }
                String search = q.get("search");
                sendJson(exchange, 200, successData(customerDAO.getAllCustomers(search)));
                return;
            }

            String[] segs = path.split("/");
            if (segs.length == 4) { // /api/customers/:id
                int custId = Integer.parseInt(segs[3]);
                if ("GET".equals(method)) {
                    if (user.hasRole("customer") && (user.getCustomerId() == null || user.getCustomerId() != custId)) {
                        sendJson(exchange, 403, errorResponse("Access denied."));
                        return;
                    }
                    Map<String, Object> c = customerDAO.getCustomerById(custId);
                    if (c == null) sendJson(exchange, 404, errorResponse("Customer not found."));
                    else sendJson(exchange, 200, successData(c));
                    return;
                }
                if ("PUT".equals(method)) {
                    if (user.hasRole("customer") && (user.getCustomerId() == null || user.getCustomerId() != custId)) {
                        sendJson(exchange, 403, errorResponse("Access denied."));
                        return;
                    }
                    JsonObject body = parseBody(exchange);
                    String fn = body.has("firstName") ? body.get("firstName").getAsString() : "";
                    String ln = body.has("lastName") ? body.get("lastName").getAsString() : "";
                    String ph = body.has("phone") ? body.get("phone").getAsString() : "";
                    String ad = body.has("address") ? body.get("address").getAsString() : "";
                    boolean ok = customerDAO.updateCustomer(custId, fn, ln, ph, ad);
                    if (!ok) sendJson(exchange, 404, errorResponse("Customer not found."));
                    else sendJson(exchange, 200, successMessage("Customer updated successfully."));
                    return;
                }
                if ("DELETE".equals(method)) {
                    if (!user.hasRole("admin")) {
                        sendForbidden(exchange, "admin");
                        return;
                    }
                    boolean ok = customerDAO.deactivateCustomer(custId);
                    if (!ok) sendJson(exchange, 404, errorResponse("Customer not found."));
                    else sendJson(exchange, 200, successMessage("Customer deactivated."));
                    return;
                }
            }

            if (segs.length == 5 && "orders".equals(segs[4]) && "GET".equals(method)) { // /api/customers/:id/orders
                int custId = Integer.parseInt(segs[3]);
                if (user.hasRole("customer") && (user.getCustomerId() == null || user.getCustomerId() != custId)) {
                    sendJson(exchange, 403, errorResponse("Access denied."));
                    return;
                }
                sendJson(exchange, 200, successData(customerDAO.getCustomerOrders(custId)));
                return;
            }
        }

        // =========================================================================
        // 4. ORDER ROUTES (/api/orders/...)
        // =========================================================================
        if (path.equals("/api/orders") || path.startsWith("/api/orders/")) {
            UserPrincipal user = requireAuth(exchange);
            if (user == null) return;

            if ("/api/orders".equals(path)) {
                if ("GET".equals(method)) {
                    String status = q.get("status");
                    String from = q.get("from");
                    String to = q.get("to");
                    Integer custFilter = null;

                    if (user.hasRole("customer")) {
                        custFilter = user.getCustomerId();
                    } else if (q.containsKey("customerID") && !q.get("customerID").isBlank()) {
                        custFilter = Integer.parseInt(q.get("customerID"));
                    }

                    sendJson(exchange, 200, successData(orderDAO.getAllOrders(status, custFilter, from, to)));
                    return;
                }
                if ("POST".equals(method)) {
                    JsonObject body = parseBody(exchange);
                    int custId = body.get("customerID").getAsInt();
                    if (user.hasRole("customer") && (user.getCustomerId() == null || user.getCustomerId() != custId)) {
                        sendJson(exchange, 403, errorResponse("Access denied."));
                        return;
                    }

                    String pickupDate = body.has("pickupDate") && !body.get("pickupDate").isJsonNull() ? body.get("pickupDate").getAsString() : null;
                    String notes = body.has("notes") && !body.get("notes").isJsonNull() ? body.get("notes").getAsString() : null;

                    JsonArray arr = body.getAsJsonArray("details");
                    if (arr == null || arr.isEmpty()) {
                        sendJson(exchange, 400, errorResponse("At least one order detail is required."));
                        return;
                    }

                    List<Map<String, Object>> details = new ArrayList<>();
                    for (JsonElement el : arr) {
                        JsonObject obj = el.getAsJsonObject();
                        Map<String, Object> d = new HashMap<>();
                        d.put("serviceID", obj.get("serviceID").getAsInt());
                        d.put("garmentType", obj.get("garmentType").getAsString());
                        d.put("quantity", obj.get("quantity").getAsInt());
                        d.put("weightKg", obj.get("weightKg").getAsBigDecimal());
                        details.add(d);
                    }

                    Map<String, Object> result = orderDAO.createOrder(custId, pickupDate, notes, details);
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    res.put("message", "Order placed successfully.");
                    res.put("orderID", result.get("orderID"));
                    res.put("totalWeight", result.get("totalWeight"));
                    res.put("totalCost", result.get("totalCost"));
                    sendJson(exchange, 201, res);
                    return;
                }
            }

            String[] segs = path.split("/");
            if (segs.length == 4) { // /api/orders/:id
                int orderId = Integer.parseInt(segs[3]);
                if ("GET".equals(method)) {
                    Map<String, Object> o = orderDAO.getOrderById(orderId);
                    if (o == null) {
                        sendJson(exchange, 404, errorResponse("Order not found."));
                        return;
                    }
                    if (user.hasRole("customer") && !Objects.equals(user.getCustomerId(), o.get("CustomerID"))) {
                        sendJson(exchange, 403, errorResponse("Access denied."));
                        return;
                    }
                    sendJson(exchange, 200, successData(o));
                    return;
                }
                if ("DELETE".equals(method)) {
                    Map<String, Object> o = orderDAO.getOrderById(orderId);
                    if (o == null) {
                        sendJson(exchange, 404, errorResponse("Order not found."));
                        return;
                    }
                    if (user.hasRole("customer") && !Objects.equals(user.getCustomerId(), o.get("CustomerID"))) {
                        sendJson(exchange, 403, errorResponse("Access denied."));
                        return;
                    }
                    try {
                        boolean ok = orderDAO.cancelOrder(orderId);
                        if (ok) sendJson(exchange, 200, successMessage("Order cancelled."));
                        else sendJson(exchange, 404, errorResponse("Order not found."));
                    } catch (IllegalStateException ise) {
                        sendJson(exchange, 400, errorResponse(ise.getMessage()));
                    }
                    return;
                }
            }

            if (segs.length == 5 && "status".equals(segs[4]) && "PUT".equals(method)) { // /api/orders/:id/status
                if (!user.hasRole("admin", "staff")) {
                    sendForbidden(exchange, "admin or staff");
                    return;
                }
                int orderId = Integer.parseInt(segs[3]);
                JsonObject body = parseBody(exchange);
                String newStatus = body.get("status").getAsString();

                boolean ok = orderDAO.updateOrderStatus(orderId, newStatus);
                if (ok) sendJson(exchange, 200, successMessage("Order status updated to \"" + newStatus + "\"."));
                else sendJson(exchange, 404, errorResponse("Order not found."));
                return;
            }
        }

        // =========================================================================
        // 5. BILLING ROUTES (/api/billing/...)
        // =========================================================================
        if (path.equals("/api/billing") || path.startsWith("/api/billing/")) {
            UserPrincipal user = requireAuth(exchange);
            if (user == null) return;

            if ("/api/billing".equals(path)) {
                if ("GET".equals(method)) {
                    if (!user.hasRole("admin", "staff")) {
                        sendForbidden(exchange, "admin or staff");
                        return;
                    }
                    sendJson(exchange, 200, successData(billingDAO.getAllBilling()));
                    return;
                }
                if ("POST".equals(method)) {
                    if (!user.hasRole("admin", "staff")) {
                        sendForbidden(exchange, "admin or staff");
                        return;
                    }
                    JsonObject body = parseBody(exchange);
                    int orderId = body.get("orderID").getAsInt();
                    String dueDate = body.has("dueDate") && !body.get("dueDate").isJsonNull() ? body.get("dueDate").getAsString() : null;

                    int billId = billingDAO.createBilling(orderId, dueDate);
                    if (billId == -2) sendJson(exchange, 409, errorResponse("Bill already exists for this order."));
                    else if (billId == -1) sendJson(exchange, 404, errorResponse("Order not found."));
                    else {
                        Map<String, Object> res = new HashMap<>();
                        res.put("success", true);
                        res.put("message", "Bill created.");
                        res.put("billID", billId);
                        sendJson(exchange, 201, res);
                    }
                    return;
                }
            }

            // /api/billing/order/:orderId
            String[] segs = path.split("/");
            if (segs.length == 5 && "order".equals(segs[3]) && "GET".equals(method)) {
                int orderId = Integer.parseInt(segs[4]);
                Map<String, Object> bill = billingDAO.getBillingByOrderId(orderId);
                if (bill == null) {
                    sendJson(exchange, 404, errorResponse("Bill not found for this order."));
                    return;
                }
                if (user.hasRole("customer") && !Objects.equals(user.getCustomerId(), bill.get("CustomerID"))) {
                    sendJson(exchange, 403, errorResponse("Access denied."));
                    return;
                }
                sendJson(exchange, 200, successData(bill));
                return;
            }
        }

        // =========================================================================
        // 6. PAYMENT ROUTES (/api/payments/...)
        // =========================================================================
        if (path.equals("/api/payments") || path.startsWith("/api/payments/")) {
            UserPrincipal user = requireAuth(exchange);
            if (user == null) return;

            if ("/api/payments".equals(path)) {
                if ("GET".equals(method)) {
                    if (!user.hasRole("admin", "staff")) {
                        sendForbidden(exchange, "admin or staff");
                        return;
                    }
                    sendJson(exchange, 200, successData(paymentDAO.getAllPayments()));
                    return;
                }
                if ("POST".equals(method)) {
                    JsonObject body = parseBody(exchange);
                    int billId = body.get("billID").getAsInt();
                    BigDecimal amountPaid = body.get("amountPaid").getAsBigDecimal();
                    String methodPaid = body.get("paymentMethod").getAsString();
                    String txId = body.has("transactionID") && !body.get("transactionID").isJsonNull() ? body.get("transactionID").getAsString() : null;

                    if (amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
                        sendJson(exchange, 400, errorResponse("Amount must be positive."));
                        return;
                    }

                    try {
                        Map<String, Object> result = paymentDAO.createPayment(billId, amountPaid, methodPaid, txId);
                        Map<String, Object> res = new HashMap<>();
                        res.put("success", true);
                        res.put("message", "Payment recorded successfully.");
                        res.putAll(result);
                        sendJson(exchange, 201, res);
                    } catch (IllegalArgumentException iae) {
                        sendJson(exchange, 404, errorResponse(iae.getMessage()));
                    } catch (IllegalStateException ise) {
                        sendJson(exchange, 400, errorResponse(ise.getMessage()));
                    }
                    return;
                }
            }
        }

        // =========================================================================
        // 7. INVENTORY ROUTES (/api/inventory/...)
        // =========================================================================
        if (path.equals("/api/inventory") || path.startsWith("/api/inventory/")) {
            UserPrincipal user = requireRole(exchange, "admin", "staff");
            if (user == null) return;

            if ("/api/inventory".equals(path)) {
                if ("GET".equals(method)) {
                    sendJson(exchange, 200, successData(inventoryDAO.getAllInventory()));
                    return;
                }
                if ("POST".equals(method)) {
                    JsonObject body = parseBody(exchange);
                    String name = body.get("itemName").getAsString();
                    String unit = body.get("unit").getAsString();
                    BigDecimal qty = body.has("quantityAvailable") ? body.get("quantityAvailable").getAsBigDecimal() : BigDecimal.ZERO;
                    BigDecimal reorder = body.has("reorderLevel") ? body.get("reorderLevel").getAsBigDecimal() : BigDecimal.ZERO;
                    BigDecimal cost = body.has("costPerUnit") ? body.get("costPerUnit").getAsBigDecimal() : BigDecimal.ZERO;

                    try {
                        int id = inventoryDAO.createInventoryItem(name, unit, qty, reorder, cost);
                        Map<String, Object> res = new HashMap<>();
                        res.put("success", true);
                        res.put("message", "Inventory item added.");
                        res.put("itemID", id);
                        sendJson(exchange, 201, res);
                    } catch (SQLException ex) {
                        if (ex.getErrorCode() == 1062) sendJson(exchange, 409, errorResponse("Item name already exists."));
                        else throw ex;
                    }
                    return;
                }
            }

            if ("/api/inventory/usage".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, successData(inventoryDAO.getUsageLogs()));
                return;
            }

            String[] segs = path.split("/");
            if (segs.length == 4) { // /api/inventory/:id
                int itemId = Integer.parseInt(segs[3]);
                if ("GET".equals(method)) {
                    Map<String, Object> item = inventoryDAO.getInventoryById(itemId);
                    if (item == null) sendJson(exchange, 404, errorResponse("Item not found."));
                    else sendJson(exchange, 200, successData(item));
                    return;
                }
                if ("PUT".equals(method)) {
                    JsonObject body = parseBody(exchange);
                    String name = body.has("itemName") && !body.get("itemName").isJsonNull() ? body.get("itemName").getAsString() : null;
                    String unit = body.has("unit") && !body.get("unit").isJsonNull() ? body.get("unit").getAsString() : null;
                    BigDecimal qty = body.has("quantityAvailable") && !body.get("quantityAvailable").isJsonNull() ? body.get("quantityAvailable").getAsBigDecimal() : null;
                    BigDecimal reorder = body.has("reorderLevel") && !body.get("reorderLevel").isJsonNull() ? body.get("reorderLevel").getAsBigDecimal() : null;
                    BigDecimal cost = body.has("costPerUnit") && !body.get("costPerUnit").isJsonNull() ? body.get("costPerUnit").getAsBigDecimal() : null;

                    boolean ok = inventoryDAO.updateInventoryItem(itemId, name, unit, qty, reorder, cost);
                    if (!ok) sendJson(exchange, 404, errorResponse("Item not found."));
                    else sendJson(exchange, 200, successMessage("Inventory updated."));
                    return;
                }
                if ("DELETE".equals(method)) {
                    try {
                        boolean ok = inventoryDAO.deleteInventoryItem(itemId);
                        if (!ok) sendJson(exchange, 404, errorResponse("Item not found."));
                        else sendJson(exchange, 200, successMessage("Item deleted."));
                    } catch (SQLException ex) {
                        if (ex.getErrorCode() == 1451) {
                            sendJson(exchange, 400, errorResponse("Cannot delete item with usage history."));
                        } else throw ex;
                    }
                    return;
                }
            }

            if (segs.length == 5 && "usage".equals(segs[4]) && "POST".equals(method)) { // /api/inventory/:id/usage
                int itemId = Integer.parseInt(segs[3]);
                JsonObject body = parseBody(exchange);
                int orderId = body.get("orderID").getAsInt();
                BigDecimal qtyUsed = body.get("quantityUsed").getAsBigDecimal();

                try {
                    BigDecimal remaining = inventoryDAO.logUsage(itemId, orderId, qtyUsed);
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    res.put("message", "Usage logged.");
                    res.put("remainingQty", remaining);
                    sendJson(exchange, 201, res);
                } catch (IllegalArgumentException iae) {
                    sendJson(exchange, 404, errorResponse(iae.getMessage()));
                } catch (IllegalStateException ise) {
                    sendJson(exchange, 400, errorResponse(ise.getMessage()));
                }
                return;
            }
        }

        // =========================================================================
        // 8. DELIVERY AGENT ROUTES (/api/delivery-agents/...)
        // =========================================================================
        if (path.equals("/api/delivery-agents") || path.startsWith("/api/delivery-agents/")) {
            UserPrincipal user = requireRole(exchange, "admin", "staff", "driver");
            if (user == null) return;

            if ("/api/delivery-agents".equals(path)) {
                if ("GET".equals(method)) {
                    sendJson(exchange, 200, successData(deliveryAgentDAO.getAllAgents()));
                    return;
                }
                if ("POST".equals(method)) {
                    if (!user.hasRole("admin")) {
                        sendForbidden(exchange, "admin");
                        return;
                    }
                    JsonObject body = parseBody(exchange);
                    String name = body.get("agentName").getAsString();
                    String phone = body.get("phone").getAsString();
                    String veh = body.has("vehicleNumber") && !body.get("vehicleNumber").isJsonNull() ? body.get("vehicleNumber").getAsString() : null;

                    int id = deliveryAgentDAO.createAgent(name, phone, veh);
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    res.put("message", "Agent added.");
                    res.put("agentID", id);
                    sendJson(exchange, 201, res);
                    return;
                }
            }

            String[] segs = path.split("/");
            if (segs.length == 4 && "PUT".equals(method)) { // /api/delivery-agents/:id
                if (!user.hasRole("admin", "staff")) {
                    sendForbidden(exchange, "admin or staff");
                    return;
                }
                int agentId = Integer.parseInt(segs[3]);
                JsonObject body = parseBody(exchange);
                String name = body.has("agentName") && !body.get("agentName").isJsonNull() ? body.get("agentName").getAsString() : null;
                String phone = body.has("phone") && !body.get("phone").isJsonNull() ? body.get("phone").getAsString() : null;
                String veh = body.has("vehicleNumber") && !body.get("vehicleNumber").isJsonNull() ? body.get("vehicleNumber").getAsString() : null;
                Integer isActive = body.has("isActive") && !body.get("isActive").isJsonNull() ? body.get("isActive").getAsInt() : null;

                boolean ok = deliveryAgentDAO.updateAgent(agentId, name, phone, veh, isActive);
                if (!ok) sendJson(exchange, 404, errorResponse("Agent not found."));
                else sendJson(exchange, 200, successMessage("Agent updated."));
                return;
            }
        }

        // =========================================================================
        // 9. DELIVERY ROUTES (/api/deliveries/...)
        // =========================================================================
        if (path.equals("/api/deliveries") || path.startsWith("/api/deliveries/")) {
            UserPrincipal user = requireRole(exchange, "admin", "staff", "driver");
            if (user == null) return;

            if ("/api/deliveries".equals(path) && "GET".equals(method)) {
                String status = q.get("status");
                Integer agentId = null;
                if (q.containsKey("agentID") && !q.get("agentID").isBlank()) {
                    agentId = Integer.parseInt(q.get("agentID"));
                }
                sendJson(exchange, 200, successData(deliveryDAO.getAllDeliveries(status, agentId)));
                return;
            }

            String[] segs = path.split("/");
            if (segs.length == 4 && "PUT".equals(method)) { // /api/deliveries/:id
                int delId = Integer.parseInt(segs[3]);
                JsonObject body = parseBody(exchange);
                Integer agentId = body.has("agentID") && !body.get("agentID").isJsonNull() ? body.get("agentID").getAsInt() : null;
                String status = body.has("deliveryStatus") && !body.get("deliveryStatus").isJsonNull() ? body.get("deliveryStatus").getAsString() : null;
                String pickupTime = body.has("pickupTime") && !body.get("pickupTime").isJsonNull() ? body.get("pickupTime").getAsString() : null;
                String deliveryTime = body.has("deliveryTime") && !body.get("deliveryTime").isJsonNull() ? body.get("deliveryTime").getAsString() : null;

                boolean ok = deliveryDAO.updateDelivery(delId, agentId, status, pickupTime, deliveryTime);
                if (!ok) sendJson(exchange, 404, errorResponse("Delivery not found."));
                else sendJson(exchange, 200, successMessage("Delivery updated."));
                return;
            }
        }

        // =========================================================================
        // 10. FEEDBACK ROUTES (/api/feedback/...)
        // =========================================================================
        if (path.equals("/api/feedback") || path.startsWith("/api/feedback/")) {
            UserPrincipal user = requireAuth(exchange);
            if (user == null) return;

            if ("/api/feedback".equals(path)) {
                if ("GET".equals(method)) {
                    if (!user.hasRole("admin", "staff")) {
                        sendForbidden(exchange, "admin or staff");
                        return;
                    }
                    sendJson(exchange, 200, successData(feedbackDAO.getAllFeedback()));
                    return;
                }
                if ("POST".equals(method)) {
                    if (!user.hasRole("customer")) {
                        sendForbidden(exchange, "customer");
                        return;
                    }
                    JsonObject body = parseBody(exchange);
                    int orderId = body.get("orderID").getAsInt();
                    int rating = body.get("rating").getAsInt();
                    String comments = body.has("comments") && !body.get("comments").isJsonNull() ? body.get("comments").getAsString() : null;

                    try {
                        int fbId = feedbackDAO.submitFeedback(user.getCustomerId(), orderId, rating, comments);
                        Map<String, Object> res = new HashMap<>();
                        res.put("success", true);
                        res.put("message", "Feedback submitted. Thank you!");
                        res.put("feedbackID", fbId);
                        sendJson(exchange, 201, res);
                    } catch (IllegalArgumentException iae) {
                        sendJson(exchange, 400, errorResponse(iae.getMessage()));
                    } catch (IllegalStateException ise) {
                        if (ise.getMessage().contains("already")) {
                            sendJson(exchange, 409, errorResponse(ise.getMessage()));
                        } else {
                            sendJson(exchange, 400, errorResponse(ise.getMessage()));
                        }
                    }
                    return;
                }
            }

            if ("/api/feedback/my".equals(path) && "GET".equals(method)) {
                if (!user.hasRole("customer") || user.getCustomerId() == null) {
                    sendForbidden(exchange, "customer");
                    return;
                }
                sendJson(exchange, 200, successData(feedbackDAO.getMyFeedback(user.getCustomerId())));
                return;
            }
        }

        // =========================================================================
        // 11. DASHBOARD ROUTES (/api/dashboard/...)
        // =========================================================================
        if ("/api/dashboard/stats".equals(path) && "GET".equals(method)) {
            UserPrincipal user = requireRole(exchange, "admin", "staff");
            if (user == null) return;
            sendJson(exchange, 200, successData(dashboardDAO.getDashboardStats()));
            return;
        }

        // =========================================================================
        // 12. REPORT ROUTES (/api/reports/...)
        // =========================================================================
        if (path.startsWith("/api/reports/")) {
            UserPrincipal user = requireRole(exchange, "admin", "staff");
            if (user == null) return;

            if ("/api/reports/daily-revenue".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, successData(reportDAO.getDailyRevenue(q.get("from"), q.get("to"))));
                return;
            }
            if ("/api/reports/monthly-revenue".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, successData(reportDAO.getMonthlyRevenue()));
                return;
            }
            if ("/api/reports/pending-deliveries".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, successData(reportDAO.getPendingDeliveries()));
                return;
            }
            if (path.startsWith("/api/reports/customer-history/") && "GET".equals(method)) {
                String[] segs = path.split("/");
                if (segs.length == 5) {
                    int custId = Integer.parseInt(segs[4]);
                    sendJson(exchange, 200, successData(reportDAO.getCustomerHistory(custId)));
                    return;
                }
            }
            if ("/api/reports/inventory-usage".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, successData(reportDAO.getInventoryUsage()));
                return;
            }
            if ("/api/reports/popular-services".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, successData(reportDAO.getPopularServices()));
                return;
            }
            if ("/api/reports/outstanding-payments".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, successData(reportDAO.getOutstandingPayments()));
                return;
            }
        }

        // Unmatched endpoint
        sendJson(exchange, 404, errorResponse("Endpoint not found: " + method + " " + path));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Handlers
    // ─────────────────────────────────────────────────────────────────────────

    private void handleRegister(HttpExchange exchange) throws Exception {
        JsonObject body = parseBody(exchange);
        if (!body.has("firstName") || !body.has("lastName") || !body.has("phone") ||
            !body.has("address") || !body.has("email") || !body.has("password")) {
            sendJson(exchange, 400, errorResponse("All fields are required."));
            return;
        }

        String fn = body.get("firstName").getAsString();
        String ln = body.get("lastName").getAsString();
        String ph = body.get("phone").getAsString();
        String ad = body.get("address").getAsString();
        String em = body.get("email").getAsString();
        String pw = body.get("password").getAsString();
        String un = body.has("username") && !body.get("username").isJsonNull() ? body.get("username").getAsString() : null;

        if (pw.length() < 6) {
            sendJson(exchange, 400, errorResponse("Password must be at least 6 characters."));
            return;
        }

        String hashed = PasswordUtil.hashPassword(pw);
        Map<String, Object> registered = userDAO.registerCustomer(fn, ln, ph, ad, em, hashed, un);

        if (registered == null) {
            sendJson(exchange, 409, errorResponse("Email already registered."));
            return;
        }

        int userId = (Integer) registered.get("userID");
        int custId = (Integer) registered.get("customerID");
        String role = (String) registered.get("role");

        String token = JwtUtil.generateToken(userId, role, custId, null, em);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Registration successful.");
        res.put("token", token);
        res.put("user", registered);
        sendJson(exchange, 201, res);
    }

    private void handleLogin(HttpExchange exchange) throws Exception {
        JsonObject body = parseBody(exchange);
        if (!body.has("email") || !body.has("password")) {
            sendJson(exchange, 400, errorResponse("Email and password are required."));
            return;
        }

        String email = body.get("email").getAsString();
        String password = body.get("password").getAsString();

        Map<String, Object> user = userDAO.getUserForLogin(email);
        if (user == null) {
            sendJson(exchange, 401, errorResponse("Invalid email or password."));
            return;
        }

        boolean isActive = Boolean.TRUE.equals(user.get("isActive"));
        if (!isActive) {
            sendJson(exchange, 403, errorResponse("Account is inactive. Contact admin."));
            return;
        }

        String storedHash = (String) user.get("password");
        if (!PasswordUtil.verifyPassword(password, storedHash)) {
            sendJson(exchange, 401, errorResponse("Invalid email or password."));
            return;
        }

        int userId = (Integer) user.get("userID");
        String role = (String) user.get("role");
        Integer custId = (Integer) user.get("customerID");
        Integer agentId = (Integer) user.get("agentID");

        String token = JwtUtil.generateToken(userId, role, custId, agentId, email);

        Map<String, Object> userData = new HashMap<>();
        userData.put("userID", userId);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("customerID", custId);
        userData.put("agentID", agentId);
        userData.put("firstName", user.get("firstName"));
        userData.put("lastName", user.get("lastName"));

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Login successful.");
        res.put("token", token);
        res.put("user", userData);

        sendJson(exchange, 200, res);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Security & Parsing Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private UserPrincipal requireAuth(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            sendJson(exchange, 401, errorResponse("Access denied. No token provided."));
            return null;
        }

        UserPrincipal principal = JwtUtil.verifyToken(authHeader);
        if (principal == null) {
            sendJson(exchange, 401, errorResponse("Invalid or expired token."));
            return null;
        }

        return principal;
    }

    private UserPrincipal requireRole(HttpExchange exchange, String... roles) throws IOException {
        UserPrincipal principal = requireAuth(exchange);
        if (principal == null) return null;

        if (!principal.hasRole(roles)) {
            sendForbidden(exchange, String.join(" or ", roles));
            return null;
        }
        return principal;
    }

    private void sendForbidden(HttpExchange exchange, String requiredRoles) throws IOException {
        sendJson(exchange, 403, errorResponse("Access denied. Required role: " + requiredRoles + "."));
    }

    private JsonObject parseBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (content.isBlank()) return new JsonObject();
        return JsonParser.parseString(content).getAsJsonObject();
    }

    private Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return map;

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(key, val);
            } else {
                map.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            }
        }
        return map;
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, Object> successData(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("data", data);
        return map;
    }

    private Map<String, Object> successMessage(String msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("message", msg);
        return map;
    }

    private Map<String, Object> errorResponse(String msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("message", msg);
        return map;
    }
}
