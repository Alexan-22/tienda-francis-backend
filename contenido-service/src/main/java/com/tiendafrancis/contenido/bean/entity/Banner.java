package com.tiendafrancis.contenido.bean.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "home_banners")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 500)
    private String subtitulo;

    @Column(name = "imagen_url", nullable = false, length = 1024)
    private String imagenUrl;

    @Column(name = "enlace_url", length = 1024)
    private String enlaceUrl;

    @Column(name = "texto_boton", length = 100)
    private String textoBoton;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(nullable = false)
    private int orden;

    @Column(name = "fecha_inicio")
    private Instant fechaInicio;

    @Column(name = "fecha_fin")
    private Instant fechaFin;

    @Version
    private long version;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    public Banner(
            String titulo,
            String subtitulo,
            String imagenUrl,
            String enlaceUrl,
            String textoBoton,
            int orden,
            boolean visible,
            Instant fechaInicio,
            Instant fechaFin
    ) {
        actualizar(titulo, subtitulo, imagenUrl, enlaceUrl,
                textoBoton, orden, visible, fechaInicio, fechaFin);
    }

    public void actualizar(
            String titulo,
            String subtitulo,
            String imagenUrl,
            String enlaceUrl,
            String textoBoton,
            int orden,
            boolean visible,
            Instant fechaInicio,
            Instant fechaFin
    ) {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.imagenUrl = imagenUrl;
        this.enlaceUrl = enlaceUrl;
        this.textoBoton = textoBoton;
        this.orden = orden;
        this.visible = visible;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public void cambiarVisibilidad(boolean visible) {
        this.visible = visible;
    }
}