package com.tiendafrancis.auth.config;

import com.tiendafrancis.auth.security.JwtAuthenticationFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Clock;

@Configuration
public class AuthConfig {

 @Bean
 public PasswordEncoder passwordEncoder() {

  return new BCryptPasswordEncoder(12);
 }

 @Bean
 public Clock clock() {

  return Clock.systemUTC();
 }

 @Bean
 public SecurityFilterChain securityFilterChain(

         HttpSecurity http,

         JwtAuthenticationFilter filtro

 ) throws Exception {

  return http

          .csrf(
                  AbstractHttpConfigurer::disable
          )

          .httpBasic(
                  AbstractHttpConfigurer::disable
          )

          .formLogin(
                  AbstractHttpConfigurer::disable
          )

          .sessionManagement(s ->
                  s.sessionCreationPolicy(
                          SessionCreationPolicy.STATELESS
                  )
          )

          .exceptionHandling(e -> e

                  .authenticationEntryPoint(
                          (req, res, ex) ->
                                  res.sendError(
                                          HttpStatus.UNAUTHORIZED.value()
                                  )
                  )

                  .accessDeniedHandler(
                          (req, res, ex) ->
                                  res.sendError(
                                          HttpStatus.FORBIDDEN.value()
                                  )
                  )
          )

          .authorizeHttpRequests(a -> a

                  .requestMatchers(
                          HttpMethod.POST,
                          "/api/auth/login",
                          "/api/auth/registro"
                  ).permitAll()

                  .requestMatchers(
                          HttpMethod.GET,
                          "/internal/auth/introspect"
                  ).permitAll()

                  .requestMatchers(
                          HttpMethod.GET,
                          "/actuator/health"
                  ).permitAll()

                  .requestMatchers(
                          HttpMethod.GET,
                          "/api/usuarios"
                  ).hasRole("ADMIN")

                  .requestMatchers(
                          HttpMethod.PATCH,
                          "/api/usuarios/*/estado"
                  ).hasRole("ADMIN")

                  .requestMatchers(
                          HttpMethod.GET,
                          "/api/usuarios/me"
                  ).authenticated()

                  .requestMatchers(
                          HttpMethod.PUT,
                          "/api/usuarios/me",
                          "/api/usuarios/me/password"
                  ).authenticated()

                  .anyRequest().denyAll()
          )

          .addFilterBefore(
                  filtro,
                  UsernamePasswordAuthenticationFilter.class
          )

          .build();
 }

 @Bean
 public FilterRegistrationBean<JwtAuthenticationFilter>
 noDuplicarFiltro(JwtAuthenticationFilter filtro) {

  FilterRegistrationBean<JwtAuthenticationFilter> registro =
          new FilterRegistrationBean<>(filtro);

  registro.setEnabled(false);

  return registro;
 }
}
