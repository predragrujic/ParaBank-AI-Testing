# Autonomous AI QA Agent — ParaBank

An experimental **AI-powered autonomous web testing project** built with Java, OpenAI Codex CLI, and Microsoft Playwright MCP.

The goal is to explore how an AI agent can independently inspect a web application, design meaningful test scenarios, execute them in a real browser, verify actual behavior, and generate evidence-based QA reports.

The application under test is [ParaBank](https://parabank.parasoft.com/parabank/index.htm), a public demonstration banking application.

## How It Works

Instead of relying on predefined test scripts, the AI agent is responsible for the testing process.

1. **Explore** — Inspect the application's pages, forms, navigation, validation rules, and available functionality.
2. **Plan** — Independently select 10 test scenarios: 5 positive and 5 negative.
3. **Execute** — Perform the selected scenarios against the live application using Playwright MCP.
4. **Verify** — Compare expected behavior with actual browser observations.
5. **Investigate** — Examine unexpected behavior and identify potential defects.
6. **Report** — Generate Markdown and JSON test reports with screenshots and execution evidence.

The scenarios are **not hardcoded**. The agent decides which functionality to test based on what it discovers during exploration.

## Positive vs. Negative Testing

Each run contains:

- **5 positive scenarios** — Verify that supported application functionality works with valid input.
- **5 negative scenarios** — Verify that the application correctly handles invalid input, rejected actions, and error conditions.

**Negative does not mean failed.**

For example, if the agent submits incorrect login credentials and the application correctly rejects them, the negative test is marked **PASS**.

The agent independently determines the outcome of every test:

| Status       | Meaning                                                        |
|--------------|----------------------------------------------------------------|
| PASS         | Actual behavior matches the expected result                    |
| FAIL         | Actual behavior contradicts the expected result                |
| BLOCKED      | A required precondition or execution capability is unavailable |
| INCONCLUSIVE | There is insufficient evidence to determine the result         |

The objective is not to produce a predetermined number of passing or failing tests, but to identify the application's actual behavior.

## Technology Stack

| Technology               | Purpose                                                                          |
|--------------------------|----------------------------------------------------------------------------------|
| Java 25                  | Application launcher and execution orchestration                                 |
| OpenAI Codex CLI         | Autonomous AI QA agent — exploration, reasoning, planning, and result evaluation |
| Microsoft Playwright MCP | Browser automation through Model Context Protocol                                |
| Chromium                 | Real browser used for interacting with the application                           |
| Maven                    | Java project build and dependency management                                     |
| Jackson                  | Processing structured JSON test results                                          |
| Markdown / JSON / PNG    | QA reporting and evidence                                                        |

Codex CLI is used through an authenticated ChatGPT account, without a separately configured paid AI API integration.

## Application Under Test

**ParaBank — Online Banking Demo**

The agent may discover and test functionality such as customer registration, login, account overview, account details, customer profile updates, transaction searches, and form validation.

These are examples of available functionality, **not a predefined list of test cases**. Actual test selection is performed autonomously during each run.

Synthetic customer data is used for registration and authenticated testing. Real financial operations are excluded from the permitted test scope.

## Architecture

```text
Java 25 — AI QA Test Launcher
          |
          v
     OpenAI Codex CLI
       AI QA Agent
          |
          v
  Microsoft Playwright MCP
          |
          v
     Chromium Browser
          |
          v
    ParaBank Demo App
          |
          v
  Test Execution & Verification
          |
          v
   Markdown + JSON + Screenshots
```

Java handles application startup, execution directories, process management, and console reporting.

Codex handles autonomous exploration, test design, execution decisions, and analysis.

Playwright MCP provides browser interaction with the real application.

## Example Execution

The application is started by running:

`com.aiqa.AiWebTesterApplication`

Example console output from a completed execution:

```text
AI QA TEST
Application: https://parabank.parasoft.com/parabank/index.htm
Date: 2026-10-09 18:55:35

TC-01   PASS           Register a fictional customer
TC-02   PASS           View the new customer's account overview
TC-03   PASS           Update and verify synthetic contact details
TC-04   PASS           Search transactions by amount
TC-05   PASS           Log in with the created customer
TC-06   PASS           Reject unknown login credentials
TC-07   PASS           Reject mismatched registration passwords
TC-08   PASS           Require registration fields
TC-09   PASS           Reject malformed transaction search date
TC-10   PASS           Reject nonnumeric transaction ID

Total: 10 | PASS: 10 | FAIL: 0 | BLOCKED: 0 | INCONCLUSIVE: 0
```

This is an example of one actual run. Future runs may select different scenarios and produce different results.

## Test Evidence and Reporting

Every execution receives its own report directory:

```text
reports/
  runs/
    <run-id>/
      TEST_REPORT.md
      test-results.json
      codex.log
      screenshots/
        TC-01.png
        TC-02.png
        ...
        TC-10.png
```

The generated reports contain the planned scenarios, execution steps, expected outcomes, observed application responses, test statuses, and available screenshot evidence.

Potential bugs are reported only when supported by actual browser observations.

The detailed Codex execution log is stored separately, keeping the IntelliJ console output concise.

## Project Goal

This project explores **agentic AI in software quality assurance**: moving beyond AI-assisted test generation toward an agent that performs the broader QA workflow autonomously.

The primary focus is on:

- Autonomous exploration rather than predefined browser scripts.
- Independent selection of meaningful positive and negative scenarios.
- Real browser execution using Playwright MCP.
- Evidence-based verification rather than trusting a successful click.
- Honest reporting of passed, failed, blocked, and inconclusive tests.
- Minimal Java orchestration, leaving testing decisions to the AI agent.

**The AI agent selects the tests. The real application determines the observed behavior. The agent evaluates the evidence and reports the outcome.**

This is an experimental QA prototype, not a replacement for deterministic regression tests or a guarantee of complete application coverage.