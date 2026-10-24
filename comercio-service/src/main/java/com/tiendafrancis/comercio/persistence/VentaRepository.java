package com.tiendafrancis.comercio.persistence;

import com.tiendafrancis.comercio.bean.entity.Venta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface VentaRepository extends JpaRepository<Venta, Long> {

 List<Venta> findByUsuarioIdAndTipoOrderByIdDesc(
         Long usuarioId,
         String tipo
 );

 Optional<Venta> findByIdempotencia(UUID key);

 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select v from Venta v where v.id = :id")
 Optional<Venta> bloquear(@Param("id") Long id);

 @Query(
         value = """
            SELECT id
            FROM ventas
            WHERE tipo = 'ONLINE'
              AND estado = 'RECIBIDO'
              AND pago_estado = 'PENDIENTE'
              AND vence <= :instant
            ORDER BY vence, id
            FOR UPDATE SKIP LOCKED
            LIMIT :lim
            """,
         nativeQuery = true
 )
 List<Long> findExpiradasParaBloquear(
         @Param("instant") Instant instant,
         @Param("lim") int limit
 );
}