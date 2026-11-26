package org.sequeless.knowledge.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.sequeless.kernel.identity.Principal;
import org.sequeless.kernel.identity.Scope;
import org.sequeless.kernel.identity.TenantId;
import org.sequeless.query.field.api.ClassRef;
import org.sequeless.query.field.api.LocalizedText;
import org.sequeless.query.field.api.TraversalConfig;
import org.sequeless.query.field.api.VersionPolicy;

class AnswerRequestTest {

    private static final Scope SCOPE = new Scope(new TenantId("tenant-a"), new Principal("alice", Set.of()));
    private static final ClassRef ROOT = new ClassRef(
            UUID.randomUUID(), "invoice", "https://example.org/Invoice", new LocalizedText("Invoice", "en"));

    private static AnswerRequest request(int maxObjects) {
        return new AnswerRequest(
                SCOPE,
                "Which invoices are open?",
                ROOT,
                new VersionPolicy.Union(),
                TraversalConfig.defaults(),
                Optional.<Locale>empty(),
                maxObjects);
    }

    @Test
    void accepts_a_valid_request() {
        AnswerRequest request = request(10);

        assertThat(request.question()).isEqualTo("Which invoices are open?");
        assertThat(request.maxObjects()).isEqualTo(10);
    }

    @Test
    void rejects_a_null_scope() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AnswerRequest(
                        null, "q", ROOT, new VersionPolicy.Union(), TraversalConfig.defaults(), Optional.empty(), 1))
                .withMessageContaining("scope");
    }

    @Test
    void rejects_a_null_question() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AnswerRequest(
                        SCOPE, null, ROOT, new VersionPolicy.Union(), TraversalConfig.defaults(), Optional.empty(), 1))
                .withMessageContaining("question");
    }

    @Test
    void rejects_a_blank_question() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AnswerRequest(
                        SCOPE, "   ", ROOT, new VersionPolicy.Union(), TraversalConfig.defaults(), Optional.empty(), 1))
                .withMessageContaining("question");
    }

    @Test
    void rejects_a_null_root() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AnswerRequest(
                        SCOPE, "q", null, new VersionPolicy.Union(), TraversalConfig.defaults(), Optional.empty(), 1))
                .withMessageContaining("root");
    }

    @Test
    void rejects_a_null_version_policy() {
        assertThatNullPointerException()
                .isThrownBy(() ->
                        new AnswerRequest(SCOPE, "q", ROOT, null, TraversalConfig.defaults(), Optional.empty(), 1))
                .withMessageContaining("versionPolicy");
    }

    @Test
    void rejects_a_null_traversal() {
        assertThatNullPointerException()
                .isThrownBy(
                        () -> new AnswerRequest(SCOPE, "q", ROOT, new VersionPolicy.Union(), null, Optional.empty(), 1))
                .withMessageContaining("traversal");
    }

    @Test
    void rejects_a_null_locale_wrapper() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AnswerRequest(
                        SCOPE, "q", ROOT, new VersionPolicy.Union(), TraversalConfig.defaults(), null, 1))
                .withMessageContaining("locale");
    }

    @Test
    void rejects_maxObjects_of_zero() {
        assertThatIllegalArgumentException().isThrownBy(() -> request(0)).withMessageContaining("maxObjects");
    }

    @Test
    void accepts_maxObjects_of_one() {
        assertThat(request(1).maxObjects()).isEqualTo(1);
    }

    @Test
    void accepts_maxObjects_of_500() {
        assertThat(request(500).maxObjects()).isEqualTo(500);
    }

    @Test
    void rejects_maxObjects_of_501() {
        assertThatIllegalArgumentException().isThrownBy(() -> request(501)).withMessageContaining("maxObjects");
    }
}
