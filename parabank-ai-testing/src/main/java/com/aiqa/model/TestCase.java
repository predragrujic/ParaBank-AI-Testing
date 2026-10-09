package com.aiqa.model;

import java.util.List;
import java.util.Objects;

public record TestCase(
    String id,
    Classification classification,
    String title,
    String objective,
    String preconditions,
    String testData,
    List<String> steps,
    String expected,
    String verificationStrategy,
    ExecutionSafety executionSafety
) {

  public TestCase {
    requireText(id, "id");
    requireText(title, "title");
    requireText(objective, "objective");
    requireText(expected, "expected");
    requireText(verificationStrategy, "verificationStrategy");

    Objects.requireNonNull(classification, "classification");
    Objects.requireNonNull(executionSafety, "executionSafety");

    if (!id.matches("TC-(0[1-9]|10)")) {
      throw new IllegalArgumentException(
          "Invalid test ID: " + id);
    }

    preconditions = normalize(preconditions);
    testData = normalize(testData);

    steps = validateSteps(steps);
  }

  public enum Classification {
    POSITIVE,
    NEGATIVE
  }

  public enum ExecutionSafety {
    SAFE,
    REQUIRES_APPROVAL,
    BLOCKED
  }

  private static List<String> validateSteps(
      List<String> steps
  ) {
    Objects.requireNonNull(steps, "steps");

    if (steps.isEmpty()) {
      throw new IllegalArgumentException(
          "Test case requires at least one step");
    }

    if (steps.stream().anyMatch(
        step -> step == null || step.isBlank())) {
      throw new IllegalArgumentException(
          "Test steps cannot be blank");
    }

    return List.copyOf(steps);
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
