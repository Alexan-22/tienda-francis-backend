package com.tiendafrancis.contenido.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ContenidoExceptionHandler {

    @ExceptionHandler(BannerNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> noEncontrado(Exception ex) {
        return problema(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({
            ConflictoDeVersionException.class,
            ObjectOptimisticLockingFailureException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ProblemDetail> conflicto(Exception ex) {
        return problema(HttpStatus.CONFLICT, "Conflicto al actualizar el banner");
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ProblemDetail> invalido(Exception ex) {
        return problema(HttpStatus.BAD_REQUEST, "Datos de banner inválidos");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validacion(
            MethodArgumentNotValidException ex
    ) {
        var pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Hay campos inválidos en la solicitud"
        );

        Map<String, String> campos = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.putIfAbsent(
                        e.getField(), e.getDefaultMessage()
                ));

        pd.setProperty("campos", campos);

        return ResponseEntity.badRequest().body(pd);
    }

    private ResponseEntity<ProblemDetail> problema(
            HttpStatus estado,
            String detalle
    ) {
        var pd = ProblemDetail.forStatusAndDetail(estado, detalle);
        pd.setTitle(estado.getReasonPhrase());

        return ResponseEntity.status(estado).body(pd);
    }
}