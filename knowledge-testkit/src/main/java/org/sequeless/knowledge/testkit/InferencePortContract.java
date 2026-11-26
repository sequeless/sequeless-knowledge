package org.sequeless.knowledge.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;
import org.sequeless.knowledge.spi.InferencePort;

/**
 * The contract every {@link InferencePort} adapter must pass.
 *
 * <p>An adapter's test class extends this class and supplies its port by overriding {@link
 * #newPort()}. JUnit runs the contract's tests against that port, so the adapter is checked
 * against the same rules as every other adapter:
 *
 * <pre>{@code
 * class MyModelAdapterTest extends InferencePortContract {
 *     @Override
 *     protected InferencePort newPort() {
 *         return new MyModelAdapter(...);
 *     }
 * }
 * }</pre>
 *
 * <p>The contract checks the null rules, and that a valid call returns a reply. The valid-call test
 * calls {@code complete()}, so {@link #newPort()} must return a port that answers without a live
 * model: a stub, or a local endpoint. A test that needs a real model belongs in the adapter's own
 * test class.
 */
public abstract class InferencePortContract {

    /**
     * Supplies the port under test. JUnit calls this factory once for each test method, on a new
     * subclass instance, so each test gets a fresh port.
     *
     * @return a new port, ready for one call
     */
    protected abstract InferencePort newPort();

    /** A {@code null} system prompt throws {@link NullPointerException}. */
    @Test
    public void null_system_prompt_throws_null_pointer_exception() {
        InferencePort port = newPort();

        assertThatNullPointerException().isThrownBy(() -> port.complete(null, "user prompt"));
    }

    /** A {@code null} user prompt throws {@link NullPointerException}. */
    @Test
    public void null_user_prompt_throws_null_pointer_exception() {
        InferencePort port = newPort();

        assertThatNullPointerException().isThrownBy(() -> port.complete("system prompt", null));
    }

    /** A valid call returns a non-null reply. */
    @Test
    public void valid_call_returns_a_non_null_reply() {
        InferencePort port = newPort();

        String reply = port.complete("system prompt", "user prompt");

        assertThat(reply).isNotNull();
    }
}
