// =====================================================================
// LUMIÈRE CINEMA BOOKING SYSTEM - TICKET ENTITY & BILLING
// Contributed by: Shivani Dudhane
// =====================================================================

public class Ticket {
    private String bookingId;
    private Movie movie;        // Composition: HAS-A Movie
    private Customer customer;  // Composition: HAS-A Customer
    private String showTime;
    private String[] seats;
    private double pricePerSeat;

    private static int totalBookings = 0;
    private static String theatreName = "Lumière Cinemas";
    private static double gstPercent = 18.0;
    private static final double CONVENIENCE_FEE = 10.0;

    public Ticket(String bookingId, Movie movie, Customer customer, String showTime, String[] seats) {
        if (seats == null || seats.length == 0) {
            throw new IllegalArgumentException("Please select at least one seat.");
        }
        this.bookingId = bookingId;
        this.movie = movie;
        this.customer = customer;
        this.showTime = showTime;
        this.seats = seats.clone();
        this.pricePerSeat = movie.getPrice();
        totalBookings++;
    }

    public String getBookingId()       { return bookingId; }
    public Movie getMovie()            { return movie; }
    public Customer getCustomer()      { return customer; }
    public String getShowTime()        { return showTime; }
    public String[] getSeats()         { return seats.clone(); }
    public double getPricePerSeat()    { return pricePerSeat; }
    public String getType()            { return "Regular"; }

    public double getSeatPriceWithExtras() {
        return pricePerSeat;
    }

    protected double gstMultiplier() {
        return 1.0 + (gstPercent / 100.0);
    }

    public double calculateBill() {
        double base = seats.length * pricePerSeat;
        double fee = seats.length * CONVENIENCE_FEE;
        return (base + fee) * gstMultiplier();
    }

    public static int getTotalBookings()     { return totalBookings; }
    public static String getTheatreName()    { return theatreName; }
    public static double getGstPercent()     { return gstPercent; }
    public static double getConvenienceFee() { return CONVENIENCE_FEE; }

    public static double[] breakdown(double unitPrice, int qty) {
        double base = qty * unitPrice;
        double fee = qty * CONVENIENCE_FEE;
        double gst = (base + fee) * (gstPercent / 100.0);
        return new double[]{base, fee, gst, base + fee + gst};
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"id\":\"").append(bookingId).append("\",");
        sb.append("\"movie\":").append(movie.toJson()).append(",");
        sb.append("\"customer\":").append(customer.toJson()).append(",");
        sb.append("\"showTime\":\"").append(showTime).append("\",");
        sb.append("\"seats\":[");
        for (int i = 0; i < seats.length; i++) {
            sb.append("\"").append(seats[i]).append("\"").append(i < seats.length - 1 ? "," : "");
        }
        sb.append("],");
        sb.append("\"type\":\"").append(getType()).append("\",");
        sb.append("\"total\":").append(String.format(java.util.Locale.US, "%.2f", calculateBill())).append("}");
        return sb.toString();
    }
}
