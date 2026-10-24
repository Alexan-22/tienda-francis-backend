ALTER TABLE home_banners
    ADD COLUMN subtitulo VARCHAR(500),
    ADD COLUMN enlace_url VARCHAR(1024),
    ADD COLUMN texto_boton VARCHAR(100),
    ADD COLUMN fecha_inicio TIMESTAMPTZ,
    ADD COLUMN fecha_fin TIMESTAMPTZ,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE home_banners
    ADD CONSTRAINT chk_home_banners_orden CHECK (orden >= 0),
    ADD CONSTRAINT chk_home_banners_fechas
        CHECK (
            fecha_inicio IS NULL
            OR fecha_fin IS NULL
            OR fecha_fin > fecha_inicio
        );

CREATE INDEX idx_home_banners_visible_orden
    ON home_banners (visible, orden, id);