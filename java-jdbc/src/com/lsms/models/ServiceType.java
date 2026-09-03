package com.lsms.models;

import java.math.BigDecimal;

public class ServiceType {
    private int serviceId;
    private String serviceName;
    private String description;
    private BigDecimal pricePerKg;
    private boolean isActive;

    public ServiceType() {}

    public ServiceType(int serviceId, String serviceName, String description, BigDecimal pricePerKg, boolean isActive) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.description = description;
        this.pricePerKg = pricePerKg;
        this.isActive = isActive;
    }

    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(BigDecimal pricePerKg) { this.pricePerKg = pricePerKg; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return String.format("[#%d] %-18s | ₹%-7.2f/kg | %s", serviceId, serviceName, pricePerKg, description);
    }
}
