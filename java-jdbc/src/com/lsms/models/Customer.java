package com.lsms.models;

public class Customer {
    private int customerId;
    private int userId;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;

    public Customer() {}

    public Customer(int customerId, int userId, String firstName, String lastName, String phone, String address) {
        this.customerId = customerId;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.address = address;
    }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return firstName + " " + lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String toString() {
        return String.format("CustID: #%-3d | UserID: %-3d | Name: %-20s | Phone: %-12s | Address: %s",
                customerId, userId, getFullName(), phone, address);
    }
}
