package com.praxis.mdp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.source.MarketDataSource;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RunConflictTest {

  @Autowired MockMvc mvc;

  @MockitoBean MarketDataSource source;

  @Test
  void duplicateRunIs409AndStopIsAccepted() throws Exception {
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(source.reader(any(), any()))
        .thenReturn(
            new ItemStreamReader<MarketDataRecord>() {
              @Override
              public MarketDataRecord read() throws InterruptedException {
                entered.countDown();
                if (!release.await(20, TimeUnit.SECONDS)) {
                  throw new IllegalStateException("timed out waiting to release the blocked read");
                }
                return null;
              }

              @Override
              public void open(ExecutionContext executionContext) {}

              @Override
              public void update(ExecutionContext executionContext) {}

              @Override
              public void close() {}
            });
    String body = "{\"jobName\":\"fxFuturesEodJob\",\"businessDate\":\"2024-02-02\"}";
    try {
      String response =
          mvc.perform(post("/api/v1/runs").contentType(MediaType.APPLICATION_JSON).content(body))
              .andExpect(status().isAccepted())
              .andReturn()
              .getResponse()
              .getContentAsString();
      long id =
          new com.fasterxml.jackson.databind.ObjectMapper()
              .readTree(response)
              .get("executionId")
              .asLong();
      assertThatEntered(entered);
      mvc.perform(post("/api/v1/runs").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isConflict())
          .andExpect(
              jsonPath("$.type").value("https://mdp.praxis.com/problems/run-already-active"));
      mvc.perform(post("/api/v1/runs/" + id + "/stop")).andExpect(status().isAccepted());
    } finally {
      release.countDown();
    }
  }

  private static void assertThatEntered(CountDownLatch entered) throws InterruptedException {
    if (!entered.await(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS)) {
      throw new AssertionError("job did not start reading");
    }
  }
}
