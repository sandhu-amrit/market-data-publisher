package com.praxis.mdp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RunApiTest {

  private static final Path OUTPUT = TestDirs.newTempDir("mdp-api-out");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("mdp.output-dir", OUTPUT::toString);
    registry.add("mdp.work-dir", () -> TestDirs.newTempDir("mdp-api-work").toString());
  }

  @Autowired MockMvc mvc;

  @Autowired ObjectMapper json;

  @Autowired JobRepository jobRepository;

  @BeforeEach
  void clean() {
    new JobRepositoryTestUtils(jobRepository).removeJobExecutions();
  }

  @Test
  void startRunThenPollUntilCompleted() throws Exception {
    String body = "{\"jobName\":\"fxFuturesEodJob\",\"businessDate\":\"2026-11-11\"}";
    String response =
        mvc.perform(post("/api/v1/runs").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isAccepted())
            .andExpect(
                header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/runs/")))
            .andExpect(jsonPath("$.executionId").isNumber())
            .andExpect(jsonPath("$.runSequence").value(1))
            .andExpect(jsonPath("$.triggeredBy").value("local-operator"))
            .andExpect(jsonPath("$.triggerSource").value("API"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = json.readTree(response).get("executionId").asLong();

    await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () -> {
              String bodyJson =
                  mvc.perform(get("/api/v1/runs/" + id))
                      .andExpect(status().isOk())
                      .andReturn()
                      .getResponse()
                      .getContentAsString();
              assertThat(json.readTree(bodyJson).get("status").asText()).isEqualTo("COMPLETED");
            });

    JsonNode run =
        json.readTree(
            mvc.perform(get("/api/v1/runs/" + id)).andReturn().getResponse().getContentAsString());
    assertThat(step(run, "extract").get("readCount").asInt()).isEqualTo(7);
    assertThat(step(run, "publish-file").get("writeCount").asInt()).isEqualTo(5);

    mvc.perform(
            get("/api/v1/runs")
                .param("job", "fxFuturesEodJob")
                .param("date", "2026-11-11")
                .param("status", "COMPLETED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].executionId").value(id));

    String second =
        mvc.perform(post("/api/v1/runs").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.runSequence").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();
    awaitTerminal(json.readTree(second).get("executionId").asLong());
  }

  @Test
  void rejectsUnknownJobInvalidBodyAndUnknownRun() throws Exception {
    mvc.perform(
            post("/api/v1/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jobName\":\"nope\",\"businessDate\":\"2026-11-11\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value("https://mdp.praxis.com/problems/unknown-job"));
    mvc.perform(
            post("/api/v1/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jobName\":\"fxFuturesEodJob\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/runs/999999")).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/runs").param("status", "NOT_A_STATUS"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void restartsAFailedRun() throws Exception {
    String response =
        mvc.perform(
                post("/api/v1/runs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"jobName\":\"fxFuturesEodJob\",\"businessDate\":\"1999-01-01\"}"))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = json.readTree(response).get("executionId").asLong();
    await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () ->
                assertThat(
                        json.readTree(
                                mvc.perform(get("/api/v1/runs/" + id))
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString())
                            .get("status")
                            .asText())
                    .isEqualTo("FAILED"));
    String restarted =
        mvc.perform(post("/api/v1/runs/" + id + "/restart"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.runSequence").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();
    awaitTerminal(json.readTree(restarted).get("executionId").asLong());
  }

  private void awaitTerminal(long executionId) {
    await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () -> {
              String statusText =
                  json.readTree(
                          mvc.perform(get("/api/v1/runs/" + executionId))
                              .andReturn()
                              .getResponse()
                              .getContentAsString())
                      .get("status")
                      .asText();
              assertThat(statusText).isIn("COMPLETED", "FAILED", "STOPPED");
            });
  }

  private static JsonNode step(JsonNode run, String name) {
    for (JsonNode step : run.get("steps")) {
      if (name.equals(step.get("name").asText())) {
        return step;
      }
    }
    throw new AssertionError("missing step " + name);
  }
}
