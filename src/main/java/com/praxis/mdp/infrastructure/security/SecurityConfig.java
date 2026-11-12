package com.praxis.mdp.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * JWT resource server for deployed profiles. The {@code local} profile (and the default settings)
 * use a permit-all chain that authenticates every caller as {@code local-operator}. That chain must
 * not be used outside a workstation.
 */
@Configuration
public class SecurityConfig {

  @Bean
  @ConditionalOnProperty(
      name = "mdp.security.mode",
      havingValue = "permit-all",
      matchIfMissing = true)
  SecurityFilterChain permitAllChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .anonymous(
            anon ->
                anon.principal("local-operator")
                    .authorities("ROLE_MDP_OPERATOR", "ROLE_MDP_VIEWER"));
    return http.build();
  }

  @Bean
  @ConditionalOnProperty(name = "mdp.security.mode", havingValue = "jwt")
  SecurityFilterChain jwtChain(HttpSecurity http) throws Exception {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new JwtRolesConverter());
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/info",
                        "/actuator/prometheus")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/runs", "/api/v1/runs/**")
                    .hasRole("MDP_OPERATOR")
                    .requestMatchers(HttpMethod.GET, "/api/v1/**")
                    .hasAnyRole("MDP_OPERATOR", "MDP_VIEWER")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth ->
                oauth
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                    .authenticationEntryPoint(
                        (request, response, ex) ->
                            writeProblem(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "unauthorized",
                                ex.getMessage())))
        .exceptionHandling(
            ex ->
                ex.accessDeniedHandler(
                    (request, response, denied) ->
                        writeProblem(
                            response, HttpStatus.FORBIDDEN, "forbidden", denied.getMessage())));
    return http.build();
  }

  private static void writeProblem(
      HttpServletResponse response, HttpStatus status, String type, String detail)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "https://mdp.praxis.com/problems/" + type);
    body.put("title", status.getReasonPhrase());
    body.put("status", status.value());
    body.put("detail", detail == null ? status.getReasonPhrase() : detail);
    new ObjectMapper().writeValue(response.getOutputStream(), body);
  }
}
