package com.praxis.mdp.run.internal.cli;

import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.run.RunService;
import com.praxis.mdp.run.RunStatus;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * {@code --mdp.cli.job=fxFuturesEodJob --mdp.cli.business-date=2026-11-11}. The server stays up
 * afterwards.
 */
@Component
@Order
@ConditionalOnProperty(prefix = "mdp.cli", name = "job")
public class CliRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(CliRunner.class);

  private final MdpProperties props;
  private final RunService runs;

  public CliRunner(MdpProperties props, RunService runs) {
    this.props = props;
    this.runs = runs;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    String job = props.getCli().getJob();
    String date = props.getCli().getBusinessDate();
    if (date == null || date.isBlank()) {
      throw new IllegalStateException("mdp.cli.business-date is required when mdp.cli.job is set");
    }
    String user = props.getCli().getTriggeredBy();
    if (user == null || user.isBlank()) {
      user = System.getProperty("user.name", "cli");
    }
    RunStatus status = runs.launchAndWait(job, LocalDate.parse(date), user, "CLI");
    log.info("CLI run {} finished with status {}", status.executionId(), status.status());
    if ("FAILED".equals(status.status()) || "STOPPED".equals(status.status())) {
      throw new IllegalStateException(
          "CLI run " + status.executionId() + " finished " + status.status());
    }
  }
}
