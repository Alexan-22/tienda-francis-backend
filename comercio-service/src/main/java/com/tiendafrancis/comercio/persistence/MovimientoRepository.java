
package com.tiendafrancis.comercio.persistence;

import com.tiendafrancis.comercio.bean.entity.MovimientoInventario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovimientoRepository
        extends JpaRepository<MovimientoInventario, Long> {

 // Conservado para las devoluciones.
 List<MovimientoInventario> findByRecepcionId(
         Long recepcionId
 );

 // Recuperar una reposición ya procesada.
 Optional<MovimientoInventario> findByIdempotencia(
         UUID idempotencia
 );

 // Últimos 100 movimientos para auditoría.
 List<MovimientoInventario>
 findTop100ByOrderByCreadoEnDescIdDesc();
}
