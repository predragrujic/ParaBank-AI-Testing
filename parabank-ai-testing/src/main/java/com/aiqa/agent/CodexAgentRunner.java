package com.aiqa.agent;

import com.aiqa.config.QaConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class CodexAgentRunner {

  private final QaConfiguration configuration;
  private final Path projectRoot;

  public CodexAgentRunner(
      QaConfiguration configuration,
      Path projectRoot
  ) {
    this.configuration = Objects.requireNonNull(configuration);
    this.projectRoot = Objects.requireNonNull(projectRoot)
        .toAbsolutePath()
        .normalize();
  }

  public AgentExecutionResult execute(
      String prompt,
      Path runDirectory
  ) throws IOException, InterruptedException {

    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException(
          "Codex prompt cannot be empty");
    }

    Path directory = Objects.requireNonNull(runDirectory)
        .toAbsolutePath()
        .normalize();

    Files.createDirectories(directory);

    Path logFile = directory.resolve("codex.log");

    ProcessBuilder builder =
        new ProcessBuilder(buildCommand());

    builder.directory(projectRoot.toFile());
    builder.redirectErrorStream(true);
    builder.redirectOutput(logFile.toFile());

    Instant started = Instant.now();
    Process process = builder.start();

    try {
      // codex exec - reads the prompt from stdin.
      try (OutputStream stdin = process.getOutputStream()) {
        stdin.write(prompt.getBytes(StandardCharsets.UTF_8));
        stdin.flush();
      }

      boolean finished = process.waitFor(
          configuration.agentTimeout().toMillis(),
          TimeUnit.MILLISECONDS
      );

      if (!finished) {
        stopProcessTree(process);
      }

      return new AgentExecutionResult(
          finished ? process.exitValue() : -1,
          finished,
          Duration.between(started, Instant.now()),
          logFile
      );

    } catch (IOException | InterruptedException e) {
      stopProcessTree(process);

      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

      throw e;
    }
  }

  private List<String> buildCommand() {
    String executable = configuration.agentExecutable();

    List<String> command = new ArrayList<>();

    if (isWindows()
        && executable.toLowerCase(Locale.ROOT)
        .endsWith(".cmd")) {
      command.add("cmd.exe");
      command.add("/d");
      command.add("/c");
    }

    command.add(executable);
    command.add("exec");
    command.add("--skip-git-repo-check");

    // Already verified with your Codex CLI installation.
    command.add("-c");
    command.add(
        "mcp_servers.playwright.default_tools_approval_mode=\"approve\""
    );

    command.add("-");

    return List.copyOf(command);
  }

  private static boolean isWindows() {
    return System.getProperty("os.name")
        .toLowerCase(Locale.ROOT)
        .contains("win");
  }

  private static void stopProcessTree(Process process) {
    List<ProcessHandle> children =
        process.descendants().toList();

    for (int i = children.size() - 1; i >= 0; i--) {
      ProcessHandle child = children.get(i);

      if (child.isAlive()) {
        child.destroyForcibly();
      }
    }

    if (process.isAlive()) {
      process.destroyForcibly();
    }

    try {
      process.waitFor(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
