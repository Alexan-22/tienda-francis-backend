CREATE TABLE direcciones_envio (
                                   id BIGSERIAL PRIMARY KEY,
                                   usuario_id BIGINT NOT NULL,
                                   destinatario VARCHAR(120) NOT NULL,
                                   linea VARCHAR(255) NOT NULL,
                                   distrito VARCHAR(100) NOT NULL,
                                   provincia VARCHAR(100) NOT NULL,
                                   departamento VARCHAR(100) NOT NULL,
                                   activa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX ix_direcciones_usuario
    ON direcciones_envio(usuario_id, activa);

ALTER TABLE ventas
    ADD COLUMN direccion_id BIGINT REFERENCES direcciones_envio(id);

ALTER TABLE ventas
    ADD COLUMN direccion_snapshot TEXT;

ALTER TABLE ventas
    ADD COLUMN subtotal NUMERIC(12, 2);

ALTER TABLE ventas
    ADD COLUMN delivery_costo NUMERIC(12, 2);

ALTER TABLE ventas
    ADD CONSTRAINT ck_delivery_costo
        CHECK (delivery_costo IS NULL OR delivery_costo >= 0);

CREATE INDEX ix_ventas_vence
    ON ventas(vence, id)
    WHERE estado = 'RECIBIDO'
      AND pago_estado = 'PENDIENTE'
      AND tipo = 'ONLINE';

CREATE TABLE historial_pedidos (
                                   id BIGSERIAL PRIMARY KEY,
                                   venta_id BIGINT NOT NULL REFERENCES ventas(id),
                                   origen VARCHAR(30) NOT NULL,
                                   destino VARCHAR(30) NOT NULL,
                                   responsable_id BIGINT,
                                   motivo VARCHAR(500) NOT NULL,
                                   ocurrido_en TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_historial_pedido_fecha
    ON historial_pedidos(venta_id, ocurrido_en, id);