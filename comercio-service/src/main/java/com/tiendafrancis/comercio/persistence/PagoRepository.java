package com.tiendafrancis.comercio.persistence;

import com.tiendafrancis.comercio.bean.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByIdempotencia(UUID key);

    List<Pago> findByVentaIdOrderByIdAsc(Long id);
}