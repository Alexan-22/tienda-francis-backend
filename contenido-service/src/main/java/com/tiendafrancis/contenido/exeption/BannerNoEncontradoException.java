package com.tiendafrancis.contenido.exception;

public class BannerNoEncontradoException extends RuntimeException {

    public BannerNoEncontradoException(Long id) {
        super("No existe el banner con ID " + id);
    }
}