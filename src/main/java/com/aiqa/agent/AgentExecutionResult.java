package com.aiqa.agent;

import java.nio.file.Path;
import java.time.Duration;

public record AgentExecutionResult(
    int exitCode,
    boolean completedWithinTimeout,
    Duration duration,
    Path logFile
) {

  public boolean successful() {
    return completedWithinTimeout && exitCode == 0;
  }
}
