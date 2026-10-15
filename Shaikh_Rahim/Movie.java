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
    private String certification;
    private String synopsis;
    private String cast;
    private String trailerId;

    public Movie(String title, String language, String genre, int durationMinutes, double price, double rating, String poster) {
        this(title, language, genre, durationMinutes, price, rating, poster, "UA 13+", "Experience the cinematic spectacle on the grand screen.", "Starring Leading Ensemble", "");
    }

    public Movie(String title, String language, String genre, int durationMinutes, double price, double rating, 
                 String poster, String certification, String synopsis, String cast, String trailerId) {
        this.title = title.trim();
        this.language = language.trim();
        this.genre = genre.trim();
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.rating = rating;
        this.poster = poster.trim();
        this.certification = certification.trim();
        this.synopsis = synopsis.trim();
        this.cast = cast.trim();
        this.trailerId = trailerId.trim();
    }

    public String getTitle()           { return title; }
    public String getLanguage()        { return language; }
    public String getGenre()           { return genre; }
    public int getDurationMinutes()    { return durationMinutes; }
    public double getPrice()           { return price; }
    public double getRating()          { return rating; }
    public String getPoster()          { return poster; }
    public String getCertification()   { return certification; }
    public String getSynopsis()        { return synopsis; }
    public String getCast()            { return cast; }
    public String getTrailerId()       { return trailerId; }

    public String getDurationText() {
        return (durationMinutes / 60) + "h " + (durationMinutes % 60) + "m";
    }

    public String toJson() {
        return String.format(
            "{\"title\":\"%s\",\"language\":\"%s\",\"genre\":\"%s\",\"duration\":\"%s\",\"price\":%.2f,\"rating\":%.1f,\"poster\":\"%s\",\"certification\":\"%s\",\"synopsis\":\"%s\",\"cast\":\"%s\",\"trailerId\":\"%s\"}",
            escape(title), escape(language), escape(genre), getDurationText(), price, rating, escape(poster),
            escape(certification), escape(synopsis), escape(cast), escape(trailerId)
        );
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ");
    }
}
