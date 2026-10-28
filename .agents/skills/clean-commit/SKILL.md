---

name: clean-commit
description: Draft evidence-based commit messages and minimal commit plans from Git diffs, following repository conventions. Never stage or commit without explicit approval.
---

# Clean Commit

One commit, one coherent purpose. Prefer a clean history over an artificially granular one.

## Principles

1. **Diff over intent.** Every claim must be supported by the actual changes.
2. **Repository convention wins.** Match existing categories, scopes, formatting, and chain numbering.
3. **Split by purpose, not file type.** Keep tightly coupled code, tests, configuration, and documentation together.
4. **No unnecessary commits.** One coherent change is one commit, even when it touches several layers.
5. **Drafting is not execution.** Never stage, commit, amend, or push without explicit authorization.

## 1. Inspect

- Read `AGENTS.md` and any applicable local instructions.
- Run `git status`, inspect staged and unstaged diffs, and identify untracked files. Read untracked content when it is part of the intended change; do not assume its contents.
- Treat staged changes as the default target when present. Do not silently include unstaged or unrelated work. Ask when the intended scope is ambiguous.
- Read `git log -n 20` for message conventions. Use `git diff --check` when useful; do not modify files.
- Flag secrets, credentials, generated output, unrelated changes, and suspicious binaries. Never propose committing secrets.
- If there is no inspectable diff, request the relevant changes rather than inventing a message.

## 2. Choose commit boundaries

**Keep together:**
- A behavior change and its regression tests.
- Implementation, required configuration, and documentation of the same behavior.
- Changes that only make sense or pass checks together.

**Split only when:**
- Changes have independently meaningful purposes and can be reviewed separately.
- A standalone refactor is distinct from a feature or fix.
- Independent fixes or features happen to share a working tree.
- A prerequisite migration must land before a dependent change.

Do not split solely because code spans packages, file types, or many files. Each proposed commit should leave the repository in a valid state. Identify files requiring partial staging; never assume whole-file staging is safe when purposes overlap.

## 3. Write messages

Follow the repository's format. Otherwise use:

```text
type(scope): imperative summary
```

- Use established types (`feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `build`, `ci`, `chore`, `style`, `revert`) and existing scope names.
- Describe the observable change or engineering purpose, not a list of files.
- Use imperative present tense, no period, emoji, filler, or marketing language.
- Keep the subject at most 72 characters, including any prefix, unless the repository specifies otherwise.
- Include a body only to explain non-obvious **why**, compatibility constraints, or migration requirements; wrap at 72 columns.
- Mark breaking changes according to repository convention, including required consumer action.
- Include issue references only when known and consistent with repository practice.
- Check every claim against the diff. Do not imply tests passed unless they were run.

### Chains

If the repository uses `(k/n)` numbering, follow its convention. Otherwise use `(k/n)` only when the user requests numbered chains or the planned commits form one deliberately ordered feature, fix, or migration series. Never number unrelated commits together. Recalculate `n` when the plan changes. A single commit is unnumbered.

## 4. Output: minimal by default

**Single commit:** Return only the message in one fenced text block. Add a short warning outside the block only when there is a real staging or safety concern.

```text
fix(voice): prevent duplicate room claims
```

**Multiple commits:** Show one compact plan table, then copy-ready message blocks in order. Include a body only when needed.

```text
| # | Message | Scope |
|---|---------|-------|
| 1 | refactor(voice): centralize room ownership | room domain |
| 2 | fix(voice): reject concurrent room claims | claim flow and tests |
```

Do not repeat a narrative explanation of the diff. State partial-staging requirements or excluded files in one short `Flags` line only when necessary. Avoid long file lists and redundant summaries.

## 5. Execution safety

Draft-only requests are read-only. Do not run `git add`, `git commit`, `git push`, `git reset`, `git restore`, or `git stash`.

If the user asks to commit, first present the exact messages and staging plan and obtain approval as required by `AGENTS.md`. Once approved:
1. Recheck `git status` and the diff. Stop if the intended changes differ from the approved plan.
2. Stage only approved files or hunks, preserving unrelated user changes.
3. Commit in dependency order using the approved messages exactly.
4. Do not amend, squash, rebase, or push unless separately authorized.
5. Report commit hashes and any checks actually run, concisely.
