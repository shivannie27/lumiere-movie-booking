// =====================================================================
// LUMIÈRE CINEMA BOOKING SYSTEM - CUSTOMER ENTITY & VALIDATION
// Contributed by: Shruti Wadhonkar
// =====================================================================

public class Customer {
    private String name;
    private String phone;

    public Customer(String name, String phone) {
        setName(name);
        setPhone(phone);
    }

    public String getName()  { return name; }
    public String getPhone() { return phone; }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty.");
        }
        this.name = name.trim();
    }

    public void setPhone(String phone) {
        if (phone == null || !phone.trim().matches("\\d{10}")) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits.");
        }
        this.phone = phone.trim();
    }

    public String toJson() {
        return String.format("{\"name\":\"%s\",\"phone\":\"%s\"}", name.replace("\"", "\\\""), phone);
    }

    @Override
    public String toString() {
        return name + " (" + phone + ")";
    }
}
