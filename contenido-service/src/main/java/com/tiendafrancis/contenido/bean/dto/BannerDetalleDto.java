package com.tiendafrancis.contenido.bean.dto;

import java.time.Instant;

public record BannerDetalleDto(
        Long id,
        String titulo,
        String subtitulo,
        String imagenUrl,
        String enlaceUrl,
        String textoBoton,
        boolean visible,
        int orden,
        Instant fechaInicio,
        Instant fechaFin,
        long version,
        Instant creadoEn,
        Instant actualizadoEn
) {}