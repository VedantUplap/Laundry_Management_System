package com.lsms.server;

import com.lsms.config.AppConfig;
import com.lsms.config.DBConnection;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

/**
 * HttpServerApp — Main entry point for the LSMS Java Web Application Server.
 * Replaces the Node.js/Express backend completely with a high-performance Java HTTP Server.
 */
public class HttpServerApp {

    public static void main(String[] args) {
        try {
            int port = AppConfig.getServerPort();

            // Locate frontend and docs directories
            Path frontendDir = resolveDirectory("frontend", "../frontend");
            Path docsDir     = resolveDirectory("docs", "../docs");

            System.out.println("===============================================================");
            System.out.println("      🧺 LSMS — Laundry Service Management System             ");
            System.out.println("      🚀 Java + JDBC Web Application Server                   ");
            System.out.println("===============================================================");

            // Test database connectivity
            System.out.println("Connecting to database via JDBC (" + AppConfig.getJdbcUrl() + ")...");
            if (DBConnection.testConnection()) {
                System.out.println("✅ JDBC Database Connection Verified!");
            } else {
                System.err.println("⚠️ Warning: Could not connect to MySQL at " + AppConfig.getJdbcUrl() + ". Ensure MySQL is running.");
            }

            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // API Endpoint Router
            server.createContext("/api", new ApiDispatcher());

            // Static Documentation Files (/docs)
            if (docsDir != null && docsDir.toFile().exists()) {
                server.createContext("/docs", new StaticFileHandler(docsDir, "/docs", false));
            }

            // Static Frontend Files & SPA Fallback (/)
            if (frontendDir != null && frontendDir.toFile().exists()) {
                server.createContext("/", new StaticFileHandler(frontendDir, "", true));
            } else {
                System.err.println("⚠️ Warning: Frontend directory not found at " + frontendDir);
            }

            // Multi-threaded executor
            server.setExecutor(Executors.newCachedThreadPool());

            // Clean shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Shutting down LSMS Java Web Server...");
                server.stop(1);
                System.out.println("Server stopped.");
            }));

            server.start();

            System.out.println("\n🌐 Server running at:     http://localhost:" + port);
            System.out.println("🧺 Web Application:       http://localhost:" + port + "/index.html");
            System.out.println("📡 API Base URL:          http://localhost:" + port + "/api");
            System.out.println("📄 Documentation:         http://localhost:" + port + "/docs");
            System.out.println("⚙️ Database:              MySQL (" + AppConfig.getDbName() + " on " + AppConfig.getDbHost() + ":" + AppConfig.getDbPort() + ")");
            System.out.println("===============================================================");
            System.out.println("Press Ctrl+C to stop the server.\n");

        } catch (Exception e) {
            System.err.println("❌ Fatal error starting Java Web Server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Path resolveDirectory(String... relativePaths) {
        String baseDir = System.getProperty("user.dir");
        for (String rel : relativePaths) {
            Path p = Paths.get(baseDir, rel).normalize();
            if (p.toFile().exists()) return p;
        }
        for (String rel : relativePaths) {
            Path p = Paths.get(rel).normalize();
            if (p.toFile().exists()) return p;
        }
        return Paths.get(relativePaths[0]).toAbsolutePath();
    }
}
