// =====================================================================
// LUMIÈRE CINEMA BOOKING SYSTEM - MOVIE ENTITY
// Contributed by: Shaikh Rahim
// =====================================================================

public class Movie {
    private String title;
    private String language;
    private String genre;
    private int durationMinutes;
    private double price;
    private double rating;
    private String poster;

    public Movie(String title, String language, String genre, int durationMinutes, double price, double rating, String poster) {
        this.title = title.trim();
        this.language = language.trim();
        this.genre = genre.trim();
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.rating = rating;
        this.poster = poster.trim();
    }

    public String getTitle()           { return title; }
    public String getLanguage()        { return language; }
    public String getGenre()           { return genre; }
    public int getDurationMinutes()    { return durationMinutes; }
    public double getPrice()           { return price; }
    public double getRating()          { return rating; }
    public String getPoster()          { return poster; }

    public String getDurationText() {
        return (durationMinutes / 60) + "h " + (durationMinutes % 60) + "m";
    }

    public String toJson() {
        return String.format(
            "{\"title\":\"%s\",\"language\":\"%s\",\"genre\":\"%s\",\"duration\":\"%s\",\"price\":%.2f,\"rating\":%.1f,\"poster\":\"%s\"}",
            escape(title), escape(language), escape(genre), getDurationText(), price, rating, escape(poster)
        );
    }

    private String escape(String s) {
        return s.replace("\"", "\\\"");
    }
}
