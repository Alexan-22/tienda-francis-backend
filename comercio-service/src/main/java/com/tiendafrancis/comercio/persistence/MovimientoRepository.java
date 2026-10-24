package com.tiendafrancis.comercio.persistence;

import com.tiendafrancis.comercio.bean.entity.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovimientoRepository
        extends JpaRepository<MovimientoInventario, Long> {

 List<MovimientoInventario> findByRecepcionId(Long recepcionId);
}