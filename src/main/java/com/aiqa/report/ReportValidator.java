
package com.aiqa.report;

import com.aiqa.config.QaConfiguration;
import com.aiqa.execution.QaRunManager.QaRun;
import com.aiqa.model.BugReport;
import com.aiqa.model.TestCase;
import com.aiqa.model.TestResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class ReportValidator {

  private static final ObjectMapper JSON = new ObjectMapper();

  public ValidationResult validate(
      QaRun run,
      QaConfiguration configuration
  ) throws IOException {

    Objects.requireNonNull(run);
    Objects.requireNonNull(configuration);

    List<String> issues = new ArrayList<>();
    List<TestCase> testCases = new ArrayList<>();
    List<TestResult> testResults = new ArrayList<>();
    List<BugReport> bugs = new ArrayList<>();

    Path runDirectory = run.directory()
        .toAbsolutePath()
        .normalize();

    Path jsonFile = runDirectory.resolve("test-results.json");
    Path markdownFile = run.reportFile()
        .toAbsolutePath()
        .normalize();

    if (!Files.isRegularFile(jsonFile)) {
      issues.add("Missing test-results.json");
    }

    if (!Files.isRegularFile(markdownFile)) {
      issues.add("Missing TEST_REPORT.md");
    }

    if (!issues.isEmpty()) {
      return result(testCases, testResults, issues);
    }

    if (Files.getLastModifiedTime(jsonFile)
        .toInstant().isBefore(run.startedAt())) {
      issues.add("JSON report predates the current run");
    }

    if (Files.getLastModifiedTime(markdownFile)
        .toInstant().isBefore(run.startedAt())) {
      issues.add("Markdown report predates the current run");
    }

    JsonNode root;

    try {
      root = JSON.readTree(jsonFile.toFile());
    } catch (IOException e) {
      issues.add("Invalid JSON report: " + e.getMessage());
      return result(testCases, testResults, issues);
    }

    if (root == null || !root.isObject()) {
      issues.add("JSON report must contain an object");
      return result(testCases, testResults, issues);
    }

    if (!run.runId().equals(text(root, "runId"))) {
      issues.add("JSON runId does not match current run");
    }

    if (!configuration.applicationUrl().toString()
        .equals(text(root, "applicationUrl"))) {
      issues.add("Application URL does not match configuration");
    }

    JsonNode testsNode = root.path("tests");

    if (!testsNode.isArray()) {
      issues.add("JSON tests must be an array");
    } else {
      for (JsonNode node : testsNode) {
        try {
          testCases.add(parseTestCase(node));
          testResults.add(parseTestResult(node));
        } catch (RuntimeException e) {
          issues.add("Invalid test case: " + e.getMessage());
        }
      }
    }

    JsonNode bugsNode = root.path("bugs");

    if (!bugsNode.isArray()) {
      issues.add("JSON bugs must be an array");
    } else {
      for (JsonNode node : bugsNode) {
        try {
          bugs.add(parseBug(node));
        } catch (RuntimeException e) {
          issues.add("Invalid bug: " + e.getMessage());
        }
      }
    }

    validateTests(testCases, testResults, configuration, issues);
    validateBugs(bugs, testCases, runDirectory, issues);
    validateMarkdown(markdownFile, testCases, testResults, bugs,
        issues);

    return result(testCases, testResults, issues);
  }

  private TestCase parseTestCase(JsonNode node) {
    return new TestCase(
        required(node, "id"),
        enumValue(
            TestCase.Classification.class,
            required(node, "classification")
        ),
        required(node, "title"),
        required(node, "objective"),
        text(node, "preconditions"),
        text(node, "testData"),
        strings(node.path("steps")),
        required(node, "expected"),
        required(node, "verificationStrategy"),
        enumValue(
            TestCase.ExecutionSafety.class,
            required(node, "executionSafety")
        )
    );
  }

  private TestResult parseTestResult(JsonNode node) {
    return new TestResult(
        required(node, "id"),
        enumValue(
            TestResult.Status.class,
            required(node, "status")
        ),
        required(node, "actual"),
        strings(node.path("executedSteps")),
        strings(node.path("evidence")),
        strings(node.path("bugIds")),
        optionalInstant(node, "startedAt"),
        optionalInstant(node, "completedAt"),
        required(node, "rationale")
    );
  }

  private BugReport parseBug(JsonNode node) {
    return new BugReport(
        required(node, "id"),
        required(node, "title"),
        enumValue(
            BugReport.Severity.class,
            required(node, "severity")
        ),
        strings(node.path("affectedTestCaseIds")),
        text(node, "applicationUrl"),
        text(node, "preconditions"),
        strings(node.path("stepsToReproduce")),
        required(node, "expected"),
        required(node, "actual"),
        text(node, "impact"),
        enumValue(
            BugReport.Reproducibility.class,
            required(node, "reproducibility")
        ),
        required(node, "screenshot")
    );
  }

  private void validateTests(
      List<TestCase> cases,
      List<TestResult> results,
      QaConfiguration configuration,
      List<String> issues
  ) {
    if (cases.size() != configuration.totalTests()) {
      issues.add("Expected " + configuration.totalTests()
          + " test cases, found " + cases.size());
    }

    long positive = cases.stream()
        .filter(test -> test.classification()
            == TestCase.Classification.POSITIVE)
        .count();

    long negative = cases.stream()
        .filter(test -> test.classification()
            == TestCase.Classification.NEGATIVE)
        .count();

    if (positive != configuration.positiveTests()) {
      issues.add("Incorrect positive test count: " + positive);
    }

    if (negative != configuration.negativeTests()) {
      issues.add("Incorrect negative test count: " + negative);
    }

    Set<String> ids = new HashSet<>();

    for (TestCase test : cases) {
      if (!ids.add(test.id())) {
        issues.add("Duplicate test ID: " + test.id());
      }
    }

    for (TestResult result : results) {
      if (!ids.contains(result.testCaseId())) {
        issues.add("Unknown test result ID: "
            + result.testCaseId());
      }

      if (result.status() == TestResult.Status.PASS
          && result.evidence().isEmpty()) {
        issues.add("PASS without evidence: "
            + result.testCaseId());
      }
    }
  }

  private void validateBugs(
      List<BugReport> bugs,
      List<TestCase> testCases,
      Path runDirectory,
      List<String> issues
  ) throws IOException {

    Set<String> testIds = new HashSet<>();

    for (TestCase test : testCases) {
      testIds.add(test.id());
    }

    Set<String> bugIds = new HashSet<>();

    for (BugReport bug : bugs) {
      if (!bugIds.add(bug.id())) {
        issues.add("Duplicate bug ID: " + bug.id());
      }

      for (String testId : bug.affectedTestCaseIds()) {
        if (!testIds.contains(testId)) {
          issues.add("Bug " + bug.id()
              + " references unknown test: " + testId);
        }
      }

      Path screenshot = runDirectory
          .resolve(bug.screenshot())
          .normalize();

      if (!screenshot.startsWith(runDirectory)) {
        issues.add("Screenshot escapes run directory: "
            + bug.screenshot());
        continue;
      }

      if (!Files.isRegularFile(screenshot)) {
        issues.add("Missing bug screenshot: "
            + bug.screenshot());
        continue;
      }

      if (Files.size(screenshot) == 0
          || ImageIO.read(screenshot.toFile()) == null) {
        issues.add("Invalid screenshot: "
            + bug.screenshot());
      }
    }
  }

  private void validateMarkdown(
      Path file,
      List<TestCase> cases,
      List<TestResult> results,
      List<BugReport> bugs,
      List<String> issues
  ) throws IOException {

    String markdown = Files.readString(file);

    for (String section : List.of(
        "## Executive Summary",
        "## Discovered Functionality",
        "## Test Cases",
        "## Confirmed Bugs",
        "## Coverage and Limitations"
    )) {
      if (!markdown.contains(section)) {
        issues.add("Missing Markdown section: " + section);
      }
    }

    for (TestCase test : cases) {
      if (!markdown.contains("### " + test.id())) {
        issues.add("Test missing in Markdown: " + test.id());
      }
    }

    for (TestResult result : results) {
      if (!markdown.contains(result.status().name())) {
        issues.add("Status missing from Markdown: "
            + result.testCaseId());
      }
    }

    for (BugReport bug : bugs) {
      if (!markdown.contains("### " + bug.id())) {
        issues.add("Bug missing in Markdown: " + bug.id());
      }

      String imageReference =
          "![" + bug.id() + "]("
              + bug.screenshot() + ")";

      if (!markdown.contains(imageReference)) {
        issues.add("Bug screenshot not embedded: "
            + bug.id());
      }
    }
  }

  private static String text(JsonNode node, String field) {
    return node.path(field).asText("").trim();
  }

  private static String required(JsonNode node, String field) {
    String value = text(node, field);

    if (value.isBlank()) {
      throw new IllegalArgumentException(
          "Missing field: " + field);
    }

    return value;
  }

  private static List<String> strings(JsonNode array) {
    if (!array.isArray()) {
      return List.of();
    }

    List<String> values = new ArrayList<>();

    for (JsonNode element : array) {
      String value = element.asText("").trim();

      if (!value.isBlank()) {
        values.add(value);
      }
    }

    return List.copyOf(values);
  }

  private static Instant optionalInstant(
      JsonNode node,
      String field
  ) {
    String value = text(node, field);
    return value.isBlank() ? null : Instant.parse(value);
  }

  private static <E extends Enum<E>> E enumValue(
      Class<E> type,
      String value
  ) {
    return Enum.valueOf(
        type,
        value.toUpperCase(Locale.ROOT)
    );
  }

  private static ValidationResult result(
      List<TestCase> cases,
      List<TestResult> results,
      List<String> issues
  ) {
    int passed = 0;
    int failed = 0;
    int blocked = 0;
    int inconclusive = 0;

    for (TestResult result : results) {
      switch (result.status()) {
        case PASS -> passed++;
        case FAIL -> failed++;
        case BLOCKED -> blocked++;
        case INCONCLUSIVE -> inconclusive++;
      }
    }

    return new ValidationResult(
        issues.isEmpty(),
        cases.size(),
        passed,
        failed,
        blocked,
        inconclusive,
        List.copyOf(issues)
    );
  }

  public record ValidationResult(
      boolean valid,
      int total,
      int passed,
      int failed,
      int blocked,
      int inconclusive,
      List<String> issues
  ) {
  }
}
