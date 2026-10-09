
package com.aiqa;

import com.aiqa.config.QaConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class QaConfigurationTest {

  @TempDir
  Path projectRoot;

  @Test
  void shouldLoadValidConfiguration() throws IOException {
    writeProperties("""
                qa.application.url=https://parabank.parasoft.com/parabank/index.htm
                qa.tests.positive=5
                qa.tests.negative=5
                qa.agent.executable=codex.cmd
                qa.agent.timeout.minutes=30
                qa.reports.directory=reports
                qa.reports.filename=TEST_REPORT.md
                qa.browser.headless=true
                """);

    QaConfiguration config =
        QaConfiguration.load(projectRoot);

    assertEquals(
        URI.create("https://parabank.parasoft.com/parabank/index.htm"),
        config.applicationUrl()
    );

    assertEquals(5, config.positiveTests());
    assertEquals(5, config.negativeTests());
    assertEquals(10, config.totalTests());
    assertEquals("codex.cmd", config.agentExecutable());
    assertEquals(Duration.ofMinutes(30), config.agentTimeout());
    assertEquals(Path.of("reports"), config.reportsDirectory());
    assertEquals("TEST_REPORT.md", config.reportFilename());
    assertTrue(config.headless());

    assertEquals(
        projectRoot.resolve("reports/TEST_REPORT.md").normalize(),
        config.reportPath(projectRoot)
    );
  }

  @Test
  void shouldRejectMissingConfigurationFile() {
    IOException exception = assertThrows(
        IOException.class,
        () -> QaConfiguration.load(projectRoot)
    );

    assertTrue(exception.getMessage()
        .contains("Configuration file not found"));
  }

  @Test
  void shouldRejectInvalidApplicationUrl() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new QaConfiguration(
            URI.create("file:///etc/passwd"),
            5,
            5,
            "codex.cmd",
            Duration.ofMinutes(30),
            Path.of("reports"),
            "TEST_REPORT.md",
            true
        )
    );

    assertTrue(exception.getMessage().contains("HTTP(S)"));
  }

  @Test
  void shouldRejectInvalidTestCounts() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new QaConfiguration(
            URI.create("https://example.com"),
            0,
            5,
            "codex.cmd",
            Duration.ofMinutes(30),
            Path.of("reports"),
            "TEST_REPORT.md",
            true
        )
    );

    assertThrows(
        IllegalArgumentException.class,
        () -> new QaConfiguration(
            URI.create("https://example.com"),
            5,
            -1,
            "codex.cmd",
            Duration.ofMinutes(30),
            Path.of("reports"),
            "TEST_REPORT.md",
            true
        )
    );
  }

  @Test
  void shouldRejectInvalidTimeout() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new QaConfiguration(
            URI.create("https://example.com"),
            5,
            5,
            "codex.cmd",
            Duration.ZERO,
            Path.of("reports"),
            "TEST_REPORT.md",
            true
        )
    );
  }

  @Test
  void shouldRejectInvalidReportFilename() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new QaConfiguration(
            URI.create("https://example.com"),
            5,
            5,
            "codex.cmd",
            Duration.ofMinutes(30),
            Path.of("reports"),
            "../TEST_REPORT.md",
            true
        )
    );
  }

  @Test
  void shouldRejectInvalidBooleanConfiguration() throws IOException {
    writeProperties("""
                qa.application.url=https://parabank.parasoft.com/parabank/index.htm
                qa.tests.positive=5
                qa.tests.negative=5
                qa.agent.executable=codex.cmd
                qa.agent.timeout.minutes=30
                qa.reports.directory=reports
                qa.reports.filename=TEST_REPORT.md
                qa.browser.headless=maybe
                """);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> QaConfiguration.load(projectRoot)
    );

    assertTrue(exception.getMessage().contains("true or false"));
  }

  private void writeProperties(String content) throws IOException {
    Path configDirectory = projectRoot.resolve("config");

    Files.createDirectories(configDirectory);

    Files.writeString(
        configDirectory.resolve("application.properties"),
        content
    );
  }
}
