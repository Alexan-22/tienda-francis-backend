-- Agregar versión de credenciales a cada usuario.
-- Incrementar este número invalida los tokens previos.

ALTER TABLE usuarios
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_usuarios_activo_rol
    ON usuarios (activo, rol);