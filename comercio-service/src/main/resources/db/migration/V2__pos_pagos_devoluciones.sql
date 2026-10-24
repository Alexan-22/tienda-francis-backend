ALTER TABLE ventas
    ALTER COLUMN usuario_id DROP NOT NULL;

ALTER TABLE ventas
    ADD COLUMN responsable_id BIGINT;

ALTER TABLE ventas
    ADD COLUMN cliente_presencial_nombre VARCHAR(255);

ALTER TABLE ventas
    ADD COLUMN solicitud_hash VARCHAR(64);

ALTER TABLE ventas
    ADD CONSTRAINT ck_tipo
        CHECK (tipo IN ('ONLINE', 'PRESENCIAL'));

ALTER TABLE ventas
    ADD CONSTRAINT ck_venta_total
        CHECK (total >= 0);

CREATE TABLE pagos (
                       id BIGSERIAL PRIMARY KEY,
                       venta_id BIGINT NOT NULL REFERENCES ventas(id),
                       idempotencia UUID NOT NULL UNIQUE,
                       monto NUMERIC(12, 2) NOT NULL CHECK (monto > 0),
                       medio VARCHAR(20) NOT NULL
                           CHECK (medio IN ('EFECTIVO', 'YAPE', 'TARJETA')),
                       referencia VARCHAR(255),
                       responsable_id BIGINT NOT NULL,
                       estado VARCHAR(30) NOT NULL DEFAULT 'CONFIRMADO'
                           CHECK (estado IN ('CONFIRMADO', 'ANULADO')),
                       creado TIMESTAMPTZ NOT NULL,
                       anulado_en TIMESTAMPTZ,
                       motivo_anulacion VARCHAR(500),

                       CHECK (
                           (
                               estado = 'ANULADO'
                                   AND anulado_en IS NOT NULL
                                   AND motivo_anulacion IS NOT NULL
                               )
                               OR
                           (
                               estado = 'CONFIRMADO'
                                   AND anulado_en IS NULL
                                   AND motivo_anulacion IS NULL
                               )
                           )
);

CREATE INDEX ix_pagos_venta
    ON pagos(venta_id);

CREATE TABLE devoluciones (
                              id BIGSERIAL PRIMARY KEY,
                              pago_id BIGINT NOT NULL REFERENCES pagos(id),
                              idempotencia UUID NOT NULL UNIQUE,
                              monto NUMERIC(12, 2) NOT NULL CHECK (monto > 0),
                              motivo VARCHAR(500) NOT NULL,
                              responsable_id BIGINT NOT NULL,
                              creada TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_devoluciones_pago
    ON devoluciones(pago_id);
