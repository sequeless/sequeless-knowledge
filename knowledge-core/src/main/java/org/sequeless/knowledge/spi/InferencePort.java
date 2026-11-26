package org.sequeless.knowledge.spi;

/**
 * The outbound port to a language model. The host implements it; this library only calls it.
 *
 * <p>Contract: both arguments must be non-{@code null} (a {@code null} argument throws
 * {@link NullPointerException}). The returned reply is the model's text and is never {@code null}.
 * The use case makes at most one call per answer, and none when nothing is retrieved. The port is
 * not streaming, makes no tool calls and keeps no conversation state. Exceptions propagate to the
 * caller: the implementation does not retry, and the core does not catch them.
 */
public interface InferencePort {

    /**
     * Sends one prompt pair to the model and returns its reply.
     *
     * @param systemPrompt the instructions for the model; never {@code null}
     * @param userPrompt the numbered objects and the question; never {@code null}
     * @return the model's reply text; never {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    String complete(String systemPrompt, String userPrompt);
}
