package org.sequeless.knowledge.api;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sequeless.kernel.identity.Scope;
import org.sequeless.kernel.identity.TenantId;
import org.sequeless.knowledge.spi.InferencePort;
import org.sequeless.ontology.schema.spi.SchemaSource;
import org.sequeless.query.api.BrowseQuery;
import org.sequeless.query.api.BrowseResult;
import org.sequeless.query.api.IndexPreparationResult;
import org.sequeless.query.api.ObjectView;
import org.sequeless.query.api.QueryService;
import org.sequeless.query.api.ReadRequest;
import org.sequeless.query.field.api.FieldScopeRequest;
import org.sequeless.query.field.api.OntologyVersionRef;

class AnswerQuestionFactoryTest {

    private static final QueryService QUERY = new UnusedQueryService();
    private static final SchemaSource SCHEMAS = tenant -> {
        throw new UnsupportedOperationException("the factory does not read the schema");
    };
    private static final InferencePort INFERENCE = (system, user) -> {
        throw new UnsupportedOperationException("the factory does not call the model");
    };

    @Test
    void create_rejects_a_null_query_service() {
        assertThatNullPointerException()
                .isThrownBy(() -> AnswerQuestionFactory.create(null, SCHEMAS, INFERENCE))
                .withMessageContaining("queryService");
    }

    @Test
    void create_rejects_a_null_schema_source() {
        assertThatNullPointerException()
                .isThrownBy(() -> AnswerQuestionFactory.create(QUERY, null, INFERENCE))
                .withMessageContaining("schemaSource");
    }

    @Test
    void create_rejects_a_null_inference_port() {
        assertThatNullPointerException()
                .isThrownBy(() -> AnswerQuestionFactory.create(QUERY, SCHEMAS, null))
                .withMessageContaining("inferencePort");
    }

    /** A Query side that is never called: the factory only checks and wires its collaborators. */
    private static final class UnusedQueryService implements QueryService {

        @Override
        public BrowseResult browse(FieldScopeRequest request, BrowseQuery query) {
            throw new UnsupportedOperationException("browse is not used by this test");
        }

        @Override
        public Optional<ObjectView> read(Scope scope, ReadRequest request) {
            throw new UnsupportedOperationException("read is not used by this test");
        }

        @Override
        public IndexPreparationResult prepare(TenantId tenant, OntologyVersionRef version) {
            throw new UnsupportedOperationException("prepare is not used by this test");
        }
    }
}
