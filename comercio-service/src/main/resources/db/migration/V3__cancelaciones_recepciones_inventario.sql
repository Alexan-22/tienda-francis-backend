CREATE TABLE recepciones_devolucion (
                                        id BIGSERIAL PRIMARY KEY,
                                        venta_id BIGINT NOT NULL REFERENCES ventas(id),
                                        detalle_id BIGINT NOT NULL REFERENCES detalle_ventas(id),
                                        idempotencia UUID NOT NULL UNIQUE,
                                        cantidad INT NOT NULL CHECK (cantidad > 0),
                                        condicion VARCHAR(20) NOT NULL
                                            CHECK (condicion IN ('APTO', 'DANADO')),
                                        motivo VARCHAR(500) NOT NULL
                                            CHECK (length(trim(motivo)) > 0),
                                        responsable_id BIGINT NOT NULL,
                                        repuesto BOOLEAN NOT NULL,
                                        recibido_en TIMESTAMPTZ NOT NULL,
                                        CHECK (repuesto = (condicion = 'APTO'))
);

CREATE INDEX ix_recepciones_venta
    ON recepciones_devolucion(venta_id);

CREATE INDEX ix_recepciones_detalle
    ON recepciones_devolucion(detalle_id);

CREATE TABLE movimientos_inventario (
                                        id BIGSERIAL PRIMARY KEY,
                                        variante_id BIGINT NOT NULL REFERENCES producto_tallas(id),
                                        recepcion_id BIGINT NOT NULL UNIQUE
                                            REFERENCES recepciones_devolucion(id),
                                        cantidad INT NOT NULL CHECK (cantidad > 0),
                                        tipo VARCHAR(30) NOT NULL
                                            CHECK (tipo = 'REPOSICION_DEVOLUCION'),
                                        motivo VARCHAR(500) NOT NULL,
                                        responsable_id BIGINT NOT NULL,
                                        creado_en TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_movimientos_variante_fecha
    ON movimientos_inventario(variante_id, creado_en);