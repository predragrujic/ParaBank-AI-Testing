package com.aiqa.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

public record QaConfiguration(
    URI applicationUrl,
    int positiveTests,
    int negativeTests,
    String agentExecutable,
    Duration agentTimeout,
    Path reportsDirectory,
    String reportFilename,
    boolean headless
) {

  public QaConfiguration {
    Objects.requireNonNull(applicationUrl, "applicationUrl");
    Objects.requireNonNull(agentExecutable, "agentExecutable");
    Objects.requireNonNull(agentTimeout, "agentTimeout");
    Objects.requireNonNull(reportsDirectory, "reportsDirectory");
    Objects.requireNonNull(reportFilename, "reportFilename");

    String scheme = applicationUrl.getScheme();

    if (scheme == null
        || (!scheme.equalsIgnoreCase("http")
        && !scheme.equalsIgnoreCase("https"))
        || applicationUrl.getHost() == null) {
      throw new IllegalArgumentException(
          "Application URL must be a valid HTTP(S) URL");
    }

    if (positiveTests <= 0 || negativeTests <= 0) {
      throw new IllegalArgumentException(
          "Test counts must be positive");
    }

    if (agentExecutable.isBlank()) {
      throw new IllegalArgumentException(
          "Agent executable cannot be empty");
    }

    if (agentTimeout.isZero() || agentTimeout.isNegative()) {
      throw new IllegalArgumentException(
          "Agent timeout must be greater than zero");
    }

    if (reportFilename.isBlank()
        || !reportFilename.endsWith(".md")
        || !Path.of(reportFilename)
        .getFileName()
        .toString()
        .equals(reportFilename)) {
      throw new IllegalArgumentException(
          "Report filename must be a simple .md filename");
    }

    reportsDirectory = reportsDirectory.normalize();
  }

  public static QaConfiguration load(Path projectRoot)
      throws IOException {

    Objects.requireNonNull(projectRoot, "projectRoot");

    Path file = projectRoot.resolve("config")
        .resolve("application.properties");

    if (!Files.isRegularFile(file)) {
      throw new IOException(
          "Configuration file not found: "
              + file.toAbsolutePath());
    }

    Properties properties = new Properties();

    try (InputStream input = Files.newInputStream(file)) {
      properties.load(input);
    }

    return new QaConfiguration(
        URI.create(required(properties, "qa.application.url")),
        positiveInt(properties, "qa.tests.positive"),
        positiveInt(properties, "qa.tests.negative"),
        required(properties, "qa.agent.executable"),
        Duration.ofMinutes(
            positiveInt(properties,
                "qa.agent.timeout.minutes")),
        Path.of(required(properties,
            "qa.reports.directory")),
        required(properties, "qa.reports.filename"),
        parseBoolean(required(
            properties, "qa.browser.headless"))
    );
  }

  public int totalTests() {
    return positiveTests + negativeTests;
  }

  public Path reportPath(Path projectRoot) {
    return projectRoot.resolve(reportsDirectory)
        .resolve(reportFilename)
        .normalize();
  }

  private static String required(
      Properties properties,
      String key
  ) {
    String value = properties.getProperty(key);

    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
          "Missing required configuration: " + key);
    }

    return value.trim();
  }

  private static int positiveInt(
      Properties properties,
      String key
  ) {
    String value = required(properties, key);

    try {
      int result = Integer.parseInt(value);

      if (result <= 0) {
        throw new IllegalArgumentException(
            key + " must be greater than zero");
      }

      return result;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          key + " must be a valid integer", e);
    }
  }

  private static boolean parseBoolean(String value) {
    return switch (value.toLowerCase(Locale.ROOT)) {
      case "true" -> true;
      case "false" -> false;
      default -> throw new IllegalArgumentException(
          "Boolean configuration must be true or false");
    };
  }
}
