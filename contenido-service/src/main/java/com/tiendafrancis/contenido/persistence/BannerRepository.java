package com.tiendafrancis.contenido.persistence;

import com.tiendafrancis.contenido.bean.entity.Banner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannerRepository
        extends JpaRepository<Banner, Long> {

    List<Banner> findByVisibleTrueOrderByOrdenAscIdAsc();

    List<Banner> findAllByOrderByOrdenAscIdAsc();
}