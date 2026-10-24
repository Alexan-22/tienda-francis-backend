# Tienda Francis Backend

Proyecto Maven multimódulo **parcial** de tienda de ropa en Java 21, Spring Boot y Spring Cloud. No se considera terminado ni listo para producción. Véase [estado](docs/ESTADO.md).

## Módulos

| Módulo | Puerto | Función |
|---|---:|---|
| discovery-server | 8761 | Eureka |
| api-gateway | 8085 | Rutas explícitas |
| auth-service | 8081 | Registro, login JWT |
| comercio-service | 8082 | Catálogo, reservas online, POS, cobro manual y devoluciones (sin pruebas de integración) |
| contenido-service | 8083 | Banners visibles |

## Requisitos

- JDK 21, Maven 3.9+, PostgreSQL local.
- Crear bases `tienda_auth_db`, `tienda_comercio_db`, `tienda_contenido_db` (por ejemplo `createdb -U postgres tienda_auth_db`, repetir para las otras).
- Establecer `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` independientes por servicio, y `JWT_SECRET` (mínimo 32 bytes aleatorios), `INTERNAL_API_KEY` en Gateway y Auth. `.env` no se carga automáticamente por Spring Boot.

## Compilar

Linux/macOS: `mvn clean verify`

PowerShell: `mvn.cmd clean verify`

## Arranque local, en 5 consolas

Desde raíz, con variables de entorno configuradas para cada proceso:

```sh
mvn -pl discovery-server spring-boot:run
mvn -pl auth-service spring-boot:run
mvn -pl comercio-service spring-boot:run
mvn -pl contenido-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```

PowerShell (una consola para cada comando): `mvn.cmd -pl discovery-server spring-boot:run`, etc. Cambiar `DB_URL` de cada consola.

## API pública provisional

```sh
curl -X POST http://localhost:8085/api/auth/registro -H 'Content-Type: application/json' -d '{"email":"cliente@example.com","nombre":"Cliente","password":"UnaClaveRobusta"}'
curl -X POST http://localhost:8085/api/auth/login -H 'Content-Type: application/json' -d '{"email":"cliente@example.com","password":"UnaClaveRobusta"}'
curl http://localhost:8085/api/productos
curl http://localhost:8085/api/home/banners
```

**No intentar ventas reales**. La API privada inicial requiere token y clave interna en todos los servicios, pero falta cierre de flujos y pruebas de seguridad, vencimientos e idempotencia. Ver docs/ESTADO.md.

## Extensión: ventas presenciales POS, pagos y devoluciones

Se agregó la migración `V2__pos_pagos_devoluciones.sql`, que amplía `ventas` y crea tablas `pagos` y `devoluciones`. Aplicar Flyway únicamente a una base de ensayo respaldada, NO a producción.

Todos los endpoints siguientes requieren `Authorization: Bearer <JWT_ADMIN>` y se consumen por el Gateway en `http://localhost:8085`. Los UUID de ejemplo deben generarse nuevos en cada operación independiente:

```bash
# POS: registra venta, descuenta stock físico y registra cobro manual en una transacción
curl -X POST http://localhost:8085/api/ventas/pos -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"idempotencia":"b063c91f-08fb-4c45-a8fa-b9e9842641c3","cliente":"Cliente de mostrador","medio":"EFECTIVO","items":[{"varianteId":1,"cantidad":1}]}'

# Cobro online manual: confirma pago íntegro, convierte reserva en salida de stock
curl -X POST http://localhost:8085/api/ventas/1/pagos -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"idempotencia":"6e36b848-b0d4-4e4e-93a3-afb39d726d6a","monto":59.90,"medio":"YAPE","referencia":"OPERACION-123"}'

# Devolución monetaria: indicar el ID del pago, nunca el de la venta
curl -X POST http://localhost:8085/api/pagos/1/devoluciones -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"idempotencia":"bb293648-555e-40b8-8667-1d3788e4757d","monto":10.00,"motivo":"Devolución acordada"}'
```

Consultas ADMIN: `GET /api/ventas/{id}/pagos` y `GET /api/pagos/{id}/devoluciones`. `POST /api/pagos/{id}/anular` existe solo para cobros sin venta confirmada; se bloquea cuando el dinero ya fue registrado como cobrado y exige otro flujo de regularización.

**Importante:** Son registros manuales de dinero; no verifican depósitos Yape ni procesan tarjetas. Devolver dinero no repone stock. La ruta antigua `POST /api/ventas/{id}/confirmar-pago` fue retirada para evitar la confirmación de cobros sin registro monetario. No hay aún tareas de expiración automáticas ni devolución física de prendas, ni política completa de reversión de ventas pagadas. No se han probado carreras concurrentes ni el arranque con PostgreSQL real; no usar en producción.

## Ampliación POS y caja manual (octubre 2026)

La migración Flyway `V2__pos_pagos_devoluciones.sql` permite ventas presenciales sin cuenta, con nombre de cliente opcional. El usuario ADMIN autenticado queda como responsable, se registra un pago manual por el total y se descuenta stock físico (sin usar reservas). **No procesa pagos bancarios reales**.

Todos estos endpoints exigen un JWT de ADMIN y, en el servicio, introspección activa del token:

```sh
# Sustituir TOKEN_ADMIN por un token auténtico de usuario ADMIN; no hay admin demo por defecto.
export TOKEN_ADMIN='...'
BASE=http://localhost:8085
curl -X POST "$BASE/api/ventas/pos" -H "Authorization: Bearer $TOKEN_ADMIN" -H 'Content-Type: application/json' -d '{"idempotencia":"b5389803-4509-4b72-a00f-bfd9f0bf4101","clienteNombre":"Cliente de mostrador","items":[{"varianteId":1,"cantidad":2}],"medio":"EFECTIVO","referencia":null}'
# Para confirmar un pedido ONLINE pendiente; monto debe ser igual al total que calcula el servidor.
curl -X POST "$BASE/api/ventas/1/pagos" -H "Authorization: Bearer $TOKEN_ADMIN" -H 'Content-Type: application/json' -d '{"idempotencia":"9b0bb98a-5612-4399-933d-11a69eefbdce","monto":90.00,"medio":"YAPE","referencia":"Operacion manual verificada"}'
curl "$BASE/api/ventas/1/pagos" -H "Authorization: Bearer $TOKEN_ADMIN"
curl -X POST "$BASE/api/pagos/1/devoluciones" -H "Authorization: Bearer $TOKEN_ADMIN" -H 'Content-Type: application/json' -d '{"idempotencia":"539fac49-64c4-4ea3-b1cc-4660b4d48d85","monto":20.00,"motivo":"Devolución parcial aprobada"}'
```

**Reglas**: POS admite EFECTIVO, YAPE y TARJETA, entrega inmediata, y no figura como compra de un cliente registrado. ONLINE acepta únicamente pago completo, por YAPE o TARJETA, confirmado por ADMIN antes del vencimiento. Un reembolso requiere pago original, monto positivo, motivo y UUID; no puede superar el cobro pendiente de devolución. **Devolver dinero nunca repone inventario**. Las claves repetidas con distinto contenido o responsable responden 409 al detectarse secuencialmente; falta robustecer colisiones simultáneas con transacciones separadas y verificar integración real PostgreSQL.

La ruta antigua `POST /api/ventas/{id}/confirmar-pago` responde 410: usar `POST /api/ventas/{id}/pagos` para no confirmar cobros sin historial. Requiere adaptación de cualquier frontend que usara esa ruta.


## Pruebas de POS y concurrencia

Véase [docs/PRUEBAS_POS.md](docs/PRUEBAS_POS.md) para ejecutar JUnit y Testcontainers sobre PostgreSQL real.

## Cancelaciones y devoluciones físicas (V3)

Solo un administrador autenticado puede utilizar estos endpoints:

```http
POST /api/ventas/123/cancelar
Content-Type: application/json
Authorization: Bearer <token-admin>

{"motivo":"Cancelación solicitada"}
```

Solo pedidos ONLINE en `RECIBIDO`/`EN_PREPARACION` son cancelables. Sin pago, se libera únicamente la reserva. Con cobro confirmado, es obligatorio haber devuelto íntegramente el dinero. **Nunca se agrega stock automáticamente al cancelar una compra ya cobrada.**

```http
POST /api/ventas/123/recepciones
Content-Type: application/json
Authorization: Bearer <token-admin>

{"idempotencia":"c6b71c62-2222-4333-8444-123456789012","detalleId":7,"cantidad":1,"condicion":"APTO","motivo":"Cambio de talla; producto inspeccionado"}
```

`APTO` incrementa stock y deja movimiento auditado; `DANADO` deja evidencia de recepción sin sumar al inventario. El detalle debe pertenecer a una venta `ENTREGADO`; las cantidades recibidas acumuladas no pueden superar las vendidas. Para consultar, `GET /api/ventas/{id}/recepciones`.

Para ejecutar todas las pruebas de comercio (requiere Docker, Java 21 y Maven):

```bash
mvn -pl comercio-service -am verify
```

Las pruebas `PostventaPostgresIT` son pruebas de integración reales con PostgreSQL vía Testcontainers; **se añadieron pero no se ejecutaron en este entorno**. No se recomienda producción sin compilación, tests y revisión de seguridad de la solución completa.


## Seguridad e idempotencia concurrente

El servicio comercio emplea introspección interna para operaciones privadas y bloqueos transaccionales PostgreSQL `pg_advisory_xact_lock` por clave y tipo de operación. Los controladores de POS, pago y devolución invocan el mismo servicio transaccional. Pruebas nuevas en `IdempotenciaConcurrentePostgresIT` e `InternalIntrospectionFilterTest`. Ejecutar con Java 21, Maven y Docker: `mvn -pl comercio-service -am verify`. Consulte `docs/VERIFICACION.md` para limitaciones.


## Reportes administrativos (nuevo)

Solo ADMIN, por Gateway `http://localhost:8085`. Las fechas `desde` y `hasta` se reciben como `YYYY-MM-DD` en zona **America/Lima**, ambos días incluidos; internamente se consulta `[inicio, fin)` en UTC. Máximo cinco años por solicitud. Respuestas JSON, sin PDF o Excel.

- `GET /api/reportes/ventas?desde=2026-10-01&hasta=2026-10-31`: pedidos creados, ventas entregadas, tipos, estados y variantes más vendidas (hasta 25).
- `GET /api/reportes/caja?desde=2026-10-01&hasta=2026-10-31`: bruto cobrado, ajustes por anulación, reembolsos, neto y movimientos diarios. Cada operación se contabiliza en su propia fecha, sin alterar el historial.
- `GET /api/reportes/inventario?umbral=5`: totales físicos, reservados y disponibles, primeras 100 variantes de stock bajo y resumen de reposiciones auditadas.
- `GET /api/reportes/pagos?desde=2026-10-01&hasta=2026-10-31`: cobros agrupados por medio y tipo de venta, anulaciones separadas por fecha.
- `GET /api/reportes/devoluciones?desde=2026-10-01&hasta=2026-10-31`: reembolsos, medio original, recepción física por condición y unidades repuestas.

**Limitación del esquema actual:** `ventas` solo registra el instante de creación, no la fecha exacta de entrega. La cifra `valorVentasEntregadas` representa ventas *actualmente* entregadas que se crearon dentro del intervalo, no entregas que tuvieron lugar durante ese intervalo. No usarla como métrica contable de entregas por fecha hasta incorporar historial de transiciones. El resumen de productos vendidos excluye cancelaciones según su estado actual, no según un histórico de ajustes. Las reposiciones físicas están registradas en `movimientos_inventario`; otras entradas/salidas anteriores no tienen auditoría completa en ese registro. Los reportes no crean asientos contables externos.

Pruebas: `ReportesServiceTest` (límite de fechas) y `ReportesPostgresIT` (PostgreSQL real, ingresos por fechas independientes, stock y reembolsos). Ejecutar `mvn -pl comercio-service -am verify` con Docker disponible. No se han ejecutado aquí.


## Auditoría de seguridad y regresiones — 19/10/2026

Ver [docs/AUDITORIA_2026-10-19.md](docs/AUDITORIA_2026-10-19.md) para correcciones y limitaciones verificadas.


## Direcciones, delivery, vencimiento y seguimiento (octubre 2026)

Se añade la migración `V4__direcciones_delivery_y_seguimiento.sql`. Solo CLIENTE autenticado puede gestionar sus direcciones y consultar sus pedidos; ADMIN administra la logística. `POST /api/ventas` ahora **requiere `direccionId`**, un cambio de contrato respecto a versiones anteriores.

Endpoints (vía Gateway 8085, token de acceso válido):

- `POST /api/direcciones`: `{"destinatario":"Ana","linea":"Av. Ejemplo 123","distrito":"MIRAFLORES","provincia":"LIMA","departamento":"LIMA"}`
- `GET /api/direcciones`: solo direcciones activas del cliente.
- `DELETE /api/direcciones/{id}`: desactivación lógica de dirección propia.
- `POST /api/ventas`: `{"idempotencia":"c1149b61-548a-44b6-b0c0-8219a9d39b1f","direccionId":1,"items":[{"varianteId":1,"cantidad":1}]}`
- `GET /api/ventas/mis-pedidos`: lista pedidos online del cliente.
- `GET /api/ventas/{id}/seguimiento`: eventos auditados; restringido al comprador o ADMIN.
- `POST /api/ventas/{id}/estado`: ADMIN, `{"estado":"EN_PREPARACION","motivo":"Pago confirmado"}`; flujo `RECIBIDO → EN_PREPARACION → ENVIADO → ENTREGADO`. Requiere pago confirmado.

**Tarifas sin valores inventados:** `DELIVERY_TARIFFS` define cobertura y montos por entorno, por ejemplo `LIMA|LIMA|MIRAFLORES=8.50`. Este ejemplo es **solo ilustrativo**, no una tarifa verificada ni recomendada. Si falta la zona, el pedido devuelve 422; el cliente no puede enviar un costo arbitrario. Los pedidos conservan instantánea de la dirección, subtotal y delivery al momento de comprar. El valor histórico del pedido no cambia si se edita una tarifa.

Las reservas vencidas (`STOCK_RESERVATION_TTL_SECONDS`, por defecto 900) se liberan en un proceso programado cada `STOCK_EXPIRATION_SCAN_MS` (60000 por defecto) que adquiere bloqueos `FOR UPDATE SKIP LOCKED`. No se repone inventario físico en el vencimiento porque el stock estaba solamente reservado. La liberación y el evento `CANCELADO` forman una sola transacción. Los pedidos pagados no vencen. Las compras pagadas siguen bloqueándose antes de modificar stock. Las cancelaciones manuales también registran su evento de seguimiento.

**Pruebas:** `PedidosDeliveryPostgresIT` agrega seis casos para propiedad de dirección, costo del delivery, histórico de estados, acceso a otros pedidos, vencimiento idempotente y dos workers concurrentes con PostgreSQL real. Se actualizaron fixtures de integración para proporcionar `direccionId`. Ejecutar: `mvn -pl comercio-service -am verify` (Java 21, Maven y Docker). **No se han ejecutado en este entorno**, así que el resultado no está verificado para producción.

**Limitaciones:** falta un catálogo comercial validado de zonas/tarifas reales, historial de envíos con transportistas, edición controlada de direcciones y pruebas E2E entre servicios. No migrar contra producción sin ensayo previo.
