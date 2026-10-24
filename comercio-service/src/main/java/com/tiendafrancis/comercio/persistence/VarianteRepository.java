package com.tiendafrancis.comercio.persistence;

import com.tiendafrancis.comercio.bean.entity.ProductoTalla;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VarianteRepository
        extends JpaRepository<ProductoTalla, Long> {

 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select v from ProductoTalla v where v.id = :id")
 Optional<ProductoTalla> bloquear(@Param("id") Long id);
}