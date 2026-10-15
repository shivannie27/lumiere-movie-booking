// =====================================================================
// LUMIÈRE CINEMA BOOKING SYSTEM - MAIN APPLICATION & JAVA WEB SERVER
// Contributed by: Kabir Ubale (Lead Architecture & HTTP Engine)
// =====================================================================

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class App {
    private static int port = 8080;
    private static final BookingStore store = new BookingStore();

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equalsIgnoreCase("demo")) {
            runConsoleDemo();
            return;
        }

        // Auto-detect open port (tries 8080, 8081, 8082, etc.) to prevent BindException
        HttpServer server = null;
        for (int p = 8080; p <= 8095; p++) {
            try {
                server = HttpServer.create(new InetSocketAddress(p), 0);
                port = p;
                break;
            } catch (IOException ignored) {}
        }
        if (server == null) {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            port = server.getAddress().getPort();
        }

        // REST API Endpoints
        server.createContext("/api/movies", exchange -> {
            sendJson(exchange, 200, store.getMoviesJson());
        });

        server.createContext("/api/cities", exchange -> {
            sendJson(exchange, 200, store.getCitiesJson());
        });

        server.createContext("/api/bookings", exchange -> {
            sendJson(exchange, 200, store.getBookingsJson());
        });

        server.createContext("/api/booked-seats", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);
            String movie = params.getOrDefault("movie", "");
            String show = params.getOrDefault("show", "");

            List<String> booked = new ArrayList<>();
            for (Ticket t : store.search("")) {
                if (t.getMovie().getTitle().equalsIgnoreCase(movie) && t.getShowTime().equalsIgnoreCase(show)) {
                    Collections.addAll(booked, t.getSeats());
                }
            }
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < booked.size(); i++) {
                sb.append("\"").append(booked.get(i)).append("\"").append(i < booked.size() - 1 ? "," : "");
            }
            sb.append("]");
            sendJson(exchange, 200, sb.toString());
        });

        server.createContext("/api/coupon", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);
            String code = params.getOrDefault("code", "").trim().toUpperCase();
            
            if (code.equals("LUMIERE50")) {
                sendJson(exchange, 200, "{\"valid\":true,\"code\":\"LUMIERE50\",\"discount\":50.0,\"amount\":50.0,\"desc\":\"Flat Rs. 50 Off\"}");
            } else if (code.equals("STUDENT10")) {
                sendJson(exchange, 200, "{\"valid\":true,\"code\":\"STUDENT10\",\"discount\":40.0,\"amount\":40.0,\"desc\":\"Rs. 40 Student Special Discount\"}");
            } else if (code.equals("FIRSTSHOW")) {
                sendJson(exchange, 200, "{\"valid\":true,\"code\":\"FIRSTSHOW\",\"discount\":60.0,\"amount\":60.0,\"desc\":\"Rs. 60 Welcome Discount\"}");
            } else {
                sendJson(exchange, 400, "{\"valid\":false,\"error\":\"Invalid or expired coupon code\"}");
            }
        });

        server.createContext("/api/book", exchange -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            String body = readBody(exchange);
            try {
                Map<String, String> map = parseSimpleJson(body);
                String movieTitle = map.get("movie");
                String showTime = map.get("show");
                boolean premium = Boolean.parseBoolean(map.get("premium"));
                String custName = map.get("name");
                String custPhone = map.get("phone");
                String rawSeats = map.get("seats");
                if (rawSeats.startsWith("[")) rawSeats = rawSeats.substring(1);
                if (rawSeats.endsWith("]")) rawSeats = rawSeats.substring(0, rawSeats.length() - 1);
                rawSeats = rawSeats.replace("\"", "").replace(" ", "");
                String[] seats = rawSeats.split(",");
                double snacks = map.containsKey("snacks") ? Double.parseDouble(map.get("snacks")) : 0.0;
                double discount = map.containsKey("discount") ? Double.parseDouble(map.get("discount")) : 0.0;

                Movie movie = store.findMovie(movieTitle);
                if (movie == null) throw new IllegalArgumentException("Movie not found.");

                Customer customer = new Customer(custName, custPhone);
                String id = store.nextBookingId();
                Ticket ticket = premium
                    ? new PremiumTicket(id, movie, customer, showTime, seats, snacks, discount)
                    : new Ticket(id, movie, customer, showTime, seats, snacks, discount);

                store.add(ticket);
                sendJson(exchange, 200, ticket.toJson());
            } catch (Exception ex) {
                sendJson(exchange, 400, "{\"error\":\"" + ex.getMessage().replace("\"", "\\\"") + "\"}");
            }
        });

        server.createContext("/api/cancel", exchange -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            try {
                String body = readBody(exchange);
                Map<String, String> map = parseSimpleJson(body);
                String id = map.get("id");
                boolean ok = store.cancel(id);
                sendJson(exchange, 200, "{\"success\":" + ok + "}");
            } catch (Exception ex) {
                sendJson(exchange, 400, "{\"error\":\"" + ex.getMessage().replace("\"", "\\\"") + "\"}");
            }
        });

        server.createContext("/api/summary", exchange -> {
            String json = String.format(
                java.util.Locale.US,
                "{\"active\":%d,\"seats\":%d,\"revenue\":%.2f,\"created\":%d,\"gst\":%.1f,\"fee\":%.2f}",
                store.getCount(), store.getTotalSeatsBooked(), store.getTotalRevenue(),
                Ticket.getTotalBookings(), Ticket.getGstPercent(), Ticket.getConvenienceFee()
            );
            sendJson(exchange, 200, json);
        });

        // Multi-folder Static Web Asset Handler (serves assets directly from teammate folders)
        server.createContext("/", new MultiFolderFileHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("=================================================");
        System.out.println("  LUMIÈRE CINEMA WEB APP STARTED SUCCESSFULLY!");
        System.out.println("  Server running at: http://localhost:" + port);
        System.out.println("  Lead Architect & Backend: Kabir Ubale");
        System.out.println("=================================================");

        // Automatically open browser
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("http://localhost:" + port));
            }
        } catch (Exception ignored) {}
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                try {
                    map.put(URLDecoder.decode(kv[0], "UTF-8"), URLDecoder.decode(kv[1], "UTF-8"));
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"?([a-zA-Z0-9_-]+)\"?\\s*:\\s*(\"[^\"]*\"|\\[[^\\]]*\\]|[^,}]+)");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            String k = matcher.group(1).trim().replace("\"", "");
            String v = matcher.group(2).trim();
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                v = v.substring(1, v.length() - 1);
            } else if (v.startsWith("[") && v.endsWith("]") && v.length() >= 2) {
                v = v.substring(1, v.length() - 1).replace("\"", "").replace(" ", "");
            }
            map.put(k, v);
        }
        return map;
    }

    /** Dispatches static files directly from the 4 teammate folders */
    static class MultiFolderFileHandler implements HttpHandler {
        private static final String[] FOLDERS = {
            "Shaikh_Rahim",
            "Shruti_Wadhonkar",
            "Shivani_Dudhane",
            "Kabir_Ubale",
            "."
        };

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String rawPath = exchange.getRequestURI().getPath();
            if (rawPath.equals("/")) rawPath = "/index.html";
            String path = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
            if (path.startsWith("/")) path = path.substring(1);

            File target = null;
            for (String folder : FOLDERS) {
                File test = new File(folder, path);
                if (test.exists() && !test.isDirectory()) {
                    target = test;
                    break;
                }
            }

            if (target == null) {
                String notFound = "404 Not Found: " + path;
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(notFound.getBytes()); }
                return;
            }

            String mime = "text/plain";
            if (path.endsWith(".html")) mime = "text/html; charset=UTF-8";
            else if (path.endsWith(".css")) mime = "text/css; charset=UTF-8";
            else if (path.endsWith(".js")) mime = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) mime = "image/jpeg";
            else if (path.endsWith(".png")) mime = "image/png";

            byte[] bytes = Files.readAllBytes(target.toPath());
            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    private static void runConsoleDemo() {
        System.out.println("=== LUMIÈRE CINEMA BOOKING ENGINE - CORE TEST ===");
        Movie dune = store.findMovie("Dune: Part Two");
        Customer annie = new Customer("Shivani", "9876543210");
        Ticket t1 = new Ticket("LUM100001", dune, annie, "PVR | 04:30 PM", new String[]{"E5", "E6"});
        PremiumTicket t2 = new PremiumTicket("LUM100002", dune, annie, "PVR | 04:30 PM", new String[]{"F5", "F6"});
        store.add(t1);
        store.add(t2);
        System.out.printf(java.util.Locale.US, "Regular Ticket Bill: Rs. %.2f%n", t1.calculateBill());
        System.out.printf(java.util.Locale.US, "Premium Ticket Bill: Rs. %.2f%n", t2.calculateBill());
        System.out.println("Total Seats Booked: " + store.getTotalSeatsBooked());
        System.out.printf(java.util.Locale.US, "Total Revenue: Rs. %.2f%n", store.getTotalRevenue());
        store.cancel("LUM100001");
        System.out.println("After Cancellation - Active Bookings: " + store.getCount());
        System.out.println("Is Seat E5 Booked?: " + store.isSeatBooked("Dune: Part Two", "PVR | 04:30 PM", "E5"));
    }
}
