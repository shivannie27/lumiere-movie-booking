# Contribution: Shaikh Rahim (Movie Catalog & Frontend Layout Lead)

## Owned Components
1. **`Movie.java`**: Domain model storing movie metadata:
   - `title`, `language`, `genre`, `durationMinutes`, `price`, `rating`, `poster`.
   - Enhanced metadata: `certification` (U, UA, A), `synopsis` (plot description), `cast` (ensemble cast), and `trailerId` (YouTube video identifier).
   - Formatted JSON serializer for client consumption.
2. **`index.html`**: The entire frontend HTML structure and user interface views:
   - Top navigation with brand and interactive City Selector dropdown (Bengaluru, Mumbai, Delhi NCR, Pune, Hyderabad).
   - Hero banner with real-time multiplex availability badge and multi-attribute search.
   - Movie catalog cards with official certification badges, rating stars, and instant trailer triggers.
   - Comprehensive Movie Details screen with format tags (IMAX 4K, 2D, Dolby Atmos), synopsis, cast list, dynamic city cinema pills, and showtimes.
   - Interactive 10x12 Seat Matrix screen with curved ambient projector glow.
   - Theater Concessions F&B drawer with counter controls.
   - Promo Code discount box with real-time application feedback.
   - Booking confirmation screen and luxury Digital Boarding Pass modal with print controls.
   - Video Trailer modal overlay for embedded YouTube playback.
   - Bookings table and Theater Analytics dashboard tiles.
3. **`posters/`**: Official theatrical poster collection for all 8 featured movies (Dune 2, Deadpool & Wolverine, Inside Out 2, Oppenheimer, Kalki 2898 AD, Laapataa Ladies, Interstellar, Stree 2).

