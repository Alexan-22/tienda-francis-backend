package com.tiendafrancis.contenido.controller;

import com.tiendafrancis.contenido.bean.dto.*;
import com.tiendafrancis.contenido.usecase.BannerUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contenido/banners")
public class AdminBannerController {

    private final BannerUseCase banners;

    @GetMapping
    public List<BannerDetalleDto> listar() {
        return banners.listarTodos();
    }

    @GetMapping("/{id}")
    public BannerDetalleDto obtener(@PathVariable Long id) {
        return banners.obtener(id);
    }

    @PostMapping
    public ResponseEntity<BannerDetalleDto> crear(
            @Valid @RequestBody CrearBannerRequest request
    ) {
        var creado = banners.crear(request);

        return ResponseEntity.created(
                URI.create("/api/contenido/banners/" + creado.id())
        ).body(creado);
    }

    @PutMapping("/{id}")
    public BannerDetalleDto actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarBannerRequest request
    ) {
        return banners.actualizar(id, request);
    }

    @PatchMapping("/{id}/visibilidad")
    public BannerDetalleDto cambiarVisibilidad(
            @PathVariable Long id,
            @Valid @RequestBody VisibilidadBannerRequest request
    ) {
        return banners.cambiarVisibilidad(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        banners.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}