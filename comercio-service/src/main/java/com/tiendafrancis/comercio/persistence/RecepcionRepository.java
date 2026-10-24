package com.tiendafrancis.comercio.persistence;

import com.tiendafrancis.comercio.bean.entity.RecepcionDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RecepcionRepository
        extends JpaRepository<RecepcionDevolucion, Long> {

 Optional<RecepcionDevolucion> findByIdempotencia(UUID idempotencia);

 List<RecepcionDevolucion> findByVentaIdOrderByIdAsc(Long ventaId);

 List<RecepcionDevolucion> findByDetalleId(Long detalleId);
}
