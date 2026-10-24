package com.tiendafrancis.contenido.config;

import com.tiendafrancis.contenido.security.InternalIntrospectionFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class ContenidoSecurity {

 @Bean
 SecurityFilterChain chain(
         HttpSecurity http,
         InternalIntrospectionFilter filter
 ) throws Exception {
  return http
          .csrf(AbstractHttpConfigurer::disable)
          .httpBasic(AbstractHttpConfigurer::disable)
          .formLogin(AbstractHttpConfigurer::disable)
          .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(a -> a
                  .requestMatchers("/actuator/health").permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/home/banners").permitAll()
                  .requestMatchers("/api/contenido/banners",
                          "/api/contenido/banners/**").hasRole("ADMIN")
                  .anyRequest().denyAll())
          .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
          .build();
 }

 @Bean
 FilterRegistrationBean<InternalIntrospectionFilter> disableAutoRegistration(
         InternalIntrospectionFilter filter
 ) {
  var registro = new FilterRegistrationBean<>(filter);
  registro.setEnabled(false);
  return registro;
 }
}