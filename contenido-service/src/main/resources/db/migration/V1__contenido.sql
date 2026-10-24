CREATE TABLE home_banners (
                              id BIGSERIAL PRIMARY KEY,
                              titulo VARCHAR(255) NOT NULL,
                              imagen_url VARCHAR(1024) NOT NULL,
                              visible BOOLEAN NOT NULL DEFAULT TRUE,
                              orden INT NOT NULL DEFAULT 0
);
