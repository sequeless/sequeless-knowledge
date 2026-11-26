package org.sequeless.knowledge.testkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sequeless.knowledge.spi.InferencePort;

/**
 * An {@link InferencePort} that records every call and returns one scripted reply.
 *
 * <p>It follows the port contract: a {@code null} argument throws {@link NullPointerException} and
 * is not recorded. It is not thread-safe; a test drives it from one thread.
 */
public final class FakeInferencePort implements InferencePort {

    private final String reply;
    private final List<Call> calls = new ArrayList<>();

    /**
     * @param reply the text every call returns; never {@code null}
     * @throws NullPointerException if {@code reply} is {@code null}
     */
    public FakeInferencePort(String reply) {
        this.reply = Objects.requireNonNull(reply, "reply must not be null");
    }

    /**
     * @param reply the text every call returns; never {@code null}
     * @return a fake that returns {@code reply} and records its calls
     * @throws NullPointerException if {@code reply} is {@code null}
     */
    public static FakeInferencePort replying(String reply) {
        return new FakeInferencePort(reply);
    }

    /**
     * Records the call and returns the scripted reply.
     *
     * @throws NullPointerException if an argument is {@code null}; the call is not recorded
     */
    @Override
    public String complete(String systemPrompt, String userPrompt) {
        Objects.requireNonNull(systemPrompt, "systemPrompt must not be null");
        Objects.requireNonNull(userPrompt, "userPrompt must not be null");
        calls.add(new Call(systemPrompt, userPrompt));
        return reply;
    }

    /**
     * @return the calls made so far, in the order they were made; an unmodifiable copy
     */
    public List<Call> calls() {
        return List.copyOf(calls);
    }

    /**
     * One recorded call to {@link #complete(String, String)}.
     *
     * @param systemPrompt the system prompt the call passed
     * @param userPrompt the user prompt the call passed
     */
    public record Call(String systemPrompt, String userPrompt) {}
}
