package com.praxis.mdp.publishing;

import com.praxis.mdp.publishing.internal.file.FileMarketDataSink;
import java.nio.file.Path;

/**
 * Local file publication. The directory is the process output dir, resolved when the step opens.
 */
public final class FileDestination {

  private FileDestination() {}

  public static Builder local() {
    return new Builder();
  }

  public static final class Builder implements Publication {
    private String fileName = "{type}_EOD_{businessDate}.csv";
    private Format format = Format.CSV;

    public Builder fileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    public Builder format(Format format) {
      this.format = format;
      return this;
    }

    @Override
    public String stepName() {
      return "file";
    }

    @Override
    public Format format() {
      return format;
    }

    @Override
    public boolean active() {
      return true;
    }

    @Override
    public SinkWriter open(PublishRequest request, PublishingClients clients) {
      String name = OutputNames.apply(fileName, request.jobName(), request.type(), request.runId());
      Path target = clients.outputDir().resolve(name);
      return new FileMarketDataSink(clients.outputDir()).writerFor(target);
    }
  }
}
