package com.lsms.security;

/**
 * UserPrincipal — Holds identity and RBAC role for an authenticated request.
 */
public class UserPrincipal {
    private final int userId;
    private final String role;
    private final Integer customerId;
    private final Integer agentId;
    private final String email;

    public UserPrincipal(int userId, String role, Integer customerId, Integer agentId, String email) {
        this.userId = userId;
        this.role = role != null ? role.toLowerCase() : "customer";
        this.customerId = customerId;
        this.agentId = agentId;
        this.email = email;
    }

    public int getUserId() { return userId; }
    public String getRole() { return role; }
    public Integer getCustomerId() { return customerId; }
    public Integer getAgentId() { return agentId; }
    public String getEmail() { return email; }

    public boolean hasRole(String... allowedRoles) {
        if (allowedRoles == null || allowedRoles.length == 0) return true;
        for (String r : allowedRoles) {
            if (this.role.equalsIgnoreCase(r)) return true;
        }
        return false;
    }
}
