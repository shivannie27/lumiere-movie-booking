# Contribution: Kabir Ubale (Lead Architecture & Backend Core)

## Owned Components (The Core & Best Part of the Project)
1. **`App.java` (Embedded Java Web Server & REST API Engine)**:
   - Built with high-performance standard Java `com.sun.net.httpserver.HttpServer`.
   - Dynamic multi-folder static file dispatcher serving web assets directly from all 4 teammates' folders.
   - REST API endpoints:
     - `GET /api/movies`: Delivers movie catalog to the web client.
     - `GET /api/bookings`: Delivers live ticket list.
     - `GET /api/booked-seats`: Queries real-time occupied seats for a movie and show.
     - `POST /api/book`: Creates bookings with full OOP composition, polymorphism, validation, and bill computation.
     - `POST /api/cancel`: Cancels bookings and frees occupied seats dynamically.
     - `GET /api/summary`: Calculates live revenue, active bookings, seats, and GST rate.
   - Automatically opens the default browser on server start.
   - Built-in console test mode (`java -cp ... App demo`).

2. **`BookingStore.java` (Central Transaction Store & Seat Matrix Engine)**:
   - Thread-safe synchronized transactions for seat reservation.
   - Real-time seat collision detector preventing double bookings.
   - Dynamic search engine and instantaneous cancellation logic that releases occupied seats.
   - Aggregates system metrics (Total Revenue, Total Seats Booked).
