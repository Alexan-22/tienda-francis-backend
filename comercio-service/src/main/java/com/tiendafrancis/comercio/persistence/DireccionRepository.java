package com.tiendafrancis.comercio.persistence;

import com.tiendafrancis.comercio.bean.entity.DireccionEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface DireccionRepository
        extends JpaRepository<DireccionEnvio, Long> {

 List<DireccionEnvio> findByUsuarioIdAndActivaTrueOrderByIdAsc(
         Long usuarioId
 );

 Optional<DireccionEnvio> findByIdAndUsuarioIdAndActivaTrue(
         Long id,
         Long usuarioId
 );
}
