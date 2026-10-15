# Lumière Cinemas — Web-Based Cinema Booking System

A modular, full-stack cinema ticketing application built with Object-Oriented Java (JDK 11+) and a modern, responsive web client. The application features an embedded Java HTTP server, real-time seat collision detection, customizable ticket tiers (Regular and Premium), customer verification, and dynamic pricing calculations with GST.

---

## Team Architecture & Module Ownership

The system follows a modular object-oriented architecture, with distinct component ownership across the development team:

### 1. Kabir Ubale — Lead Architecture & Backend Core
- **`App.java`**: Embedded Java HTTP Web Server (`com.sun.net.httpserver.HttpServer`), REST API routing (`/api/movies`, `/api/bookings`, `/api/booked-seats`, `/api/book`, `/api/cancel`, `/api/summary`), auto-port detection, and multi-folder static asset dispatcher.
- **`BookingStore.java`**: Thread-safe transactional storage, real-time seat collision prevention, search indexing, dynamic cancellation releasing booked seats, and financial metrics calculation.
- **`run_lumiere.bat`**: Unified cross-module compilation and launcher script.

### 2. Shaikh Rahim — Movie Domain Model & Frontend Catalog
- **`Movie.java`**: Domain entity encapsulating movie metadata (title, language, genre, duration in minutes, base price, rating, and poster asset mapping).
- **`index.html`**: Responsive single-page application structure, screen router containers, cinema selection tabs, and showtime selectors.
- **`posters/`**: Theatrical poster collection for all cataloged movies.

### 3. Shruti Wadhonkar — Customer Verification & UI Design System
- **`Customer.java`**: Customer domain entity enforcing strict input constraints and 10-digit mobile number validation via regex (`^\d{10}$`).
- **`style.css`**: Comprehensive CSS design system utilizing Lumière's burgundy and peach palette, responsive movie cards, interactive seat matrix states (Available, Selected, Booked), and modal dialogs.

### 4. Shivani Dudhane — Billing Engine & Client Controller
- **`Ticket.java`**: Core entity demonstrating object composition (`Ticket HAS-A Movie` and `Ticket HAS-A Customer`), base billing calculations, and statutory tax breakdown (18% GST and convenience fee).
- **`PremiumTicket.java`**: Demonstrates OOP inheritance (`IS-A Ticket`) and runtime polymorphism by overriding bill calculation to incorporate recliner surcharges.
- **`app.js`**: Client-side controller interfacing with the backend REST endpoints, handling real-time pricing updates, and managing interactive seat selection.

---

## Project Structure

```
lumiere-movie-booking/
├── Kabir_Ubale/                   # Lead Architecture & HTTP Engine
│   ├── App.java
│   ├── BookingStore.java
│   └── README_KABIR.md
│
├── Shaikh_Rahim/                  # Movie Domain & Frontend Templates
│   ├── Movie.java
│   ├── index.html
│   ├── posters/
│   └── README_RAHIM.md
│
├── Shruti_Wadhonkar/              # Customer Domain & Design System
│   ├── Customer.java
│   ├── style.css
│   └── README_SHRUTI.md
│
├── Shivani_Dudhane/               # Billing Engine & Client Scripting
│   ├── Ticket.java
│   ├── PremiumTicket.java
│   ├── app.js
│   └── README_SHIVANI.md
│
├── run_lumiere.bat                # 1-Click Launch Script
├── README.md                      # Project Documentation
└── .gitignore                     # Git Configuration
```

---

## How to Run

### Option 1: 1-Click Launch (Windows)
Double-click `run_lumiere.bat`. The script automatically compiles all modules, starts the server on `http://localhost:8080`, and opens the default browser.

### Option 2: Command Line
```bash
# Compile all modules
javac -encoding UTF-8 Kabir_Ubale/*.java Shaikh_Rahim/*.java Shruti_Wadhonkar/*.java Shivani_Dudhane/*.java

# Run the application
java -cp "Kabir_Ubale;Shaikh_Rahim;Shruti_Wadhonkar;Shivani_Dudhane;." App
```

### Option 3: Terminal Verification Mode
To run automated test assertions across all OOP concepts (constructors, composition, polymorphism, validation, seat collision, and cancellation) in the console:
```bash
java -cp "Kabir_Ubale;Shaikh_Rahim;Shruti_Wadhonkar;Shivani_Dudhane;." App demo
```
