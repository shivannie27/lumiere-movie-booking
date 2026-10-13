// =====================================================================
// LUMIÈRE CINEMA CLIENT CONTROLLER
// Contributed by: Shivani Dudhane
// =====================================================================

const UI = {
    movies: [],
    bookings: [],
    bookedSeats: [],
    currentMovie: null,
    currentCinema: "PVR Koramangala",
    currentShowTime: "04:30 PM",
    selectedSeats: [],
    isPremium: false,
    selectedGenre: "All",

    cinemas: ["PVR Koramangala", "INOX Orion Mall", "Cinepolis Nexus", "Miraj Cinemas"],
    times: ["10:30 AM", "01:30 PM", "04:45 PM", "08:00 PM", "11:15 PM"],
    genres: ["All", "Action", "Comedy", "Drama", "Sci-Fi", "Animation", "Thriller"],

    async init() {
        this.renderGenres();
        await this.loadMovies();
        this.showScreen("home");
    },

    showScreen(screenId) {
        document.querySelectorAll(".screen").forEach(s => s.classList.remove("active"));
        document.querySelectorAll(".nav-pills .pill-btn").forEach(b => b.classList.remove("active"));
        const screen = document.getElementById(`screen-${screenId}`);
        if (screen) screen.classList.add("active");
        const navBtn = document.getElementById(`nav-${screenId}`);
        if (navBtn) navBtn.classList.add("active");
        if (screenId === "bookings") this.loadBookings();
        if (screenId === "summary") this.loadSummary();
    },

    // ---------- Movie Display & Filtering ----------
    renderGenres() {
        document.getElementById("genre-bar").innerHTML = this.genres.map(g =>
            `<button class="genre-pill ${g === this.selectedGenre ? 'active' : ''}" onclick="UI.setGenre('${g}')">${g}</button>`
        ).join("");
    },

    setGenre(g) {
        this.selectedGenre = g;
        this.renderGenres();
        this.renderMovies();
    },

    filterMovies() { this.renderMovies(); },

    async loadMovies() {
        try {
            const res = await fetch("/api/movies");
            this.movies = await res.json();
            this.renderMovies();
        } catch (e) {
            console.error("Failed to load movies from Java server", e);
        }
    },

    renderMovies() {
        const query = (document.getElementById("movie-search")?.value || "").toLowerCase();
        const grid = document.getElementById("movies-grid");
        const filtered = this.movies.filter(m => {
            const matchesGenre = this.selectedGenre === "All" || m.genre.includes(this.selectedGenre);
            const matchesQuery = !query || m.title.toLowerCase().includes(query);
            return matchesGenre && matchesQuery;
        });
        if (filtered.length === 0) {
            grid.innerHTML = `<p style="grid-column: 1/-1; color: var(--sage);">No movies found.</p>`;
            return;
        }
        grid.innerHTML = filtered.map(m => `
            <div class="movie-card" onclick="UI.openDetails('${m.title}')">
                <div class="poster-wrap">
                    <img src="${m.poster}" alt="${m.title}" class="poster-img" onerror="this.src='https://placehold.co/300x450/3A0E12/FED7B8?text=${encodeURIComponent(m.title)}'">
                </div>
                <div class="movie-info">
                    <h3>${m.title}</h3>
                    <div class="movie-meta">${m.genre} &bull; Rating ${m.rating}/10</div>
                </div>
            </div>
        `).join("");
    },

    openDetails(title) {
        this.currentMovie = this.movies.find(m => m.title === title);
        if (!this.currentMovie) return;
        document.getElementById("details-poster-img").src = this.currentMovie.poster;
        document.getElementById("details-title").innerText = this.currentMovie.title;
        document.getElementById("details-rating").innerText = `Rating ${this.currentMovie.rating} / 10`;
        document.getElementById("details-meta").innerText = `Language: ${this.currentMovie.language} | Duration: ${this.currentMovie.duration} | Base Price: Rs. ${this.currentMovie.price.toFixed(2)}`;
        document.getElementById("cinema-pills").innerHTML = this.cinemas.map(c => `
            <button class="genre-pill ${c === this.currentCinema ? 'active' : ''}" onclick="UI.selectCinema('${c}')">${c}</button>
        `).join("");
        this.renderShowtimes();
        this.showScreen("details");
    },

    selectCinema(c) {
        this.currentCinema = c;
        this.openDetails(this.currentMovie.title);
    },

    renderShowtimes() {
        document.getElementById("showtime-pills").innerHTML = this.times.map(t => `
            <button class="pill-btn outline" onclick="UI.startBooking('${t}')">${t}</button>
        `).join("");
    },

    // ---------- Interactive Seat Selection ----------
    async startBooking(time) {
        this.currentShowTime = `${this.currentCinema} | ${time}`;
        this.selectedSeats = [];
        this.isPremium = false;
        document.getElementById("book-movie-title").innerText = this.currentMovie.title;
        document.getElementById("book-movie-show").innerText = this.currentShowTime;
        document.getElementById("cust-name").value = "";
        document.getElementById("cust-phone").value = "";

        // Query Java Backend for occupied seats
        const encMovie = encodeURIComponent(this.currentMovie.title);
        const encShow = encodeURIComponent(this.currentShowTime);
        const res = await fetch(`/api/booked-seats?movie=${encMovie}&show=${encShow}`);
        this.bookedSeats = await res.json();

        this.updateTypeButtons();
        this.renderSeats();
        this.updateBill();
        this.showScreen("booking");
    },

    renderSeats() {
        const matrix = document.getElementById("seat-matrix");
        const rows = 10, cols = 12;
        let html = "";
        for (let r = 0; r < rows; r++) {
            const rowLetter = String.fromCharCode(65 + r);
            html += `<div class="seat-row"><span class="row-label">${rowLetter}</span>`;
            for (let c = 1; c <= cols; c++) {
                if (c === 7) html += `<div class="aisle-gap"></div>`;
                const seatName = `${rowLetter}${c}`;
                const isBooked = this.bookedSeats.includes(seatName);
                const isSelected = this.selectedSeats.includes(seatName);
                const cls = isBooked ? "seat-btn booked" : isSelected ? "seat-btn selected" : "seat-btn";
                html += `<button class="${cls}" onclick="UI.toggleSeat('${seatName}', ${isBooked})">${seatName}</button>`;
            }
            html += `</div>`;
        }
        matrix.innerHTML = html;
        document.getElementById("selected-seats-display").innerText = this.selectedSeats.length > 0 ? this.selectedSeats.join(", ") : "None";
    },

    toggleSeat(seatName, isBooked) {
        if (isBooked) return;
        const idx = this.selectedSeats.indexOf(seatName);
        if (idx !== -1) {
            this.selectedSeats.splice(idx, 1);
        } else {
            if (this.selectedSeats.length >= 8) {
                alert("You can select at most 8 seats.");
                return;
            }
            this.selectedSeats.push(seatName);
        }
        this.renderSeats();
        this.updateBill();
    },

    // ---------- Price Calculations & Form Handling ----------
    setTicketType(premium) {
        this.isPremium = premium;
        this.updateTypeButtons();
        this.updateBill();
    },

    updateTypeButtons() {
        document.getElementById("btn-type-regular").className = this.isPremium ? "pill-btn outline" : "pill-btn filled";
        document.getElementById("btn-type-premium").className = this.isPremium ? "pill-btn filled" : "pill-btn outline";
    },

    updateBill() {
        const unit = this.currentMovie.price + (this.isPremium ? 100 : 0);
        const count = this.selectedSeats.length;
        const base = count * unit;
        const fee = count * 10;
        const gst = (base + fee) * 0.18;
        const total = base + fee + gst;
        document.getElementById("bill-base").innerText = `Rs. ${base.toFixed(2)}`;
        document.getElementById("bill-fee").innerText = `Rs. ${fee.toFixed(2)}`;
        document.getElementById("bill-gst").innerText = `Rs. ${gst.toFixed(2)}`;
        document.getElementById("bill-total").innerText = `Rs. ${total.toFixed(2)}`;
    },

    async proceedToPay() {
        const name = document.getElementById("cust-name").value.trim();
        const phone = document.getElementById("cust-phone").value.trim();

        if (this.selectedSeats.length === 0) {
            alert("Please select at least one seat.");
            return;
        }

        try {
            const res = await fetch("/api/book", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    movie: this.currentMovie.title,
                    show: this.currentShowTime,
                    premium: this.isPremium.toString(),
                    name: name,
                    phone: phone,
                    seats: this.selectedSeats
                })
            });

            const data = await res.json();
            if (data.error) throw new Error(data.error);

            this.showConfirmation(data);
        } catch (err) {
            alert(err.message);
        }
    },

    showConfirmation(ticket) {
        document.getElementById("confirm-details").innerHTML = `
            <div class="price-row"><span>Booking ID</span><b>${ticket.id}</b></div>
            <div class="price-row"><span>Movie</span><b>${ticket.movie.title}</b></div>
            <div class="price-row"><span>Show</span><b>${ticket.showTime}</b></div>
            <div class="price-row"><span>Customer</span><b>${ticket.customer.name} (${ticket.customer.phone})</b></div>
            <div class="price-row"><span>Seats</span><b>${ticket.seats.join(", ")} (${ticket.type})</b></div>
            <div class="price-row total"><span>Total Paid</span><b>Rs. ${parseFloat(ticket.total).toFixed(2)}</b></div>
        `;
        this.showScreen("confirmed");
    },

    // ---------- Bookings & Receipts ----------
    async loadBookings() {
        try {
            const res = await fetch("/api/bookings");
            this.bookings = await res.json();
            this.renderBookings();
        } catch (e) {
            console.error("Failed to load bookings from Java server", e);
        }
    },

    renderBookings() {
        const q = (document.getElementById("bookings-search")?.value || "").toLowerCase();
        const filtered = this.bookings.filter(t =>
            t.id.toLowerCase().includes(q) ||
            t.customer.name.toLowerCase().includes(q) ||
            t.movie.title.toLowerCase().includes(q)
        );

        const tbody = document.getElementById("bookings-table-body");
        document.getElementById("bookings-count").innerText = `${filtered.length} booking(s)`;
        if (filtered.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color: var(--sage); padding: 24px;">No bookings found.</td></tr>`;
            return;
        }

        tbody.innerHTML = filtered.map(t => `
            <tr>
                <td><b>${t.id}</b></td>
                <td>${t.movie.title}</td>
                <td style="font-size: 12px;">${t.showTime}</td>
                <td>${t.seats.join(", ")}</td>
                <td><span style="font-size: 11px; padding: 2px 8px; border-radius: 6px; background: var(--cream); font-weight: bold;">${t.type}</span></td>
                <td>${t.customer.name}</td>
                <td><b>Rs. ${parseFloat(t.total).toFixed(2)}</b></td>
                <td>
                    <button class="pill-btn outline" style="padding: 4px 10px; font-size: 11px;" onclick="UI.viewBill('${t.id}')">Bill</button>
                    <button class="pill-btn outline" style="padding: 4px 10px; font-size: 11px; color: #a32b2b; border-color: #a32b2b;" onclick="UI.cancelBooking('${t.id}')">Cancel</button>
                </td>
            </tr>
        `).join("");
    },

    viewBill(id) {
        const t = this.bookings.find(b => b.id === id);
        if (!t) return;
        const count = t.seats.length;
        const unit = t.type === "Premium" ? t.movie.price + 100 : t.movie.price;
        const base = count * unit;
        const fee = count * 10;
        const gst = (base + fee) * 0.18;
        document.getElementById("modal-bill-content").innerHTML = `
            <div class="price-row"><span>Booking ID</span><b>${t.id}</b></div>
            <div class="price-row"><span>Movie</span><b>${t.movie.title}</b></div>
            <div class="price-row"><span>Show</span><b>${t.showTime}</b></div>
            <div class="price-row"><span>Customer</span><b>${t.customer.name}</b></div>
            <div class="price-row"><span>Seats</span><b>${t.seats.join(", ")} (${t.type})</b></div>
            <div class="price-row"><span>Base (${count} x Rs. ${unit.toFixed(2)})</span><span>Rs. ${base.toFixed(2)}</span></div>
            <div class="price-row"><span>Convenience Fee</span><span>Rs. ${fee.toFixed(2)}</span></div>
            <div class="price-row"><span>GST (18%)</span><span>Rs. ${gst.toFixed(2)}</span></div>
            <div class="price-row total"><span>Total Amount</span><b>Rs. ${parseFloat(t.total).toFixed(2)}</b></div>
        `;
        document.getElementById("bill-modal").style.display = "flex";
    },

    closeBillModal() {
        document.getElementById("bill-modal").style.display = "none";
    },

    async cancelBooking(id) {
        if (!confirm(`Cancel booking ${id}? Its seats will be freed immediately.`)) return;
        try {
            const res = await fetch("/api/cancel", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ id: id })
            });
            const data = await res.json();
            if (data.success) {
                await this.loadBookings();
            }
        } catch (e) {
            alert("Could not cancel booking");
        }
    },

    // ---------- Summary Analytics ----------
    async loadSummary() {
        try {
            const res = await fetch("/api/summary");
            const data = await res.json();
            document.getElementById("summary-tiles").innerHTML = `
                <div class="stat-card">
                    <div class="stat-val">${data.active}</div>
                    <div class="stat-name">Active Bookings</div>
                </div>
                <div class="stat-card">
                    <div class="stat-val">${data.seats}</div>
                    <div class="stat-name">Seats Booked</div>
                </div>
                <div class="stat-card featured">
                    <div class="stat-val">Rs. ${data.revenue.toFixed(2)}</div>
                    <div class="stat-name">Total Revenue</div>
                </div>
                <div class="stat-card">
                    <div class="stat-val">${data.created}</div>
                    <div class="stat-name">Tickets Created (Counter)</div>
                </div>
                <div class="stat-card">
                    <div class="stat-val">${data.gst}%</div>
                    <div class="stat-name">GST Rate</div>
                </div>
                <div class="stat-card">
                    <div class="stat-val">Rs. ${data.fee.toFixed(2)}</div>
                    <div class="stat-name">Convenience Fee / Seat</div>
                </div>
            `;
        } catch (e) {
            console.error("Failed to load summary", e);
        }
    }
};

window.addEventListener("DOMContentLoaded", () => UI.init());
