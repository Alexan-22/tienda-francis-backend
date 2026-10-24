package com.tiendafrancis.contenido.controller;

import com.tiendafrancis.contenido.bean.dto.BannerPublicoDto;
import com.tiendafrancis.contenido.usecase.BannerUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeController {

 private final BannerUseCase banners;

 @GetMapping("/banners")
 public List<BannerPublicoDto> banners() {
  return banners.listarPublicos();
 }
}