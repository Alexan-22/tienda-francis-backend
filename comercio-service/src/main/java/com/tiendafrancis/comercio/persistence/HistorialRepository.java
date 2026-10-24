package com.tiendafrancis.comercio.persistence;

import com.tiendafrancis.comercio.bean.entity.HistorialPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialRepository
        extends JpaRepository<HistorialPedido, Long> {

 List<HistorialPedido> findByVentaIdOrderByIdAsc(Long ventaId);
}