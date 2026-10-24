package com.tiendafrancis.comercio.model;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportesService {

 private final NamedParameterJdbcTemplate jdbc;

 private static final ZoneId LIMA =
         ZoneId.of("America/Lima");

 public record Periodo(
         LocalDate desde,
         LocalDate hasta,
         Instant inicio,
         Instant fin
 ) {}

 public Periodo periodo(
         LocalDate desde,
         LocalDate hasta
 ) {
  if (desde == null || hasta == null
          || hasta.isBefore(desde)
          || ChronoUnit.DAYS.between(desde, hasta) > 1826) {
   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST,
           "Fechas inválidas; intervalo máximo de cinco años"
   );
  }

  return new Periodo(
          desde,
          hasta,
          desde.atStartOfDay(LIMA).toInstant(),
          hasta.plusDays(1).atStartOfDay(LIMA).toInstant()
  );
 }

 private MapSqlParameterSource fechas(Periodo p) {
  return new MapSqlParameterSource()
          .addValue("inicio", Timestamp.from(p.inicio()))
          .addValue("fin", Timestamp.from(p.fin()));
 }

 private BigDecimal dinero(
         String sql,
         SqlParameterSource parametros
 ) {
  BigDecimal monto = jdbc.queryForObject(
          sql, parametros, BigDecimal.class
  );

  return monto == null
          ? BigDecimal.ZERO.setScale(2)
          : monto;
 }

 private List<Map<String, Object>> filas(
         String sql,
         SqlParameterSource parametros
 ) {
  return jdbc.queryForList(sql, parametros);
 }

 private Map<String, Object> datos(Object... valores) {
  Map<String, Object> resultado = new LinkedHashMap<>();

  for (int i = 0; i < valores.length; i += 2) {
   resultado.put((String) valores[i], valores[i + 1]);
  }

  return resultado;
 }

 public Map<String, Object> ventas(Periodo p) {
  var params = fechas(p);

  return datos(
          "desde", p.desde(),
          "hasta", p.hasta(),

          "valorPedidosCreados",
          dinero("""
                    SELECT COALESCE(SUM(total), 0)
                    FROM ventas
                    WHERE creada >= :inicio
                      AND creada < :fin
                    """, params),

          "valorVentasEntregadas",
          dinero("""
                    SELECT COALESCE(SUM(total), 0)
                    FROM ventas
                    WHERE estado = 'ENTREGADO'
                      AND creada >= :inicio
                      AND creada < :fin
                    """, params),

          "porTipo",
          filas("""
                    SELECT tipo,
                           COUNT(*) AS cantidad,
                           COALESCE(SUM(total), 0) AS total
                    FROM ventas
                    WHERE creada >= :inicio
                      AND creada < :fin
                    GROUP BY tipo
                    ORDER BY tipo
                    """, params),

          "porEstado",
          filas("""
                    SELECT estado, COUNT(*) AS cantidad
                    FROM ventas
                    WHERE creada >= :inicio
                      AND creada < :fin
                    GROUP BY estado
                    ORDER BY estado
                    """, params),

          "productosMasVendidos",
          filas("""
                    SELECT d.variante_id,
                           d.producto_nombre,
                           d.talla,
                           SUM(d.cantidad) AS unidades,
                           SUM(d.precio_unitario * d.cantidad) AS importe
                    FROM detalle_ventas d
                    JOIN ventas v ON v.id = d.venta_id
                    WHERE v.creada >= :inicio
                      AND v.creada < :fin
                      AND v.estado <> 'CANCELADO'
                    GROUP BY d.variante_id,
                             d.producto_nombre,
                             d.talla
                    ORDER BY unidades DESC, variante_id
                    LIMIT 25
                    """, params)
  );
 }

 public Map<String, Object> caja(Periodo p) {
  var params = fechas(p);

  String movimientos = """
            SELECT creado AS fecha,
                   monto AS importe,
                   'COBRO' AS tipo
            FROM pagos
            WHERE creado >= :inicio AND creado < :fin

            UNION ALL

            SELECT anulado_en AS fecha,
                   -monto AS importe,
                   'ANULACION' AS tipo
            FROM pagos
            WHERE anulado_en >= :inicio AND anulado_en < :fin

            UNION ALL

            SELECT creada AS fecha,
                   -monto AS importe,
                   'DEVOLUCION' AS tipo
            FROM devoluciones
            WHERE creada >= :inicio AND creada < :fin
            """;

  return datos(
          "desde", p.desde(),
          "hasta", p.hasta(),

          "cobradoBruto",
          dinero("""
                    SELECT COALESCE(SUM(monto), 0)
                    FROM pagos
                    WHERE creado >= :inicio AND creado < :fin
                    """, params),

          "anulado",
          dinero("""
                    SELECT COALESCE(SUM(monto), 0)
                    FROM pagos
                    WHERE anulado_en >= :inicio
                      AND anulado_en < :fin
                    """, params),

          "devuelto",
          dinero("""
                    SELECT COALESCE(SUM(monto), 0)
                    FROM devoluciones
                    WHERE creada >= :inicio AND creada < :fin
                    """, params),

          "neto",
          dinero(
                  "SELECT COALESCE(SUM(importe), 0) FROM ("
                          + movimientos + ") m",
                  params
          ),

          "movimientosPorDia",
          filas("""
                    SELECT (fecha AT TIME ZONE 'America/Lima')::date AS dia,
                           tipo,
                           COUNT(*) AS operaciones,
                           SUM(importe) AS importe
                    FROM (
                    """ + movimientos + """
                    ) m
                    GROUP BY dia, tipo
                    ORDER BY dia, tipo
                    """, params)
  );
 }

 public Map<String, Object> inventario(int umbral) {
  if (umbral < 0 || umbral > 100000) {
   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST,
           "Umbral fuera de rango"
   );
  }

  var params = new MapSqlParameterSource()
          .addValue("umbral", umbral);

  String tablas = """
            FROM producto_tallas t
            JOIN productos p ON p.id = t.producto_id
            """;

  return datos(
          "umbral", umbral,

          "totales",
          jdbc.queryForMap("""
                    SELECT COALESCE(SUM(t.stock_fisico), 0) AS fisico,
                           COALESCE(SUM(t.stock_reservado), 0) AS reservado,
                           COALESCE(SUM(
                               t.stock_fisico - t.stock_reservado
                           ), 0) AS disponible
                    """ + tablas, params),

          "stockBajo",
          filas("""
                    SELECT t.id AS variante_id,
                           p.nombre AS producto,
                           t.talla,
                           t.stock_fisico AS fisico,
                           t.stock_reservado AS reservado,
                           (t.stock_fisico - t.stock_reservado) AS disponible
                    """ + tablas + """
                    WHERE p.activo = true
                      AND (t.stock_fisico - t.stock_reservado) <= :umbral
                    ORDER BY disponible ASC, t.id ASC
                    LIMIT 100
                    """, params),

          "movimientosReposicion",
          filas("""
                    SELECT variante_id,
                           COUNT(*) AS movimientos,
                           SUM(cantidad) AS unidades
                    FROM movimientos_inventario
                    GROUP BY variante_id
                    ORDER BY variante_id
                    """, params)
  );
 }

 public Map<String, Object> pagos(Periodo p) {
  var params = fechas(p);

  return datos(
          "desde", p.desde(),
          "hasta", p.hasta(),

          "porMedio",
          filas("""
                    SELECT medio,
                           COUNT(*) AS operaciones,
                           COALESCE(SUM(monto), 0) AS monto
                    FROM pagos
                    WHERE creado >= :inicio AND creado < :fin
                    GROUP BY medio
                    ORDER BY medio
                    """, params),

          "confirmaciones",
          filas("""
                    SELECT v.tipo AS tipo_venta,
                           COUNT(*) AS operaciones,
                           COALESCE(SUM(p.monto), 0) AS monto
                    FROM pagos p
                    JOIN ventas v ON v.id = p.venta_id
                    WHERE p.creado >= :inicio
                      AND p.creado < :fin
                    GROUP BY v.tipo
                    ORDER BY v.tipo
                    """, params),

          "anulaciones",
          filas("""
                    SELECT medio,
                           COUNT(*) AS operaciones,
                           COALESCE(SUM(monto), 0) AS monto
                    FROM pagos
                    WHERE anulado_en >= :inicio
                      AND anulado_en < :fin
                    GROUP BY medio
                    ORDER BY medio
                    """, params)
  );
 }

 public Map<String, Object> devoluciones(Periodo p) {
  var params = fechas(p);

  return datos(
          "desde", p.desde(),
          "hasta", p.hasta(),

          "reembolsos",
          jdbc.queryForMap("""
                    SELECT COUNT(*) AS operaciones,
                           COALESCE(SUM(monto), 0) AS monto
                    FROM devoluciones
                    WHERE creada >= :inicio
                      AND creada < :fin
                    """, params),

          "porMedioOriginal",
          filas("""
                    SELECT p.medio,
                           COUNT(*) AS operaciones,
                           COALESCE(SUM(d.monto), 0) AS monto
                    FROM devoluciones d
                    JOIN pagos p ON p.id = d.pago_id
                    WHERE d.creada >= :inicio
                      AND d.creada < :fin
                    GROUP BY p.medio
                    ORDER BY p.medio
                    """, params),

          "prendasRecibidas",
          filas("""
                    SELECT condicion,
                           COUNT(*) AS recepciones,
                           COALESCE(SUM(cantidad), 0) AS unidades
                    FROM recepciones_devolucion
                    WHERE recibido_en >= :inicio
                      AND recibido_en < :fin
                    GROUP BY condicion
                    ORDER BY condicion
                    """, params),

          "unidadesRepuestas",
          jdbc.queryForMap("""
                    SELECT COUNT(*) AS movimientos,
                           COALESCE(SUM(cantidad), 0) AS unidades
                    FROM movimientos_inventario
                    WHERE creado_en >= :inicio
                      AND creado_en < :fin
                    """, params)
  );
 }
}