# 0001. Knowledge is read-only: it reads the Query side and the Schema, and has no write port

- Status: accepted
- Date: 2026-11-26 (decided in sequeless/sequeless-prototype#25; committed to this repo with spec 19, #47)

## Context

Reasoning over instances, with an LLM that has access to the data, needs to read stored objects and the
ontology's type list. It could also write deduced facts back to the Store, as automation does. Writing
would need a Principal of its own, a write port in the Store's contract, and a way for the host to bind it.

No consumer for a write exists today, and the placement decision (#25) gives the knowledge side no write port.

## Decision

We will keep `sequeless-knowledge` read-only. It answers questions over stored objects and never writes.

- It reads stored objects through the Query side's host-facing `QueryService`, and only its `browse`
  operation. Every cited object was retrieved under the caller's `Scope` in the same call.
- It reads the ontology's type list through `SchemaSource` (schema-contract).
- It has no outbound write port, no `BehaviourSource`, and no dependency on store-core, automation's
  ports or sequeless-expression.
- It passes the caller's `Scope` through unchanged. The host checks `AuthorizationPort` at the Query
  side's API boundary, not here.

If write-back is decided later, it is an additive outbound port in knowledge-core, bound by the host to
store-core's inbound port, acting as a system knowledge Principal (the pattern of prototype issue #20,
point 5).

## Consequences

- The knowledge repo cannot change stored data, so its effects are limited to its answers.
- Only the browse operation of `QueryService` is used. Read is available and may be added later as an
  additive use case, which is a difference from the wording of #25.
- Adding a write port later is a new decision with its own ADR, and needs a consumer.
