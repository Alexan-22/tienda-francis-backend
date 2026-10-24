package com.tiendafrancis.comercio.persistence;

import com.tiendafrancis.comercio.bean.entity.Devolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {

    Optional<Devolucion> findByIdempotencia(UUID key);

    List<Devolucion> findByPagoId(Long pagoId);
}