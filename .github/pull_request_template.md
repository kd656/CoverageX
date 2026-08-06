## Summary
Briefly describe what changed and why.
Also include information whether your changes break backward compatibility, contracts or any other integration parts that might affect users.

## Area
Select all that apply:
- Maven plugin
- Gradle plugin
- Shared build-tool core (`coveragex-core` / `coveragex-api`)
- JVM agent / instrumentation
- Binary execution data format
- Line coverage
- Method coverage
- Branch coverage
- Invocation tracking
- Parameter tracking
- Test-to-code attribution
- Report generation
- Freemarker Templates (FE)
- Compatibility fixtures
- CI / build tooling
- Documentation
- Other (please describe additionally)

## Behavior Changes
Describe any user-visible changes, configuration changes, report output changes, or compatibility impact.

## Testing
Select all that apply and include commands or notes:
- [ ] Unit tests added or updated
- [ ] Compatibility fixtures added or updated
- [ ] Manual verification completed
- [ ] Documentation updated
- [ ] Not applicable

Commands run:

```bash
# Examples:
# mvn test -pl coveragex-agent
# ./gradlew -p coveragex/coveragex-gradle-plugin check
```

## CoverageX Checklist
- [ ] Hot-path instrumentation or probe-recording changes avoid unnecessary allocation and locking.
- [ ] Build-tool plugin changes (Maven/Gradle) include configuration and failure-mode coverage.
- [ ] Behavior is kept in parity across Maven and Gradle — shared logic lives in `coveragex-core`, not duplicated per plugin.
- [ ] Gradle changes stay configuration-cache compatible (no `Project` access in task actions).
- [ ] Report changes include representative output or snapshots where useful.
- [ ] Java version compatibility was considered for changed bytecode or fixture behavior.

## Related Issues
Link any related issues, discussions, or follow-up work.
