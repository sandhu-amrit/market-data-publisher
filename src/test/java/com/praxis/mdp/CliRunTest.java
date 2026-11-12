package com.praxis.mdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class CliRunTest {

  private static final Path OUTPUT = TestDirs.newTempDir("mdp-cli-out");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("mdp.output-dir", OUTPUT::toString);
    registry.add("mdp.work-dir", () -> TestDirs.newTempDir("mdp-cli-work").toString());
    registry.add("mdp.cli.job", () -> "fxFuturesEodJob");
    registry.add("mdp.cli.business-date", () -> "2026-11-11");
    registry.add("mdp.cli.triggered-by", () -> "cli-user");
  }

  @Test
  void runsTheConfiguredJobOnStartup() {
    assertThat(Files.exists(OUTPUT.resolve("FX_FUTURE_EOD_2026-11-11.csv"))).isTrue();
  }
}
