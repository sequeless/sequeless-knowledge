# 0002. The LLM sits behind a provider-neutral inference port, and this repo ships no provider adapter

- Status: accepted
- Date: 2026-11-26 (decided in sequeless/sequeless-prototype#25 and the Navigator's spec 19 batches; committed with spec 19, #47)

## Context

The choice of LLM provider is the next map's decision (#25, "Not decided here"). The knowledge use case
still needs an inference step, and the provider choice must not leak into the core.

## Decision

We will put the model behind one provider-neutral port, `InferencePort`, in `knowledge-core`'s SPI:

- The port is one method: `String complete(String systemPrompt, String userPrompt)`. It has no message
  type, no streaming, no tool calling and no conversation state.
- The use case builds both prompts from the Ontology's type list and the retrieved objects. The
  prompt wording is internal to the core and may change without a contract change. The `String`
  return is the one-way door: a structured reply would be a new method or a new port.
- The use case parses the cited ids out of the reply and keeps only ids the retrieval returned.
  The parse is a private core helper, not part of the port.
- `knowledge-core` depends on no provider SDK. Provider adapters (`knowledge-<provider>-adapter`) are
  named and built when the provider is chosen, in a later spec. This repo ships none.

## Consequences

- The host supplies the `InferencePort` bean. `knowledge-spring-boot-autoconfig` wires `AnswerQuestion`
  only when one is present.
- The contract test in `knowledge-testkit` is the gate for every future adapter.
- Object values are untrusted text inside the prompt. The citation filter is the only guard against
  prompt injection, and the prompt does not limit it further.
