package com.lsms.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * PasswordUtil — Provides secure password hashing and verification using BCrypt.
 * Compatible with Node.js bcryptjs ($2b$ and $2a$ format).
 */
public class PasswordUtil {

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }

    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        // Standard BCrypt comparison
        try {
            // jbcrypt handles $2a$, $2y$, and $2b$ prefixes
            String normalizedHash = storedHash;
            if (storedHash.startsWith("$2b$")) {
                normalizedHash = "$2a$" + storedHash.substring(4);
            }
            return BCrypt.checkpw(plainPassword, normalizedHash);
        } catch (Exception e) {
            // Fallback for non-hashed plain passwords (legacy dev safety)
            return plainPassword.equals(storedHash);
        }
    }
}
