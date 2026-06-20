---
name: Analysis accuracy issue
about: Report coverage results that look incorrect even though CoverageX completed successfully
title: "[ACCURACY] "
labels: accuracy
assignees: ''

---

## Summary
Briefly describe the analysis result that looks incorrect.

## What Looks Incorrect?
Select all that apply:
- Line coverage
- Method coverage
- Branch coverage
- Invocation count
- Parameter tracking
- Test-to-code attribution
- Over-coverage analysis
- Insights / suggestions
- Unloaded class detection
- Report totals / percentages
- Other

## Expected Coverage Result
What did you expect CoverageX to report?

## Actual CoverageX Result
What did CoverageX report instead?

## Java Code Under Test
Please include the smallest relevant class or method.

```java
// Production code example
```

## Test Code
Please include the test that should produce the expected coverage result.

```java
// Test code example
```

## CoverageX Configuration
Paste the relevant Maven or Gradle configuration.

```xml
<!-- Maven example -->
```

```gradle
// Gradle example
```

## Generated Report Snippet
Attach or paste the relevant report section, screenshot, JSON/XML output, or log lines.

## Reproduction Steps
1.
2.
3.

## Environment
- CoverageX version:
- Java version:
- Build tool and version:
- Test framework and version:
- OS:
- CI provider, if applicable:

## Notes
Mention any workaround, related issue, or reason you believe the reported result is inaccurate.
