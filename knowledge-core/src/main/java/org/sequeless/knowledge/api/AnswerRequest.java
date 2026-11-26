package org.sequeless.knowledge.api;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.sequeless.kernel.identity.Scope;
import org.sequeless.query.field.api.ClassRef;
import org.sequeless.query.field.api.TraversalConfig;
import org.sequeless.query.field.api.VersionPolicy;

/**
 * One question to answer over the stored objects reachable from {@code root}.
 *
 * <p>The host supplies every input; there are no defaults. The {@code scope} is passed through to
 * the Query side, which checks it at its own boundary.
 *
 * @param scope the tenant and principal the question is asked on behalf of; never {@code null}
 * @param question the natural-language question; never {@code null} and never blank
 * @param root the class whose objects may be retrieved; never {@code null}
 * @param versionPolicy how to reconcile the ontology versions in scope; never {@code null}
 * @param traversal how far the Field scope reaches from {@code root}; never {@code null}
 * @param locale the locale to prefer for localized labels, or {@link Optional#empty()} for none;
 *     never {@code null} as a wrapper
 * @param maxObjects the most objects to retrieve and show to the model; between 1 and 500 inclusive
 */
public record AnswerRequest(
        Scope scope,
        String question,
        ClassRef root,
        VersionPolicy versionPolicy,
        TraversalConfig traversal,
        Optional<Locale> locale,
        int maxObjects) {

    /**
     * Validates every component.
     *
     * @throws NullPointerException if a reference component is {@code null}
     * @throws IllegalArgumentException if {@code question} is blank, or {@code maxObjects} is not
     *     between 1 and 500 inclusive
     */
    public AnswerRequest {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(question, "question must not be null");
        if (question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        Objects.requireNonNull(root, "root must not be null");
        Objects.requireNonNull(versionPolicy, "versionPolicy must not be null");
        Objects.requireNonNull(traversal, "traversal must not be null");
        Objects.requireNonNull(locale, "locale must not be null (use Optional.empty())");
        if (maxObjects < 1 || maxObjects > 500) {
            throw new IllegalArgumentException("maxObjects must be between 1 and 500 inclusive");
        }
    }
}
