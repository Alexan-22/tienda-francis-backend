package com.tiendafrancis.comercio.config;

import com.tiendafrancis.comercio.security.InternalIntrospectionFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class ComercioSecurity {

 @Bean
 SecurityFilterChain chain(
         HttpSecurity http,
         InternalIntrospectionFilter filter
 ) throws Exception {
  return http
          .csrf(AbstractHttpConfigurer::disable)
          .httpBasic(AbstractHttpConfigurer::disable)
          .formLogin(AbstractHttpConfigurer::disable)

          .sessionManagement(s ->
                  s.sessionCreationPolicy(
                          SessionCreationPolicy.STATELESS
                  )
          )

          .authorizeHttpRequests(a -> a
                  .requestMatchers(
                          "/actuator/health"
                  ).permitAll()

                  .requestMatchers(
                          HttpMethod.GET,
                          "/api/productos",
                          "/api/productos/**"
                  ).permitAll()

                  .requestMatchers(
                          HttpMethod.POST,
                          "/api/ventas"
                  ).hasRole("CLIENTE")

                  .requestMatchers(
                          "/api/direcciones",
                          "/api/direcciones/**"
                  ).hasRole("CLIENTE")

                  .requestMatchers(
                          HttpMethod.GET,
                          "/api/ventas/mis-pedidos",
                          "/api/ventas/*/seguimiento"
                  ).hasAnyRole("CLIENTE", "ADMIN")

                  .anyRequest().hasRole("ADMIN")
          )

          .addFilterBefore(
                  filter,
                  UsernamePasswordAuthenticationFilter.class
          )

          .build();
 }

 @Bean
 FilterRegistrationBean<InternalIntrospectionFilter>
 disableAutoRegistration(
         InternalIntrospectionFilter filter
 ) {
  var registro =
          new FilterRegistrationBean<>(filter);

  registro.setEnabled(false);

  return registro;
 }
}