
package com.tiendafrancis.comercio.persistence;

import com.tiendafrancis.comercio.bean.entity.ProductoTalla;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VarianteRepository
        extends JpaRepository<ProductoTalla, Long> {

 // Se conserva el bloqueo utilizado
 // durante las operaciones de venta.
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select v from ProductoTalla v where v.id = :id")
 Optional<ProductoTalla> bloquear(
         @Param("id") Long id
 );

 // Obtener todas las tallas de un grupo de productos
 // mediante una sola consulta.
 List<ProductoTalla> findByProductoIdIn(
         Collection<Long> productoIds
 );
}
