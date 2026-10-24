package com.tiendafrancis.contenido.bean.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record CrearBannerRequest(
        @NotBlank @Size(max = 255) String titulo,
        @Size(max = 500) String subtitulo,
        @NotBlank @Size(max = 1024) String imagenUrl,
        @Size(max = 1024) String enlaceUrl,
        @Size(max = 100) String textoBoton,
        @Min(0) int orden,
        Boolean visible,
        Instant fechaInicio,
        Instant fechaFin
) {}