package com.lsms.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * StaticFileHandler — Serves frontend HTML/CSS/JS and documentation with proper MIME types.
 * Supports SPA fallback to index.html for navigation routes.
 */
public class StaticFileHandler implements HttpHandler {

    private final Path baseDir;
    private final String urlPrefix;
    private final boolean spaFallback;
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm",  "text/html; charset=UTF-8");
        MIME_TYPES.put("css",  "text/css; charset=UTF-8");
        MIME_TYPES.put("js",   "application/javascript; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("png",  "image/png");
        MIME_TYPES.put("jpg",  "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif",  "image/gif");
        MIME_TYPES.put("svg",  "image/svg+xml");
        MIME_TYPES.put("ico",  "image/x-icon");
        MIME_TYPES.put("pdf",  "application/pdf");
        MIME_TYPES.put("txt",  "text/plain; charset=UTF-8");
        MIME_TYPES.put("md",   "text/markdown; charset=UTF-8");
    }

    public StaticFileHandler(Path baseDir, String urlPrefix, boolean spaFallback) {
        this.baseDir = baseDir.toAbsolutePath().normalize();
        this.urlPrefix = urlPrefix;
        this.spaFallback = spaFallback;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        URI requestUri = exchange.getRequestURI();
        String pathStr = requestUri.getPath();

        // Strip prefix if any
        if (urlPrefix != null && !urlPrefix.isEmpty() && pathStr.startsWith(urlPrefix)) {
            pathStr = pathStr.substring(urlPrefix.length());
        }
        if (pathStr.startsWith("/")) pathStr = pathStr.substring(1);
        if (pathStr.isEmpty()) pathStr = "index.html";

        Path requestedPath = baseDir.resolve(pathStr).normalize();

        // Path traversal guard
        if (!requestedPath.startsWith(baseDir)) {
            send404(exchange);
            return;
        }

        File targetFile = requestedPath.toFile();

        // If directory, look for index.html
        if (targetFile.isDirectory()) {
            File indexFile = new File(targetFile, "index.html");
            if (indexFile.exists()) {
                targetFile = indexFile;
            }
        }

        if (!targetFile.exists() || !targetFile.isFile()) {
            if (spaFallback) {
                // SPA fallback to frontend/index.html
                File fallback = baseDir.resolve("index.html").toFile();
                if (fallback.exists() && fallback.isFile()) {
                    targetFile = fallback;
                } else {
                    send404(exchange);
                    return;
                }
            } else {
                send404(exchange);
                return;
            }
        }

        String mime = getMimeType(targetFile.getName());
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, must-revalidate");

        long length = targetFile.length();
        exchange.sendResponseHeaders(200, length);

        if (!"HEAD".equalsIgnoreCase(method)) {
            try (OutputStream os = exchange.getResponseBody();
                 FileInputStream fis = new FileInputStream(targetFile)) {
                fis.transferTo(os);
            }
        } else {
            exchange.getResponseBody().close();
        }
    }

    private void send404(HttpExchange exchange) throws IOException {
        String msg = "404 Not Found";
        byte[] bytes = msg.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(404, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getMimeType(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx > 0 && idx < filename.length() - 1) {
            String ext = filename.substring(idx + 1).toLowerCase();
            return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }
}
