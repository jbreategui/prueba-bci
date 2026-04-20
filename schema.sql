CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created TIMESTAMP NOT NULL,
    modified TIMESTAMP NOT NULL,
    last_login TIMESTAMP NOT NULL,
    token VARCHAR(1000) NOT NULL,
    is_active BOOLEAN NOT NULL
);

CREATE TABLE phones (
    id UUID PRIMARY KEY,
    number VARCHAR(255) NOT NULL,
    city_code VARCHAR(255) NOT NULL,
    country_code VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT fk_phones_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_phones_user_id ON phones(user_id);
