package com.tiendafrancis.comercio.model;

import com.tiendafrancis.comercio.bean.entity.DireccionEnvio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
public class DeliveryService {

 private final Map<String, BigDecimal> tarifas;

 public DeliveryService(
         @Value("${DELIVERY_TARIFFS:}") String configuracion
 ) {
  Map<String, BigDecimal> resultado = new HashMap<>();

  if (!configuracion.isBlank()) {
   for (String entrada : configuracion.split(";")) {
    String[] datos = entrada.split("=", 2);

    if (datos.length != 2) {
     throw new IllegalArgumentException(
             "DELIVERY_TARIFFS inválido"
     );
    }

    BigDecimal precio = new BigDecimal(datos[1].trim());

    if (precio.signum() < 0 || precio.scale() > 2) {
     throw new IllegalArgumentException(
             "Tarifa de delivery inválida"
     );
    }

    if (resultado.putIfAbsent(
            clave(datos[0]),
            precio.setScale(2)
    ) != null) {
     throw new IllegalArgumentException(
             "Tarifa duplicada"
     );
    }
   }
  }

  this.tarifas = Map.copyOf(resultado);
 }

 private String clave(String valor) {
  return valor.trim()
          .replaceAll("\\s+", " ")
          .toUpperCase(Locale.ROOT);
 }

 public BigDecimal costo(DireccionEnvio direccion) {
  String ubicacion = clave(
          direccion.departamento + "|" +
                  direccion.provincia + "|" +
                  direccion.distrito
  );

  BigDecimal costo = tarifas.get(ubicacion);

  if (costo == null) {
   throw new ResponseStatusException(
           HttpStatus.UNPROCESSABLE_ENTITY,
           "Destino fuera de cobertura de delivery"
   );
  }

  return costo;
 }
}