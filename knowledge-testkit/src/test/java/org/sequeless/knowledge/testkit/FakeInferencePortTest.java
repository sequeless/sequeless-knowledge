package org.sequeless.knowledge.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;
import org.sequeless.knowledge.testkit.FakeInferencePort.Call;

class FakeInferencePortTest {

    @Test
    void records_calls_in_order_and_returns_the_scripted_reply() {
        FakeInferencePort port = FakeInferencePort.replying("scripted");

        String first = port.complete("system A", "user A");
        String second = port.complete("system B", "user B");

        assertThat(first).isEqualTo("scripted");
        assertThat(second).isEqualTo("scripted");
        assertThat(port.calls()).containsExactly(new Call("system A", "user A"), new Call("system B", "user B"));
    }

    @Test
    void calls_is_an_unmodifiable_copy() {
        FakeInferencePort port = FakeInferencePort.replying("scripted");
        port.complete("system", "user");

        var snapshot = port.calls();
        port.complete("system", "user again");

        assertThat(snapshot).hasSize(1);
        assertThat(port.calls()).hasSize(2);
    }

    @Test
    void a_null_system_prompt_throws_naming_the_argument_and_records_nothing() {
        FakeInferencePort port = FakeInferencePort.replying("scripted");

        assertThatNullPointerException()
                .isThrownBy(() -> port.complete(null, "user"))
                .withMessageContaining("systemPrompt");
        assertThat(port.calls()).isEmpty();
    }

    @Test
    void a_null_user_prompt_throws_naming_the_argument_and_records_nothing() {
        FakeInferencePort port = FakeInferencePort.replying("scripted");

        assertThatNullPointerException()
                .isThrownBy(() -> port.complete("system", null))
                .withMessageContaining("userPrompt");
        assertThat(port.calls()).isEmpty();
    }

    @Test
    void a_null_reply_is_rejected_at_construction() {
        assertThatNullPointerException()
                .isThrownBy(() -> new FakeInferencePort(null))
                .withMessageContaining("reply");
    }
}
