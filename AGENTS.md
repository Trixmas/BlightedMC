# Agent Guidelines

Source of truth for every coding agent in this repository.

`MUST` and `NEVER` are literal. If a rule conflicts with what feels natural, the rule wins.

Goal: code that is correct, understandable, adaptable, and pleasant for other engineers to consume.

Optimize for clear ownership, stable contracts, and local changes—not speculative flexibility.

---

## 1. Read first

Read the code, tests, and configuration you are about to change. Know its responsibility, lifecycle, thread, and
consumers.

At the start of a task inspect `git status` and `git diff`. Re-read files immediately before editing. Preserve all
pre-existing changes; never restore, reset, or overwrite them to fit a plan.

Extend an existing concept. NEVER build a parallel one.

---

## 2. Scope

Do exactly what was asked. Nothing else.

Pick the first option that works:

1. Build nothing.
2. Reuse existing code.
3. Use the standard library or the platform.
4. Use an installed dependency.
5. Write the smallest clear implementation.

- NEVER refactor code you were not asked to touch. A small local cleanup inside the code you are changing is fine.
- Delete before adding. Boring over clever.
- Three similar lines beat the wrong abstraction. Extract only when the code is one real concept (see section 6).
- NEVER weaken correctness, security, validation, thread-safety, accessibility, or data-loss protection to write less
  code.

`// ponytail:` marks a deliberate trade-off. Optional. Use it only when a reader would otherwise "fix" it.

```java
// ponytail: kept — this operation must run on the main server thread.
```

---

## 3. Types: let the compiler infer

Write a type only where the compiler cannot work it out.

NEVER write:

- a redundant explicit local type when `var` makes the initializer and intent equally clear; keep interface types where
  they communicate the abstraction
- explicit generic arguments the diamond `<>` or inference supplies
- types on lambda parameters
- a cast after `instanceof`. Use a pattern: `if (event instanceof CreatureSpawnEvent spawnEvent)`
- a raw type, an unchecked cast, or `@SuppressWarnings` without a reason in the comment

```java
// bad
ArrayList<Player> players = new ArrayList<Player>();
candidates.

forEach((SpawnableEntity candidate) ->

spawn(candidate));
        if(event instanceof CreatureSpawnEvent){
var spawnEvent = (CreatureSpawnEvent) event;
}

// good
var players = new ArrayList<Player>();
candidates.

forEach(candidate ->

spawn(candidate));
        if(event instanceof
CreatureSpawnEvent spawnEvent){

handleSpawn(spawnEvent);
}
```

Keep the declared type when the abstraction is the point:

```java
Map<EntityType, List<SpawnableEntity>> cache = createCache();
```

Method parameters and return types are always written. That is the signature contract.

Closed sets are `enum` or `sealed` types. Immutable data carriers are `record`. Use a class only for identity, mutable
state, lifecycle, or framework constraints.

NEVER wrap every primitive in a type to look strongly typed.

---

## 4. Naming

Write whole words. A name says what the thing **is** in this domain.

Max 50 characters. The only allowed abbreviations: `id`, `url`, `http`.

```java
// bad: cmd, cfg, ctx, idx, tmp, FooHelper, FooManager
// good: command, configuration, context, index, spawnCandidates, punishmentRegistry
```

- Loop and lambda variables are words too. NEVER `i`, `x`, `a`, `b`, `e`.
- Name by domain concept, never by mechanism. `PunishmentRegistry`, not `PunishmentManager`.
- NEVER create `Utils`, `Helper`, `Common`, or `Misc` classes. Name the class for what it does.
- Booleans read as a question: `isMuted`, `hasPermission`, `canSpawnAt`.
- Types `PascalCase`. Methods and variables `camelCase`. Constants `UPPER_SNAKE_CASE`.

Before you write a name, ask: would a new teammate need a comment to decode it? If yes, rename it.

---

## 5. No slop

Slop is code nobody asked for. NEVER write any of these:

- Comments that restate the code. Comment only the **why** the code cannot show.
- `catch` blocks that log and continue, return a fallback, or are empty.
- Null checks or defensive branches for states the types already make impossible.
- Optional parameters, overloads, or options "for later".
- Boolean flag parameters. Split the method.
- Wrapper methods that only forward their arguments.
- A new class used in exactly one place and unrelated to the task.
- Static utility classes.
- Default values that hide a missing required input.
- `System.out.println`, `TODO`, commented-out code, unused imports, unused parameters, dead branches.
- Hand-written getters, setters, `equals`, or `hashCode` where a record fits.
- Emoji in code, comments, or commit messages.
- Renames, reorders, or reformatting of lines you were not asked to change.
- A README, doc file, or summary file unless asked.

Early returns over nested `if`. One responsibility per method.

### Code shape

- **No nested ternaries.** Use an early return, `switch`, or named calculation. Keep simple ternaries on one readable
  line.
- **Dispatch explicitly.** For three or more branches testing the same closed discriminant, prefer `switch` or a keyed
  lookup. Independent guard clauses are not a ladder. Use exhaustive handling where the compiler can verify a sealed
  hierarchy or enum.
- **Name complex conditions.** Break mixed `&&` / `||` expressions and conditions with several distinct rules into
  meaningful boolean names. Do not extract trivial conditions merely to satisfy a count.
- **No unexplained literals.** Name non-obvious thresholds, units, masks, and protocol codes. Self-explanatory domain
  literals are fine. Use Bukkit/Spigot constants and enums where available.
- **Prefer shallow control flow.** Flatten deeply nested logic when it improves readability; do not split a cohesive
  method to meet a line limit.
- **No duplicated domain workflows.** Commands, listeners, and scheduled tasks delegate shared invariants to one owner.
  Similar syntax alone does not justify extraction.
- **Do not extend a problematic construct.** When changing a branch or condition, improve the affected construct locally
  rather than adding another layer of complexity.

---

## 6. Interface design

Every public or package-visible class, method, event, command, and configuration key is an API. Someone else will call
it without reading its source. Design it for that caller.

**Write the call site first.** Write the code that uses the interface before the code that implements it. If the call
site reads badly, the design is wrong.

**Common case first.** The simplest use is the shortest use. NEVER make callers coordinate low-level parameters or
supply what the API can determine.

```java
// good
punishments.add(player, PunishmentType.MUTE, reason);
```

**Progressive disclosure.** Simple things simple, advanced things possible. Do not force every caller through a builder,
configuration object, or generic abstraction.

```java
spawn(entity, location);

spawn(entity, location, options);
```

**Make the right way the easy way.** Required inputs are required. Defaults are safe. Invalid states cannot be expressed
in the types.

```java
// bad: nothing stops a caller passing contradictory flags
review(application, true,false);

// good: one value from a closed set
review(application, Decision.ACCEPTED);
```

**No dead ends.** A failure says what failed, which input, and what the caller should do. Throw a specific exception
type with context. NEVER `throw new RuntimeException("error")`.

**Small surface.** Package-private by default. Everything public is a promise you keep. Do not expose methods without a
real consumer or an established framework requirement. NEVER return mutable internals.

**Consistent.** One concept, one name, everywhere. Same verbs, same return conventions, same failure behavior across
related APIs. Search for the existing name before you add one.

**Predictable.** A method does what its name says and nothing else. No hidden side effects. No parameter that changes
what the method fundamentally does.

**Safe to retry.** An operation a caller may repeat is idempotent, or its docs say it is not.

**Design from concepts, not syntax.** Extract an abstraction for a shared domain concept, invariant, or ownership
boundary. NEVER extract because two loops look alike.

```java
List<SpawnableEntity> eligible = null;
for(
var candidate :candidates){
        if(!candidate.

canSpawnAt(location, world))continue;
        if(eligible ==null)eligible =new ArrayList<>(candidates.

size());
        eligible.

add(candidate);
}
```

This loop does not deserve a shared helper because another spawner has the same loop. A probability-selection algorithm
both spawners genuinely share may deserve one.

**Understandable and flexible.** An engineer unfamiliar with the implementation should understand a common call site
from its names, types, and documented contract. Keep dependencies explicit, responsibilities cohesive, and changes
local. Avoid requiring callers to understand lifecycle internals or call methods in a hidden order.

**Extend without rewriting.** When a real new variant arrives (spawn strategy, entity behavior, punishment type), extend
the existing domain model and preserve shared invariants. A new variant should normally require a localized addition,
not copied workflows across listeners or commands. Do not prebuild a registry, factory, or strategy for hypothetical
variants.

**Integrate through contracts.** Expose the smallest stable operations and domain types that consumers can compose. Hide
mutable collections, storage layout, scheduler mechanics, and implementation classes. Prefer explicit outcomes when
callers need to distinguish expected states; `void` is correct for a command with no meaningful result.

**Design for the next engineer.** Check real callers before changing an API. Make valid use obvious, error handling
actionable, and thread/lifecycle requirements visible. Preserve a simple path for common use without blocking
demonstrated advanced use.

**Change without breaking.** Add, do not rename or remove. Before you change a public class, method, constructor, field,
enum value, or configuration format, search its usages. When consumers exist, migrate them in the same change. When none
exist, change it freely and build no compatibility layer.

**Self-documenting.** Names and types are the documentation. Public and reusable APIs get Javadoc: behavior,
assumptions, return and absence meaning, failure behavior, thread or lifecycle requirements, one usage example when
non-obvious. Use the `generate-docs` skill.

---

## 7. Modern Java

Use the Java release configured in `pom.xml` or the build tool as the source of truth; do not assume the runtime or
language level. Before hand-writing something, check whether the supported language or standard library already does it.

Use a construct when it improves clarity, correctness, safety, or the API. NEVER use it because it is new.

Prefer when they fit:

- Pattern matching for `instanceof`, and in `switch`.
- Record patterns when destructuring reads better.
- Records for data carriers. Sealed types for intentionally closed domains.
- Switch expressions when they say it more clearly.
- Text blocks for genuine multi-line text.
- Unnamed variables `_` when a value is intentionally unused.
- `getFirst()` and `getLast()` for first and last access. Indexed access when the index matters.
- Immutable collection factories for immutable snapshots.

```java
return switch(type){
        case LEFT_CLICK ->

handleLeft();
    case RIGHT_CLICK ->

handleRight();

default ->

handleAny();
};
```

**Streams.** Use for clear transformation or aggregation. NEVER convert logic with early exits, mutation, or several
conditions into a stream. Use a loop.

**Virtual threads.** They do not change Bukkit's threading rules. Use them only for isolated blocking I/O that touches
no Bukkit API.

**Preview features.** NEVER introduce one unless the project explicitly enables it. When unsure about a language
feature,
verify it first.

---

## 8. Spigot

The target is **Spigot, not Paper**. Read the exact Spigot API and Minecraft versions from the build configuration; do
not infer them from the JDK version.

- Verify every Bukkit and Spigot API against the Spigot dependency this project uses. NEVER assume an API exists because
  it appears in Paper docs, a Paper plugin, or a tutorial.
- NEVER introduce a Paper-only event, scheduler, or API.
- **Threading.** Bukkit state and any API that needs the main server thread stays on it. Keep asynchronous work
  isolated. Return to the Bukkit scheduler before touching thread-confined server state. Cancel lifecycle-bound tasks on
  shutdown or destruction; never assume an asynchronous callback still has a live player, world, or entity.
- **Deprecated APIs.** NEVER introduce one when a current replacement exists. When none exists, document the reason
  beside the usage.
- **NMS and server internals.** Prefer the public Spigot API. NEVER add mapping, remapping, or reobfuscation workflows.
  NEVER parse the Minecraft version from NMS package names. When internals are unavoidable, isolate them behind one
  project abstraction.

---

## 9. Nullability and absence

- NEVER let `null` cross a public API without documenting it or annotating it.
- Return `Optional` for "may be absent" results. NEVER use it for fields or parameters.
- A nullable local that is created only when needed is fine (see the `eligible` loop in section 6).
- NEVER add a helper only to remove that pattern.

---

## 10. Validation and errors

- Validate untrusted input where it enters the system: commands, configuration, packets, player-supplied text.
- Keep domain invariants in the object or method that owns them. NEVER re-validate what a trusted internal abstraction
  already guarantees.
- Make invalid states hard to represent.
- NEVER swallow an exception. Throw typed, useful errors (section 6).

---

## 11. Performance

Correct and readable first. Measure before you optimise.

- Avoid copies and allocations when ownership and lifecycle are already clear.
- Use immutable snapshots when immutability gives a useful guarantee.
- A small allocation is not a performance problem.
- NEVER sacrifice readability for an optimisation without a workload that needs it.

---

## 12. Testing

Read the existing tests first. Follow their conventions.

Test observable behavior and invariants, not implementation. Test an API the way another part of the plugin will call
it.

Priority:

1. Validation and permission checks at every entry point. Must reject malformed and unauthorized input.
2. Logic with real branching.
3. Failure behavior. Callers depend on it.

Run the relevant build and tests before you finish. NEVER claim a build or test passed unless you ran it.

---

## 13. Version control

NEVER commit, stage, or push without explicit authorization. If asked to commit, show the exact message and wait for
approval. Reading diffs and drafting messages is fine.

---

## 14. Before you finish

- [ ] Read the relevant code first; checked `git status` / `git diff` and preserved existing changes.
- [ ] Did only what was asked. Reused what exists.
- [ ] No type written where the compiler infers it.
- [ ] No abbreviations. Every name is a whole word.
- [ ] No slop or needless control-flow complexity from section 5.
- [ ] New or changed APIs pass section 6: clear call site, small surface, explicit contract, understandable to other
  engineers.
- [ ] New variants extend one domain owner without duplicated workflows or speculative abstractions.
- [ ] Shared contracts hide internals; changes remain local and consumers can migrate safely.
- [ ] Spigot APIs verified against the real target. No Paper-only API. No unnecessary deprecated API.
- [ ] Threading is compatible with Bukkit's model.
- [ ] No preview feature added by accident.
- [ ] Public API consumers checked before any change.
- [ ] Build and relevant tests run.
- [ ] No unrelated changes. No commit.

````