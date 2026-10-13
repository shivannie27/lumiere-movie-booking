# Contribution: Shivani Dudhane (Billing & Client Scripting Lead)

## Owned Components
1. **`Ticket.java`**: Core entity combining `Movie` and `Customer` through composition (`Ticket HAS-A Movie` and `Ticket HAS-A Customer`), along with class-level static settings.
2. **`PremiumTicket.java`**: Demonstrates OOP inheritance (`IS-A Ticket`) and runtime polymorphism by overriding `calculateBill()` to incorporate the Rs. 100 recliner surcharge.
3. **`app.js`**: Frontend JavaScript controller:
   - Connects the browser to Kabir's Java backend REST API (`/api/movies`, `/api/booked-seats`, `/api/book`, `/api/cancel`, `/api/summary`).
   - Dynamic real-time billing breakdown calculations.
   - Interactive seat selection logic (limit 8 seats) and booking confirmation rendering.
