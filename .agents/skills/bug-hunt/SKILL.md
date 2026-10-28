---

name: bug-hunt
description: Find latent defects by failure mode, prove each with a trigger and code trace, and rank by impact. Read-only; report concise, actionable findings.
---

# Bug Hunt

Find defects that exist but have not surfaced. **Hunt failures, not style.** `AGENTS.md` defines repository contracts; this skill investigates observable defects, not code-shape violations in isolation. Leave design and style reviews to `code-review`.

## Rules

1. **Evidence or silence.** Every finding has a concrete trigger, an end-to-end trace, and a wrong outcome. No speculative findings.
2. **Disprove first.** Look for guards, type constraints, transactional guarantees, tests, and platform behavior that invalidate the hypothesis.
3. **Class by class.** Sweep each selected area for failure modes, not just suspicious lines.
4. **Bounded coverage.** Prioritize by impact and exposure; disclose unexamined areas.
5. **Read-only.** Never modify, stage, or commit repository files. Reproductions belong outside the repository; do not run destructive tests against real data.

## 1. Scope and risk

- Use the named diff, module, or subsystem. If no target can be inferred, ask for one.
- Read root and applicable local `AGENTS.md`, code, tests, callers, entry points, configuration, and installed dependency/platform versions. Verify version-specific guarantees rather than assuming them.
- Map entry points (commands, events, routes, tasks, startup/shutdown) and consequential side effects.
- Prioritize trust boundaries, persistence, permissions, shared state, concurrency, external I/O, and user-visible operations. Use churn as a secondary signal, not a substitute for risk.
- Select the highest-risk 3–5 areas, or all areas if the target is small. Record what was not reached.

## 2. Defect-class sweep

Check each applicable class in every selected area:

| Class          | Probe                                                                               |
| -------------- | ----------------------------------------------------------------------------------- |
| Input / limits | Invalid, empty, oversized, duplicate, overflow, off-by-one, units, encoding         |
| Absence        | Missing keys, nulls, absent resources, misleading defaults                          |
| State          | Illegal transitions, stale caches, double initialization, partial updates, aliasing |
| Concurrency    | Check-then-act, lost writes, thread confinement, races, teardown callbacks          |
| Time           | Timeouts, clocks, time zones, expiry, ordering                                      |
| Resources      | Leaks, unbounded queues/caches, cleanup on error                                    |
| Errors         | Swallowed failures, wrong error propagation, inconsistent state, unsafe retries     |
| Security       | Authorization gaps, untrusted inputs, injection, secret leakage                     |
| Logic          | Wrong operator/variable, inverted condition, unreachable or diverged branch         |
| Platform       | API contract, lifecycle, version-specific behavior                                  |
| Compatibility  | Existing data, serialization, migrations, consumers, configuration                  |

For every consequential dependency or side effect, test the mental model against **slow, unavailable, partial, repeated, out-of-order, and concurrent** execution. Then pre-mortem: identify the most plausible production incident and inspect that path explicitly.

## 3. Prove or drop

For each candidate:
1. **Trigger:** Exact input, sequence, timing, or environment.
2. **Trace:** Relevant code path, including caller and guard, to the incorrect outcome.
3. **Countercheck:** Search for a constraint, transaction, lock, framework guarantee, or test that prevents it.
4. **Reproduce if safe and cheap:** Use an isolated scratch reproduction; never imply it ran if it did not.
5. **Confidence:** **Confirmed** = reproduced or traced end to end; **Likely** = strong trace with one explicit assumption; **Unverified** = requires missing evidence and belongs in Questions, not Findings.

Drop subjective preferences, theoretical risks without a realistic trigger, duplicate root causes, and cases already prevented by the implementation. A missing test alone is not a bug.

## 4. Rank

Severity measures **impact**, likelihood measures **exposure**. Do not equate hypothetical worst-case impact with actual severity.

| Severity | Definition                                                                            |
| -------- | ------------------------------------------------------------------------------------- |
| Critical | Credible data loss/corruption, exploitable security failure, or widespread crash/hang |
| High     | Incorrect behavior in a realistic consequential case                                  |
| Medium   | Limited impact or unusual but plausible trigger                                       |
| Low      | Small observable defect                                                               |

Likelihood: **Likely** (ordinary use), **Possible** (specific realistic conditions), **Rare** (unusual conditions). Sort by severity, then likelihood. Merge shared causes.

## 5. Report

**Scan-first, not prose-first.** Plain text, no emoji, no score, no praise. Lead with status, then one table. Each finding gets **three compact lines** (Trigger, Impact, Fix); add Repro only if run. No investigation diary, large paragraphs, or duplicated descriptions. At most **8 findings**, highest risk first; note the number omitted. Keep file:line references precise. Coverage always appears. Questions only when they change interpretation or action.

```text
## Bug hunt: <target>
Status: <N bugs: 1 Critical · 2 High | No confirmed bugs>

| # | Severity | Chance | Where | Failure |
|---|---|---|---|---|
| 1 | High | Possible | `path:42` | Duplicate request writes two records |

1. Duplicate request writes two records [Confirmed]
   Trigger  Two requests with the same key arrive concurrently.
   Impact   Both pass the existence check and persist a record.
   Fix      Enforce uniqueness in storage; handle conflict.
   Repro    <only if executed: command and observed result>

Coverage
  [hunted] <area>: input, state, concurrency
  [skip]   <area>: <reason>

Questions
  ? <missing fact and its consequence> (default: <assumption>)
```

If no findings, omit the table and finding details; keep **Status** and **Coverage**. Distinguish `[skip]` (not examined) from a class checked without findings. Never claim an area clean if it was not examined. When asked to fix, hand off confirmed defects to `patch-bug`; do not edit as part of this skill.
