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
    private double snacksTotal = 0.0;
    private double discountPercent = 0.0;
    private double discountAmount = 0.0;

    private static int totalBookings = 0;
    private static String theatreName = "Lumière Cinemas";
    private static double gstPercent = 18.0;
    private static final double CONVENIENCE_FEE = 10.0;

    public Ticket(String bookingId, Movie movie, Customer customer, String showTime, String[] seats) {
        this(bookingId, movie, customer, showTime, seats, 0.0, 0.0);
    }

    public Ticket(String bookingId, Movie movie, Customer customer, String showTime, String[] seats, double snacksTotal, double discountAmount) {
        if (seats == null || seats.length == 0) {
            throw new IllegalArgumentException("Please select at least one seat.");
        }
        this.bookingId = bookingId;
        this.movie = movie;
        this.customer = customer;
        this.showTime = showTime;
        this.seats = seats.clone();
        this.pricePerSeat = movie.getPrice();
        this.snacksTotal = Math.max(0.0, snacksTotal);
        this.discountAmount = Math.max(0.0, discountAmount);
        totalBookings++;
    }

    public String getBookingId()       { return bookingId; }
    public Movie getMovie()            { return movie; }
    public Customer getCustomer()      { return customer; }
    public String getShowTime()        { return showTime; }
    public String[] getSeats()         { return seats.clone(); }
    public double getPricePerSeat()    { return pricePerSeat; }
    public double getSnacksTotal()     { return snacksTotal; }
    public double getDiscountAmount()  { return discountAmount; }
    public static String getSeatTier(String seat) {
        if (seat == null || seat.trim().isEmpty()) return "Club";
        char r = Character.toUpperCase(seat.trim().charAt(0));
        if (r >= 'I') return "VIP Recliner";
        if (r >= 'E') return "Executive";
        return "Club";
    }

    public static double getTierSurcharge(String seat) {
        String tier = getSeatTier(seat);
        if ("VIP Recliner".equals(tier)) return 350.0;
        if ("Executive".equals(tier)) return 150.0;
        return 0.0;
    }

    public double calculateBaseSeatsTotal() {
        double sum = 0.0;
        for (String s : seats) {
            sum += pricePerSeat + getTierSurcharge(s);
        }
        return sum;
    }

    public String getType() {
        boolean hasVip = false;
        boolean hasExec = false;
        boolean hasClub = false;
        for (String s : seats) {
            String tier = getSeatTier(s);
            if ("VIP Recliner".equals(tier)) hasVip = true;
            else if ("Executive".equals(tier)) hasExec = true;
            else hasClub = true;
        }
        if (hasVip && !hasExec && !hasClub) return "VIP Recliner";
        if (hasExec && !hasVip && !hasClub) return "Executive";
        if (hasClub && !hasVip && !hasExec) return "Club Classic";
        return "Multi-Tier";
    }

    public double getSeatPriceWithExtras() {
        return pricePerSeat;
    }

    protected double gstMultiplier() {
        return 1.0 + (gstPercent / 100.0);
    }

    /** Primary polymorphic bill calculation with tiered seat prices */
    public double calculateBill() {
        double base = calculateBaseSeatsTotal();
        double netBase = Math.max(0.0, base - discountAmount);
        double fee = seats.length * CONVENIENCE_FEE;
        double ticketsTotal = (netBase + fee) * gstMultiplier();
        return ticketsTotal + snacksTotal;
    }

    /** Method Overloading: calculate bill with seat quantity */
    public double calculateBill(int qty) {
        return calculateBill(qty, 0.0);
    }

    /** Method Overloading: calculate bill with coupon discount percentage */
    public double calculateBill(int qty, double discountPct) {
        double base = qty * pricePerSeat;
        double disc = base * (discountPct / 100.0);
        double fee = qty * CONVENIENCE_FEE;
        return ((base - disc) + fee) * gstMultiplier() + snacksTotal;
    }

    public static int getTotalBookings()     { return totalBookings; }
    public static String getTheatreName()    { return theatreName; }
    public static double getGstPercent()     { return gstPercent; }
    public static double getConvenienceFee() { return CONVENIENCE_FEE; }

    public static double[] breakdown(double unitPrice, int qty, double discount, double snacks) {
        double base = qty * unitPrice;
        double netBase = Math.max(0.0, base - discount);
        double fee = qty * CONVENIENCE_FEE;
        double gst = (netBase + fee) * (gstPercent / 100.0);
        double total = (netBase + fee + gst) + snacks;
        return new double[]{base, fee, gst, total, discount, snacks};
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
        sb.append("\"snacks\":").append(String.format(java.util.Locale.US, "%.2f", snacksTotal)).append(",");
        sb.append("\"discount\":").append(String.format(java.util.Locale.US, "%.2f", discountAmount)).append(",");
        sb.append("\"total\":").append(String.format(java.util.Locale.US, "%.2f", calculateBill())).append("}");
        return sb.toString();
    }
}
