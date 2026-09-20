---
description: Turn a spec into an ADR + PR-sized tasks, then implement with tests
allowed-tools: Read, Edit, Write, Bash
---
Given the feature spec in $ARGUMENTS:
1. Write a short ADR in docs/adr/ if it changes architecture.
2. Break the work into PR-sized tasks (each independently testable).
3. Implement the first task with unit/golden tests. Keep :core modules pure JVM.
4. Run /build-and-test and stop for review before the next task.
