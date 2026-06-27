## Summary
Briefly describe what changed and why.
Also include information whether your changes break backward compatibility, contracts or any other integration parts that might affect users.

## Area
Select all that apply:
- Maven plugin
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
# Example:
# mvn test -pl coveragex-agent
```

## CoverageX Checklist
- [ ] Hot-path instrumentation or probe-recording changes avoid unnecessary allocation and locking.
- [ ] Maven plugin changes include configuration and failure-mode coverage.
- [ ] Report changes include representative output or snapshots where useful.
- [ ] Java version compatibility was considered for changed bytecode or fixture behavior.

## Related Issues
Link any related issues, discussions, or follow-up work.
