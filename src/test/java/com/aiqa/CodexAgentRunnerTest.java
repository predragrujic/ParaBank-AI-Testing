package com.aiqa;

import com.aiqa.agent.AgentExecutionResult;
import com.aiqa.agent.CodexAgentRunner;
import com.aiqa.config.QaConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class CodexAgentRunnerTest {

  @TempDir
  Path projectRoot;

  @Test
  void shouldExecuteAgentSuccessfully() throws Exception {
    Path executable = createFakeAgent("""
                echo Fake Codex agent started
                echo Browser exploration simulated
                """, 0);

    CodexAgentRunner runner = createRunner(
        executable,
        Duration.ofSeconds(10)
    );

    Path runDirectory = projectRoot.resolve("reports/run-001");

    AgentExecutionResult result = runner.execute(
        "Execute autonomous QA tests",
        runDirectory
    );

    assertTrue(result.successful());
    assertEquals(0, result.exitCode());
    assertTrue(result.completedWithinTimeout());
    assertTrue(Files.isRegularFile(result.logFile()));

    String log = Files.readString(result.logFile());

    assertTrue(log.contains("Fake Codex agent started"));
    assertTrue(log.contains("Browser exploration simulated"));
  }

  @Test
  void shouldHandleAgentFailure() throws Exception {
    Path executable = createFakeAgent(
        "echo Simulated agent failure",
        7
    );

    CodexAgentRunner runner = createRunner(
        executable,
        Duration.ofSeconds(10)
    );

    AgentExecutionResult result = runner.execute(
        "Run QA",
        projectRoot.resolve("reports/run-002")
    );

    assertFalse(result.successful());
    assertEquals(7, result.exitCode());
    assertTrue(result.completedWithinTimeout());

    assertTrue(
        Files.readString(result.logFile())
            .contains("Simulated agent failure")
    );
  }

  @Test
  void shouldHandleAgentTimeout() throws Exception {
    Path executable = createFakeAgent(
        windows()
            ? "ping -n 8 127.0.0.1 > nul"
            : "sleep 7",
        0
    );

    CodexAgentRunner runner = createRunner(
        executable,
        Duration.ofMillis(500)
    );

    AgentExecutionResult result = runner.execute(
        "Run a slow QA task",
        projectRoot.resolve("reports/run-003")
    );

    assertFalse(result.successful());
    assertFalse(result.completedWithinTimeout());
    assertEquals(-1, result.exitCode());
  }

  @Test
  void shouldRejectBlankPrompt() throws Exception {
    Path executable = createFakeAgent(
        "echo Test",
        0
    );

    CodexAgentRunner runner = createRunner(
        executable,
        Duration.ofSeconds(10)
    );

    assertThrows(
        IllegalArgumentException.class,
        () -> runner.execute(
            " ",
            projectRoot.resolve("reports/run-004")
        )
    );
  }

  @Test
  void shouldReportMissingExecutable() {
    Path missingExecutable = projectRoot.resolve(
        windows() ? "missing-codex.cmd" : "missing-codex"
    );

    CodexAgentRunner runner = createRunner(
        missingExecutable,
        Duration.ofSeconds(10)
    );

    assertThrows(
        IOException.class,
        () -> runner.execute(
            "Run QA",
            projectRoot.resolve("reports/run-005")
        )
    );
  }

  private CodexAgentRunner createRunner(
      Path executable,
      Duration timeout
  ) {
    QaConfiguration configuration = new QaConfiguration(
        URI.create(
            "https://parabank.parasoft.com/parabank/index.htm"
        ),
        5,
        5,
        executable.toAbsolutePath().toString(),
        timeout,
        Path.of("reports"),
        "TEST_REPORT.md",
        true
    );

    return new CodexAgentRunner(
        configuration,
        projectRoot
    );
  }

  private Path createFakeAgent(
      String commands,
      int exitCode
  ) throws IOException {

    boolean isWindows = windows();

    Path executable = projectRoot.resolve(
        isWindows ? "fake-codex.cmd" : "fake-codex.sh"
    );

    String script;

    if (isWindows) {
      script = """
                    @echo off
                    %s
                    exit /b %d
                    """.formatted(commands, exitCode);
    } else {
      script = """
                    #!/bin/sh
                    %s
                    exit %d
                    """.formatted(commands, exitCode);
    }

    Files.writeString(executable, script);

    if (!isWindows && !executable.toFile().setExecutable(true)) {
      throw new IOException(
          "Cannot make fake agent executable: " + executable
      );
    }

    return executable;
  }

  private static boolean windows() {
    return System.getProperty("os.name")
        .toLowerCase(Locale.ROOT)
        .contains("win");
  }
}
