package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("restriction")
public class SimpleAssetServer {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        try {
            if (args != null && args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
        } catch (NumberFormatException ignored) {}
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
        File assetDir = new File("assets");
        server.createContext("/assets", new StaticHandler(assetDir));
        server.setExecutor(null);
        server.start();
        System.out.println("Serving assets from " + assetDir.getAbsolutePath());
        System.out.println("Preview URL: http://localhost:" + port + "/assets/crmenu-features.jpg");
    }

    static class StaticHandler implements com.sun.net.httpserver.HttpHandler {
        private final File base;
        private final Map<String, String> types = new HashMap<>();

        StaticHandler(File base) {
            this.base = base;
            types.put(".jpg", "image/jpeg");
            types.put(".jpeg", "image/jpeg");
            types.put(".png", "image/png");
            types.put(".svg", "image/svg+xml");
            types.put(".gif", "image/gif");
            types.put(".html", "text/html; charset=utf-8");
            types.put(".txt", "text/plain; charset=utf-8");
        }

        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath().replace("/assets", "");
            if (path.isEmpty() || path.equals("/")) {
                String index = "<html><body><h1>Assets</h1><ul>" +
                        listLinks() + "</ul></body></html>";
                byte[] data = index.getBytes();
                addCORS(exchange.getResponseHeaders());
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
                return;
            }
            File file = new File(base, path);
            if (!file.getCanonicalPath().startsWith(base.getCanonicalPath()) || !file.exists() || file.isDirectory()) {
                byte[] notFound = "Not Found".getBytes();
                addCORS(exchange.getResponseHeaders());
                exchange.sendResponseHeaders(404, notFound.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound);
                }
                return;
            }
            String ct = contentType(file.getName());
            addCORS(exchange.getResponseHeaders());
            exchange.getResponseHeaders().add("Content-Type", ct);
            exchange.sendResponseHeaders(200, Files.size(file.toPath()));
            try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            }
        }

        private void addCORS(com.sun.net.httpserver.Headers headers) {
            headers.add("Access-Control-Allow-Origin", "*");
        }

        private String contentType(String name) {
            int i = name.lastIndexOf('.');
            String ext = i >= 0 ? name.substring(i).toLowerCase() : "";
            return types.getOrDefault(ext, "application/octet-stream");
        }

        private String listLinks() {
            StringBuilder sb = new StringBuilder();
            File[] files = base.listFiles();
            if (files != null) {
                for (File f : files) {
                    sb.append("<li><a href=\"")
                      .append(f.getName())
                      .append("\">")
                      .append(f.getName())
                      .append("</a></li>");
                }
            }
            return sb.toString();
        }
    }
}