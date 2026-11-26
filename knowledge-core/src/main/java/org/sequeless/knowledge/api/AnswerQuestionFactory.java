package org.sequeless.knowledge.api;

import java.util.Objects;
import org.sequeless.knowledge.internal.DefaultAnswerQuestion;
import org.sequeless.knowledge.spi.InferencePort;
import org.sequeless.ontology.schema.spi.SchemaSource;
import org.sequeless.query.api.QueryService;

/**
 * Builds the {@link AnswerQuestion} use case from its collaborators.
 *
 * <p>This is the one api class that depends on {@code internal}, the same way query-core's
 * {@code QueryServiceFactory} wires its implementation.
 */
public final class AnswerQuestionFactory {

    private AnswerQuestionFactory() {}

    /**
     * @param queryService the Query side the objects are retrieved through; never {@code null}
     * @param schemaSource the read port for the active ontology's Schema; never {@code null}
     * @param inferencePort the outbound port the model is called through; never {@code null}
     * @return a new {@link AnswerQuestion} that uses the given collaborators
     * @throws NullPointerException if an argument is {@code null}
     */
    public static AnswerQuestion create(
            QueryService queryService, SchemaSource schemaSource, InferencePort inferencePort) {
        Objects.requireNonNull(queryService, "queryService must not be null");
        Objects.requireNonNull(schemaSource, "schemaSource must not be null");
        Objects.requireNonNull(inferencePort, "inferencePort must not be null");
        return new DefaultAnswerQuestion(queryService, schemaSource, inferencePort);
    }
}
