# sequeless-knowledge

Read-only knowledge deduction over Sequeless data. A question goes to the Query side, which returns the
matching stored objects. Those objects, with the Ontology's type list, go to an inference port the host
supplies. The answer cites the object ids it used, and only ids the retrieval returned are kept.

This repo is a library. It has no write port and no behaviour source, and it ships no inference provider:
the host supplies an `InferencePort` (spec 19, [ADR 0002](docs/adr/0002-inference-port-has-no-adapter-here.md)).

## Modules

| Module | Purpose |
|---|---|
| `knowledge-core` | `AnswerQuestion`, the `InferencePort` SPI, and the prompt and citation rules |
| `knowledge-testkit` | the `InferencePort` contract and a recording fake |
| `knowledge-spring-boot-autoconfig` | wires `AnswerQuestion` when the host supplies an `InferencePort` |

The specification is in [docs/extraction/spec-19-sequeless-knowledge.md](docs/extraction/spec-19-sequeless-knowledge.md).
Decisions are in [docs/adr](docs/adr).

## Local setup

The parent POM and the released Sequeless artifacts come from GitHub Packages, so export a token with
`read:packages` as `GITHUB_PACKAGES_TOKEN` before any `mvn` run. The `github` server in `~/.m2/settings.xml`
reads it as `<password>${env.GITHUB_PACKAGES_TOKEN}</password>`; `sequeless-build`'s README has the full entry.

If `spotless:check` fails at `verify`, run `mvn spotless:apply`, then `mvn verify` again.
