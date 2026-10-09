package com.aiqa.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record TestResult(
    String testCaseId,
    Status status,
    String actual,
    List<String> executedSteps,
    List<String> evidence,
    List<String> bugIds,
    Instant startedAt,
    Instant completedAt,
    String rationale
) {

  public TestResult {
    requireText(testCaseId, "testCaseId");
    requireText(actual, "actual");
    requireText(rationale, "rationale");

    Objects.requireNonNull(status, "status");

    if (!testCaseId.matches("TC-(0[1-9]|10)")) {
      throw new IllegalArgumentException(
          "Invalid test case ID: " + testCaseId);
    }

    executedSteps = copy(executedSteps);
    evidence = copy(evidence);
    bugIds = copy(bugIds);

    if (startedAt != null && completedAt != null
        && completedAt.isBefore(startedAt)) {
      throw new IllegalArgumentException(
          "Completion time cannot precede start time");
    }

    for (String bugId : bugIds) {
      if (!bugId.matches("BUG-\\d{3}")) {
        throw new IllegalArgumentException(
            "Invalid bug reference: " + bugId);
      }
    }

    if (status == Status.PASS && executedSteps.isEmpty()) {
      throw new IllegalArgumentException(
          "PASS requires executed steps");
    }

    if (status == Status.PASS && evidence.isEmpty()) {
      throw new IllegalArgumentException(
          "PASS requires verification evidence");
    }
  }

  public enum Status {
    PASS,
    FAIL,
    BLOCKED,
    INCONCLUSIVE
  }

  private static List<String> copy(List<String> values) {
    if (values == null) {
      return List.of();
    }

    if (values.stream().anyMatch(
        value -> value == null || value.isBlank())) {
      throw new IllegalArgumentException(
          "List contains null or blank values");
    }

    return List.copyOf(values);
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

