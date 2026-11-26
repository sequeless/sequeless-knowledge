package org.sequeless.knowledge.internal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sequeless.kernel.values.ObjectId;
import org.sequeless.knowledge.api.Answer;
import org.sequeless.knowledge.api.AnswerQuestion;
import org.sequeless.knowledge.api.AnswerRequest;
import org.sequeless.knowledge.spi.InferencePort;
import org.sequeless.ontology.schema.api.Schema;
import org.sequeless.ontology.schema.spi.SchemaSource;
import org.sequeless.query.api.BrowseQuery;
import org.sequeless.query.api.BrowseResult;
import org.sequeless.query.api.ObjectView;
import org.sequeless.query.api.Page;
import org.sequeless.query.api.QueryService;
import org.sequeless.query.field.api.FieldScopeRequest;

/**
 * The {@link AnswerQuestion} use case. It reads the Schema first, then makes one Query-side call (a
 * browse with no filter, text, sort or facet), and makes one model call over the retrieved objects.
 * An empty retrieval makes no model call.
 */
public final class DefaultAnswerQuestion implements AnswerQuestion {

    private final QueryService queryService;
    private final SchemaSource schemaSource;
    private final InferencePort inferencePort;

    /**
     * @param queryService the Query side the objects are retrieved through; never {@code null}
     * @param schemaSource the read port for the active ontology's Schema; never {@code null}
     * @param inferencePort the outbound port the model is called through; never {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    public DefaultAnswerQuestion(QueryService queryService, SchemaSource schemaSource, InferencePort inferencePort) {
        this.queryService = Objects.requireNonNull(queryService, "queryService must not be null");
        this.schemaSource = Objects.requireNonNull(schemaSource, "schemaSource must not be null");
        this.inferencePort = Objects.requireNonNull(inferencePort, "inferencePort must not be null");
    }

    @Override
    public Answer answer(AnswerRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Schema schema = schemaSource.schema(request.scope().tenantId());

        FieldScopeRequest fieldScopeRequest = new FieldScopeRequest(
                request.scope().tenantId(),
                request.root(),
                request.versionPolicy(),
                request.traversal(),
                Optional.of(request.scope().principal()),
                request.locale());
        BrowseQuery browseQuery = new BrowseQuery(
                Optional.empty(), Optional.empty(), List.of(), new Page(0, request.maxObjects()), List.of());
        BrowseResult result = queryService.browse(fieldScopeRequest, browseQuery);

        if (result.items().isEmpty()) {
            return Answer.noAnswer();
        }

        String system = PromptBuilder.system(schema);
        String user = PromptBuilder.user(result.items(), request.question());
        String reply = inferencePort.complete(system, user);

        Set<ObjectId> retrieved = new LinkedHashSet<>();
        for (ObjectView item : result.items()) {
            retrieved.add(item.id());
        }
        List<ObjectId> cited = CitationParser.cited(reply, retrieved);
        return new Answer(reply, cited, true);
    }
}
