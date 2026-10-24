package com.tiendafrancis.contenido.usecase;

import com.tiendafrancis.contenido.bean.dto.*;
import java.util.List;

public interface BannerUseCase {

    List<BannerPublicoDto> listarPublicos();

    List<BannerDetalleDto> listarTodos();

    BannerDetalleDto obtener(Long id);

    BannerDetalleDto crear(CrearBannerRequest request);

    BannerDetalleDto actualizar(Long id, ActualizarBannerRequest request);

    BannerDetalleDto cambiarVisibilidad(Long id, VisibilidadBannerRequest request);

    void desactivar(Long id);
}