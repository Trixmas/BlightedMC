---

name: patch-bug
description: Diagnose a bug, verify its root cause, fix confirmed occurrences with a focused regression test, and report concise evidence. Supports diagnosis-only mode.
---

# Patch Bug

**Fix the cause, not the symptom.** Follow the repository's `AGENTS.md` and existing architecture. A correct small fix beats a broad cleanup.

## Rules

1. **Inspect before editing.** Read current code, callers, tests, config, installed versions, `git status`, and `git diff`. Re-read each target file immediately before changing it; preserve user edits.
2. **Reproduce or qualify.** Prefer a failing test or safe minimal reproduction. A traced root cause can justify a fix when reproduction is impractical, but say so.
3. **Prove the mechanism.** Test competing explanations; do not patch an unverified guess.
4. **Minimal change.** Fix confirmed siblings, not merely similar-looking code. No unrelated refactors, renames, or formatting.
5. **Verify and disclose.** Never claim a test or build passed without running it. Never stage, commit, or push without explicit authorization.

## 1. Diagnose

- State the **observed behavior**, **expected behavior**, and **trigger** (input, sequence, environment, version).
- Trace the actual path: entry point → caller → domain action → external dependency/state → result. Include lifecycle, scheduling, concurrency, and permission paths where relevant.
- Check `git diff`, `git log`, and `git blame` when a regression is suspected. Verify platform and library behavior against installed versions.
- If unclear, list at most **three falsifiable hypotheses**, run a targeted check for each, and discard refuted explanations.
- State the root cause in **1–2 sentences**: why this trigger produces the symptom and, if applicable, why the regression began.
- If the mechanism remains uncertain, stop and report the missing evidence. Do not ship a shotgun fix.

## 2. Find sibling occurrences

Search for the **same failure mechanism**, not matching syntax. Classify relevant hits:
- **Affected:** same mechanism and trigger; fix in the same change.
- **Unaffected:** similar code but protected or semantically different; leave alone.
- **Unclear:** insufficient evidence; report, do not edit.

Do not enumerate every unaffected match in the final report. Include only those necessary to explain scope.

## 3. Fix

- Make the smallest change at the responsible domain boundary, preserving established contracts and caller behavior.
- Add a regression test that would fail before the change and pass after it. Prefer testing observable behavior through the public interface.
- Do not swallow exceptions, add blanket guards, retries, sleeps, casts, or new abstractions unless the verified root cause specifically requires them.
- Avoid new flags, duplicated workflows, and speculative compatibility layers. Follow the repository's current `AGENTS.md` code-shape and API rules.
- If no suitable test harness exists, explain the limitation and the concrete manual verification needed.

## 4. Verify

Run the relevant checks, as available:
1. Regression test before and after the fix (when practical).
2. Affected unit/integration tests and confirmed sibling cases.
3. Type-check, build, lint/format checks appropriate to touched code.
4. `git diff` to ensure no user changes or unrelated lines were lost.

Record **pass / fail / skip** accurately. A failing check is not a pass because it is unrelated; note the reason. Do not run unsafe destructive verification against production or shared data.

## 5. Report

**Status first. Evidence over narrative.** Plain text, no emoji, no praise, no investigation diary. Keep the report to roughly **10–15 short lines** when one cause is involved. No location table for a single fix; use a small table only for multiple affected locations. One line per label. Never repeat the same fact in a paragraph and table. Include only checks actually run; use `[skip]` for important unrun checks.

```text
## Patch: <symptom>
Status: <Fixed and verified | Fixed, partly verified | Fixed, unverified | Not fixed>

Trigger  <exact reproduction condition>
Cause    <mechanism> — `path:42`
Fix      <smallest change> — `path:42`
Test     <regression behavior> — `test-path:18`

| Other location | Result | Reason |
|---|---|---|
| `path:88` | Fixed | Same mechanism |
| `path:130` | Unaffected | Existing guard |

Checks
  [pass] <command>: <result>
  [skip] <check>: <reason>

Open  <only unresolved risk or question>
```

Omit the **Other location** table, **Test**, or **Open** when not applicable. Keep **Status**, **Cause**, **Fix**, and **Checks**. If nothing was fixed, say so; do not present a proposed change as completed.

### Diagnosis-only mode

When asked to investigate, diagnose, or prepare a handoff **without edits**, stop after root-cause and sibling analysis. Do not change repository files. Use this shorter report:

```text
## Diagnosis: <symptom>
Status: <Diagnosed | Partly diagnosed | Not reproduced>

Trigger   <known condition>
Cause     <verified mechanism, or what remains uncertain> — `path:42`
Proposed  <smallest fix> — `path:42`
Test      <regression scenario to add>

Coverage
  [checked] <relevant callers and siblings>
  [skip]    <unverified area>: <reason>

Open  <specific missing evidence, if any>
```

Do not claim a diagnosis is confirmed when it relies on an unverified assumption.
