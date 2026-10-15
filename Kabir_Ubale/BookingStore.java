// =====================================================================
// LUMIÈRE CINEMA BOOKING SYSTEM - CENTRAL TRANSACTION STORE & SEAT ENGINE
// Contributed by: Kabir Ubale (Lead Architecture & Backend Core)
// =====================================================================

import java.util.ArrayList;
import java.util.List;

public class BookingStore {
    private final List<Movie> movies = new ArrayList<>();
    private final List<Ticket> bookings = new ArrayList<>();
    private int nextBookingNumber = 100001;

    public BookingStore() {
        // Initialize default movie catalog with real poster assets
        movies.add(new Movie("Dune: Part Two", "English", "Sci-Fi / Adventure", 166, 180, 8.8, "posters/dune2.jpg"));
        movies.add(new Movie("Deadpool & Wolverine", "English", "Action / Comedy", 128, 200, 8.2, "posters/deadpool.jpg"));
        movies.add(new Movie("Inside Out 2", "English", "Animation / Family", 96, 150, 8.4, "posters/insideout2.jpg"));
        movies.add(new Movie("Oppenheimer", "English", "Biography / Drama", 180, 220, 8.6, "posters/oppenheimer.jpg"));
        movies.add(new Movie("Kalki 2898 AD", "Hindi", "Action / Sci-Fi", 181, 190, 8.1, "posters/kalki.jpg"));
        movies.add(new Movie("Laapataa Ladies", "Hindi", "Comedy / Drama", 122, 160, 8.4, "posters/laapataa.jpg"));
        movies.add(new Movie("Maharaja", "Tamil", "Thriller / Crime", 150, 170, 8.5, "posters/maharaja.jpg"));
        movies.add(new Movie("Furiosa", "English", "Action / Adventure", 148, 210, 7.9, "posters/furiosa.jpg"));
    }

    public List<Movie> getMovies() {
        return movies;
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

    /** Seat reservation engine with collision detection */
    public synchronized void add(Ticket ticket) {
        if (ticket == null) throw new IllegalArgumentException("Booking cannot be null.");
        for (String seat : ticket.getSeats()) {
            if (isSeatBooked(ticket.getMovie().getTitle(), ticket.getShowTime(), seat)) {
                throw new IllegalStateException("Seat " + seat + " is already booked for this show.");
            }
        }
        bookings.add(ticket);
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
}
