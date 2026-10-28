---
name: generate-docs
description: Write or improve concise, accurate docs for front-facing APIs in any language without changing behavior. Covers public types, functions, server functions, HTTP endpoints, schemas, and shared component props. Use when asked to document code, add doc comments, or improve API docs.
---

# Generate Docs

Write docs for the people who call the API without reading its source. Precise, short, grounded in the real contract.

Quality beats coverage. A missing comment is better than a wrong or empty one.

## What counts as front-facing

Document an API when someone outside its file calls it:

* public or exported types, functions, methods, constants, and modules
* anything reachable from a package or library entry point
* server functions and HTTP endpoints
* schemas that define a boundary (inputs, outputs, shared contracts)
* shared component props used by other features
* command-line flags and configuration keys that users set

Skip internal modules, private members, local helpers, and one-use components. A non-obvious invariant a maintainer must keep belongs in a normal source comment, not in API docs.

## Workflow

1. Read the whole touched file before editing docs. Read related code when the contract depends on it: inheritance, lifecycle, nullability, authorization, schemas, error types, callers.
2. Infer the contract from the code and existing docs. Document what a caller can rely on. Never document trivia or guesses.
3. Preserve implementation, structure, and behavior unless the user asks for code changes.
4. Use the language's own doc-comment format and tags. Match the project's existing style and terminology. When the project has none, use the language's standard convention.
5. Return only what was asked. For doc-only work, do not reproduce a whole file when the doc changes suffice.

## What a good API doc answers

In this order, only what applies:

1. **What it is or does.** One strong sentence about observable behavior.
2. **What the caller must provide.** Non-obvious preconditions, units, ranges, formats.
3. **What the caller gets.** The meaning of the return value, including null, empty, or absent cases that are part of the contract.
4. **How it fails.** Each failure a caller can handle, with its stable error code or type and the condition that triggers it. The caller must know what to do next.
5. **What else happens.** Side effects, state changes, lifecycle, concurrency requirements, idempotency, retry safety, rate or size limits.
6. **Who may call it.** Authorization, when the API enforces it.
7. **One example.** Only when usage is non-obvious.

Skip any item the name and types already make obvious.

## Rules

* Describe behavior, not the name in different words.
* Explain what a value **means**, not that it is returned.
* No filler: "the specified", "the given", "this function is used to", "returns the result", "provides functionality for".
* No marketing language, implementation walkthroughs, or obvious language explanations.
* No speculative behavior. Never invent guarantees, nullability, or error cases.
* Where the language has a type system, the compiler owns types. NEVER put a type in a doc tag. NEVER restate a type in prose.
* Respect existing nullability or optionality annotations. When absent, claim nullability only if the code and surrounding API make it clear.
* Use project terminology. Write whole words. No new abbreviations beyond `id`, `url`, `http`.
* Add links and tags only when they help navigation. Follow the project's existing conventions for deprecation, version, and cross-reference tags. Add none the project does not use.
* One strong sentence beats several weak ones.

```
// bad: restates the name and the types
// Gets an application.
// param applicationId (string): the id of the application
// returns (Application): the application

// good: behavior, failures, authorization
// Loads an application with its answers, for staff review.
// Fails with `application_not_found` when no application has this id.
// Fails with `forbidden` when the caller lacks the staff role.
```

Comment syntax above is illustrative. Use the language's own.

## Kinds of API

**Types, classes, interfaces, records.** The summary says what the type represents or provides. Add a second paragraph only for an important contract, lifecycle rule, usage constraint, or behavioral distinction.

**Interfaces, traits, protocols, and callbacks.** Document the contract implementers must honor. For a single-operation interface or callback type, explain the operation it represents and the input/output semantics. Document other members by their real behavior. Do not assume a helper or factory needs docs.

**Functions, methods, constructors.** Document important side effects, non-obvious preconditions, state or lifecycle changes, meaningful return and failure behavior, and concurrency requirements. A constructor needs no summary when the type doc and parameter descriptions make its purpose clear.

**Parameters and returns.** Describe a parameter only when its name and type do not carry the meaning (units, ranges, formats, side effects). Describe a return only when its meaning is not obvious. Skip a redundant return description.

**Enumerations.** Document the type. Document constants only when their meaning is not clear from the name or the project requires it. If the user says to leave constants alone, leave them alone.

**Server functions and endpoints.** Document:

* what it does, in observable terms
* who may call it (the authorization it enforces)
* input constraints the schema does not make obvious (units, limits, formats)
* the shape of what it returns, when it is a deliberate subset of stored data
* every handled failure: error code, trigger, what the caller should do
* side effects, idempotency or retry safety, rate and size limits

```
// Accepts a pending application and notifies the applicant.
//
// Staff only. Safe to retry: accepting an already accepted application
// succeeds without notifying again.
//
// Fails with `forbidden` when the caller lacks the staff role.
// Fails with `application_not_pending` when the application was rejected or withdrawn.
```

**Schemas.** Document a field only when meaning, unit, limit, or format is not obvious from its name and constraints. Document the schema when it is a shared contract. NEVER describe every field.

**Components.** Document props only for shared components used across features, and only the props whose behavior is not obvious.

**Error codes.** Every stable code a caller can branch on is documented where it is raised or where the error type is defined. Say what triggers it and what to do about it. If an error has no code or field, say so separately (see API quality).

**Private members.** No docs.

**Standalone reference pages.** Write a README or endpoint reference only when the user asks. Keep one example per non-obvious endpoint and one table of error codes. Never invent endpoints, fields, or codes.

## Existing docs

When improving docs: keep useful information, remove redundancy, resolve ambiguity, tighten wording, keep project terminology. Do not rewrite for stylistic variety.

## API quality

Docs complement the API. They do not rescue a confusing one.

When the contract is ambiguous or surprising and cannot be documented accurately, say so in a separate note. Examples: a function that returns nothing in one branch and fails in another, an error with no stable code, an endpoint returning raw storage rows, two names for one concept, a mutation with unclear retry behavior.

Never silently change the API or implementation to make the docs easier to write. Never invent the intended behavior.

## Output

The result must be:

* valid for its format (the language's doc-comment syntax, or Markdown)
* accurate to the code and contract
* consistent with the surrounding project
* useful to a caller
* free of unrelated code changes

For doc-only requests, do not refactor or touch unrelated code. When an implementation issue materially affects the documented contract, report it separately.
