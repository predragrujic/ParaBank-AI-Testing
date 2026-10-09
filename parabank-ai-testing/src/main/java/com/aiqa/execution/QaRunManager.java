package com.aiqa.execution;

import com.aiqa.config.QaConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public final class QaRunManager {

  private static final DateTimeFormatter RUN_TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
          .withZone(ZoneOffset.UTC);

  private final Path projectRoot;
  private final QaConfiguration configuration;

  public QaRunManager(
      Path projectRoot,
      QaConfiguration configuration
  ) {
    this.projectRoot = Objects.requireNonNull(projectRoot)
        .toAbsolutePath()
        .normalize();

    this.configuration = Objects.requireNonNull(configuration);
  }

  public QaRun startRun() throws IOException {
    Instant startedAt = Instant.now();

    String runId = RUN_TIMESTAMP.format(startedAt)
        + "-"
        + UUID.randomUUID().toString().substring(0, 8);

    Path runDirectory = projectRoot
        .resolve(configuration.reportsDirectory())
        .resolve("runs")
        .resolve(runId)
        .normalize();

    Path screenshots = runDirectory.resolve("screenshots");

    Files.createDirectories(screenshots);

    Path report = runDirectory.resolve(
        configuration.reportFilename()
    );

    return new QaRun(
        runId,
        startedAt,
        runDirectory,
        screenshots,
        report
    );
  }

  public record QaRun(
      String runId,
      Instant startedAt,
      Path directory,
      Path screenshotDirectory,
      Path reportFile
  ) {
    public QaRun {
      Objects.requireNonNull(runId);
      Objects.requireNonNull(startedAt);
      Objects.requireNonNull(directory);
      Objects.requireNonNull(screenshotDirectory);
      Objects.requireNonNull(reportFile);
    }
  }
}

