package com.tiendafrancis.contenido.model;

import com.tiendafrancis.contenido.bean.dto.*;
import com.tiendafrancis.contenido.bean.entity.Banner;
import com.tiendafrancis.contenido.exception.*;
import com.tiendafrancis.contenido.persistence.BannerRepository;
import com.tiendafrancis.contenido.usecase.BannerUseCase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerModel implements BannerUseCase {

    private final BannerRepository repositorio;
    private final Clock clock;

    @Override
    public List<BannerPublicoDto> listarPublicos() {
        Instant ahora = clock.instant();

        return repositorio.findByVisibleTrueOrderByOrdenAscIdAsc()
                .stream()
                .filter(b -> (b.getFechaInicio() == null || !ahora.isBefore(b.getFechaInicio()))
                        && (b.getFechaFin() == null || ahora.isBefore(b.getFechaFin())))
                .map(this::aPublico)
                .toList();
    }

    @Override
    public List<BannerDetalleDto> listarTodos() {
        return repositorio.findAllByOrderByOrdenAscIdAsc()
                .stream()
                .map(this::aDetalle)
                .toList();
    }

    @Override
    public BannerDetalleDto obtener(Long id) {
        return aDetalle(buscar(id));
    }

    @Override
    @Transactional
    public BannerDetalleDto crear(CrearBannerRequest s) {
        validar(s.imagenUrl(), s.enlaceUrl(), s.fechaInicio(), s.fechaFin());

        Banner banner = new Banner(
                s.titulo().trim(),
                limpiar(s.subtitulo()),
                s.imagenUrl().trim(),
                limpiar(s.enlaceUrl()),
                limpiar(s.textoBoton()),
                s.orden(),
                s.visible() == null || s.visible(),
                s.fechaInicio(),
                s.fechaFin()
        );

        return aDetalle(repositorio.saveAndFlush(banner));
    }

    @Override
    @Transactional
    public BannerDetalleDto actualizar(Long id, ActualizarBannerRequest s) {
        Banner banner = buscar(id);

        verificarVersion(banner, s.version());
        validar(s.imagenUrl(), s.enlaceUrl(), s.fechaInicio(), s.fechaFin());

        banner.actualizar(
                s.titulo().trim(),
                limpiar(s.subtitulo()),
                s.imagenUrl().trim(),
                limpiar(s.enlaceUrl()),
                limpiar(s.textoBoton()),
                s.orden(),
                s.visible(),
                s.fechaInicio(),
                s.fechaFin()
        );

        return aDetalle(repositorio.saveAndFlush(banner));
    }

    @Override
    @Transactional
    public BannerDetalleDto cambiarVisibilidad(
            Long id,
            VisibilidadBannerRequest s
    ) {
        Banner banner = buscar(id);
        verificarVersion(banner, s.version());

        banner.cambiarVisibilidad(s.visible());

        return aDetalle(repositorio.saveAndFlush(banner));
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Banner banner = buscar(id);

        if (banner.isVisible()) {
            banner.cambiarVisibilidad(false);
            repositorio.saveAndFlush(banner);
        }
    }

    private Banner buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new BannerNoEncontradoException(id));
    }

    private void verificarVersion(Banner banner, Long version) {
        if (version == null || banner.getVersion() != version) {
            throw new ConflictoDeVersionException();
        }
    }

    private void validar(
            String imagen,
            String enlace,
            Instant inicio,
            Instant fin
    ) {
        if (inicio != null && fin != null && !fin.isAfter(inicio)) {
            throw new IllegalArgumentException(
                    "fechaFin debe ser posterior a fechaInicio"
            );
        }

        validarUrl(imagen, true);

        if (enlace != null && !enlace.isBlank()) {
            validarUrl(enlace, false);
        }
    }

    private void validarUrl(String valor, boolean imagen) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La URL no puede estar vacía");
        }

        String url = valor.trim();

        if (url.startsWith("/") && !url.startsWith("//")
                && !url.contains("..") && !url.contains("\\")) {
            if (!imagen || url.startsWith("/uploads/contenido/")) {
                return;
            }
        }

        try {
            URI uri = URI.create(url);

            if ("https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getUserInfo() == null) {
                return;
            }
        } catch (IllegalArgumentException ignored) {
            // Se lanza un error de validación uniforme.
        }

        throw new IllegalArgumentException(
                "La URL debe usar HTTPS o una ruta local permitida"
        );
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private BannerPublicoDto aPublico(Banner b) {
        return new BannerPublicoDto(
                b.getId(), b.getTitulo(), b.getImagenUrl(), b.getOrden(),
                b.getSubtitulo(), b.getEnlaceUrl(), b.getTextoBoton()
        );
    }

    private BannerDetalleDto aDetalle(Banner b) {
        return new BannerDetalleDto(
                b.getId(), b.getTitulo(), b.getSubtitulo(),
                b.getImagenUrl(), b.getEnlaceUrl(), b.getTextoBoton(),
                b.isVisible(), b.getOrden(), b.getFechaInicio(),
                b.getFechaFin(), b.getVersion(),
                b.getCreadoEn(), b.getActualizadoEn()
        );
    }
}
