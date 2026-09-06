package com.lsms.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * AppConfig — Dynamic configuration manager for LSMS.
 * Reads environment variables first, falling back to .env files, and then defaults.
 * Prevents hardcoding of sensitive credentials into Java source code.
 */
public class AppConfig {

    private static final Map<String, String> ENV_VARS = new HashMap<>();

    static {
        loadDotEnv();
    }

    private static void loadDotEnv() {
        // Attempt to load from multiple standard locations
        String[] potentialPaths = {
            ".env",
            "../.env",
            "../backend/.env",
            "java-jdbc/.env",
            System.getProperty("user.dir") + "/.env",
            System.getProperty("user.dir") + "/../backend/.env"
        };

        for (String path : potentialPaths) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int idx = line.indexOf('=');
                        if (idx > 0) {
                            String key = line.substring(0, idx).trim();
                            String val = line.substring(idx + 1).trim();
                            // strip quotes if present
                            if ((val.startsWith("\"") && val.endsWith("\"")) ||
                                (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            ENV_VARS.putIfAbsent(key, val);
                        }
                    }
                    break;
                } catch (IOException e) {
                    System.err.println("Notice: Could not read " + path + ": " + e.getMessage());
                }
            }
        }
    }

    public static String get(String key, String defaultValue) {
        // 1. System environment variable
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) return val.trim();

        // 2. JVM System property
        val = System.getProperty(key);
        if (val != null && !val.isBlank()) return val.trim();

        // 3. Loaded .env
        val = ENV_VARS.get(key);
        if (val != null && !val.isBlank()) return val.trim();

        // 4. Default value
        return defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public static String getDbHost()     { return get("DB_HOST", "localhost"); }
    public static int    getDbPort()     { return getInt("DB_PORT", 3306); }
    public static String getDbName()     { return get("DB_NAME", "lsms"); }
    public static String getDbUser()     { return get("DB_USER", "root"); }
    public static String getDbPassword() { return get("DB_PASSWORD", ""); }

    public static int    getServerPort() { return getInt("PORT", 3001); }
    public static String getJwtSecret()  { return get("JWT_SECRET", "lsms_super_secret_jwt_key_change_in_production_2024"); }
    public static int    getJwtExpiryHours() { return getInt("JWT_EXPIRES_IN_HOURS", 24); }

    public static String getJdbcUrl() {
        return String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            getDbHost(), getDbPort(), getDbName()
        );
    }
}
