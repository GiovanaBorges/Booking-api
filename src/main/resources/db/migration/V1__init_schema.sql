-- =========================
-- USERS
-- =========================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    roles VARCHAR(50),
    description TEXT,
    linkedin_profile VARCHAR(500),
    github_profile VARCHAR(500),
    portfolio_url VARCHAR(500),
    experience_years INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- USER SKILLS (ElementCollection)
-- =========================
CREATE TABLE users_skills (
    users_id BIGINT NOT NULL,
    skills VARCHAR(100),
    CONSTRAINT fk_users_skills_user
        FOREIGN KEY (users_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- =========================
-- BOOKINGS
-- =========================
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    provider_id BIGINT,
    customer_id BIGINT,

    start_ts TIMESTAMP,
    end_ts TIMESTAMP,

    status VARCHAR(50),

    title VARCHAR(255) NOT NULL,
    description TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_bookings_provider
        FOREIGN KEY (provider_id)
        REFERENCES users(id),

    CONSTRAINT fk_bookings_customer
        FOREIGN KEY (customer_id)
        REFERENCES users(id)
);

-- =========================
-- PROVIDER AVAILABILITY
-- =========================
CREATE TABLE provider_availability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    day_of_week INT,
    start_time TIME,
    end_time TIME,

    provider_id BIGINT,

    CONSTRAINT fk_provider_availability_provider
        FOREIGN KEY (provider_id)
        REFERENCES users(id)
);

-- =========================
-- INDEXES USERS
-- =========================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_keycloak_id ON users(keycloak_id);
CREATE INDEX idx_users_roles ON users(roles);
CREATE INDEX idx_users_skills_skill ON users_skills(skills);

-- =========================
-- INDEXES BOOKINGS
-- =========================
CREATE INDEX idx_bookings_customer ON bookings(customer_id);

CREATE INDEX idx_bookings_provider_start_end 
ON bookings(provider_id, start_ts, end_ts);

CREATE INDEX idx_bookings_provider_status 
ON bookings(provider_id, status);

CREATE INDEX idx_bookings_provider_created 
ON bookings(provider_id, created_at);

-- =========================
-- INDEXES PROVIDER AVAILABILITY
-- =========================
CREATE INDEX idx_provider_availability_provider_day 
ON provider_availability(provider_id, day_of_week);