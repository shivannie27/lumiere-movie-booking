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

        server.createContext("/api/server-info", exchange -> {
            String hostIp = findBestHostIp();
            String json = String.format("{\"host\":\"%s\",\"port\":%d}", hostIp, port);
            sendJson(exchange, 200, json);
        });

        server.createContext("/ticket", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);
            String id = params.getOrDefault("id", "").trim();

            Ticket ticket = null;
            if (!id.isEmpty()) {
                for (Ticket t : store.search("")) {
                    if (t.getBookingId().equalsIgnoreCase(id)) {
                        ticket = t;
                        break;
                    }
                }
            }

            String html = renderStandaloneTicketHtml(ticket, id, port);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
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

    private static String renderStandaloneTicketHtml(Ticket ticket, String searchId, int port) {
        if (ticket == null) {
            return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Ticket Verification - Lumière Cinemas</title>" +
                "<style>body{font-family:system-ui,sans-serif;background:#26070A;color:#FED7B8;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;padding:20px;text-align:center;}" +
                ".card{background:#3A0E12;border:1px solid #59171B;border-radius:18px;padding:36px;max-width:440px;width:100%;box-shadow:0 12px 36px rgba(0,0,0,0.4);}" +
                ".badge{background:#D32F2F;color:#fff;padding:6px 14px;border-radius:999px;font-size:12px;font-weight:bold;display:inline-block;margin-bottom:16px;}" +
                "a{color:#FED7B8;font-weight:bold;text-decoration:none;border-bottom:1px solid #FED7B8;}</style></head><body>" +
                "<div class='card'>" +
                "<div class='badge'>ENTRY INVALID / NOT FOUND</div>" +
                "<h2 style='font-family:Georgia,serif;margin:0 0 10px;color:#fff;'>Ticket ID Not Found</h2>" +
                "<p style='color:#DAEBE3;font-size:14px;line-height:1.6;'>No active reservation was found matching reference <code>" + (searchId != null ? searchId : "") + "</code>. The ticket may have been cancelled or the reference code is invalid.</p>" +
                "<p style='margin-top:24px;'><a href='/'>&larr; Return to Lumière Cinemas</a></p>" +
                "</div></body></html>";
        }

        String seats = String.join(", ", ticket.getSeats());
        String snacks = ticket.getSnacksTotal() > 0 ? String.format(java.util.Locale.US, "Rs. %.2f", ticket.getSnacksTotal()) : "None";
        String discount = ticket.getDiscountAmount() > 0 ? String.format(java.util.Locale.US, "-Rs. %.2f", ticket.getDiscountAmount()) : "Standard Rate";

        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>" + ticket.getMovie().getTitle() + " - Lumière Cinema Pass</title>" +
            "<style>" +
            ":root{--burgundy:#59171B;--deep:#26070A;--dark:#3A0E12;--peach:#FED7B8;--gold:#D4AF37;--gold-lt:#F9E79F;--mint:#DAEBE3;}" +
            "* {box-sizing:border-box;margin:0;padding:0;}" +
            "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:var(--deep);color:#fff;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px 16px;}" +
            ".pass-wrapper{max-width:440px;width:100%;margin:0 auto;}" +
            ".verified-pill{display:flex;align-items:center;justify-content:center;gap:8px;background:rgba(46,125,50,0.2);border:1px solid #2E7D32;color:#81C784;padding:8px 16px;border-radius:999px;font-size:12px;font-weight:bold;margin-bottom:18px;letter-spacing:0.5px;}" +
            ".pulse-dot{width:8px;height:8px;border-radius:50%;background:#4CAF50;box-shadow:0 0 8px #4CAF50;}" +
            ".pass-card{background:linear-gradient(145deg, var(--dark), var(--burgundy));border:1px solid rgba(254,215,184,0.3);border-radius:22px;padding:28px;position:relative;box-shadow:0 18px 48px rgba(0,0,0,0.5);overflow:hidden;}" +
            ".pass-card::before,.pass-card::after{content:'';position:absolute;width:24px;height:24px;background:var(--deep);border-radius:50%;top:68%;}" +
            ".pass-card::before{left:-12px;}.pass-card::after{right:-12px;}" +
            ".pass-brand{display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid rgba(254,215,184,0.25);padding-bottom:12px;margin-bottom:16px;}" +
            ".brand-title{font-family:Georgia,serif;font-size:22px;font-weight:bold;color:var(--peach);letter-spacing:1px;}" +
            ".tier-tag{background:var(--peach);color:var(--burgundy);font-size:11px;font-weight:800;padding:4px 10px;border-radius:999px;text-transform:uppercase;}" +
            ".movie-head{font-family:Georgia,serif;font-size:22px;color:#fff;margin-bottom:4px;line-height:1.3;}" +
            ".movie-sub{font-size:12px;color:var(--peach);margin-bottom:18px;}" +
            ".info-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:18px;}" +
            ".info-label{font-size:10px;color:rgba(254,215,184,0.7);text-transform:uppercase;letter-spacing:0.5px;}" +
            ".info-val{font-size:13px;font-weight:600;margin-top:2px;color:#fff;}" +
            ".info-val.gold{color:var(--gold-lt);font-size:15px;font-weight:bold;}" +
            ".perforation{border-top:1px dashed rgba(254,215,184,0.35);margin:18px 0 16px;}" +
            ".pass-foot{display:flex;justify-content:space-between;align-items:center;}" +
            ".actions{display:flex;gap:10px;margin-top:20px;}" +
            ".btn{flex:1;padding:12px;border-radius:999px;font-size:13px;font-weight:bold;cursor:pointer;text-align:center;text-decoration:none;transition:all 0.2s;}" +
            ".btn-gold{background:linear-gradient(135deg,var(--gold),#B8860B);color:#1E0A0C;border:none;}" +
            ".btn-outline{border:1px solid rgba(254,215,184,0.6);color:var(--peach);background:transparent;}" +
            "@media print{body{background:#fff;color:#000;padding:0;}.verified-pill,.actions{display:none;}.pass-card{background:#26070A !important;color:#fff !important;box-shadow:none;}}" +
            "</style></head><body>" +
            "<div class='pass-wrapper'>" +
            "<div class='verified-pill'><span class='pulse-dot'></span> TURNSTILE ENTRY VERIFIED &bull; OFFICIAL E-PASS</div>" +
            "<div class='pass-card'>" +
            "<div class='pass-brand'>" +
            "<div class='brand-title'>LUMIÈRE CINEMAS</div>" +
            "<div class='tier-tag'>" + ticket.getType() + " RECLINER</div>" +
            "</div>" +
            "<div class='movie-head'>" + ticket.getMovie().getTitle() + "</div>" +
            "<div class='movie-sub'>" + ticket.getMovie().getLanguage() + " &bull; " + ticket.getMovie().getDurationText() + " &bull; " + ticket.getMovie().getCertification() + "</div>" +
            "<div class='info-grid'>" +
            "<div><div class='info-label'>Cinema & Screen</div><div class='info-val'>" + ticket.getShowTime() + "</div></div>" +
            "<div><div class='info-label'>Reserved Seats</div><div class='info-val gold'>" + seats + "</div></div>" +
            "<div><div class='info-label'>Guest Name</div><div class='info-val'>" + ticket.getCustomer().getName() + "</div></div>" +
            "<div><div class='info-label'>Phone</div><div class='info-val'>" + ticket.getCustomer().getPhone() + "</div></div>" +
            "<div><div class='info-label'>F&B Concessions</div><div class='info-val'>" + snacks + "</div></div>" +
            "<div><div class='info-label'>Total Amount Paid</div><div class='info-val gold'>Rs. " + String.format(java.util.Locale.US, "%.2f", ticket.calculateBill()) + "</div></div>" +
            "</div>" +
            "<div class='perforation'></div>" +
            "<div class='pass-foot'>" +
            "<div><div class='info-label'>BOOKING PASSPORT ID</div><div style='font-family:monospace;font-size:16px;font-weight:bold;letter-spacing:2px;color:var(--gold-lt);'>" + ticket.getBookingId() + "</div><div style='font-size:10px;color:rgba(254,215,184,0.6);margin-top:4px;'>Present at cinema turnstile</div></div>" +
            "<div style='background:#fff;padding:6px;border-radius:8px;'><img src='https://api.qrserver.com/v1/create-qr-code/?size=150x150&margin=2&data=http://localhost:" + port + "/ticket?id=" + ticket.getBookingId() + "' width='64' height='64' alt='QR' style='display:block;'></div>" +
            "</div></div>" +
            "<div class='actions'>" +
            "<button class='btn btn-gold' onclick='window.print()'>🖨️ Print Pass</button>" +
            "<a class='btn btn-outline' href='/'>&larr; Main App</a>" +
            "</div></div></body></html>";
    }

    public static String findBestHostIp() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            String fallbackCandidate = null;
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface nif = interfaces.nextElement();
                if (nif.isLoopback() || !nif.isUp()) continue;
                java.util.Enumeration<java.net.InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        // Top priority: Mobile Hotspot subnets (Android 192.168.43.*, iOS 172.20.10.*)
                        if (ip.startsWith("192.168.43.") || ip.startsWith("172.20.10.")) {
                            return ip;
                        }
                        // Secondary priority: Standard LAN/Wi-Fi
                        if (fallbackCandidate == null && (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172."))) {
                            fallbackCandidate = ip;
                        }
                    }
                }
            }
            if (fallbackCandidate != null) return fallbackCandidate;
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
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
