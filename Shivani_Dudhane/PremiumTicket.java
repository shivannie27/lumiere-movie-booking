// =====================================================================
// LUMIÈRE CINEMA BOOKING SYSTEM - PREMIUM TICKET (INHERITANCE)
// Contributed by: Shivani Dudhane
// =====================================================================

public class PremiumTicket extends Ticket {
    public static final double RECLINER_FEE = 100.0;

    public PremiumTicket(String bookingId, Movie movie, Customer customer, String showTime, String[] seats) {
        super(bookingId, movie, customer, showTime, seats);
    }

    public PremiumTicket(String bookingId, Movie movie, Customer customer, String showTime, String[] seats, double snacksTotal, double discountAmount) {
        super(bookingId, movie, customer, showTime, seats, snacksTotal, discountAmount);
    }

    @Override
    public String getType() {
        return "Premium";
    }

    @Override
    public double getSeatPriceWithExtras() {
        return super.getSeatPriceWithExtras() + RECLINER_FEE;
    }

    @Override
    public double calculateBill() {
        double reclinerTotal = getSeats().length * RECLINER_FEE;
        return super.calculateBill() + (reclinerTotal * gstMultiplier());
    }
}
