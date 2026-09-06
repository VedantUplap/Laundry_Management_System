package com.lsms.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lsms.config.AppConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * JwtUtil — Handles creation and cryptographic verification of JWTs using HMAC-SHA256.
 * Completely compatible with Node.js jsonwebtoken library and the frontend Auth client.
 */
public class JwtUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private static byte[] sign(byte[] data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(key, HMAC_SHA256));
        return mac.doFinal(data);
    }

    public static String generateToken(int userId, String role, Integer customerId, Integer agentId, String email) {
        try {
            long nowSec = Instant.now().getEpochSecond();
            long expSec = nowSec + (AppConfig.getJwtExpiryHours() * 3600L);

            JsonObject header = new JsonObject();
            header.addProperty("alg", "HS256");
            header.addProperty("typ", "JWT");

            JsonObject payload = new JsonObject();
            payload.addProperty("userID", userId);
            payload.addProperty("role", role);
            if (customerId != null) payload.addProperty("customerID", customerId);
            else payload.add("customerID", null);
            if (agentId != null) payload.addProperty("agentID", agentId);
            else payload.add("agentID", null);
            if (email != null) payload.addProperty("email", email);
            payload.addProperty("iat", nowSec);
            payload.addProperty("exp", expSec);

            String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(header.toString().getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));

            String content = encodedHeader + "." + encodedPayload;
            byte[] secretBytes = AppConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8);
            byte[] signature = sign(content.getBytes(StandardCharsets.UTF_8), secretBytes);
            String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

            return content + "." + encodedSignature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT: " + e.getMessage(), e);
        }
    }

    public static UserPrincipal verifyToken(String token) {
        if (token == null || token.isBlank()) return null;
        if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
            token = token.substring(7).trim();
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;

        try {
            String content = parts[0] + "." + parts[1];
            byte[] secretBytes = AppConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8);
            byte[] expectedSig = sign(content.getBytes(StandardCharsets.UTF_8), secretBytes);
            byte[] actualSig = Base64.getUrlDecoder().decode(parts[2]);

            if (!MessageDigest.isEqual(expectedSig, actualSig)) {
                return null; // Signature invalid
            }

            // Decode payload
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonObject payload = JsonParser.parseString(payloadJson).getAsJsonObject();

            // Check expiration
            if (payload.has("exp") && !payload.get("exp").isJsonNull()) {
                long exp = payload.get("exp").getAsLong();
                if (Instant.now().getEpochSecond() > exp) {
                    return null; // Expired
                }
            }

            int userId = payload.get("userID").getAsInt();
            String role = payload.has("role") && !payload.get("role").isJsonNull() ? payload.get("role").getAsString() : "customer";
            Integer customerId = payload.has("customerID") && !payload.get("customerID").isJsonNull() ? payload.get("customerID").getAsInt() : null;
            Integer agentId = payload.has("agentID") && !payload.get("agentID").isJsonNull() ? payload.get("agentID").getAsInt() : null;
            String email = payload.has("email") && !payload.get("email").isJsonNull() ? payload.get("email").getAsString() : null;

            return new UserPrincipal(userId, role, customerId, agentId, email);
        } catch (Exception e) {
            return null;
        }
    }
}
