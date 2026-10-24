package com.tiendafrancis.contenido.bean.dto;

import jakarta.validation.constraints.NotNull;

public record VisibilidadBannerRequest(
        @NotNull Boolean visible,
        @NotNull Long version
) {}