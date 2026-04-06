# 🎬 Movie Ticket Booking Platform

A Spring Boot REST API for an online movie ticket booking platform serving **B2B (Theatre Partners)** and **B2C (End Customers)**.

---

## 🚀 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Database | H2 In-Memory |
| ORM | Spring Data JPA / Hibernate |
| Cache | Caffeine (via Spring Cache) |
| Connection Pool | HikariCP (Spring Boot default) |
| Build Tool | Maven |
| Utilities | Lombok, Bean Validation |

---

## ▶️ How to Run

```bash
# Clone / extract the project
cd movie-booking

# Run with Maven
mvn spring-boot:run

# OR build and run the jar
mvn clean package
java -jar target/movie-booking-1.0.0.jar
```

App starts at: **http://localhost:8080**

---

## 🗄️ H2 Database Console

| Field | Value |
|---|---|
| URL | http://localhost:8080/h2-console |
| JDBC URL | `jdbc:h2:mem:moviebookingdb` |
| Username | `sa` |
| Password | *(leave empty)* |

Useful SQL:
```sql
SELECT * FROM MOVIE;
SELECT * FROM THEATRE;
SELECT * FROM SHOW;
SELECT * FROM BOOKING;
SELECT * FROM BOOKED_SEAT;
```

---

## 📁 Project Structure

```
src/main/java/com/booking/
│
├── MovieBookingApplication.java     ← Entry point
│
├── config/
│   ├── CacheConfig.java             ← Caffeine cache setup
│   └── WebConfig.java               ← CORS configuration
│
├── controller/                      ← HTTP layer — REST endpoints
│   ├── MovieController.java
│   ├── ShowController.java
│   ├── BookingController.java
│   └── TheatreController.java
│
├── service/                         ← Business logic layer
│   ├── MovieService.java            ← with @Cacheable
│   ├── ShowService.java             ← with @Cacheable + @CacheEvict
│   ├── BookingService.java
│   ├── TheatreService.java
│   ├── PricingService.java          ← discount orchestrator
│   ├── DiscountStrategy.java        ← Strategy Pattern interface
│   ├── AfternoonDiscountStrategy.java
│   └── ThirdTicketDiscountStrategy.java
│
├── repository/                      ← Data access layer
│   ├── MovieRepository.java
│   ├── ShowRepository.java
│   ├── BookingRepository.java
│   ├── TheatreRepository.java
│   └── ScreenRepository.java
│
├── entity/                          ← JPA entities (DB tables)
│   ├── City.java
│   ├── Movie.java
│   ├── Theatre.java
│   ├── Screen.java
│   ├── Show.java
│   ├── Booking.java
│   └── BookedSeat.java
│
├── dto/                             ← Request / Response objects
│   ├── ApiResponse.java             ← Generic wrapper
│   ├── BookingRequest.java
│   ├── BookingResponse.java
│   ├── ShowRequest.java
│   ├── ShowResponse.java
│   ├── BulkBookingRequest.java
│   ├── TheatreDashboardResponse.java
│   └── TheatreBookingResponse.java
│
├── exception/                       ← Error handling
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── BookingException.java
│
└── oops/
    └── OOPPrinciplesDemo.java       ← OOP reference (learning)
```

---

## 🌐 API Endpoints

### 🎬 Movies

| Method | URL | Description |
|---|---|---|
| GET | `/api/v1/movies` | Get all movies |
| GET | `/api/v1/movies/{id}` | Get movie by ID |
| GET | `/api/v1/movies?city=Mumbai` | Movies in a city |
| GET | `/api/v1/movies?city=Mumbai&language=English` | Filter by city + language |
| GET | `/api/v1/movies?genre=ACTION` | Filter by genre |

### 🎭 Shows

| Method | URL | Description |
|---|---|---|
| GET | `/api/v1/shows?movie=X&city=Y&date=Z` | Browse shows (READ scenario) |
| GET | `/api/v1/shows/{showId}` | Get show by ID |
| GET | `/api/v1/theatres/{id}/shows?date=Z` | Shows by theatre + date |
| POST | `/api/v1/theatres/{id}/shows` | Create show (B2B) |
| PUT | `/api/v1/theatres/{id}/shows/{showId}` | Update show (B2B) |
| DELETE | `/api/v1/theatres/{id}/shows/{showId}` | Cancel show (B2B) |
| PATCH | `/api/v1/theatres/{id}/shows/{showId}/inventory` | Update seats (B2B) |

### 🎟️ Bookings

| Method | URL | Description |
|---|---|---|
| POST | `/api/v1/bookings` | Book tickets (B2C) |
| POST | `/api/v1/bookings/bulk` | Bulk book tickets |
| GET | `/api/v1/bookings/{reference}` | Get booking by reference |
| GET | `/api/v1/bookings?email=x@y.com` | Bookings by customer |
| DELETE | `/api/v1/bookings/{reference}` | Cancel booking |
| DELETE | `/api/v1/bookings/bulk` | Bulk cancel |

### 📊 Theatre Dashboard (B2B)

| Method | URL | Description |
|---|---|---|
| GET | `/api/v1/theatres/{id}/dashboard` | Revenue + booking stats |
| GET | `/api/v1/theatres/{id}/bookings` | All bookings |
| GET | `/api/v1/theatres/{id}/bookings?date=Z` | Bookings by date |
| GET | `/api/v1/theatres/{id}/shows/{showId}/bookings` | Bookings for one show |
| GET | `/api/v1/theatres/{id}/revenue?date=Z` | Revenue by date |

---

## 💰 Discount Rules

| Rule | Condition | Discount |
|---|---|---|
| Afternoon Show | Show time 12:00 PM – 4:59 PM | 20% off total |
| Third Ticket | 3 or more seats booked | 50% off every 3rd ticket |
| Best Wins | Both applicable | Higher discount applied only |

**Example:**
```
3 seats × Rs.250 = Rs.750 total
Afternoon discount = 20% × 750 = Rs.150
Third ticket discount = 50% × 250 = Rs.125
Best discount = Rs.150 (afternoon wins)
Final amount = Rs.750 - Rs.150 = Rs.600
```

---

## 🧠 Design Patterns

### 1. Strategy Pattern — Discount Calculation
```
DiscountStrategy (interface)
    ├── AfternoonDiscountStrategy  → 20% off for 12PM-5PM shows
    └── ThirdTicketDiscountStrategy → 50% off every 3rd ticket

PricingService loops through all strategies,
picks the highest discount — pluggable and extensible.
```

### 2. Builder Pattern — Object Construction
```java
Booking booking = Booking.builder()
    .bookingReference(ref)
    .customerName(name)
    .finalAmount(amount)
    .build();
```

### 3. Repository Pattern — Data Access
```
Service → Repository Interface → Spring Data JPA → Database
Service never writes SQL directly.
```

### 4. DTO Pattern — API Shape Control
```
Entity  →  never exposed directly in API
DTO     →  controls exactly what fields are in request/response
```

---

## ⚡ Caching

Spring Cache with **Caffeine** (in-memory, high performance).

| Cache | Key Pattern | TTL | What is cached |
|---|---|---|---|
| `movies` | `all` | 10 min | All movies list |
| `movies` | `{id}` | 10 min | Single movie |
| `movies` | `city_Mumbai` | 10 min | Movies by city |
| `shows` | `browse_X_Y_Z` | 2 min | Browse results |
| `shows` | `{showId}` | 2 min | Show details |
| `theatres` | `{theatreId}` | 5 min | Theatre data |

**Cache Eviction:**
- `addMovie()` → evicts `movies::all`
- `createShow()`, `updateShow()`, `deleteShow()` → evicts shows cache
- `updateSeatInventory()` → evicts that show's cache entry

**Cache Flow:**
```
First request  → Cache MISS → hit DB → store in cache → return
Second request → Cache HIT  → return from memory (no DB hit) ✅
```

---

## 🏗️ OOP Principles Applied

### Encapsulation
- All entity fields are `private` with Lombok `@Getter/@Setter`
- Validation logic in `private` methods inside services
  - `validateScreenBelongsToTheatre()`
  - `validateShowBelongsToTheatre()`
  - `validateNoConflictingShow()`

### Abstraction
- `DiscountStrategy` interface hides how each discount is calculated
- `MovieRepository` interface hides how SQL is executed
- `mapToShowResponse()` hides entity-to-DTO conversion

### Inheritance
- `ResourceNotFoundException extends RuntimeException`
- `BookingException extends RuntimeException`
- All entities follow same JPA lifecycle inherited from Spring

### Polymorphism
- `DiscountStrategy` — runtime polymorphism
- `ApiResponse<T>` — generic polymorphism
- `@Cacheable` — same annotation, different behaviour per method

### SOLID
| Principle | Where |
|---|---|
| S — Single Responsibility | Each service class has one job |
| O — Open/Closed | New discount = new class, no existing changes |
| L — Liskov Substitution | Any DiscountStrategy is interchangeable |
| I — Interface Segregation | DiscountStrategy has only 3 focused methods |
| D — Dependency Inversion | Services depend on interfaces not implementations |

---

## 🔌 HikariCP Connection Pool

Configured in `application.properties`:

```properties
spring.datasource.hikari.pool-name=MovieBookingPool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

HikariCP pre-creates a pool of DB connections and reuses them
instead of opening a new connection per request — dramatically
reduces latency under load.

---

## 📦 Sample Data (Auto-loaded)

| Entity | Data |
|---|---|
| Cities | Mumbai, Delhi, Bangalore |
| Movies | Avengers: Endgame, RRR, Inception |
| Theatres | PVR Juhu (Mumbai), INOX Nariman (Mumbai), PVR Select City (Delhi) |
| Shows | 5 shows on 2025-07-01 — morning, afternoon, evening |

---

## 🧪 Running Tests

```bash
mvn test
```

`PricingServiceTest.java` covers:
- Afternoon show 20% discount
- 3 tickets 50% on third ticket
- Both discounts — best wins
- 6 tickets — discount on 2 third tickets
- Evening show no discount

---

## 📬 Postman Collection

Import `MovieBooking-Postman-Collection.json` into Postman.

35 requests across 7 folders covering all endpoints,
discount scenarios, error cases, and B2B dashboard APIs.
