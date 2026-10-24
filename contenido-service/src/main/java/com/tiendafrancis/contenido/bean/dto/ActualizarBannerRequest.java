package com.tiendafrancis.contenido.bean.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record ActualizarBannerRequest(
        @NotBlank @Size(max = 255) String titulo,
        @Size(max = 500) String subtitulo,
        @NotBlank @Size(max = 1024) String imagenUrl,
        @Size(max = 1024) String enlaceUrl,
        @Size(max = 100) String textoBoton,
        @Min(0) int orden,
        @NotNull Boolean visible,
        Instant fechaInicio,
        Instant fechaFin,
        @NotNull Long version
) {}
