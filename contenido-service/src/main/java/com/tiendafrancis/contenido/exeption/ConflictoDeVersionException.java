package com.tiendafrancis.contenido.exception;

public class ConflictoDeVersionException extends RuntimeException {

    public ConflictoDeVersionException() {
        super("El banner fue modificado. Recarga e intenta nuevamente.");
    }
}