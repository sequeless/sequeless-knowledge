# Spec 19: `sequeless-knowledge`, read-only knowledge deduction

Issue: [#47](https://github.com/sequeless/sequeless-prototype/issues/47). Map: [#11](https://github.com/sequeless/sequeless-prototype/issues/11). Plan: [#23](https://github.com/sequeless/sequeless-prototype/issues/23). Placement: [#25](https://github.com/sequeless/sequeless-prototype/issues/25) (its Resolution is the decision this spec implements). Earlier specs it waits for: [spec 14](https://github.com/sequeless/sequeless-query/blob/main/docs/extraction/spec-14-sequeless-query.md) ([#42](https://github.com/sequeless/sequeless-prototype/issues/42), closed).

Status: draft, written 2026-11-26 against `sequeless-prototype` `origin/main` `8389ec6` (cycle 12 base). This document rides in the bootstrap PR (branch `spec-19-bootstrap`), the first code PR of `sequeless/sequeless-knowledge`, not in a docs-only PR. Decisions marked **Navigator** were settled in Navigator session `a5893be1ec70425abb1e8d214a666438` (batch 1: five questions; batch 2: three, section 10).

## 1. Identity, stage and dependencies

- **Stage 4 and after.** Spec 19 of 19. A new read-only library repo, placed after the query stage (#25, Resolution). Spec 17 (#45) gains a dependency on it. The critical-path cost is an assumption, since no source gives an effort estimate.
- **Repo.** `sequeless/sequeless-knowledge`, public, created from `sequeless/sequeless-template` on 2026-11-26 (the agent's creation call was pre-approved by the parent and succeeded). The initial commit is the template tree, `e33306c`.
- **Waits for** (checked with `gh issue view`): spec 1 (`sequeless-build`), spec 3 (the prototype's adoption of the parent), spec 14 ([#42](https://github.com/sequeless/sequeless-prototype/issues/42), `sequeless-query`, closed). All three are closed.
- **Blocks** spec 17 (#45), which gains knowledge as a dependency. Nothing else.
- **Prototype.** This spec makes no prototype PR. The prototype keeps its old behaviour until spec 17 (ruling of the parent, 2026-11-26). The prototype has no reasoning over instances today (DR-05; #25 "What is already placed"), so the knowledge repo has no prototype behaviour to match. The divergence risk that remains is recorded in section 11 (D-1).
- **This spec does not:**
  - change `sequeless-query`, `sequeless-ontology`, `sequeless-filter` or `sequeless-kernel`;
  - change `sequeless-build` (the BOM entry waits for the parent's approval, section 6);
  - add a provider adapter (section 5, ADR 0002).

## 2. Moves

No source file moves into this repo. It is new code. The PROVENANCE content is in `PROVENANCE.md`, in the shape of spec 15's:

- **Created from:** `sequeless/sequeless-template` at `ed229b6` (its `main` on 2026-11-26). Template-only files are removed in this PR: `template-core/`, `scripts/init-repo.sh`, `docs/specs/stage-0-sequeless-template.md` and `.github/workflows/template-smoke.yml`. The `init` target of the `Makefile` is removed with `scripts/init-repo.sh`.
- **Source repo, SHA and paths:** none moved. The decisions come from `sequeless/sequeless-prototype` issues #25, #23 and #47 at `8389ec6`.
- **Ports consumed (not copied):** `QueryService` (query-core, `org.sequeless.query.api`, released 0.2.1); `SchemaSource` and `Schema` (schema-contract, `org.sequeless.ontology.schema`, released 0.2.0); `Scope`, `TenantId`, `Principal` (kernel-identity 0.1.1); `ObjectId`, `Value` (kernel-values 0.1.1); and the query-field api types `FieldScopeRequest`, `ClassRef`, `VersionPolicy`, `TraversalConfig` (query-field 0.1.1).

## 3. Prerequisite refactors

None. Nothing in another repo changes for this spec. Every prerequisite is a released artifact, checked against its `origin/main` manifest on 2026-11-26: kernel-identity and kernel-values 0.1.1, schema-contract 0.2.0, query-core 0.2.1, query-field 0.1.1 (the BOM lists the same versions).

## 4. Package renames

None. The packages are new, and they follow the query repo's layout (`api`, `spi`, `internal`) and the expression and automation precedent for the testkit and the autoconfiguration:

| Module | Packages |
|---|---|
| `knowledge-core` | `org.sequeless.knowledge.api` (contract types and the factory), `org.sequeless.knowledge.spi` (`InferencePort`), `org.sequeless.knowledge.internal` (use case implementation, prompts, citations, value rendering) |
| `knowledge-testkit` | `org.sequeless.knowledge.testkit` |
| `knowledge-spring-boot-autoconfig` | `org.sequeless.knowledge.autoconfigure` |

## 5. Modules and their dependencies

Three modules, as #25 Resolution lists them. There is no provider adapter in this spec (ADR 0002).

| Module | Depends on (main) | Test scope |
|---|---|---|
| `knowledge-core` | kernel-identity 0.1.1, kernel-values 0.1.1, schema-contract 0.2.0 (`api` and `spi`), query-core 0.2.1, query-field 0.1.1 (declared directly) | query-testkit 0.2.1, query-field-testfixtures 0.2.0, JUnit, ArchUnit, AssertJ |
| `knowledge-testkit` | knowledge-core 0.1.0; JUnit and AssertJ are main-scope, because the contract is an abstract JUnit test | JUnit, ArchUnit, AssertJ |
| `knowledge-spring-boot-autoconfig` | knowledge-core 0.1.0, schema-contract, query-core; Spring Boot autoconfigure and Spring context (the Spring Boot 4.1.1 BOM import, as automation pins it) | knowledge-testkit 0.1.0, kernel-identity, kernel-values, query-field (test-only: the QueryService stub names its api types), Spring Boot test and starter test, ArchUnit, AssertJ |

**Query surface (Navigator, batch 1, Q2 and the challenge).** Knowledge consumes the host-facing `QueryService`, not `BrowsePort` or `ReadPort`. `QueryService.browse` takes a `FieldScopeRequest`, a query-field api record, and that is why `query-field` is a direct dependency. The rule is: core may import `org.sequeless.query.api` and `org.sequeless.query.field.api` (types only). It must not import `org.sequeless.query.spi`, `org.sequeless.query.internal`, `org.sequeless.query.field.spi` or `org.sequeless.query.field.internal`, nor Spring. Option B (a knowledge-owned port with the `QueryService` adapter in the autoconfiguration module) was rejected: it adds a port with no second implementer.

**Schema ports.** Core imports `org.sequeless.ontology.schema.api` (`Schema`, `TypeDefinition`) and `org.sequeless.ontology.schema.spi` (`SchemaSource`, the decided port).

**Deviation from #25's wording.** #25 says the repo consumes "Browse and Read". This spec consumes the Query side's browse only (Navigator, batch 2, Q3). `QueryService.read` is not used, because every cited object was retrieved under the caller's `Scope` in the same call. Adding read later is additive (ADR 0001).

## 6. Release config (first version, tags, BOM change)

- **First version.** 0.1.0 for each of the three modules. Not a major release, so the parent's standing pre-approval applies.
- **Mechanism.** release-please in manifest mode with the `maven-workspace` plugin, `initial-version` 0.1.0, `always-bump-patch` and `skip-snapshot` (the automation config, section 6 of spec 15). The manifest starts `{}`; the first release writes the entries.
- **Tags.** `<module>-v0.1.0`, `include-component-in-tag` true.
- **Deploy namespace.** Each module POM sets `sequeless.github.repository` to `sequeless-knowledge`. Without it the parent deploys to `sequeless-build` and gets a 403.
- **BOM change (BLOCKED, owner-of-BOM).** `sequeless-bom` in `sequeless/sequeless-build` gains the three knowledge artifacts at 0.1.0 after their release, and a `KnowledgeBomContractTest` in `bom-contracts` runs the testkit's contract. **Nothing in `sequeless-build` changes in this PR.** The parent must approve the edit first (the parent's ruling, cycle 12). This is the only DoD item that waits on the parent.
- **Release PR.** Pre-approved (minor or patch). Merged after its checks pass, with no `--admin` bypass of a red check.

## 7. CI and workflows

- `ci.yml` and `release.yml` call the reusable workflows of `sequeless/sequeless-build` pinned to `workflows-v0.1.3` (`8cebaa03c8ba188b31f6c0c7b4c4bb0e2c9dacd2`), the latest tag at PR time (`git ls-remote --tags`). The template's `workflows-v0.1.2` pin is replaced. The template's `release.yml` guard against the template repo is removed (this repo is released).
- `codeql.yml` and `pr-title.yml` come from the template unchanged.
- **Gate, per PR.** `source ~/.bashrc.ext && export JAVA_HOME=/opt/sdkman/candidates/java/25.0.3-tem && mvn spotless:apply && mvn -B -Prelease verify`, run under `flock -w 1800 $HOME/.mvn-build.lock`, with `MAVEN_OPTS=-Xmx1g` and `-T 1`. `~/.bashrc.ext` (the Packages token) is sourced in the same command as every `mvn` run.
- **Java level.** The parent compiles at `maven.compiler.release` 21 (sequeless-build-parent 0.1.3, its default). The gate runs on JDK 25, so the release is 21 and the build JDK is 25.

## 8. ADRs

- **ADR 0001, "Knowledge is read-only: it reads the Query side and the Schema, and has no write port"** (`docs/adr/0001-knowledge-is-read-only-over-the-query-side.md`, accepted, decided in #25).
- **ADR 0002, "The LLM sits behind a provider-neutral inference port, and this repo ships no provider adapter"** (`docs/adr/0002-inference-port-has-no-adapter-here.md`, accepted, decided in #25 and the Navigator batches).

## 9. Definition of done

Copied from #47, with the status at this PR:

- "The new repo's CI is green." **Open until the bootstrap PR's CI runs green.**
- "The prototype's CI is green against released artifacts (never a SNAPSHOT on main)." **Not affected by a code change here: the prototype makes no PR in spec 19.** Checked by a read-only build of the prototype at `origin/main` in a throwaway checkout, recorded in the handoff; no prototype PR.
- "The BOM's CI, which runs every testkit contract, is green." **Blocked** on the BOM entry (section 6).
- "The release and the BOM re-release are done." **Open.** The release follows the merge of this PR; the BOM re-release waits for the parent's approval.
- "For cut-over specs: the prototype deleted the moved code in a commit that names the target repo and the import commit's SHA." **Not applicable.** Nothing moves.

## 10. Navigator decisions and open items

**Batch 1 (session `a5893be1ec70425abb1e8d214a666438`, five questions):**

| # | Question | Decision | Reason |
|---|---|---|---|
| 1 (`set_plan`) | Scope of the first code cut | **Option B**: skeleton plus one read-only use case, `answer(Scope, question)` | The DoD needs a testkit contract; a use case is the consumer of the inference port |
| 2 | Query surface | **`QueryService`** (api), not `BrowsePort` or `ReadPort` | The version guard and query checks stay at the Query boundary, and the host checks `AuthorizationPort` there |
| 3 | Dependency on query-field | **Option A**: core may import query-field api types only; query-field 0.1.1 declared directly | `FieldScopeRequest` is a query-field type; a knowledge-owned port (B) has no second implementer |
| 4 | Inputs for the root class, version policy and traversal | **Option A with one change**: `AnswerRequest` bundles them with the scope, the question, the locale and `maxObjects`; the host supplies every input; no defaults in core | Root resolution from question text (C) needs heuristics or an LLM call; a global setting (B) is wrong for callers with different version policies |
| 5 | `InferencePort` shape, citations and retrieval bound | **One method**, `String complete(String systemPrompt, String userPrompt)`; citations parsed by a private core helper and filtered to retrieved ids; `maxObjects` in `AnswerRequest`; empty retrieval gives `answered = false` with no inference call; port exceptions propagate | The smallest provider-neutral contract; a `String` return is the one-way door |

**Batch 2 (same session, four questions):**

| # | Question | Decision | Reason |
|---|---|---|---|
| 1 | Prompt content | **Option A**: a fixed instruction text; one `<iri> - <label>` line per type; retrieved objects numbered `[n] id= type= state=`; properties sorted by IRI; `Sources: <id>, <id>` instruction. Prompt wording is internal; tests assert invariants only | JSON and property definitions add tokens and a serialization dependency with no requirement |
| 2 | Value rendering | **Option A**: one private core helper, an exhaustive switch over the sealed `Value` variants, no default branch; references render their UUID text | `Value.toString()` prints record text such as `TextValue[value=...]`; a `kernel-values` change would need a kernel release |
| 3 | `QueryService.read` | **Browse only** (see section 5) | A read per citation adds N reads and a second visibility check, with no new guarantee |
| 4 | Schema read before or after the empty-retrieval check | **Before**: the order is schema read, browse, inference. `NoConsistentOntologyException` propagates even when retrieval is empty; an empty retrieval returns `answered = false` with zero inference calls | A missing or inconsistent ontology is a configuration fault and should fail the same way on every call. The stricter contract can be relaxed later without breaking callers |

**Checks the driver made against source (recorded in the Navigator session).**

- `Page(number, size)` is 0-based; `size` is 1 to 500, so `maxObjects` is validated to the same range.
- `Scope` is `record Scope(TenantId tenantId, Principal principal)`, with a non-null principal, so `Optional.of(scope.principal())` is always present.
- `Schema.types()` is the type list; `TypeDefinition.iri()` and `label()` are the lines of the system prompt.
- `ObjectView.id()` is an `ObjectId`; its record `toString` is `ObjectId[value=...]`, so the prompt renders `id.value().toString()`.
- `QueryServiceFactory.create` takes four ports: `FieldSource`, `BrowsePort`, `ReadPort` and `IndexPreparationPort`. The test setup needs all four.
- The Navigator's rule wording `org.sequeless.schema..` is wrong. The packages are `org.sequeless.ontology.schema.api` and `.spi`.
- The Navigator's `#24 point 4` citation for "no port before a consumer" does not match issue #24: its body has points (a) to (d), and no consumer rule. The owner-confirmation carry-forward was routed to the Navigator, not the owner, and the Navigator's batch 1 decision stands.
- The prototype's `BrowsePort` (in its own `org.sequeless.spi.query` package) is not the query-core `BrowsePort`. This spec uses neither.

**Open items:**

- **O-1 (parent, BOM).** The `sequeless-build` BOM entry waits for the parent's approval (section 6). Nothing in `sequeless-build` changes until then.
- **O-2 (Navigator, consumer).** The `InferencePort` is consumed by the use case in this repo. The "no port before a consumer" rule has no source in #24. The Navigator's batch 1 decision (option B) stands. If a later review disagrees, it goes to the Navigator.
- **O-3 (autoconfiguration order).** `KnowledgeAutoConfiguration` must run after the query autoconfiguration, so that the host's `QueryService` bean exists when the knowledge bean's conditions are evaluated. It names that class with `afterName` and does not depend on the query artifact. Unverified until the context test runs against the query autoconfiguration's class name.

## 11. Hazards and risks

- **H1. The query-field api exception.** Core imports `org.sequeless.query.field.api`, so the rule is types only. A later change to a query-field api record is a change to this repo's public signature. Reversible while the Query types stay out of knowledge's own public types (they appear only in `AnswerRequest`'s inputs).
- **H2. Prompt injection.** Object values are untrusted text. They are rendered raw, with no escaping and no length cap. The citation filter is the only guard. Do not claim more.
- **H3. The inference port is the one-way door.** `String complete(String, String)` is public. A structured reply later is a new method or a new port, not a change to this one.
- **H4. The prototype and this repo differ until spec 17 (D-1).** The prototype has no reasoning over instances, so the knowledge repo adds behaviour rather than changing it. The prototype's Browse and Read ports (`org.sequeless.spi.query`) are different types from query-core's. Spec 17 is where the prototype starts to consume released query artifacts, and it is the first point where the two can be compared.
- **H5. Maven resolution.** The parent and the released artifacts come from GitHub Packages (`~/.bashrc.ext` must be sourced in the same command). query-testkit 0.2.1 and query-field-testfixtures 0.2.0 are test-scope only.
- **H6. The first release.** The three modules release at 0.1.0. The released API is additive from then on (H-notes of spec 15 apply: exceptions and records stay additive).

## 12. As built

Counts from the final `spotless:apply verify` over the three modules (JDK 25, release 21, 2026-11-26). Each module was re-verified after its last change.

| Module | Tests | Notes |
|---|---|---|
| `knowledge-core` | 75 | `AnswerQuestion` (use case), `AnswerRequest`, `Answer`, `AnswerQuestionFactory`, the `InferencePort` SPI, prompt and citation rules, value rendering, boundary rules (the factory may reach internal only through `DefaultAnswerQuestion`) |
| `knowledge-testkit` | 11 | `InferencePortContract` (abstract JUnit contract, adapters extend it), `FakeInferencePort` (recording fake), boundary rules |
| `knowledge-spring-boot-autoconfig` | 8 | `KnowledgeAutoConfiguration` (wires `AnswerQuestion` only when `QueryService`, `SchemaSource` and `InferencePort` beans exist, and backs off to a host bean); context tests use stubs, not query-core's real autoconfiguration (O-3 stays open); boundary rules |
