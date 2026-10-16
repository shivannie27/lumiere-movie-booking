// =====================================================================
// LUMIÈRE CINEMA BOOKING SYSTEM - CENTRAL TRANSACTION STORE & SEAT ENGINE
// Contributed by: Kabir Ubale (Lead Architecture & Backend Core)
// =====================================================================

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingStore {
    private final List<Movie> movies = new ArrayList<>();
    private final List<Ticket> bookings = new ArrayList<>();
    private final Map<String, List<String>> cityCinemas = new HashMap<>();
    private int nextBookingNumber = 100001;

    public BookingStore() {
        // Initialize detailed movie catalog with trailers, synopsis & cast
        movies.add(new Movie(
            "Dune: Part Two", "English", "Sci-Fi / Adventure", 166, 350, 8.8,
            "posters/dune2.jpg", "UA 16+",
            "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
            "Timothée Chalamet, Zendaya, Rebecca Ferguson, Javier Bardem",
            "Way9Dexny3w"
        ));
        movies.add(new Movie(
            "Deadpool & Wolverine", "English", "Action / Comedy", 128, 380, 8.2,
            "posters/deadpool.jpg", "A 18+",
            "Wolverine is recovering from his injuries when he crosses paths with the loudmouth Deadpool. They team up to defeat a common enemy.",
            "Ryan Reynolds, Hugh Jackman, Emma Corrin, Matthew Macfadyen",
            "73_1biulkYk"
        ));
        movies.add(new Movie(
            "Inside Out 2", "English", "Animation / Family", 96, 300, 8.4,
            "posters/insideout2.jpg", "U",
            "Joy, Sadness, Anger, Fear and Disgust are back as Riley enters puberty, welcoming brand new emotions including Anxiety and Envy.",
            "Amy Poehler, Maya Hawke, Kensington Tallman, Liza Lapira",
            "LEjhY15eCx0"
        ));
        movies.add(new Movie(
            "Oppenheimer", "English", "Biography / Drama", 180, 400, 8.6,
            "posters/oppenheimer.jpg", "UA 16+",
            "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb during World War II.",
            "Cillian Murphy, Emily Blunt, Matt Damon, Robert Downey Jr.",
            "uYPbbksJxIg"
        ));
        movies.add(new Movie(
            "Kalki 2898 AD", "Hindi", "Action / Sci-Fi", 181, 360, 8.1,
            "posters/kalki.jpg", "UA 13+",
            "In a post-apocalyptic world in the year 2898 AD, a modern avatar of Vishnu descends to Earth to protect the world from evil forces.",
            "Prabhas, Amitabh Bachchan, Kamal Haasan, Deepika Padukone",
            "Vv3Z6yV9R2Y"
        ));
        movies.add(new Movie(
            "Laapataa Ladies", "Hindi", "Comedy / Drama", 122, 280, 8.4,
            "posters/laapataa.jpg", "UA 13+",
            "Two young brides get accidentally swapped on a train journey in rural India, leading to a heartwarming voyage of self-discovery.",
            "Nitanshi Goel, Pratibha Ranta, Sparsh Shrivastava, Ravi Kishan",
            "1U_2A1fK59A"
        ));
        movies.add(new Movie(
            "Maharaja", "Tamil", "Thriller / Crime", 150, 320, 8.5,
            "posters/maharaja.jpg", "UA 16+",
            "A humble barber sets out on a relentless quest to retrieve his stolen 'Lakshmi', leading the police into a complex labyrinth of crime.",
            "Vijay Sethupathi, Anurag Kashyap, Mamta Mohandas, Natty",
            "Yx3b0GjKkQ0"
        ));
        movies.add(new Movie(
            "Furiosa: A Mad Max Saga", "English", "Action / Adventure", 148, 380, 7.9,
            "posters/furiosa.jpg", "A 18+",
            "As the world falls, young Furiosa is snatched from the Green Place of Many Mothers and falls into the hands of a great Biker Horde.",
            "Anya Taylor-Joy, Chris Hemsworth, Tom Burke, Alyla Browne",
            "XJMuhwVlca4"
        ));

        // Location & Cinema Directory
        List<String> bng = new ArrayList<>();
        bng.add("PVR Koramangala (Dolby Atmos)");
        bng.add("INOX Orion Mall (IMAX 4K)");
        bng.add("Cinepolis Nexus Whitefield (VIP)");
        bng.add("Miraj Cinemas Bangalore");
        cityCinemas.put("Bengaluru", bng);

        List<String> mum = new ArrayList<>();
        mum.add("PVR ICON Phoenix Palladium (Laser)");
        mum.add("INOX Megaplex Inorbit Malad (IMAX)");
        mum.add("Cinepolis Fun Republic Andheri");
        mum.add("Miraj Cinemas Wadala");
        cityCinemas.put("Mumbai", mum);

        List<String> del = new ArrayList<>();
        del.add("PVR Director's Cut Vasant Kunj (Luxury)");
        del.add("INOX Nehru Place (Dolby 7.1)");
        del.add("Cinepolis DLF Avenue Saket");
        del.add("Miraj Cinemas Subhash Nagar");
        cityCinemas.put("Delhi NCR", del);

        List<String> pun = new ArrayList<>();
        pun.add("PVR Phoenix Marketcity Viman Nagar");
        pun.add("INOX Bund Garden Road");
        pun.add("Cinepolis Seasons Mall Magarpatta");
        pun.add("City Pride Kothrud");
        cityCinemas.put("Pune", pun);

        List<String> hyd = new ArrayList<>();
        hyd.add("PVR Next Galleria Mall Panjagutta");
        hyd.add("INOX GVK One Banjara Hills");
        hyd.add("Cinepolis Manjeera Mall Kukatpally");
        hyd.add("Prasads Multiplex NTR Marg");
        cityCinemas.put("Hyderabad", hyd);
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public Map<String, List<String>> getCityCinemas() {
        return cityCinemas;
    }

    public synchronized String nextBookingId() {
        return "LUM" + (nextBookingNumber++);
    }

    public Movie findMovie(String title) {
        for (Movie m : movies) {
            if (m.getTitle().equalsIgnoreCase(title.trim())) return m;
        }
        return null;
    }

    // Temporary 5-minute Seat Hold Registry with TTL (Concurrency Control)
    private final Map<String, Long> heldSeats = new HashMap<>();

    /** Seat reservation engine with collision detection */
    public synchronized void add(Ticket ticket) {
        if (ticket == null) throw new IllegalArgumentException("Booking cannot be null.");
        for (String seat : ticket.getSeats()) {
            if (isSeatBooked(ticket.getMovie().getTitle(), ticket.getShowTime(), seat)) {
                throw new IllegalStateException("Seat " + seat + " is already booked for this show.");
            }
        }
        // Remove temporary holds once confirmed
        for (String seat : ticket.getSeats()) {
            heldSeats.remove(ticket.getMovie().getTitle() + "|" + ticket.getShowTime() + "|" + seat.toUpperCase());
        }
        bookings.add(ticket);
    }

    /** Place temporary hold on seats for 5 minutes (TTL concurrency) */
    public synchronized boolean holdSeats(String movieTitle, String showTime, String[] seats, long durationMs) {
        long now = System.currentTimeMillis();
        heldSeats.entrySet().removeIf(e -> e.getValue() < now);

        for (String seat : seats) {
            String s = seat.toUpperCase();
            if (isSeatBooked(movieTitle, showTime, s)) return false;
            String key = movieTitle + "|" + showTime + "|" + s;
            Long exp = heldSeats.get(key);
            if (exp != null && exp > now) return false;
        }

        long expiryTime = now + durationMs;
        for (String seat : seats) {
            heldSeats.put(movieTitle + "|" + showTime + "|" + seat.toUpperCase(), expiryTime);
        }
        return true;
    }

    /** Release held seats when user cancels or selection changes */
    public synchronized void releaseSeats(String movieTitle, String showTime, String[] seats) {
        for (String seat : seats) {
            heldSeats.remove(movieTitle + "|" + showTime + "|" + seat.toUpperCase());
        }
    }

    /** Check if a seat is currently held */
    public synchronized boolean isSeatHeld(String movieTitle, String showTime, String seat) {
        long now = System.currentTimeMillis();
        String key = movieTitle + "|" + showTime + "|" + seat.toUpperCase();
        Long exp = heldSeats.get(key);
        return exp != null && exp > now;
    }

    /** Check if a specific seat is already occupied */
    public synchronized boolean isSeatBooked(String movieTitle, String showTime, String seat) {
        for (Ticket t : bookings) {
            if (t.getMovie().getTitle().equalsIgnoreCase(movieTitle) && t.getShowTime().equalsIgnoreCase(showTime)) {
                for (String s : t.getSeats()) {
                    if (s.equalsIgnoreCase(seat)) return true;
                }
            }
        }
        return false;
    }

    /** Instant cancellation releasing occupied seats */
    public synchronized boolean cancel(String bookingId) {
        if (bookingId == null) return false;
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).getBookingId().equalsIgnoreCase(bookingId.trim())) {
                bookings.remove(i);
                return true;
            }
        }
        return false;
    }

    public synchronized Ticket findById(String bookingId) {
        for (Ticket t : bookings) {
            if (t.getBookingId().equalsIgnoreCase(bookingId.trim())) return t;
        }
        return null;
    }

    public synchronized List<Ticket> search(String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>(bookings);
        String q = query.trim().toLowerCase();
        List<Ticket> results = new ArrayList<>();
        for (Ticket t : bookings) {
            if (t.getBookingId().toLowerCase().contains(q) ||
                t.getCustomer().getName().toLowerCase().contains(q) ||
                t.getMovie().getTitle().toLowerCase().contains(q)) {
                results.add(t);
            }
        }
        return results;
    }

    public synchronized double getTotalRevenue() {
        double total = 0;
        for (Ticket t : bookings) total += t.calculateBill();
        return total;
    }

    public synchronized int getTotalSeatsBooked() {
        int seats = 0;
        for (Ticket t : bookings) seats += t.getSeats().length;
        return seats;
    }

    public synchronized int getCount() {
        return bookings.size();
    }

    public synchronized String getBookingsJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bookings.size(); i++) {
            sb.append(bookings.get(i).toJson()).append(i < bookings.size() - 1 ? "," : "");
        }
        sb.append("]");
        return sb.toString();
    }

    public String getMoviesJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < movies.size(); i++) {
            sb.append(movies.get(i).toJson()).append(i < movies.size() - 1 ? "," : "");
        }
        sb.append("]");
        return sb.toString();
    }

    public String getCitiesJson() {
        StringBuilder sb = new StringBuilder("{");
        int cIdx = 0;
        for (Map.Entry<String, List<String>> entry : cityCinemas.entrySet()) {
            sb.append("\"").append(entry.getKey()).append("\":[");
            List<String> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                sb.append("\"").append(list.get(i)).append("\"").append(i < list.size() - 1 ? "," : "");
            }
            sb.append("]").append(cIdx < cityCinemas.size() - 1 ? "," : "");
            cIdx++;
        }
        sb.append("}");
        return sb.toString();
    }
}
