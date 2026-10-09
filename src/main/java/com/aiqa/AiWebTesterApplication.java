package com.aiqa;

import com.aiqa.agent.AgentExecutionResult;
import com.aiqa.agent.CodexAgentRunner;
import com.aiqa.config.QaConfiguration;
import com.aiqa.execution.QaRunManager;
import com.aiqa.execution.QaRunManager.QaRun;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public final class AiWebTesterApplication {

  private static final ObjectMapper JSON = new ObjectMapper();

  private AiWebTesterApplication() {
  }

  static void main() {
    int exitCode = run();

    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  private static int run() {
    Path projectRoot = Path.of("")
        .toAbsolutePath()
        .normalize();

    Path logFile = null;

    try {
      QaConfiguration config =
          QaConfiguration.load(projectRoot);

      QaRunManager manager =
          new QaRunManager(projectRoot, config);

      QaRun qaRun = manager.startRun();

      System.out.println("AI QA TEST");
      System.out.println("Application: "
          + config.applicationUrl());

      System.out.println("Date: " + LocalDateTime.now()
          .format(DateTimeFormatter.ofPattern(
              "yyyy-MM-dd HH:mm:ss")));

      System.out.println();

      CodexAgentRunner runner =
          new CodexAgentRunner(config, projectRoot);

      AgentExecutionResult execution = runner.execute(
          createPrompt(config, qaRun),
          qaRun.directory()
      );

      logFile = execution.logFile();

      if (!execution.successful()) {
        System.err.println(
            "ERROR: AI agent execution failed or timed out.");
        System.err.println("Log: " + logFile);
        return 1;
      }

      Path report = qaRun.reportFile();
      Path jsonFile = qaRun.directory()
          .resolve("test-results.json");

      if (!Files.isRegularFile(report)
          || Files.size(report) == 0) {
        System.err.println("ERROR: Test report was not created.");
        System.err.println("Log: " + logFile);
        return 2;
      }

      if (!Files.isRegularFile(jsonFile)
          || Files.size(jsonFile) == 0) {
        System.err.println("ERROR: Test results JSON is missing.");
        System.err.println("Log: " + logFile);
        return 2;
      }

      JsonNode root = JSON.readTree(jsonFile.toFile());

      if (!qaRun.runId().equals(
          root.path("runId").asText())) {
        System.err.println(
            "ERROR: Results belong to another run.");
        System.err.println("Log: " + logFile);
        return 2;
      }

      JsonNode tests = root.path("tests");

      if (!tests.isArray()) {
        System.err.println(
            "ERROR: Invalid tests array in JSON.");
        return 2;
      }

      int passed = 0;
      int failed = 0;
      int blocked = 0;
      int inconclusive = 0;
      int positive = 0;
      int negative = 0;

      Set<String> ids = new HashSet<>();

      for (JsonNode test : tests) {
        String id = test.path("id").asText("");
        String title = test.path("title").asText("");
        String status = test.path("status").asText("");
        String classification =
            test.path("classification").asText("");

        if (!id.matches("TC-(0[1-9]|10)")
            || !ids.add(id)
            || title.isBlank()) {
          System.err.println(
              "ERROR: Invalid or duplicate test case.");
          return 2;
        }

        switch (classification) {
          case "POSITIVE" -> positive++;
          case "NEGATIVE" -> negative++;
          default -> {
            System.err.println(
                "ERROR: Invalid classification: " + id);
            return 2;
          }
        }

        switch (status) {
          case "PASS" -> passed++;
          case "FAIL" -> failed++;
          case "BLOCKED" -> blocked++;
          case "INCONCLUSIVE" -> inconclusive++;
          default -> {
            System.err.println(
                "ERROR: Unknown status for " + id);
            return 2;
          }
        }

        System.out.printf(
            "%-7s %-14s %s%n",
            id,
            status,
            title
        );
      }

      System.out.println();

      System.out.printf(
          "Total: %d | PASS: %d | FAIL: %d "
              + "| BLOCKED: %d | INCONCLUSIVE: %d%n",
          tests.size(),
          passed,
          failed,
          blocked,
          inconclusive
      );

      if (tests.size() != config.totalTests()
          || positive != config.positiveTests()
          || negative != config.negativeTests()) {
        System.err.println(
            "WARNING: Incomplete or incorrectly "
                + "classified test suite.");
      }

      if (failed > 0) {
        System.out.println(
            "Application failures detected: " + failed);
      }

      System.out.println("Report: " + report);

      return 0;

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("ERROR: Execution interrupted.");
      return 130;

    } catch (Exception e) {
      System.err.println(
          "ERROR: " + e.getMessage());

      if (logFile != null) {
        System.err.println("Log: " + logFile);
      }

      return 1;
    }
  }

  private static String createPrompt(
      QaConfiguration config,
      QaRun qaRun
  ) {
    return """
                You are an autonomous AI QA engineer.

                Target application:
                %s

                Use official Microsoft Playwright MCP
                browser tools to explore and execute tests.

                Read and follow:
                AGENTS.md
                prompts/exploration.md
                prompts/test-planning.md
                prompts/test-execution.md
                prompts/bug-reporting.md

                Independently discover functionality and
                create exactly %d POSITIVE and %d NEGATIVE
                test cases.

                Prefer meaningful functional user workflows
                over basic informational-page checks.

                In this ParaBank demonstration environment,
                creating fictional test customers and updating
                their synthetic contact details is permitted.

                Do not submit money transfers, bill payments,
                withdrawals, or loan requests without explicit
                approval.

                Execute all feasible test cases against the
                actual browser and independently verify outcomes.

                PASS means expected behavior was verified.
                FAIL means actual behavior contradicted expected.
                Negative tests can PASS.

                Never fabricate failures, bugs, or test results.

                Save a real screenshot per test when available.
                Embed existing screenshots into Markdown.

                Run ID:
                %s

                Markdown report:
                %s

                JSON results:
                %s

                Screenshots directory:
                %s

                Do not create another run directory.
                Do not modify Java code or restart this application.
                """.formatted(
        config.applicationUrl(),
        config.positiveTests(),
        config.negativeTests(),
        qaRun.runId(),
        qaRun.reportFile().toAbsolutePath(),
        qaRun.directory()
            .resolve("test-results.json")
            .toAbsolutePath(),
        qaRun.screenshotDirectory().toAbsolutePath()
    );
  }
}
