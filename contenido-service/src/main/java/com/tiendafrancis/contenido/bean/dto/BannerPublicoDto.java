package com.tiendafrancis.contenido.bean.dto;

public record BannerPublicoDto(
        Long id,
        String titulo,
        String imagenUrl,
        int orden,
        String subtitulo,
        String enlaceUrl,
        String textoBoton
) {}