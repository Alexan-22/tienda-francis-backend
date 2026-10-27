
-- =====================================================
-- FRANCIS COLLECTION
-- V5 - REPOSICIONES MANUALES DE INVENTARIO
-- =====================================================

-- Las reposiciones manuales no tienen una
-- recepción de devolución asociada.

ALTER TABLE movimientos_inventario
    ALTER COLUMN recepcion_id DROP NOT NULL;

-- La clave permite evitar duplicados por reintentos.

ALTER TABLE movimientos_inventario
    ADD COLUMN idempotencia UUID;

-- Sustituir la restricción creada en V3.
-- Los movimientos históricos permanecen válidos.

ALTER TABLE movimientos_inventario
DROP CONSTRAINT movimientos_inventario_tipo_check;

ALTER TABLE movimientos_inventario
    ADD CONSTRAINT ck_movimientos_tipo_origen
        CHECK (
            (
                tipo = 'REPOSICION_DEVOLUCION'
                    AND recepcion_id IS NOT NULL
                    AND idempotencia IS NULL
                )
                OR
            (
                tipo = 'REPOSICION_MANUAL'
                    AND recepcion_id IS NULL
                    AND idempotencia IS NOT NULL
                )
            );

-- No puede registrarse dos veces
-- una reposición con el mismo UUID.

CREATE UNIQUE INDEX ux_movimientos_idempotencia
    ON movimientos_inventario(idempotencia)
    WHERE idempotencia IS NOT NULL;
