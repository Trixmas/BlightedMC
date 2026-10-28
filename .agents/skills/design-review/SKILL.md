---

name: design-review
description: Audit architecture and developer experience against real requirements and usage. Rank evidence-backed simplifications by payoff and migration cost; identify what to keep. Read-only.
---

# Design Review

Judge whether the design fits the problem it actually solves. Favor smaller, deeper interfaces and clear domain ownership, not abstraction for its own sake.

## Rules

1. **Requirements over ideals.** Evaluate the actual problem, consumers, platform constraints, and likely direction—not a hypothetical architecture.
2. **Usage before claims.** Never call something unused, redundant, or removable without searching definitions, callers, implementations, and indirect consumers.
3. **Concrete cost or silence.** Report design friction only when it causes meaningful maintenance, correctness, compatibility, or caller cost.
4. **Disprove recommendations.** Look for constraints, historical reasons, external consumers, and future requirements before proposing change.
5. **Leave alone is an outcome.** Never invent findings to justify a review.
6. **Read-only.** Do not modify, stage, commit, or push. Apply findings only after a separate explicit request.

## 1. Establish the design

Read the relevant code, root and local `AGENTS.md`, tests, manifests, and installed dependency versions. Verify platform behavior against installed versions, not memory. Inspect recent history or plans where relevant.

Determine, briefly:
- **Problem:** what behavior the system provides and for whom.
- **Scope:** the named system or the smallest area answering the question.
- **Consumers:** internal callers, other packages/apps, extension points, and possible external callers.
- **Ownership:** state, workflows, and constraints deliberately centralized here.
- **Constraints:** platform, lifecycle, threading, security, compatibility, and deployment requirements.

If the problem or consumers cannot be established from code or context, ask instead of inventing an architecture review.

## 2. Trace actual usage

For each candidate (module, interface, factory, builder, registry, overload, generic, option, context, event, public API):
- Locate its definition, implementations, variants, and call sites.
- Search symbols **and** string/config references; inspect indirect use through reflection, dependency injection, serialization, tests, and documented extensions.
- Record the search scope and relevant counts; do not equate a text-search count with a complete call graph.
- For public APIs, say **"no internal consumers found"**, never **"unused"**, when external use cannot be ruled out.
- Read the callers before judging the interface. A design that looks complex internally may make common use simpler.

## 3. Review lenses

Apply only relevant lenses:

| Lens            | Questions                                                                                                                                 |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| Fit             | Does each abstraction solve a current requirement? Is flexibility exercised? Does a repeated _domain rule_ have one owner?                |
| Depth           | Does a small interface hide substantial useful behavior, or merely forward arguments and rename types?                                    |
| Boundaries      | Are state and domain actions owned once? Do storage, protocol, framework, or lifecycle details leak into callers?                         |
| State and types | Are invalid states representable? Do discriminated unions improve the contract? Are wrappers or generics carrying real constraints?       |
| API and DX      | Write the common call site. Is usage predictable, composable, discoverable, consistently named, and safe? Are errors and outcomes useful? |
| Change locality | Does one domain change require coordinated edits across unrelated modules? Can consumers migrate safely?                                  |
| Compatibility   | Which existing consumers break? Are external consumers known or unknown? Is an additive migration necessary?                              |

Apply the repository's `AGENTS.md` as the standard. Prefer Stripe-level interface quality: the common case is obvious, correct usage is easy, and complexity is hidden behind meaningful contracts. Do not mechanically demand a `switch`, shorter function, typed mutation result, or new abstraction when the existing code is clearer. Independent guard clauses, cohesive long functions, self-explanatory literals, and meaningful `void` operations are not defects by themselves.

Do not flag similar syntax as duplication without a shared domain rule. Do not flag a single implementation merely for lacking a second one. Do not suggest a new seam for speculative future use.

## 4. Prove or drop

A Change finding requires:
1. **Evidence:** exact `path:line`, relevant callers, and search scope/count where applicable.
2. **Cost:** what breaks, becomes error-prone, or makes common use harder; name the affected consumers.
3. **Countercheck:** test whether a platform rule, lifecycle requirement, existing consumer, history, or planned feature justifies the design.
4. **Fix:** the smallest change that removes the cost without introducing more interface surface.
5. **Migration:** approximate affected call sites, compatibility impact, and safe order of steps.

Confidence: **Certain** (traced and counterchecked), **Likely** (strong evidence with one explicit assumption), **Unverified** (put under Questions, not Changes).

Drop opinions, hypothetical flexibility concerns, unverified removal claims, and changes whose migration cost outweighs demonstrated benefit.

## 5. Decide and rank

Classify observations:
- **Change:** demonstrated cost, proportionate fix, and credible migration.
- **Consider:** genuine trade-off whose decision depends on a named future condition or owner.
- **Keep:** complexity justified by real requirements, platform constraints, or useful reuse.

Rank Changes by **payoff relative to migration cost**, not by stylistic preference:
- **Payoff:** High (removes a bug source, unblocks a requirement, or simplifies many call sites); Medium (clear maintenance saving); Low (minor improvement).
- **Cost:** S (local, no consumers move); M (several files or one consumer group); L (many consumers, public contract, or risky migration).

Prefer removing unnecessary code, simplifying existing interfaces, and relocating a domain responsibility before introducing abstractions. A large redesign with uncertain payoff belongs in Consider; propose a safe first step instead.

## 6. Report

**Keep the output scannable.** Status first, one ranked table, brief evidence, explicit leave-alone decision. No essay, praise, score, or tour of the codebase. Maximum **5 Changes** and **3 Consider** items. Omit empty sections except Status, Coverage, and Leave alone. Details: at most **four short lines per Change**. Cite exact code locations. Never imply searches or checks were run when they were not.

```text
## Design review: <area>
Status: <N changes recommended | Design fits; no changes recommended>
Scope: <what was reviewed> · Consumers: <who uses it>

| # | Payoff | Cost | Where | Recommendation |
|---|--------|------|-------|----------------|
| 1 | High | S | `path/file:42` | Move room ownership rules into one action |

1. <short title> [Certain]
   Evidence  <locations, callers, search count>
   Cost      <specific developer or behavior impact>
   Change    <smallest improvement>
   Migration <N callers; order; compatibility risk>

Consider
  - <trade-off> — decide when <condition>; owner: <who>

Coverage
  [read] <areas and callers inspected>
  [skip] <unverified area or external consumers>: <reason>

Leave alone
  - <existing design> — <specific requirement or constraint justifying it>

Questions
  ? <only a question that changes a decision; assumed default>
```

When no change is warranted, omit the table and detail blocks. Keep **Leave alone** specific: identify an actual design decision supported by evidence, not generic reassurance. If no design choice can be justified, say what remains unknown rather than fabricate one.

## 7. Applying findings (only when requested)

Re-read the current code and `git status`/`git diff` before editing. Re-run usage searches before touching shared or public interfaces. Apply **confirmed Changes** only; do not implement Consider items without a decision. Preserve user changes, follow `AGENTS.md`, and make one coherent structural change at a time. Run relevant tests and checks, report failures or skips accurately, and stop if new evidence invalidates a finding. Never stage, commit, or push without explicit authorization.
