package com.tiendafrancis.comercio.model;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
class ReportesServiceTest {
 private final ReportesService service=new ReportesService(mock(NamedParameterJdbcTemplate.class));
 @Test void limitesDeDiaRespetanZonaLima(){
  var p=service.periodo(LocalDate.of(2026,10,19),LocalDate.of(2026,10,19));
  assertEquals(Instant.parse("2026-10-19T05:00:00Z"),p.inicio());
  assertEquals(Instant.parse("2026-10-20T05:00:00Z"),p.fin());
 }
 @Test void evitaRangosInvalidos(){
  assertThrows(ResponseStatusException.class,()->service.periodo(LocalDate.of(2020,1,1),LocalDate.of(2026,10,19)));
  assertThrows(ResponseStatusException.class,()->service.periodo(LocalDate.of(2026,10,19),LocalDate.of(2026,10,18)));
 }
}
