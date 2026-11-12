package com.praxis.mdp;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RunApiSecurityTest.JwtTestConfig.class)
class RunApiSecurityTest {

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("mdp.security.mode", () -> "jwt");
    registry.add("mdp.output-dir", () -> TestDirs.newTempDir("mdp-sec-out").toString());
    registry.add("mdp.work-dir", () -> TestDirs.newTempDir("mdp-sec-work").toString());
  }

  @Autowired MockMvc mvc;

  @Test
  void anonymousIsUnauthorized() throws Exception {
    mvc.perform(get("/api/v1/runs/1"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void viewerCanReadButCannotStart() throws Exception {
    mvc.perform(
            get("/api/v1/runs/999999")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MDP_VIEWER"))))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/v1/runs")
                .with(
                    jwt()
                        .jwt(token -> token.subject("viewer"))
                        .authorities(new SimpleGrantedAuthority("ROLE_MDP_VIEWER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jobName\":\"fxFuturesEodJob\",\"businessDate\":\"2026-11-11\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void operatorIsRecordedAsTriggeredBy() throws Exception {
    mvc.perform(
            post("/api/v1/runs")
                .with(
                    jwt()
                        .jwt(token -> token.subject("alice"))
                        .authorities(new SimpleGrantedAuthority("ROLE_MDP_OPERATOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jobName\":\"fxFuturesEodJob\",\"businessDate\":\"2026-11-11\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.triggeredBy").value("alice"))
        .andExpect(jsonPath("$.triggerSource").value("API"));
  }

  @TestConfiguration
  static class JwtTestConfig {
    @Bean
    JwtDecoder jwtDecoder() {
      return token -> Jwt.withTokenValue(token).header("alg", "none").subject("test").build();
    }
  }
}
