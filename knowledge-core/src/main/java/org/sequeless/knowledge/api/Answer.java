package org.sequeless.knowledge.api;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sequeless.kernel.values.ObjectId;

/**
 * The result of answering one question.
 *
 * <p>Citations are always a subset of the ids the retrieval returned: an id the model names that
 * was not retrieved is dropped, so a citation never points at an object the caller could not see.
 *
 * @param text the reply exactly as the model wrote it; empty when {@code answered} is {@code false}
 * @param citations the retrieved ids the reply cites, in order of first appearance, without
 *     duplicates; empty when {@code answered} is {@code false}
 * @param answered {@code false} when no object was retrieved, so the model was not asked
 */
public record Answer(String text, List<ObjectId> citations, boolean answered) {

    /**
     * Validates the components and copies {@code citations}.
     *
     * @throws NullPointerException if {@code text} or {@code citations} is {@code null}
     * @throws IllegalArgumentException if {@code answered} is {@code false} and {@code text} or
     *     {@code citations} is not empty, or if {@code citations} contains a duplicate
     */
    public Answer {
        Objects.requireNonNull(text, "text must not be null");
        Objects.requireNonNull(citations, "citations must not be null");
        citations = List.copyOf(citations);
        if (!answered) {
            if (!text.isEmpty()) {
                throw new IllegalArgumentException("a non-answer must have empty text");
            }
            if (!citations.isEmpty()) {
                throw new IllegalArgumentException("a non-answer must have no citations");
            }
        }
        if (Set.copyOf(citations).size() != citations.size()) {
            throw new IllegalArgumentException("the citations must be unique");
        }
    }

    /**
     * @return the answer for a question no object could be retrieved for: empty text, no citations,
     *     and {@code answered} set to {@code false}
     */
    public static Answer noAnswer() {
        return new Answer("", List.of(), false);
    }
}
