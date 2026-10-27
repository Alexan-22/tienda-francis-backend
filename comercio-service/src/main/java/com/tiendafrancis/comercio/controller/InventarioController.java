
package com.tiendafrancis.comercio.controller;

import com.tiendafrancis.comercio.bean.entity.MovimientoInventario;
import com.tiendafrancis.comercio.model.InventarioService;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventario;

    // =================================================
    // SOLICITUD
    // =================================================

    public record ReposicionRequest(
            UUID idempotencia,
            Long varianteId,
            Integer cantidad,
            String motivo
    ) {}

    // =================================================
    // HISTORIAL
    // =================================================

    public record MovimientoDto(
            Long id,
            Long varianteId,
            Long recepcionId,
            UUID idempotencia,
            int cantidad,
            String tipo,
            String motivo,
            Long responsableId,
            Instant creadoEn
    ) {}

    // =================================================
    // REPONER STOCK - SOLO ADMIN
    // =================================================

    @PostMapping("/reposiciones")
    @ResponseStatus(HttpStatus.CREATED)
    public InventarioService.ReposicionResultado reponer(
            @RequestBody ReposicionRequest req,
            Authentication authentication
    ) {

        Long administradorId = obtenerAdministrador(
                authentication
        );

        try {
            return inventario.reponer(
                    req.idempotencia(),
                    req.varianteId(),
                    req.cantidad(),
                    req.motivo(),
                    administradorId
            );

        } catch (DataIntegrityViolationException ex) {

            // Por ejemplo, una clave UUID registrada
            // por una solicitud concurrente.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflicto al registrar la reposición"
            );
        }
    }

    // =================================================
    // CONSULTAR HISTORIAL - SOLO ADMIN
    // =================================================

    @GetMapping("/movimientos")
    public List<MovimientoDto> movimientos(
            Authentication authentication
    ) {

        obtenerAdministrador(authentication);

        return inventario.ultimosMovimientos()
                .stream()
                .map(this::convertir)
                .toList();
    }

    private MovimientoDto convertir(
            MovimientoInventario movimiento
    ) {

        return new MovimientoDto(
                movimiento.id,
                movimiento.varianteId,
                movimiento.recepcionId,
                movimiento.idempotencia,
                movimiento.cantidad,
                movimiento.tipo,
                movimiento.motivo,
                movimiento.responsableId,
                movimiento.creadoEn
        );
    }

    // =================================================
    // VERIFICAR IDENTIDAD ADMINISTRATIVA
    // =================================================

    private Long obtenerAdministrador(
            Authentication authentication
    ) {

        if (
                authentication == null
                        || !authentication.isAuthenticated()
                        || authentication.getAuthorities()
                        .stream()
                        .noneMatch(a ->
                                "ROLE_ADMIN".equals(a.getAuthority())
                        )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Se requiere rol ADMIN"
            );
        }

        try {
            return Long.parseLong(authentication.getName());

        } catch (NumberFormatException ex) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Identidad de usuario inválida"
            );
        }
    }
}
