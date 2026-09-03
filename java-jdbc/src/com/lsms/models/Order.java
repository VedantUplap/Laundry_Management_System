package com.lsms.models;

import java.math.BigDecimal;
import java.sql.Date;

public class Order {
    private int orderId;
    private int customerId;
    private String customerName;
    private Date orderDate;
    private Date pickupDate;
    private Date deliveryDate;
    private String status;
    private BigDecimal totalWeight;
    private BigDecimal totalCost;
    private String notes;

    public Order() {}

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

    public Date getPickupDate() { return pickupDate; }
    public void setPickupDate(Date pickupDate) { this.pickupDate = pickupDate; }

    public Date getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(Date deliveryDate) { this.deliveryDate = deliveryDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalWeight() { return totalWeight; }
    public void setTotalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return String.format("Order #%-4d | Cust: %-18s | Date: %s | Status: %-16s | Wt: %-5.2fkg | Cost: ₹%.2f",
                orderId, customerName != null ? customerName : "ID:" + customerId, orderDate, status, totalWeight, totalCost);
    }
}
