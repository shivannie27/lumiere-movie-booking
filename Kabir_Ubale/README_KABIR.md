# Contribution: Kabir Ubale (Lead Architecture & Backend Core)

## Owned Components (The Core & Best Part of the Project)
1. **`App.java` (Embedded Java Web Server & REST API Engine)**:
   - Built with high-performance standard Java `com.sun.net.httpserver.HttpServer`.
   - Dynamic multi-folder static file dispatcher serving web assets directly from all 4 teammates' folders.
   - REST API endpoints:
     - `GET /api/movies`: Delivers rich movie catalog (synopsis, cast, certification, trailer ID) to the web client.
     - `GET /api/cities`: Delivers multi-city multiplex directory mapping 5 major metropolitan hubs to certified multiplexes.
     - `GET /api/coupon`: Verifies promotional codes (`LUMIERE50`, `STUDENT10`, `FIRSTSHOW`) for instant bill discounts.
     - `GET /api/bookings`: Delivers live ticket list including concessions and discounts.
     - `GET /api/booked-seats`: Queries real-time occupied seats for a movie and show.
     - `POST /api/book`: Creates bookings with full OOP composition, polymorphism, validation, F&B add-ons, and overloaded bill computation.
     - `POST /api/cancel`: Cancels bookings and frees occupied seats dynamically.
     - `GET /api/summary`: Calculates live revenue, active bookings, seats, and GST rate.
   - Automatically opens the default browser on server start.
   - Built-in console test mode (`java -cp ... App demo`).

2. **`BookingStore.java` (Central Transaction Store & Seat Matrix Engine)**:
   - Thread-safe synchronized transactions for seat reservation.
   - Multi-city cinema registry for Bengaluru, Mumbai, Delhi NCR, Pune, and Hyderabad.
   - Real-time seat collision detector preventing double bookings.
   - Dynamic search engine and instantaneous cancellation logic that releases occupied seats.
   - Aggregates system metrics (Total Revenue, Total Seats Booked).

