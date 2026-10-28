---
name: write-tests
description: Write useful tests for new, changed, or untested behavior, derived from the public contract and proven able to fail with deliberate breaks. Rejects test smells and filler. Produces a short report that shows what each test catches. Use when asked to add tests, raise confidence in a change, or cover untested code.
---

# Write Tests

Write tests that catch real bugs.

Four rules:

1. **Every test catches a named bug.** If you cannot say which bug, delete the test.
2. **Test behavior through the public contract.** Not implementation, not private code.
3. **Prove each test can fail.** Break the code on purpose and watch the test go red.
4. **Fewer, sharper tests beat coverage.** "Nothing worth adding" is a valid result.

## Scope

Edit test files and test fixtures only. The one exception is the temporary break in section 5, which is reverted before you finish.

If a test exposes a real bug in production code, do NOT fix it here. Keep the failing test, report the bug, and hand off to `patch-bug`.

NEVER commit, stage, or push without explicit authorization.

## 1. Establish

1. **Target.** The diff, file, or behavior the user named. If none, ask.
2. **Contract.** Read the public signatures, docs, schemas, and error types of the target. Read callers to see how it is really used.
3. **Conventions.** Read the existing tests and the repository's `AGENTS.md`. Match the framework, naming, layout, fixtures, and helpers already in use. Do not add a framework or assertion library.
4. **Run command.** Find how tests run (manifest scripts, build files, CI config). State the command.
5. **Baseline.** Run the existing tests first. If some already fail, note them so you do not blame or hide them.

## 2. Derive cases from the contract

Pick only the categories that apply to the target.

| Category | Cases |
|---|---|
| Behavior | One per distinct documented behavior |
| Boundaries | Empty, single, maximum, duplicate, off-by-one, malformed |
| Validation | One per rejected input, with the exact error |
| Failure | Each documented error code or exception, with its trigger |
| Authorization | Unauthenticated, unauthorized, authorized |
| State | Each valid transition, each invalid transition |
| Retry | Repeat the call: idempotent or safely rejected |
| Time and order | Expiry, ordering, thread confinement (inject the clock) |
| Cleanup | Resources released on failure paths |
| Compatibility | Serialization or configuration formats consumers depend on |

For every candidate, write one line: **Catches:** the specific bug this test would expose. No line, no test.

## 3. Filter for value

Do NOT write tests for:

* getters, setters, and constructors that only assign fields
* behavior of the framework or a library
* generated code
* private methods directly. Test them through the public API.
* logging output
* trivial delegation
* the same behavior again at a second layer

Test at the lowest level where the behavior lives. Add an integration test only for wiring that is a real risk.

## 4. Write

* **One behavior per test.** Arrange, act, assert, in that order.
* **Name the behavior.** `rejectsExpiredToken`, not `test1` or `testValidate`.
* **Assert outcomes.** Return values, resulting state, the exact error code, what was persisted. NEVER assert only that something was called.
* **Assert specifically.** The exact error code, not "throws something". The exact value, not "is not null".
* **Write expected values literally.** NEVER compute the expected value with the production logic or a copy of it.
* **Prefer fakes and in-memory implementations over mocks.** Mock only boundaries you do not own: network, clock, randomness, filesystem when unavoidable.
* **Inject the clock and randomness.** Seed anything random.
* **No sleeps.** Wait on a condition or a signal.
* **No logic in tests.** No `if`, no loops, no try/catch. Use a table of inputs and expected outputs for one behavior over many values.
* **Independent tests.** No shared mutable state. No order dependence.
* **Minimal fixtures.** Add a builder only when two or more tests share it.

## 5. Prove each test can fail

A test that passes against broken code is worthless.

For each behavior you tested:

1. Make the smallest deliberate break in the production code that the test must catch: invert a condition, remove a guard, change a constant by one, skip an authorization check, drop an error.
2. Run the test. It must fail, with a message that points at the cause.
3. Revert the break exactly.

If the repository has a mutation-testing tool already configured, run it on the changed files instead.

Rules:

* At most two breaks per behavior.
* After the last revert, `git diff` must show **no production file changes**. If it does, stop and revert before anything else.
* If a test still passes under its break, it is weak. Fix its assertion or delete it. Do not leave it.
* Breaks are never committed and never left in the working tree.

## 6. Smell check

Fix or delete any test that shows one of these:

| Smell | Sign |
|---|---|
| Tautology | Expected value computed by the code under test |
| Assertion-free | Runs code, asserts nothing, or only "does not throw" |
| Implementation coupling | Breaks on a harmless refactor; verifies call order or call counts |
| Over-mocking | The test mostly checks the mocks |
| Giant arrange | More than about ten setup lines for one assertion |
| Multiple behaviors | Several unrelated asserts, so a failure does not say which behavior broke |
| Shared state | Passes alone, fails in a suite, or the reverse |
| Time or randomness | Depends on the real clock, sleeps, or an unseeded random value |
| Duplicate | Covers the same behavior as another test |
| Vague name | Name does not say the behavior |

## 7. Run

1. Run the new tests. They must pass.
2. Run them again in a different order if the framework supports it, to catch order dependence.
3. Run the affected suite and the build or type-check.

NEVER claim a check passed unless you ran it. If something cannot run here, say so and say what would run it.

## 8. Report

Short and scannable. Plain text. No emoji. No coverage percentage.

Limits:

* The whole report fits in about 20 lines, table included.
* One line per field. The table has one row per test, or per group of table-driven cases.
* Omit any block with nothing to say, except Status, Added, and Verified.

Layout:

* **Status line first.** One of: `Added and verified`, `Added, partly verified`, `Found a bug`, `Nothing worth adding`.
* **Table of tests.** Each row names what the test catches and the break that proved it.
* **Verified as a tagged list.** `[pass]`, `[fail]`, `[skip]`, so failures show at the left edge.
* **Not tested** says what you left out and why, so a gap is a decision, not an oversight.

Template:

```text
## Tests: <target>
Status: <Added and verified | Added, partly verified | Found a bug | Nothing worth adding>

Added  <N> tests in `path/test` (<framework>)
Run    <command>

| # | Test | Catches | Break tried | Result |
|---|------|---------|-------------|--------|
| 1 | `rejectsExpiredToken` | expired token accepted | removed expiry check (`path/file:31`) | failed as expected |
| 2 | `returnsForbiddenForNonStaff` | missing authorization | skipped role check (`path/file:18`) | failed as expected |

Found  <real bug in production code, `path/file:42`, and the failing test that shows it>

Not tested
  <behavior>: <why: trivial, framework behavior, needs infrastructure>

Verified
  [pass] new tests pass, and fail under each break
  [pass] affected suite: `<command>`
  [skip] <check>: <why it could not run>

Open  <questions for the author, or risks that remain>
```

Example:

```text
## Tests: PunishmentRegistry.isActive
Status: Added and verified

Added  3 tests in `PunishmentRegistryTest.java` (JUnit 5)
Run    ./gradlew test --tests PunishmentRegistryTest

| # | Test | Catches | Break tried | Result |
|---|------|---------|-------------|--------|
| 1 | `expiredMuteIsInactive` | mute stays active after expiry | compared against ticks, not ms (`PunishmentRegistry.java:57`) | failed as expected |
| 2 | `permanentMuteNeverExpires` | permanent mute expiring | treated `0` expiry as expired (`PunishmentRegistry.java:55`) | failed as expected |
| 3 | `muteActiveUntilExpiryInstant` | off-by-one at the expiry instant | changed `<` to `<=` (`PunishmentRegistry.java:57`) | failed as expected |

Not tested
  `getReason`: returns a stored field, nothing to break

Verified
  [pass] new tests pass, and fail under each break
  [pass] ./gradlew test
  [skip] mutation tool: none configured
```

When nothing is worth adding:

```text
## Tests: SpawnMode
Status: Nothing worth adding

Not tested
  `SpawnMode`: a closed enum with no logic, nothing a test would catch
```