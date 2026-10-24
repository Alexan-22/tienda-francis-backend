package com.tiendafrancis.comercio.model;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservasScheduler {

 private final PedidosService pedidos;

 @Scheduled(
         fixedDelayString = "${STOCK_EXPIRATION_SCAN_MS:60000}"
 )
 public void ejecutar() {
  int cantidad;

  do {
   cantidad = pedidos.vencer(50);
  } while (cantidad == 50);
 }
}