# Contribution: Shivani Dudhane (Billing & Client Scripting Lead)

## Owned Components
1. **`Ticket.java`**: Core domain entity demonstrating:
   - OOP Composition: `Ticket HAS-A Movie` and `Ticket HAS-A Customer`.
   - Encapsulation with private attributes, getters, and defensive array cloning (`seats.clone()`).
   - Method Overloading: `calculateBill()` (standard polymorphic calculation), `calculateBill(qty)` (quantity-based), and `calculateBill(qty, discountPct)` (discount-adjusted).
   - Static breakdown helper computing base, fee, GST, discount, and snacks.
2. **`PremiumTicket.java`**: Demonstrates OOP inheritance (`IS-A Ticket`) and runtime polymorphism by overriding `calculateBill()` to incorporate the Rs. 100 recliner surcharge while preserving all base discount and concession logic.
3. **`app.js`**: Frontend JavaScript controller:
   - Connects the browser to Kabir's Java backend REST API (`/api/movies`, `/api/cities`, `/api/coupon`, `/api/booked-seats`, `/api/book`, `/api/cancel`, `/api/summary`).
   - Multi-city selector updating multiplexes dynamically.
   - YouTube trailer modal opening and playback lifecycle.
   - Interactive 10x12 seat selection with Web Audio API sound synthesis (crisp click on selection, triumphant chime on booking).
   - Theater Concessions F&B cart management (Popcorn, Nachos, Cold Drinks, Artisanal Coffee, Hot Dogs).
   - Promo Code verification and live bill breakdown synchronization.
   - Deterministic SVG QR Code generator for digital cinema boarding pass without third-party dependencies.
   - Browser printing trigger (`window.print()`).

