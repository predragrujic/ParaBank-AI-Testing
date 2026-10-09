package com.aiqa.model;

import java.util.List;
import java.util.Objects;

public record BugReport(
    String id,
    String title,
    Severity severity,
    List<String> affectedTestCaseIds,
    String applicationUrl,
    String preconditions,
    List<String> stepsToReproduce,
    String expected,
    String actual,
    String impact,
    Reproducibility reproducibility,
    String screenshot
) {

  public BugReport {
    requireText(id, "id");
    requireText(title, "title");
    requireText(expected, "expected");
    requireText(actual, "actual");
    requireText(screenshot, "screenshot");

    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(
        reproducibility, "reproducibility");

    if (!id.matches("BUG-\\d{3}")) {
      throw new IllegalArgumentException(
          "Invalid bug ID: " + id);
    }

    affectedTestCaseIds =
        validateList(affectedTestCaseIds);

    stepsToReproduce =
        validateList(stepsToReproduce);

    if (affectedTestCaseIds.isEmpty()) {
      throw new IllegalArgumentException(
          "Bug must reference at least one test");
    }

    if (stepsToReproduce.isEmpty()) {
      throw new IllegalArgumentException(
          "Bug must contain reproduction steps");
    }

    for (String testId : affectedTestCaseIds) {
      if (!testId.matches("TC-(0[1-9]|10)")) {
        throw new IllegalArgumentException(
            "Invalid test reference: " + testId);
      }
    }

    if (!screenshot.matches(
        "screenshots/[a-zA-Z0-9_-]+\\.png")) {
      throw new IllegalArgumentException(
          "Invalid screenshot path: " + screenshot);
    }

    applicationUrl = normalize(applicationUrl);
    preconditions = normalize(preconditions);
    impact = normalize(impact);
  }

  public enum Severity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
  }

  public enum Reproducibility {
    REPRODUCED,
    OBSERVED_ONCE,
    NOT_SAFELY_REPEATABLE
  }

  private static List<String> validateList(
      List<String> values
  ) {
    Objects.requireNonNull(values, "values");

    if (values.stream().anyMatch(
        value -> value == null || value.isBlank())) {
      throw new IllegalArgumentException(
          "List contains null or blank values");
    }

    return List.copyOf(values);
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim();
  }

  private static void requireText(
      String value,
      String field
  ) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
          field + " cannot be empty");
    }
  }
}
