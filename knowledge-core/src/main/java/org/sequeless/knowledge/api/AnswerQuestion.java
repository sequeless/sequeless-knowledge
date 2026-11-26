package org.sequeless.knowledge.api;

/**
 * The knowledge use case: answer one question over the objects the Query side returns for it.
 *
 * <p>Read-only: it never writes. The caller's {@link org.sequeless.kernel.identity.Scope} is passed
 * through unchanged. This interface holds no authorization logic; the Query side checks access at
 * its boundary.
 *
 * <p>Order: schema read, browse, inference. NoConsistentOntologyException propagates even when
 * retrieval is empty. An empty retrieval returns answered=false with zero inference calls.
 *
 * <p>Any exception the {@link org.sequeless.knowledge.spi.InferencePort} throws propagates unchanged.
 */
public interface AnswerQuestion {

    /**
     * Answers the question.
     *
     * @param request the question and the Field scope, principal and limits to answer it under;
     *     never {@code null}
     * @return the answer; {@link Answer#noAnswer()} when no object is retrieved
     * @throws NullPointerException if {@code request} is {@code null}
     * @throws org.sequeless.query.api.InvalidQueryException if the Query side rejects the browse
     *     (propagated from {@link org.sequeless.query.api.QueryService#browse})
     * @throws org.sequeless.query.field.api.FieldResolutionException if the Field scope cannot be
     *     resolved from the request (propagated from the Query side)
     * @throws org.sequeless.ontology.schema.spi.NoConsistentOntologyException if no consistent
     *     Ontology is active (propagated from {@code SchemaSource})
     */
    Answer answer(AnswerRequest request);
}
