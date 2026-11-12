package com.praxis.mdp.publishing.internal.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.praxis.mdp.domain.EodPrice;
import com.praxis.mdp.domain.Instrument;
import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.domain.RunId;
import com.praxis.mdp.infrastructure.config.Connections.RestConnection;
import com.praxis.mdp.publishing.SinkWriter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.web.client.HttpClientErrorException;

class RestMarketDataSinkTest {

  private WireMockServer server;

  @AfterEach
  void stop() {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  void retriesServerErrorsThenSucceeds() throws Exception {
    server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    server.start();
    server.stubFor(
        post(urlEqualTo("/api/eod"))
            .inScenario("retry")
            .whenScenarioStateIs(STARTED)
            .willReturn(serverError())
            .willSetStateTo("once"));
    server.stubFor(
        post(urlEqualTo("/api/eod"))
            .inScenario("retry")
            .whenScenarioStateIs("once")
            .willReturn(aResponse().withStatus(200)));

    SinkWriter writer =
        sink(3, 20, 10)
            .open(
                "fxFuturesEodJob",
                InstrumentType.FX_FUTURE,
                RunId.of(LocalDate.parse("2026-11-11"), 1));
    writer.open(new ExecutionContext());
    writer.write(new Chunk<>(List.of(sample())));
    writer.finish(true);

    server.verify(2, postRequestedFor(urlEqualTo("/api/eod")));
  }

  @Test
  void doesNotRetryClientErrors() throws Exception {
    server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    server.start();
    server.stubFor(post(urlEqualTo("/api/eod")).willReturn(aResponse().withStatus(400)));
    SinkWriter writer =
        sink(3, 20, 10)
            .open(
                "fxFuturesEodJob",
                InstrumentType.FX_FUTURE,
                RunId.of(LocalDate.parse("2026-11-11"), 1));
    writer.open(new ExecutionContext());
    assertThatThrownBy(() -> writer.write(new Chunk<>(List.of(sample()))))
        .isInstanceOf(HttpClientErrorException.class);
    server.verify(1, postRequestedFor(urlEqualTo("/api/eod")));
  }

  private RestMarketDataSink sink(int attempts, int window, int minimum) {
    RestConnection rest =
        new RestConnection(
            server.baseUrl(),
            "none",
            "",
            "",
            "",
            Duration.ofSeconds(2),
            Duration.ofSeconds(10),
            attempts,
            Duration.ofMillis(10),
            50f,
            window,
            minimum,
            true);
    return new RestMarketDataSink(rest, "/api/eod");
  }

  private static MarketDataRecord sample() {
    LocalDate date = LocalDate.parse("2026-11-11");
    return new MarketDataRecord(
        new Instrument("FXF-1", InstrumentType.FX_FUTURE, "6EZ6", "USD", date),
        new EodPrice(date, new BigDecimal("1.1")));
  }
}
