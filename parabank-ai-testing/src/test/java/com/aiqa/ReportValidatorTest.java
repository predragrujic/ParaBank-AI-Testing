package com.aiqa;

import com.aiqa.config.QaConfiguration;
import com.aiqa.execution.QaRunManager;
import com.aiqa.execution.QaRunManager.QaRun;
import com.aiqa.report.ReportValidator;
import com.aiqa.report.ReportValidator.ValidationResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ReportValidatorTest {

  @TempDir
  Path projectRoot;

  private QaConfiguration configuration;
  private QaRun run;
  private ReportValidator validator;

  @BeforeEach
  void setUp() throws IOException {
    configuration = new QaConfiguration(
        URI.create(
            "https://parabank.parasoft.com/parabank/index.htm"
        ),
        5,
        5,
        "codex.cmd",
        Duration.ofMinutes(30),
        Path.of("reports"),
        "TEST_REPORT.md",
        true
    );

    QaRunManager manager = new QaRunManager(
        projectRoot,
        configuration
    );

    run = manager.startRun();
    validator = new ReportValidator();
  }

  @Test
  void shouldAcceptCompleteValidReport() throws IOException {
    Files.writeString(
        run.reportFile(),
        generateReport(10, false, false)
    );

    ValidationResult result = validator.validate(
        run, configuration
    );

    assertTrue(result.valid(), () ->
        "Validation errors: " + result.issues());

    assertEquals(10, result.total());
    assertEquals(10, result.passed());
    assertEquals(0, result.failed());
    assertEquals(0, result.blocked());
    assertEquals(0, result.inconclusive());
    assertTrue(result.issues().isEmpty());
  }

  @Test
  void shouldRejectMissingReport() throws IOException {
    ValidationResult result = validator.validate(
        run, configuration
    );

    assertFalse(result.valid());
    assertEquals(0, result.total());

    assertTrue(result.issues().stream()
        .anyMatch(issue -> issue.contains("Missing report")));
  }

  @Test
  void shouldRejectIncompleteTestSuite() throws IOException {
    Files.writeString(
        run.reportFile(),
        generateReport(7, false, false)
    );

    ValidationResult result = validator.validate(
        run, configuration
    );

    assertFalse(result.valid());
    assertEquals(7, result.total());

    assertTrue(result.issues().stream()
        .anyMatch(issue -> issue.contains("Expected 10")));
  }

  @Test
  void shouldRejectMissingTestStatus() throws IOException {
    Files.writeString(
        run.reportFile(),
        generateReport(10, true, false)
    );

    ValidationResult result = validator.validate(
        run, configuration
    );

    assertFalse(result.valid());

    assertTrue(result.issues().stream()
        .anyMatch(issue ->
            issue.contains("Missing execution status")));
  }

  @Test
  void shouldRejectNonexistentBugScreenshot() throws IOException {
    Files.writeString(
        run.reportFile(),
        generateReport(10, false, true)
    );

    ValidationResult result = validator.validate(
        run, configuration
    );

    assertFalse(result.valid());

    assertTrue(result.issues().stream()
        .anyMatch(issue ->
            issue.contains("Screenshot does not exist")));
  }

  @Test
  void shouldAcceptBugWithExistingScreenshot() throws IOException {
    Path screenshot = run.screenshotDirectory()
        .resolve("BUG-001.png");

    Files.write(screenshot, new byte[]{
        (byte) 0x89, 'P', 'N', 'G',
        0x0D, 0x0A, 0x1A, 0x0A
    });

    Files.writeString(
        run.reportFile(),
        generateReport(10, false, true)
    );

    ValidationResult result = validator.validate(
        run, configuration
    );

    assertTrue(result.valid(), () ->
        "Validation errors: " + result.issues());
  }

  private String generateReport(
      int numberOfTests,
      boolean omitLastStatus,
      boolean includeBug
  ) {
    StringBuilder markdown = new StringBuilder("""
                # ParaBank Autonomous AI QA Report

                ## Executive Summary

                Simulated JUnit report.

                ## Discovered Functionality

                Login and registration were simulated.

                ## Test Cases

                """);

    for (int i = 1; i <= numberOfTests; i++) {
      String testId = "TC-%02d".formatted(i);

      markdown.append("### ")
          .append(testId)
          .append(" - Simulated scenario ")
          .append(i)
          .append("\n\n");

      markdown.append("- Classification: ")
          .append(i <= 5 ? "POSITIVE" : "NEGATIVE")
          .append("\n");

      markdown.append("- Expected: Simulated expected result\n");
      markdown.append("- Actual: Simulated actual result\n");

      if (!(omitLastStatus && i == numberOfTests)) {
        markdown.append("- Status: PASS\n");
      }

      markdown.append("\n");
    }

    markdown.append("## Confirmed Bugs\n\n");

    if (includeBug) {
      markdown.append("""
                    ### BUG-001 - Simulated validation defect

                    - Expected: Invalid input rejected
                    - Actual: Invalid input accepted

                    ![BUG-001](screenshots/BUG-001.png)

                    """);
    } else {
      markdown.append(
          "No confirmed bugs were identified.\n\n");
    }

    markdown.append("""
                ## Coverage and Limitations

                This is simulated data for a unit test.
                """);

    return markdown.toString();
  }
}
