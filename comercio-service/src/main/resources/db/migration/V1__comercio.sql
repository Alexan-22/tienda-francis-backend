CREATE TABLE productos (
                           id BIGSERIAL PRIMARY KEY,
                           nombre VARCHAR(255) NOT NULL,
                           precio NUMERIC(12, 2) NOT NULL CHECK (precio > 0),
                           precio_oferta NUMERIC(12, 2)
                               CHECK (precio_oferta > 0 AND precio_oferta < precio),
                           activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE producto_tallas (
                                 id BIGSERIAL PRIMARY KEY,
                                 producto_id BIGINT NOT NULL REFERENCES productos(id),
                                 talla VARCHAR(30) NOT NULL,
                                 stock_fisico INT NOT NULL DEFAULT 0 CHECK (stock_fisico >= 0),
                                 stock_reservado INT NOT NULL DEFAULT 0
                                     CHECK (
                                         stock_reservado >= 0
                                             AND stock_reservado <= stock_fisico
                                         ),
                                 version BIGINT NOT NULL DEFAULT 0,
                                 UNIQUE (producto_id, talla)
);

CREATE TABLE ventas (
                        id BIGSERIAL PRIMARY KEY,
                        idempotencia UUID NOT NULL UNIQUE,
                        usuario_id BIGINT NOT NULL,
                        tipo VARCHAR(20) NOT NULL,
                        estado VARCHAR(30) NOT NULL,
                        pago_estado VARCHAR(30) NOT NULL,
                        total NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
                        creada TIMESTAMPTZ NOT NULL,
                        vence TIMESTAMPTZ
);

CREATE TABLE detalle_ventas (
                                id BIGSERIAL PRIMARY KEY,
                                venta_id BIGINT NOT NULL REFERENCES ventas(id),
                                variante_id BIGINT NOT NULL REFERENCES producto_tallas(id),
                                producto_nombre VARCHAR(255) NOT NULL,
                                talla VARCHAR(30) NOT NULL,
                                cantidad INT NOT NULL CHECK (cantidad > 0),
                                precio_unitario NUMERIC(12, 2) NOT NULL
                                    CHECK (precio_unitario > 0)
);

CREATE INDEX ix_ventas_usuario
    ON ventas(usuario_id, creada);