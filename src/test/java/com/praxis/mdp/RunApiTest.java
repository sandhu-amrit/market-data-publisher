package com.praxis.mdp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
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

    private static final Path OUTPUT_DIR = TestDirs.newTempDir("mdp-api-out");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("mdp.input-dir", () -> "src/test/resources/input");
        r.add("mdp.output-dir", OUTPUT_DIR::toString);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void startRunThenPollUntilCompleted() throws Exception {
        String body = """
                {"jobName":"fxFuturesEodJob","businessDate":"2026-11-11","triggeredBy":"alice"}""";
        String response = mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.executionId").isNumber())
                .andExpect(jsonPath("$.runSequence").value(1))
                .andExpect(jsonPath("$.triggeredBy").value("alice"))
                .andExpect(jsonPath("$.triggerSource").value("API"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(response).get("executionId").asLong();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            String s = mvc.perform(get("/api/runs/" + id)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(json.readTree(s).get("status").asText()).isEqualTo("COMPLETED");
        });

        JsonNode run = json.readTree(mvc.perform(get("/api/runs/" + id)).andReturn().getResponse().getContentAsString());
        JsonNode step = run.get("steps").get(0);
        assertThat(step.get("name").asText()).isEqualTo("publishFile");
        assertThat(step.get("readCount").asInt()).isEqualTo(7);
        assertThat(step.get("writeCount").asInt()).isEqualTo(5);
        assertThat(step.get("readSkipCount").asInt()).isEqualTo(1);
        assertThat(step.get("processSkipCount").asInt()).isEqualTo(2);
        assertThat(Files.exists(OUTPUT_DIR.resolve("FX_FUTURE_EOD_2026-11-11.csv"))).isTrue();

        // A second run of the same date gets the next run sequence.
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.runSequence").value(2));
    }

    @Test
    void unknownJobIsRejected() throws Exception {
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobName\":\"nope\",\"businessDate\":\"2026-11-11\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestIsRejected() throws Exception {
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content("{\"jobName\":\"fxFuturesEodJob\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownRunIs404() throws Exception {
        mvc.perform(get("/api/runs/999999")).andExpect(status().isNotFound());
    }
}
