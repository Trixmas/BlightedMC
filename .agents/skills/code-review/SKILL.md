---

name: code-review
description: Review code for correctness, security, Stripe-level API design, maintainability, and AGENTS.md compliance. Report actionable findings without modifying files.
---

# Code Review

Review the change as code that will be maintained and consumed.

Five rules:
1. **Evidence or silence.** Report a finding only if you can trace it through the code.
2. **Try to disprove it.** Every finding survives an attempt to break it, or it is dropped.
3. **Short beats thorough-looking.** The report is read in under two minutes.
4. **The bar is a Stripe API review.** A concrete breach of the repository standard or a costly API design defect is a finding, not a note. "It works" and "it was already like that" do not lower the bar.
5. **Use the vocabulary.** Findings use the terms below, exactly. Do not drift to "service", "component", "manager", "boundary", or "helper".

## Vocabulary

| Term      | Meaning                                                                                                                                         |
| --------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| Module    | Anything with an interface and an implementation: function, class, file, package                                                                |
| Interface | Everything a caller must know: signature, types, ordering, errors, side effects, limits                                                         |
| Depth     | Behavior hidden behind an interface, relative to its size. Deep: small interface, large behavior. Shallow: interface about as large as the body |
| Seam      | A place behavior can be swapped without editing callers                                                                                         |
| Adapter   | A thin entry point (command, button, route, job): validate, call a module, render the outcome                                                   |
| Leverage  | What callers gain per unit of interface they learn                                                                                              |
| Locality  | A change or a bug stays in one place                                                                                                            |

If the repository has a `CONTEXT.md` or glossary, its domain terms win. Flag inconsistent domain terminology only when it creates concrete ambiguity or breaks an established contract.

## Review-only

NEVER modify, stage, or commit anything. Run read-only commands. Run build or tests only when they do not change tracked files, and say when you did not run them. NEVER claim a check passed unless you ran it. Apply fixes only when the user explicitly asks.

## 1. Establish the change

1. **Comparison point.** Use the range the user gave. Otherwise use the merge-base with the default branch. State the range in the report.
2. **Size it.** `git diff --stat`. Then read every changed hunk. Skip generated files and lockfiles, and list them under Coverage.
3. **Read around the diff.** Read each changed file around its hunks. Search callers and consumers of every changed exported symbol, schema, and configuration key. A diff alone is not enough when behavior depends on callers, lifecycle, shared abstractions, configuration, or platform APIs.
4. **Find the spec.** Issue, task, PR description, commit messages, linked docs. If there is none, write "none found" and review for internal consistency. Do not demand a spec.
5. **Load the standard.** Read `AGENTS.md` (root and nearest to the changed files), lint and format configuration, and the platform and dependency versions in the manifest. These are the review standard. Do not substitute your own taste. Do not assume an API exists from another fork, version, or ecosystem. Verify against the installed version. Section 4 of this skill (code shape) is the minimum floor: apply it even when `AGENTS.md` is silent.
6. **Scope.** Changed lines are in scope. When the user names files or a directory instead of a diff, every line in them is in scope. Two extra cases pull old code in: a change that adds an arm or operand to an existing ladder, ternary, or condition puts the whole construct in scope, and a change that depends on a pre-existing violation puts that violation in scope.
7. **Shape scan.** Before reading for logic, search the in-scope lines mechanically. The scan finds candidates. Reading confirms them.
   - `else if`, and 3 or more sequential `if (x.type === …)` / `kind ===` / `status ===` checks on one variable
   - a `?` after a `:` in one expression, or any multi-line ternary
   - conditions with 3 or more `&&` / `||` operands, or both operators mixed
   - unexplained numeric literals, protocol codes, and bit shifts (`1 << 6`)
   - long functions with mixed responsibilities or repeated branching; line count is a signal, not a defect
   - blocks that repeat with one field changed
   - `any`, unsafe `as` / `!`, mode-like boolean parameters, more than 3 positional parameters, deeply nested responsibilities
8. **Large diffs.** Over about 800 changed lines or 25 files, review by risk: public APIs, trust boundaries, data, concurrency first. State the coverage under Coverage. The shape scan still runs on everything.

## 2. Passes

Run in this order.

**A. Requirements.** Map each requirement to where it is implemented. Missing, partial, and extra behavior are all findings. Unrelated refactors and reformatting are findings.

**B. Correctness.** Edge cases (empty, absent, maximum, duplicate, malformed), state transitions, error paths, resource cleanup, ordering, idempotency, time and numeric limits, concurrency and thread confinement, lifecycle, integration with callers. For every ladder or `switch` over a closed set: what happens to a value no arm handles? Silent fall-through is a Bug, not a Shape finding.

**C. Trust and safety.** Input validated where it enters. Authorization on every entry point. Secrets and user content kept out of logs. Injection, unsafe rendering, data loss.

**D. Compatibility.** For every changed or removed public symbol, schema, or configuration format, search its consumers. A break without migration is a finding. Do not report consumers that do not exist.

**E. API and DX (Stripe bar).** Write the call site a newcomer would write, then write the two-line manual (what it does, what it returns, how it fails). If either reads badly, report it. Check:
- **Nested over flat.** Related inputs sit in a named object. Flag flat fields when their relationships are unclear or extending the contract would require coordinated caller changes.
- **Union over boolean.** A boolean that is really a mode, kind, or state (`ephemeral`, `isInteraction`, `force`) is a closed union. Two optional booleans that interact (`a`, `b`, and `a === false` checks) are a union waiting to happen.
- **Return the result.** A mutation returns the updated object or a typed outcome when callers need the new state or must distinguish expected outcomes. `void` is valid when no useful result is needed; a bare boolean is a finding only when it hides meaningful outcomes. Use discriminated unions for expected, actionable outcomes.
- **Explicit verbs.** A side effect has a verb and a domain object (`claimVoiceRoom`, `transferOwnership`). `process`, `handle`, `run`, or an `update` that takes a mode argument is a finding.
- **Plain words.** No jargon, wire codes, or internal names in a public surface. Protocol numbers appear as named values.
- **Predictable and consistent.** Same verbs, list shape, pagination, and error shape as sibling APIs.
- **Safe to retry.** A mutation a client may repeat is idempotent (set-state, not toggle) or says it is not.
- **Additive.** Fields are added, not renamed, removed, or retyped, unless every consumer migrates in the same change.
- **Errors.** Stable code, the input name, a next step.
- **Bounded and shaped.** Lists are paginated. No raw rows, implementation types, or third-party payloads leak out.
- **Documented.** A non-obvious contract has a one-line doc and one example, and the docs still match the code.
- **Primitives over flows.** A hard-coded branch tree per case, or long copy built in conditionals, where typed data and a small caller-composed action would do.

Prefer APIs that make normal use obvious without blocking advanced use.

**F. Design.** Do responsibilities and seams fit the actual problem? Abstractions come from domain concepts, not repeated syntax.
- **Depth.** Is the interface small relative to what it hides? Flag pass-through modules only when they add interface burden without useful isolation, validation, or policy.
- **Deletion test.** Imagine deleting the module. If the complexity vanishes, it was pass-through: finding. If the complexity reappears in several callers, it earns its place. Flag a new abstraction only when it adds concrete maintenance cost without reducing complexity.
- **Seam.** Flag an interface or injection point when it adds complexity without a real substitution need. A single implementation is not itself a defect.
- **Locality.** Flag changes scattered across modules when a shared domain invariant or workflow lacks a clear owner. Editing several legitimate layers is not itself a defect.
- **One owner per action.** The same domain workflow in two adapters (slash command and button, route and job) is one concept implemented twice. That is a finding. The fix is one module that returns a typed outcome, with each adapter reduced to validate, call, render.
- **The interface is the test surface.** Prefer testing observable behavior through public interfaces. Internal tests alone are not proof that an interface is wrong.
- Do not flag duplication that is only similar syntax. Do not recommend extraction otherwise. Do not praise reusability.

**G. Code shape.** Mechanical and strict. Apply section 4.

**H. Repository standards.** Check the in-scope lines against `AGENTS.md`: naming, type annotations, slop, platform API and version use, threading, deprecated APIs, preview features. Cite the rule's section. A `NEVER` rule is at least Minor. Hard bans from the types section (`any`, `as`, `!`) are Major. Skip anything the linter or formatter already enforces.

**I. Tests.** Is important new or changed behavior covered, including failure and authorization paths? Do tests assert behavior, not implementation? Name meaningful untested behavior. Do not demand tests that add little.

## 3. Prove or drop

For every candidate finding:
1. **Trigger.** Name the input, call sequence, caller, or configuration that makes it fail. For a Shape finding, identify the concrete maintenance cost or likely extension scenario, not an invented failure.
2. **Trace.** Follow it through the code to the wrong outcome.
3. **Disprove.** Look for an upstream guard, a type that prevents it, a test that covers it, a framework guarantee. Check the real version's behavior.
4. **Confidence.**
   - **Certain.** Traced end to end, no counter found.
   - **Likely.** Strong trace with one stated assumption.
   - **Unverified.** Needs author input. It goes under Questions, never in the findings.

Drop it when:
- you cannot name a trigger (not a Bug; at most a Question)
- it is hypothetical with no concrete cost
- it is a subjective preference outside `AGENTS.md` and outside section 4
- it is pre-existing, unrelated to the change, and not pulled in by the scope rules (section 1, step 6)
- it duplicates another finding (merge findings with one root cause)

Shape findings need a demonstrable readability, extension, or maintenance cost, not necessarily a runtime failure. Numeric thresholds are scan heuristics, not automatic defects.

## 4. Code shape

The minimum floor. Each rule is checked on in-scope lines.

| Pattern                                                                                     | Severity               | Required fix                                                                                       |
| ------------------------------------------------------------------------------------------- | ---------------------- | -------------------------------------------------------------------------------------------------- |
| Repeated discrimination on one closed value (3 arms)                                        | Minor                  | Use `switch` or a typed lookup when it makes the alternatives clearer; independent guards may stay |
| Repeated discrimination on one closed value (4+ arms), with duplicated or obscured dispatch | Major                  | Use an exhaustive `switch` or typed lookup; preserve meaningful guards                             |
| Closed-set dispatch silently ignores an unsupported value                                   | Major (Kind: Bug)      | Handle the value or enforce exhaustiveness where the type supports it                              |
| Nested ternary                                                                              | Major                  | Early return, `switch`, or lookup                                                                  |
| Multi-line ternary obscuring a nontrivial object or operation                               | Minor                  | Use an explicit branch or named expression                                                         |
| Compound condition with 3 meaningful operands lacking a clear domain name                   | Minor                  | Extract the domain predicate or meaningful parts                                                   |
| Compound condition with 4+ operands or mixed `&&` / `                                       |                        | ` obscuring intent                                                                                 | Major | Name the decision and group meaningful predicates; retain clear direct guards |
| Wire/protocol number with a verified installed named export                                 | Major                  | Use the installed library's named value                                                            |
| Unexplained magic value affecting behavior                                                  | Minor                  | Name it with units or domain meaning; ordinary self-explanatory literals are allowed               |
| Branches that only select static configuration and repeat logic                             | Minor                  | Use a typed lookup when it reduces complexity                                                      |
| 3+ copies of the same domain workflow                                                       | Major                  | Centralize the domain action, not merely similar syntax                                            |
| Long function mixing unrelated responsibilities                                             | Minor                  | Split by responsibility, not line count                                                            |
| Deep nesting that obscures control flow or lifecycle                                        | Minor                  | Use guards or focused extraction without fragmenting cohesive logic                                |
| Boolean parameter representing a mode or multiple states                                    | Major                  | Use a closed union or distinct operations; factual booleans are allowed                            |
| More than 3 positional parameters with ambiguous call sites                                 | Minor                  | Use a named options object                                                                         |
| `any`, or `as` / `!` used to bypass required validation                                     | Major                  | Narrow or validate at the boundary; follow repository type rules                                   |
| External payload read through an unchecked cast                                             | Major (Kind: Security) | Validate the payload at entry                                                                      |

Rules for applying the table:
- **Same confirmed root cause in 3 or more places** is one finding. Raise severity only when repetition materially increases drift or defect risk. List every location under Evidence.
- **Adding an arm, operand, or branch to an existing construct** is a finding on the whole construct at its table severity. State "change extends the ladder" in Evidence.
- **Critical paths.** Raise severity when the shape demonstrably obscures authorization, money movement, or data-write correctness; location alone does not raise it.
- **Shape never reaches Blocker.** If the shape hides a defect (an unhandled value is silently dropped), report that as a Bug with its own trigger and severity.
- Independent guard clauses are valid even when there are several. Use exhaustiveness checks only for genuinely closed discriminated unions.
- A `// ponytail:` explanation can document an intentional exception, but cannot waive correctness, security, or an applicable repository hard ban.
- The Fix line names the replacement form from the table. "Refactor this" is not a fix.

## 5. Classify

**Severity** is impact.

| Severity | Meaning                                                                                                                                                    | Merge         |
| -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------- |
| Blocker  | Data loss or corruption, security hole, crash, main path wrong, breaks consumers, build or tests fail                                                      | Must fix      |
| Major    | Wrong in a realistic edge case, missing or wrong requirement, API contract that is costly to fix later, risky behavior untested, Major shape per section 4 | Should fix    |
| Minor    | Maintainability or DX cost with a concrete consequence, Minor shape per section 4                                                                          | Fix or accept |
| Nit      | Tiny, zero-risk, clearly worthwhile                                                                                                                        | Optional      |

**Kind**: Bug, Requirement, Security, Compat, API, Design, Shape, Standard, Test, Docs.

For a Design finding, state the concrete cost and the smallest reasonable improvement.

## 6. Report

Verdict first, table second, details third. Plain text. No emoji. No score, no rating. No praise. No recap of the diff.

Layout rules, so the report scans in seconds:
- **Verdict line first**, with the finding tally: `Verdict: Changes requested — 1 Blocker, 2 Major`. It answers "can this merge?" before anything else.
- **Header as two aligned lines**: range and spec. Labels padded to the same column.
- **One table of findings.** A reader can stop after it.
- **Each detail is a labelled block** (Problem, Evidence, Fix), labels padded to the same column, with the location and tags on the line under the title.
- **One Coverage block** replaces separate "not verified" and "checked" sections. Each line starts with `[clean]` or `[skip]`, so gaps show at the left edge.
- **Questions last**, so the author ends on what needs their input.

Writing rules:
- **No hedging.** Problem and Evidence state facts. Ban "might", "could potentially", "consider", "seems". If you cannot state it flatly, it is a Question.
- **Quote the line.** Evidence includes the offending expression, at most one line, in backticks.
- **Every Question carries a Default**: the answer you assumed. The author can reply "yes" and move on.
- **Coverage opens with a `[scan]` line**: candidate counts from the shape scan, so a clean result is visible, not assumed.

Before you send, check:
1. Every finding has a file and line, a trigger, a quoted expression, and a Fix that names the replacement.
2. Every severity matches the tables. The verdict was derived, not chosen.
3. Same-cause findings are merged. Locations are listed.
4. The shape scan was run on every in-scope file, and the `[scan]` line matches what you found.
5. No finding rests on taste, formatting, or something the linter enforces.
6. No hedge words, no praise, no recap.

Limits:
- Maximum 10 findings above Nit, ordered by severity, then kind. If there are more, report the top 10 and add `N more omitted: <kinds>`.
- Each detail is at most 6 lines: Where, Problem (one sentence), Evidence (trigger, then outcome), Fix (smallest). Add a snippet only when words are not enough, at most 8 lines. For API and Shape findings, a snippet of the proposed signature or switch is expected.
- Nits: at most 5, one line each. Write none if nothing is clearly worthwhile.
- Omit any empty block except the verdict and Coverage.

Template:

```text
## Review: <title or branch>
Range  <base>..<head> · <N> files · +<added> −<removed>
Spec   <link | none found>

Verdict: <Blocked | Changes requested | Ready with notes | Ready> — <tally, e.g. 1 Blocker, 2 Major, 1 Minor>
<one sentence: the reason in plain words>

| # | Severity | Kind | Where | Issue |
|---|----------|------|-------|-------|
| 1 | Blocker | Bug | `path/file:42` | <one line> |
| 2 | Major | API | `path/file:10` | <one line> |

### 1. <short description>
`path/file:42`  [Blocker · Bug · Certain]
Problem   <one sentence>
Evidence  <trigger> -> <wrong outcome>
Fix       <smallest reasonable fix>

### 2. <short description>
`path/file:10`  [Major · API · Likely]
...

Nits
  `path/file:7`  <one line>

Coverage
  [scan]  <N> ladders · <N> nested ternaries · <N> unnamed conditions · <N> magic values · <N> long functions
  [clean] <areas reviewed with zero findings>
  [skip]  <what was not read or run>: <why>

Questions
  ? <what the author must confirm, and why it changes the verdict>
    Default: <the answer assumed>
```

Example:

```text
## Review: add punishment expiry
Range  main..mute-expiry · 4 files · +96 −12
Spec   issue #212

Verdict: Changes requested — 2 Major, 1 Minor
Expiry works, but the new public method leaks storage rows and the modal serializer is a 7-arm ladder.

| # | Severity | Kind | Where | Issue |
|---|----------|------|-------|-------|
| 1 | Major | API | `PunishmentRegistry.java:64` | `findActive` returns raw `PunishmentRow` objects |
| 2 | Major | Shape | `serializer.ts:418` | 7 sequential `if (field.type === N)` blocks, raw numbers, no exhaustiveness |
| 3 | Minor | Standard | `PunishmentRegistry.java:71` | `loc` abbreviates `location` (AGENTS.md section 4) |

### 1. `findActive` returns raw `PunishmentRow` objects
`PunishmentRegistry.java:64`  [Major · API · Certain]
Problem   A public method exposes the storage type, so every caller depends on the table layout.
Evidence  `ModerationCommand.java:38` reads `row.expiresAtMs` directly; a column rename breaks it.
Fix       Return a `Punishment` record with the fields callers use.

### 2. Field-type ladder in `serializeModal`
`serializer.ts:418-559`  [Major · Shape · Certain]
Problem   One loop picks the output through 7 sequential `if (fieldDefinition.type === N)` blocks, each rebuilding the same label wrapper, with bare numbers (3, 4, 19, 21, 22, 23) carrying the meaning.
Evidence  Adding a field type means pasting an 8th block; a type no block handles ends the loop without `else`, so the field silently disappears from the modal. Change extends the ladder.
Fix       `switch (fieldDefinition.type)` on `ComponentType.*`, one `wrapInLabel(fieldDefinition, component)` for the shared wrapper, and `default: return assertNever(fieldDefinition)`.

### 3. `loc` abbreviates `location`
`PunishmentRegistry.java:71`  [Minor · Standard · Certain]
Problem   Violates the whole-word rule.
Evidence  AGENTS.md section 4 allows only `id`, `url`, `http`.
Fix       Rename to `location`.

Coverage
  [scan]  1 ladder · 0 nested ternaries · 0 unnamed conditions · 6 magic values · 0 mixed-responsibility functions
  [clean] Requirements, Compatibility, Tests
  [skip]  Concurrency not exercised: no multi-threaded test exists

Questions
  ? Is `findActive` called from async tasks? If so it also needs a thread requirement in its docs.
    Default: no, called from the main thread only.
```

**Verdict is derived from findings, not judged.**
- Blocked: any Blocker.
- Changes requested: no Blocker, at least one Major.
- Ready with notes: only Minor or Nit.
- Ready: no findings.

An Unverified item that would raise the verdict goes under Questions, and the verdict says so in its sentence.

## Re-review

When the user asks for a second pass after changes, review only the delta plus anything it touches. A fix that moves a ladder, ternary, or magic value instead of removing it is Still open. Show earlier findings in one compact table, then report new findings in the standard format. The verdict line is recomputed from open findings only.

```text
| # | Was | Status | Note |
|---|-----|--------|------|
| 1 | Major · API | Resolved | returns `Punishment` now (`PunishmentRegistry.java:64`) |
| 2 | Minor · Standard | Still open | `loc` renamed in one place, not at line 88 |
| 3 | Minor · Design | Obsolete | method removed |
```
