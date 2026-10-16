// =====================================================================
// LUMIÈRE CINEMA CLIENT CONTROLLER
// Contributed by: Shivani Dudhane
// Enhanced: Multi-City, Trailers, Concessions F&B, Promo Codes & Digital Pass
// =====================================================================

const UI = {
    movies: [],
    bookings: [],
    bookedSeats: [],
    cityMap: {},
    cities: ["Bengaluru", "Mumbai", "Delhi NCR", "Pune", "Hyderabad"],
    currentCity: "Bengaluru",
    currentCinema: "PVR Koramangala (Dolby Atmos)",
    currentShowTime: "04:30 PM",
    currentMovie: null,
    selectedSeats: [],
    isPremium: false,
    selectedGenre: "All",

    // Concessions & Promo Code State
    concessions: [
        { id: "popcorn", name: "Jumbo Caramel Popcorn", price: 280, icon: "🍿", desc: "Buttery crisp gourmet popcorn" },
        { id: "nachos", name: "Cheese Jalapeño Nachos", price: 220, icon: "🧀", desc: "Warm tortilla chips with hot melted cheese" },
        { id: "coke", name: "Chilled Coca-Cola (500ml)", price: 140, icon: "🥤", desc: "Refreshing icy fountain beverage" },
        { id: "coffee", name: "Artisanal Cappuccino", price: 160, icon: "☕", desc: "Fresh espresso with creamy steamed milk" },
        { id: "hotdog", name: "Gourmet Theater Hot Dog", price: 190, icon: "🌭", desc: "Toasted brioche bun with smoked frank" }
    ],
    snackCart: { popcorn: 0, nachos: 0, coke: 0, coffee: 0, hotdog: 0 },
    appliedCoupon: null,
    appliedDiscount: 0.0,
    lastConfirmedTicket: null,

    cinemas: ["PVR Koramangala (Dolby Atmos)", "INOX Orion Mall (IMAX 4K)", "Cinepolis Nexus Whitefield (VIP)", "Miraj Cinemas Bangalore"],
    times: ["10:30 AM", "01:30 PM", "04:45 PM", "08:00 PM", "11:15 PM"],
    genres: ["All", "Action", "Comedy", "Drama", "Sci-Fi", "Animation", "Thriller"],

    async init() {
        const urlParams = new URLSearchParams(window.location.search);
        const ticketParam = urlParams.get("ticket") || urlParams.get("id");
        if (ticketParam) {
            window.location.href = `/ticket?id=${encodeURIComponent(ticketParam)}`;
            return;
        }

        this.renderGenres();
        this.renderSnacks();
        try {
            const sRes = await fetch("/api/server-info");
            this.serverInfo = await sRes.json();
        } catch (e) {}
        await this.loadCities();
        await this.loadMovies();
        this.showScreen("home");
    },

    showScreen(screenId) {
        if (screenId !== "booking" && screenId !== "confirmed") {
            this.stopSeatTimer();
        }
        document.querySelectorAll(".screen").forEach(s => s.classList.remove("active"));
        document.querySelectorAll(".nav-pills .pill-btn").forEach(b => b.classList.remove("active"));
        const screen = document.getElementById(`screen-${screenId}`);
        if (screen) screen.classList.add("active");
        const navBtn = document.getElementById(`nav-${screenId}`);
        if (navBtn) navBtn.classList.add("active");
        if (screenId === "bookings") this.loadBookings();
        if (screenId === "summary") this.loadSummary();
    },

    // ---------- City Selection & Filtering ----------
    async loadCities() {
        try {
            const res = await fetch("/api/cities");
            this.cityMap = await res.json();
            this.cities = Object.keys(this.cityMap);
            const selector = document.getElementById("city-selector");
            if (selector && this.cities.length > 0) {
                selector.innerHTML = this.cities.map(name => 
                    `<option value="${name}" ${name === this.currentCity ? "selected" : ""}>${name}</option>`
                ).join("");
                this.updateCinemasForCity(this.currentCity);
            }
        } catch (e) {
            console.warn("Using default cities", e);
        }
    },

    changeCity(cityName) {
        this.currentCity = cityName;
        this.updateCinemasForCity(cityName);
        
        const heading = document.getElementById("current-city-heading");
        if (heading) heading.innerText = cityName;
        
        const badge = document.getElementById("city-status-badge");
        if (badge) badge.innerText = `📍 ${this.cinemas.length} Multiplexes Active`;

        const detailsLabel = document.getElementById("details-city-label");
        if (detailsLabel) detailsLabel.innerText = cityName;

        // If currently viewing details, re-render cinema pills
        if (this.currentMovie) {
            this.renderCinemaPills();
        }
    },

    updateCinemasForCity(cityName) {
        if (this.cityMap && this.cityMap[cityName]) {
            this.cinemas = this.cityMap[cityName];
        } else {
            this.cinemas = ["PVR Koramangala (Dolby Atmos)", "INOX Orion Mall (IMAX 4K)", "Cinepolis Nexus Whitefield (VIP)"];
        }
        this.currentCinema = this.cinemas[0];
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
            const matchesQuery = !query || 
                m.title.toLowerCase().includes(query) ||
                (m.synopsis && m.synopsis.toLowerCase().includes(query)) ||
                (m.cast && m.cast.toLowerCase().includes(query));
            return matchesGenre && matchesQuery;
        });

        if (filtered.length === 0) {
            grid.innerHTML = `<p style="grid-column: 1/-1; color: var(--sage); padding: 40px 0; text-align: center;">No movies found matching your search.</p>`;
            return;
        }

        grid.innerHTML = filtered.map(m => `
            <div class="movie-card" onclick="UI.openDetails('${m.title.replace(/'/g, "\\'")}')">
                <div class="poster-wrap">
                    <span class="badge-cert">${m.certification || 'UA'}</span>
                    <span class="badge-rating">&#9733; ${m.rating}</span>
                    <button class="trailer-quick-btn" onclick="event.stopPropagation(); UI.openTrailer('${m.title.replace(/'/g, "\\'")}', '${m.trailerId || 'dQw4w9WgXcQ'}')">▶ Trailer</button>
                    <img src="${m.poster}" alt="${m.title}" class="poster-img" onerror="this.src='https://placehold.co/300x450/3A0E12/FED7B8?text=${encodeURIComponent(m.title)}'">
                </div>
                <div class="movie-info">
                    <h3>${m.title}</h3>
                    <div class="movie-meta">${m.genre} &bull; ${m.duration}</div>
                    <div class="movie-card-footer">
                        <span class="movie-price-tag">From Rs. ${m.price.toFixed(0)}</span>
                        <span style="font-size: 11px; color: var(--sage);">${m.language}</span>
                    </div>
                </div>
            </div>
        `).join("");
    },

    openDetails(title) {
        this.currentMovie = this.movies.find(m => m.title === title);
        if (!this.currentMovie) return;

        document.getElementById("details-poster-img").src = this.currentMovie.poster;
        document.getElementById("details-title").innerText = this.currentMovie.title;
        document.getElementById("details-rating").innerHTML = `&#9733; Rating ${this.currentMovie.rating} / 10 &bull; <span style="color: var(--sage); font-size: 13px;">${this.currentMovie.certification || 'UA'}</span>`;
        document.getElementById("details-meta").innerText = `Language: ${this.currentMovie.language} | Duration: ${this.currentMovie.duration} | Base Ticket: Rs. ${this.currentMovie.price.toFixed(2)}`;
        
        const synEl = document.getElementById("details-synopsis");
        if (synEl) synEl.innerText = this.currentMovie.synopsis || "An extraordinary cinematic journey bringing unparalleled storytelling and state-of-the-art visuals to the silver screen.";
        
        const castEl = document.getElementById("details-cast");
        if (castEl) castEl.innerHTML = `<strong>Starring:</strong> ${this.currentMovie.cast || 'Renowned Ensemble Cast'}`;

        const detailsLabel = document.getElementById("details-city-label");
        if (detailsLabel) detailsLabel.innerText = this.currentCity;

        this.renderCinemaPills();
        this.renderShowtimes();
        this.showScreen("details");
    },

    renderCinemaPills() {
        document.getElementById("cinema-pills").innerHTML = this.cinemas.map(c => `
            <button class="genre-pill ${c === this.currentCinema ? 'active' : ''}" onclick="UI.selectCinema('${c.replace(/'/g, "\\'")}')">${c}</button>
        `).join("");
    },

    selectCinema(c) {
        this.currentCinema = c;
        this.renderCinemaPills();
    },

    renderShowtimes() {
        document.getElementById("showtime-pills").innerHTML = this.times.map(t => `
            <button class="pill-btn outline" onclick="UI.startBooking('${t}')">${t}</button>
        `).join("");
    },

    // ---------- Movie Trailer Modal ----------
    openCurrentTrailer() {
        if (!this.currentMovie) return;
        this.openTrailer(this.currentMovie.title, this.currentMovie.trailerId || "dQw4w9WgXcQ");
    },

    openTrailer(title, trailerId) {
        document.getElementById("trailer-modal-title").innerText = `${title} - Official Trailer`;
        const iframe = document.getElementById("trailer-iframe");
        iframe.src = `https://www.youtube-nocookie.com/embed/${trailerId}?autoplay=1&rel=0`;
        document.getElementById("trailer-modal").style.display = "flex";
    },

    closeTrailer() {
        const iframe = document.getElementById("trailer-iframe");
        iframe.src = "";
        document.getElementById("trailer-modal").style.display = "none";
    },

    closeTrailerModal(e) {
        if (e.target.id === "trailer-modal") {
            this.closeTrailer();
        }
    },

    // ---------- Interactive Seat Selection & Concessions ----------
    async startBooking(time) {
        this.stopSeatTimer();
        this.currentShowTime = `${this.currentCinema} (${this.currentCity}) | ${time}`;
        this.selectedSeats = [];
        this.isPremium = false;
        this.snackCart = { popcorn: 0, nachos: 0, coke: 0, coffee: 0, hotdog: 0 };
        this.appliedCoupon = null;
        this.appliedDiscount = 0.0;
        
        document.getElementById("coupon-input").value = "";
        document.getElementById("coupon-msg").innerText = "";
        document.getElementById("coupon-msg").className = "coupon-msg";

        document.getElementById("book-movie-title").innerText = this.currentMovie.title;
        document.getElementById("book-movie-show").innerText = this.currentShowTime;
        document.getElementById("cust-name").value = "";
        document.getElementById("cust-phone").value = "";

        // Query Java Backend for occupied seats
        const encMovie = encodeURIComponent(this.currentMovie.title);
        const encShow = encodeURIComponent(this.currentShowTime);
        const res = await fetch(`/api/booked-seats?movie=${encMovie}&show=${encShow}`);
        this.bookedSeats = await res.json();

        this.renderSeats();
        this.renderSnacks();
        this.updateBill();
        this.showScreen("booking");
    },

    getSeatTierInfo(seatName) {
        if (!seatName) return { id: 'club', name: 'Club Classic', surcharge: 0, badge: '🎬 Club' };
        const row = seatName.charAt(0).toUpperCase();
        if (row >= 'I') return { id: 'vip', name: 'VIP Recliner', surcharge: 120, badge: '👑 VIP' };
        if (row >= 'E') return { id: 'exec', name: 'Executive', surcharge: 60, badge: '⭐ Exec' };
        return { id: 'club', name: 'Club Classic', surcharge: 0, badge: '🎬 Club' };
    },

    renderSeats() {
        const matrix = document.getElementById("seat-matrix");
        const rows = 10, cols = 12;
        const base = this.currentMovie ? this.currentMovie.price : 200;

        // Update Left Panel Tier Pricing Cards
        const vipEl = document.getElementById("tier-price-vip");
        const execEl = document.getElementById("tier-price-exec");
        const clubEl = document.getElementById("tier-price-club");
        if (vipEl) vipEl.innerText = `Rs. ${(base + 120).toFixed(2)}`;
        if (execEl) execEl.innerText = `Rs. ${(base + 60).toFixed(2)}`;
        if (clubEl) clubEl.innerText = `Rs. ${base.toFixed(2)}`;

        let html = "";
        for (let r = 0; r < rows; r++) {
            const rowLetter = String.fromCharCode(65 + r);
            const tier = this.getSeatTierInfo(rowLetter + "1");

            // Insert Section Dividers at tier boundaries
            if (rowLetter === 'A') {
                html += `<div class="seat-tier-divider"><span class="tier-divider-tag tag-club">🎬 Club Classic &bull; Rs. ${base.toFixed(2)}</span></div>`;
            } else if (rowLetter === 'E') {
                html += `<div class="seat-tier-divider"><span class="tier-divider-tag tag-exec">⭐ Executive (Dolby Atmos) &bull; Rs. ${(base + 60).toFixed(2)}</span></div>`;
            } else if (rowLetter === 'I') {
                html += `<div class="seat-tier-divider"><span class="tier-divider-tag tag-vip">👑 VIP Royal Recliner &bull; Rs. ${(base + 120).toFixed(2)}</span></div>`;
            }

            html += `<div class="seat-row"><span class="row-label">${rowLetter}</span>`;
            for (let c = 1; c <= cols; c++) {
                if (c === 7) html += `<div class="aisle-gap"></div>`;
                const seatName = `${rowLetter}${c}`;
                const isBooked = this.bookedSeats.includes(seatName);
                const isSelected = this.selectedSeats.includes(seatName);
                const cls = `seat-btn tier-${tier.id}` + (isBooked ? " booked" : isSelected ? " selected" : "");
                html += `<button class="${cls}" onclick="UI.toggleSeat('${seatName}', ${isBooked})" title="${seatName} - ${tier.name} (Rs. ${(base + tier.surcharge).toFixed(2)})">${seatName}</button>`;
            }
            html += `</div>`;
        }
        matrix.innerHTML = html;

        const displayEl = document.getElementById("selected-seats-display");
        if (displayEl) {
            if (this.selectedSeats.length === 0) {
                displayEl.innerHTML = `<span style="color: var(--sage); font-size: 14px; font-weight: normal;">None selected</span>`;
            } else {
                displayEl.innerHTML = this.selectedSeats.map(s => {
                    const t = this.getSeatTierInfo(s);
                    return `<span style="display:inline-block; margin:2px 3px; padding:2px 8px; border-radius:6px; background:var(--cream); font-size:12px; font-weight:bold; color:var(--burgundy); border:1px solid rgba(74,14,23,0.2);">${s} <small style="color:var(--sage); font-weight:normal;">(${t.badge})</small></span>`;
                }).join("");
            }
        }
    },

    playAudioClick() {
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = "sine";
            osc.frequency.setValueAtTime(540, ctx.currentTime);
            osc.frequency.exponentialRampToValueAtTime(720, ctx.currentTime + 0.05);
            gain.gain.setValueAtTime(0.12, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.05);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.05);
        } catch (e) {}
    },

    playConfirmationChime() {
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const notes = [523.25, 659.25, 783.99, 1046.50]; // C5, E5, G5, C6
            notes.forEach((freq, idx) => {
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                osc.type = "triangle";
                osc.frequency.setValueAtTime(freq, ctx.currentTime + idx * 0.08);
                gain.gain.setValueAtTime(0.15, ctx.currentTime + idx * 0.08);
                gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + idx * 0.08 + 0.25);
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start(ctx.currentTime + idx * 0.08);
                osc.stop(ctx.currentTime + idx * 0.08 + 0.25);
            });
        } catch (e) {}
    },

    toggleSeat(seatName, isBooked) {
        if (isBooked) return;
        this.playAudioClick();
        const idx = this.selectedSeats.indexOf(seatName);
        if (idx !== -1) {
            this.selectedSeats.splice(idx, 1);
        } else {
            if (this.selectedSeats.length >= 8) {
                alert("You can select at most 8 seats per booking transaction.");
                return;
            }
            this.selectedSeats.push(seatName);
        }
        this.renderSeats();
        this.updateBill();

        if (this.selectedSeats.length > 0) {
            this.startSeatTimer();
        } else {
            this.stopSeatTimer();
        }
    },

    // ---------- 5-Minute Seat Hold Concurrency Timer ----------
    seatTimer: null,
    seatSecondsLeft: 300,

    startSeatTimer() {
        if (this.seatTimer) return;
        this.seatSecondsLeft = 300;
        this.updateSeatTimerDisplay();
        const banner = document.getElementById("seat-timer-banner");
        if (banner) banner.style.display = "flex";

        this.seatTimer = setInterval(() => {
            this.seatSecondsLeft--;
            this.updateSeatTimerDisplay();

            if (this.seatSecondsLeft <= 0) {
                this.expireSeatReservation();
            }
        }, 1000);

        this.syncBackendHold();
    },

    stopSeatTimer() {
        if (this.seatTimer) {
            clearInterval(this.seatTimer);
            this.seatTimer = null;
        }
        const banner = document.getElementById("seat-timer-banner");
        if (banner) {
            banner.style.display = "none";
            banner.classList.remove("warning");
        }
    },

    updateSeatTimerDisplay() {
        const display = document.getElementById("seat-timer-display");
        const banner = document.getElementById("seat-timer-banner");
        if (!display) return;
        const mins = Math.floor(this.seatSecondsLeft / 60);
        const secs = this.seatSecondsLeft % 60;
        display.innerText = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;

        if (this.seatSecondsLeft <= 60) {
            if (banner) banner.classList.add("warning");
        } else {
            if (banner) banner.classList.remove("warning");
        }
    },

    expireSeatReservation() {
        this.stopSeatTimer();
        const expired = [...this.selectedSeats];
        this.selectedSeats = [];
        this.renderSeats();
        this.updateBill();
        if (this.currentMovie && expired.length > 0) {
            fetch("/api/release-seats", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    movie: this.currentMovie.title,
                    show: this.currentShowTime,
                    seats: expired
                })
            }).catch(() => {});
        }
        alert("⏳ Your 5-minute seat reservation has expired! Selected seats have been released. Please re-select your seats to proceed.");
    },

    async syncBackendHold() {
        if (!this.currentMovie || this.selectedSeats.length === 0) return;
        try {
            await fetch("/api/hold-seats", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    movie: this.currentMovie.title,
                    show: this.currentShowTime,
                    seats: this.selectedSeats
                })
            });
        } catch (e) {}
    },

    // ---------- Concessions & Promo Codes ----------
    renderSnacks() {
        const list = document.getElementById("snacks-list");
        if (!list) return;
        list.innerHTML = this.concessions.map(s => `
            <div class="snack-item">
                <div class="snack-info">
                    <span class="snack-icon">${s.icon}</span>
                    <div>
                        <div class="snack-name">${s.name}</div>
                        <div class="snack-price">Rs. ${s.price} &bull; ${s.desc}</div>
                    </div>
                </div>
                <div class="snack-counter">
                    <button class="snack-count-btn" onclick="UI.changeSnackQty('${s.id}', -1)">-</button>
                    <span class="snack-qty">${this.snackCart[s.id] || 0}</span>
                    <button class="snack-count-btn" onclick="UI.changeSnackQty('${s.id}', 1)">+</button>
                </div>
            </div>
        `).join("");
    },

    changeSnackQty(id, delta) {
        const current = this.snackCart[id] || 0;
        const next = Math.max(0, Math.min(10, current + delta));
        this.snackCart[id] = next;
        this.playAudioClick();
        this.renderSnacks();
        this.updateBill();
    },

    getSnacksTotal() {
        return this.concessions.reduce((sum, s) => sum + ((this.snackCart[s.id] || 0) * s.price), 0);
    },

    getSnacksString() {
        const items = [];
        this.concessions.forEach(s => {
            const q = this.snackCart[s.id] || 0;
            if (q > 0) items.push(`${s.name} x${q}`);
        });
        return items.length > 0 ? items.join(", ") : "None";
    },

    async applyCoupon() {
        const input = document.getElementById("coupon-input");
        const msgEl = document.getElementById("coupon-msg");
        const code = (input.value || "").trim().toUpperCase();

        if (!code) {
            this.appliedCoupon = null;
            this.appliedDiscount = 0.0;
            msgEl.innerText = "";
            this.updateBill();
            return;
        }

        try {
            const res = await fetch(`/api/coupon?code=${encodeURIComponent(code)}`);
            const data = await res.json();
            if (data.valid) {
                this.appliedCoupon = data.code;
                this.appliedDiscount = data.discount || data.amount || 50.0;
                msgEl.innerText = `✓ ${data.code} applied: ${data.desc}`;
                msgEl.className = "coupon-msg success";
            } else {
                this.appliedCoupon = null;
                this.appliedDiscount = 0.0;
                msgEl.innerText = `✗ ${data.error || 'Invalid promo code'}`;
                msgEl.className = "coupon-msg error";
            }
            this.updateBill();
        } catch (e) {
            msgEl.innerText = "Error verifying coupon code";
            msgEl.className = "coupon-msg error";
        }
    },

    // ---------- Price Calculations & Form Handling ----------
    updateBill() {
        const movieBase = this.currentMovie ? this.currentMovie.price : 200;
        let base = 0;
        let hasVip = false;
        this.selectedSeats.forEach(s => {
            const t = this.getSeatTierInfo(s);
            base += movieBase + t.surcharge;
            if (t.id === 'vip') hasVip = true;
        });
        this.isPremium = hasVip;

        const count = this.selectedSeats.length;
        const snacks = this.getSnacksTotal();
        const fee = count * 10;
        
        // Exact match with Ticket.breakdown:
        const discountAmount = Math.min(base, this.appliedDiscount);
        const netBase = Math.max(0, base - discountAmount);
        const gst = (netBase + fee) * 0.18;
        const total = (netBase + fee + gst) + snacks;

        document.getElementById("bill-base").innerText = `Rs. ${base.toFixed(2)}`;
        
        const snacksRow = document.getElementById("bill-snacks-row");
        if (snacks > 0) {
            snacksRow.style.display = "flex";
            document.getElementById("bill-snacks").innerText = `Rs. ${snacks.toFixed(2)}`;
        } else {
            snacksRow.style.display = "none";
        }

        document.getElementById("bill-fee").innerText = `Rs. ${fee.toFixed(2)}`;
        document.getElementById("bill-gst").innerText = `Rs. ${gst.toFixed(2)}`;

        const discRow = document.getElementById("bill-discount-row");
        if (discountAmount > 0) {
            discRow.style.display = "flex";
            document.getElementById("bill-discount").innerText = `-Rs. ${discountAmount.toFixed(2)}`;
        } else {
            discRow.style.display = "none";
        }

        document.getElementById("bill-total").innerText = `Rs. ${total.toFixed(2)}`;
    },

    async proceedToPay() {
        const name = document.getElementById("cust-name").value.trim();
        const phone = document.getElementById("cust-phone").value.trim();

        if (this.selectedSeats.length === 0) {
            alert("Please select at least one seat from the cinema seat matrix.");
            return;
        }

        if (!name) {
            alert("Please enter customer name.");
            return;
        }

        if (!phone || phone.length < 10) {
            alert("Please enter a valid 10-digit mobile number.");
            return;
        }

        const unit = this.currentMovie.price + (this.isPremium ? 100 : 0);
        const base = this.selectedSeats.length * unit;
        const discountAmount = Math.min(base, this.appliedDiscount);

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
                    seats: this.selectedSeats,
                    snacks: this.getSnacksTotal().toString(),
                    discount: discountAmount.toString()
                })
            });

            const data = await res.json();
            if (data.error) throw new Error(data.error);

            this.lastConfirmedTicket = data;
            this.stopSeatTimer();
            this.playConfirmationChime();
            this.showConfirmation(data);
        } catch (err) {
            alert(err.message);
        }
    },

    generateBarcodeSvg(code) {
        // High-contrast clean luxury theater barcode
        const bars = [];
        const pattern = [3, 1, 4, 2, 3, 1, 4, 2, 3, 2, 4, 1, 3, 2, 4, 1, 3, 2, 4, 1, 3, 2, 4];
        let x = 2;
        for (let i = 0; i < pattern.length; i++) {
            const w = pattern[i];
            if (i % 2 === 0) {
                bars.push(`<rect x="${x}" y="0" width="${w}" height="24" fill="#1E0A0C" />`);
            }
            x += w + 1;
        }
        return `<svg width="110" height="32" viewBox="0 0 ${x + 2} 32" xmlns="http://www.w3.org/2000/svg">
            ${bars.join("")}
            <text x="${(x + 2) / 2}" y="30" font-family="monospace" font-size="7" font-weight="bold" text-anchor="middle" fill="#1E0A0C">${code}</text>
        </svg>`;
    },

    shareOnWhatsApp(ticketId) {
        const t = (ticketId && this.bookings && this.bookings.find(b => b.id === ticketId)) || this.lastConfirmedTicket;
        if (!t) return;
        const host = (this.serverInfo && this.serverInfo.host && !this.serverInfo.host.startsWith("127.")) 
            ? this.serverInfo.host 
            : window.location.hostname;
        const port = (this.serverInfo && this.serverInfo.port) ? this.serverInfo.port : (window.location.port || 8080);
        const ticketUrl = `http://${host}:${port}/ticket?id=${t.id}`;
        const snacksVal = t.snacks && parseFloat(t.snacks) > 0 ? `Rs. ${parseFloat(t.snacks).toFixed(2)}` : "None";
        const seatsWithTiers = Array.isArray(t.seats) 
            ? t.seats.map(s => `${s} (${this.getSeatTierInfo(s).name})`).join(", ") 
            : t.seats;

        const msg = 
`🎬 *LUMIÈRE CINEMAS — OFFICIAL E-PASS* 🎟️
━━━━━━━━━━━━━━━━━━━━
✨ *Movie:* ${t.movie.title} [${t.movie.certification || 'UA'}]
🏛️ *Cinema & Screen:* ${t.showTime}
💺 *Reserved Seats:* ${seatsWithTiers} [${t.type}]
👤 *Guest Name:* ${t.customer.name}
📱 *Phone:* ${t.customer.phone}
🔖 *Booking Passport ID:* ${t.id}
🍿 *Concessions:* ${snacksVal}
💰 *Total Amount Paid:* Rs. ${parseFloat(t.total).toFixed(2)}
━━━━━━━━━━━━━━━━━━━━
✅ *Turnstile Status:* ENTRY CLEARED
🌐 *Open Official Digital Pass:*
${ticketUrl}
━━━━━━━━━━━━━━━━━━━━
_Please arrive 15 minutes before showtime._`;

        window.open(`https://api.whatsapp.com/send?text=${encodeURIComponent(msg)}`, '_blank');
    },

    currentModalTicketId: null,

    shareCurrentModalPassOnWhatsApp() {
        if (this.currentModalTicketId) {
            this.shareOnWhatsApp(this.currentModalTicketId);
        } else if (this.lastConfirmedTicket) {
            this.shareOnWhatsApp(this.lastConfirmedTicket.id);
        }
    },

    downloadPdf(ticketId) {
        const t = (ticketId && this.bookings && this.bookings.find(b => b.id === ticketId)) || this.lastConfirmedTicket;
        const origTitle = document.title;
        document.title = `Lumiere_Pass_${t ? t.id : 'Official'}`;
        window.print();
        setTimeout(() => { document.title = origTitle; }, 1200);
    },

    buildBoardingPassHtml(ticket) {
        const snacksVal = ticket.snacks && parseFloat(ticket.snacks) > 0 ? `Rs. ${parseFloat(ticket.snacks).toFixed(2)}` : "None";
        const seatsVal = Array.isArray(ticket.seats) 
            ? ticket.seats.map(s => `${s} [${this.getSeatTierInfo(s).name}]`).join(", ") 
            : ticket.seats;
        const typeLabel = ticket.type.toUpperCase().includes("RECLINER") ? ticket.type.toUpperCase() : `${ticket.type.toUpperCase()} CLASS`;

        return `
            <div class="ticket-boarding-pass" id="ticket-boarding-pass">
                <div class="pass-header">
                    <div class="pass-brand">LUMIÈRE CINEMAS</div>
                    <span class="pass-type-badge">${typeLabel}</span>
                </div>

                <div class="pass-movie-title">${ticket.movie.title}</div>
                <div style="font-size: 12px; color: var(--peach); margin-bottom: 8px;">${ticket.movie.language || 'English'} &bull; ${ticket.movie.duration || '2h 30m'} &bull; ${ticket.movie.certification || 'UA'}</div>

                <div class="pass-grid">
                    <div>
                        <div class="pass-label">Cinema & City</div>
                        <div class="pass-val">${ticket.showTime}</div>
                    </div>
                    <div>
                        <div class="pass-label">Reserved Seats</div>
                        <div class="pass-val" style="color: var(--gold-light); font-size: 15px;">${seatsVal}</div>
                    </div>
                    <div>
                        <div class="pass-label">Guest Name</div>
                        <div class="pass-val">${ticket.customer.name}</div>
                    </div>
                    <div>
                        <div class="pass-label">Phone</div>
                        <div class="pass-val">${ticket.customer.phone}</div>
                    </div>
                    <div>
                        <div class="pass-label">Concessions Total</div>
                        <div class="pass-val" style="font-size: 11px;">${snacksVal}</div>
                    </div>
                    <div>
                        <div class="pass-label">Total Amount Paid</div>
                        <div class="pass-val" style="color: var(--gold-light); font-size: 15px;">Rs. ${parseFloat(ticket.total).toFixed(2)}</div>
                    </div>
                </div>

                <div class="pass-perforation"></div>

                <div class="pass-bottom">
                    <div>
                        <div style="font-size: 10px; color: var(--peach); letter-spacing: 1px;">BOOKING PASSPORT ID</div>
                        <div style="font-family: monospace; font-size: 16px; font-weight: bold; letter-spacing: 2px;">${ticket.id}</div>
                        <div style="font-size: 10px; color: rgba(254,215,184,0.7); margin-top: 4px;">Gate opens 15 mins before showtime</div>
                        <div style="margin-top: 6px;">
                            <span style="display: inline-block; background: rgba(76,175,80,0.25); border: 1px solid #4CAF50; color: #81C784; font-size: 10px; font-weight: bold; padding: 2px 8px; border-radius: 999px;">
                                ✅ TURNSTILE ENTRY CLEARED
                            </span>
                        </div>
                    </div>
                    <div class="pass-barcode-box" title="Cinema Turnstile Barcode">
                        ${this.generateBarcodeSvg(ticket.id)}
                    </div>
                </div>
            </div>
        `;
    },

    async showConfirmation(ticket) {
        try {
            const sRes = await fetch("/api/server-info");
            this.serverInfo = await sRes.json();
        } catch (e) {}
        document.getElementById("confirm-pass-preview").innerHTML = this.buildBoardingPassHtml(ticket);
        this.showScreen("confirmed");
    },

    async openPassModal(id) {
        const ticket = id ? this.bookings.find(b => b.id === id) : this.lastConfirmedTicket;
        if (!ticket) return;
        this.currentModalTicketId = ticket.id;
        try {
            const sRes = await fetch("/api/server-info");
            this.serverInfo = await sRes.json();
        } catch (e) {}
        document.getElementById("modal-pass-body").innerHTML = this.buildBoardingPassHtml(ticket);
        document.getElementById("pass-modal").style.display = "flex";
    },

    closePassModal(e) {
        if (!e || e.target.id === "pass-modal" || e.currentTarget.classList.contains("close-modal-btn") || e.currentTarget.tagName === "BUTTON") {
            document.getElementById("pass-modal").style.display = "none";
        }
    },

    printPass() {
        this.downloadPdf();
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
            t.movie.title.toLowerCase().includes(q) ||
            t.showTime.toLowerCase().includes(q)
        );

        const tbody = document.getElementById("bookings-table-body");
        document.getElementById("bookings-count").innerText = `${filtered.length} booking(s)`;
        if (filtered.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color: var(--sage); padding: 32px;">No active bookings found.</td></tr>`;
            return;
        }

        tbody.innerHTML = filtered.map(t => `
            <tr>
                <td><b>${t.id}</b></td>
                <td><b>${t.movie.title}</b></td>
                <td style="font-size: 12px; color: var(--sage);">${t.showTime}</td>
                <td><b style="color: var(--burgundy);">${t.seats.join(", ")}</b></td>
                <td><span style="font-size: 11px; padding: 3px 8px; border-radius: 6px; background: var(--cream); font-weight: bold;">${t.type}</span></td>
                <td>${t.customer.name}</td>
                <td><b>Rs. ${parseFloat(t.total).toFixed(2)}</b></td>
                <td>
                    <div style="display: flex; gap: 4px;">
                        <button class="pill-btn gold" style="padding: 4px 8px; font-size: 11px;" onclick="UI.openPassModal('${t.id}')">Pass</button>
                        <button class="pill-btn btn-whatsapp" style="padding: 4px 8px; font-size: 11px;" onclick="UI.shareOnWhatsApp('${t.id}')" title="Share via WhatsApp">📲 WA</button>
                        <button class="pill-btn outline" style="padding: 4px 8px; font-size: 11px;" onclick="UI.viewBill('${t.id}')">Bill</button>
                        <button class="pill-btn outline" style="padding: 4px 8px; font-size: 11px; color: #a32b2b; border-color: #a32b2b;" onclick="UI.cancelBooking('${t.id}')">Cancel</button>
                    </div>
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
        const snacksVal = t.snacks ? parseFloat(t.snacks) : 0.0;
        const discVal = t.discount ? parseFloat(t.discount) : 0.0;

        document.getElementById("modal-bill-content").innerHTML = `
            <div class="price-row"><span>Booking ID</span><b>${t.id}</b></div>
            <div class="price-row"><span>Movie</span><b>${t.movie.title}</b></div>
            <div class="price-row"><span>Multiplex & Show</span><b>${t.showTime}</b></div>
            <div class="price-row"><span>Customer</span><b>${t.customer.name} (${t.customer.phone})</b></div>
            <div class="price-row"><span>Seats</span><b>${t.seats.join(", ")} (${t.type})</b></div>
            <div class="price-row"><span>Base (${count} x Rs. ${unit.toFixed(2)})</span><span>Rs. ${base.toFixed(2)}</span></div>
            <div class="price-row"><span>F&B Concessions</span><span>Rs. ${snacksVal.toFixed(2)}</span></div>
            <div class="price-row"><span>Convenience Fee</span><span>Rs. ${fee.toFixed(2)}</span></div>
            <div class="price-row"><span>Discount Applied</span><span>-Rs. ${discVal.toFixed(2)}</span></div>
            <div class="price-row total"><span>Total Amount Paid</span><b>Rs. ${parseFloat(t.total).toFixed(2)}</b></div>
        `;
        document.getElementById("bill-modal").style.display = "flex";
    },

    closeBillModal() {
        document.getElementById("bill-modal").style.display = "none";
    },

    async cancelBooking(id) {
        if (!confirm(`Cancel booking ${id}? Its seats will be freed immediately and refund will be processed.`)) return;
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
                    <div class="stat-name">Seats Occupied</div>
                </div>
                <div class="stat-card featured">
                    <div class="stat-val">Rs. ${data.revenue.toFixed(2)}</div>
                    <div class="stat-name">Total Gross Revenue</div>
                </div>
                <div class="stat-card">
                    <div class="stat-val">${data.created}</div>
                    <div class="stat-name">Tickets Generated (Counter)</div>
                </div>
                <div class="stat-card">
                    <div class="stat-val">${data.gst}%</div>
                    <div class="stat-name">GST Tax Rate</div>
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
