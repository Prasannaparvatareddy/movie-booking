-- Sample Cities
INSERT INTO city (id, name, state, country) VALUES (1, 'Mumbai', 'Maharashtra', 'India');
INSERT INTO city (id, name, state, country) VALUES (2, 'Delhi', 'Delhi', 'India');
INSERT INTO city (id, name, state, country) VALUES (3, 'Bangalore', 'Karnataka', 'India');

-- Sample Movies
INSERT INTO movie (id, title, language, genre, duration_minutes, rating, description)
VALUES (1, 'Avengers: Endgame', 'English', 'ACTION', 181, 8.4, 'Marvel superhero epic');
INSERT INTO movie (id, title, language, genre, duration_minutes, rating, description)
VALUES (2, 'RRR', 'Telugu', 'ACTION', 187, 7.8, 'SS Rajamouli blockbuster');
INSERT INTO movie (id, title, language, genre, duration_minutes, rating, description)
VALUES (3, 'Inception', 'English', 'THRILLER', 148, 8.8, 'Christopher Nolan sci-fi thriller');

-- Sample Theatres
INSERT INTO theatre (id, name, city_id, address, total_screens)
VALUES (1, 'PVR Juhu', 1, 'Juhu, Mumbai, MH', 6);
INSERT INTO theatre (id, name, city_id, address, total_screens)
VALUES (2, 'INOX Nariman Point', 1, 'Nariman Point, Mumbai, MH', 4);
INSERT INTO theatre (id, name, city_id, address, total_screens)
VALUES (3, 'PVR Select City', 2, 'Select Citywalk, Delhi', 8);

-- Sample Screens
INSERT INTO screen (id, theatre_id, screen_name, total_seats)
VALUES (1, 1, 'Screen 1', 100);
INSERT INTO screen (id, theatre_id, screen_name, total_seats)
VALUES (2, 1, 'Screen 2', 80);
INSERT INTO screen (id, theatre_id, screen_name, total_seats)
VALUES (3, 2, 'Screen 1', 120);
INSERT INTO screen (id, theatre_id, screen_name, total_seats)
VALUES (4, 3, 'Screen 1', 150);

-- Sample Shows
INSERT INTO show (id, movie_id, screen_id, show_date, show_time, base_price, available_seats, status)
VALUES (1, 1, 1, '2025-07-01', '10:00:00', 250.00, 100, 'ACTIVE');
INSERT INTO show (id, movie_id, screen_id, show_date, show_time, base_price, available_seats, status)
VALUES (2, 1, 1, '2025-07-01', '14:00:00', 250.00, 100, 'ACTIVE');
INSERT INTO show (id, movie_id, screen_id, show_date, show_time, base_price, available_seats, status)
VALUES (3, 1, 2, '2025-07-01', '19:00:00', 300.00, 80, 'ACTIVE');
INSERT INTO show (id, movie_id, screen_id, show_date, show_time, base_price, available_seats, status)
VALUES (4, 2, 3, '2025-07-01', '13:00:00', 200.00, 120, 'ACTIVE');
INSERT INTO show (id, movie_id, screen_id, show_date, show_time, base_price, available_seats, status)
VALUES (5, 3, 4, '2025-07-01', '20:00:00', 350.00, 150, 'ACTIVE');
