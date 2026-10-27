
package com.tiendafrancis.comercio.model;

import com.tiendafrancis.comercio.bean.entity.MovimientoInventario;
import com.tiendafrancis.comercio.bean.entity.ProductoTalla;

import com.tiendafrancis.comercio.persistence.MovimientoRepository;
import com.tiendafrancis.comercio.persistence.VarianteRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private final VarianteRepository variantes;
    private final MovimientoRepository movimientos;

    public record ReposicionResultado(
            Long movimientoId,
            Long varianteId,
            int cantidad,
            int stockFisico,
            int stockReservado,
            int stockDisponible,
            String tipo,
            Instant creadoEn
    ) {}

    // =================================================
    // REPOSICIÓN MANUAL
    // =================================================

    @Transactional
    public ReposicionResultado reponer(
            UUID idempotencia,
            Long varianteId,
            Integer cantidad,
            String motivo,
            Long administradorId
    ) {

        if (
                idempotencia == null
                        || varianteId == null
                        || varianteId <= 0
                        || cantidad == null
                        || cantidad <= 0
                        || administradorId == null
                        || administradorId <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Datos de reposición no válidos"
            );
        }

        if (
                motivo == null
                        || motivo.isBlank()
                        || motivo.trim().length() > 500
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El motivo es obligatorio y admite hasta 500 caracteres"
            );
        }

        String motivoLimpio = motivo.trim();

        // El bloqueo evita perder actualizaciones de stock
        // si se realizan ventas y reposiciones simultáneas.

        ProductoTalla variante = variantes
                .bloquear(varianteId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "La variante no existe"
                        )
                );

        // Buscar el UUID después de adquirir el bloqueo.
        // Una solicitud repetida con los mismos datos
        // no debe sumar mercancía una segunda vez.

        var anterior = movimientos.findByIdempotencia(
                idempotencia
        );

        if (anterior.isPresent()) {

            MovimientoInventario movimiento = anterior.get();

            if (
                    !"REPOSICION_MANUAL".equals(movimiento.tipo)
                            || !varianteId.equals(movimiento.varianteId)
                            || cantidad != movimiento.cantidad
                            || !motivoLimpio.equals(movimiento.motivo)
                            || !administradorId.equals(movimiento.responsableId)
            ) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La clave de idempotencia pertenece a otra operación"
                );
            }

            return resultado(movimiento, variante);
        }

        // Evitar desbordamiento del tipo INTEGER de PostgreSQL.

        long nuevoStock = (long) variante.stockFisico
                + cantidad;

        if (nuevoStock > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La reposición supera el límite de stock"
            );
        }

        // Registrar el movimiento antes de actualizar
        // la entidad administrada. La restricción UNIQUE
        // de PostgreSQL evita UUID duplicados.
        //
        // Si falla el INSERT, la transacción se revierte.

        MovimientoInventario movimiento =
                movimientos.saveAndFlush(
                        new MovimientoInventario(
                                varianteId,
                                idempotencia,
                                cantidad,
                                motivoLimpio,
                                administradorId
                        )
                );

        variante.stockFisico = (int) nuevoStock;

        // Hibernate persistirá el cambio de stock
        // al confirmar esta misma transacción.

        return resultado(movimiento, variante);
    }

    // =================================================
    // HISTORIAL DE MOVIMIENTOS
    // =================================================

    @Transactional(readOnly = true)
    public List<MovimientoInventario> ultimosMovimientos() {

        return movimientos
                .findTop100ByOrderByCreadoEnDescIdDesc();
    }

    // =================================================
    // RESPUESTA
    // =================================================

    private ReposicionResultado resultado(
            MovimientoInventario movimiento,
            ProductoTalla variante
    ) {

        int disponible = Math.max(
                0,
                variante.stockFisico - variante.stockReservado
        );

        return new ReposicionResultado(
                movimiento.id,
                variante.id,
                movimiento.cantidad,
                variante.stockFisico,
                variante.stockReservado,
                disponible,
                movimiento.tipo,
                movimiento.creadoEn
        );
    }
}

