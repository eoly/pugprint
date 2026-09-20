# 4. Single app module with pure-JVM :core split
Date: 2026-09-20 · Status: Accepted

## Decision
One `:app` module plus pure-JVM `:core:printer` and `:core:imaging`; add `:core:bluetooth`
and `:feature:editor` as they grow. No build-logic/convention plugins yet.

## Consequences
- Fast JVM golden tests for the risky protocol/imaging code.
- Convention plugins deferred — overkill for a solo hobby app; revisit if modules multiply.
