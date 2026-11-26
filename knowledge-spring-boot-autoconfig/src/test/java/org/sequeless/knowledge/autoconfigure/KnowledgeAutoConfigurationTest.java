package org.sequeless.knowledge.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sequeless.kernel.identity.Scope;
import org.sequeless.kernel.identity.TenantId;
import org.sequeless.knowledge.api.Answer;
import org.sequeless.knowledge.api.AnswerQuestion;
import org.sequeless.knowledge.spi.InferencePort;
import org.sequeless.knowledge.testkit.FakeInferencePort;
import org.sequeless.ontology.schema.api.OntologyVersion;
import org.sequeless.ontology.schema.api.Schema;
import org.sequeless.ontology.schema.spi.SchemaSource;
import org.sequeless.query.api.BrowseQuery;
import org.sequeless.query.api.BrowseResult;
import org.sequeless.query.api.IndexPreparationResult;
import org.sequeless.query.api.ObjectView;
import org.sequeless.query.api.QueryService;
import org.sequeless.query.api.ReadRequest;
import org.sequeless.query.field.api.FieldScopeRequest;
import org.sequeless.query.field.api.OntologyVersionRef;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Context tests for {@link KnowledgeAutoConfiguration}, each run with the three beans it depends on
 * supplied by hand.
 *
 * <p>Known limit: these tests do not exercise query-core's real autoconfiguration. The
 * {@link QueryService} is a stub, so a regression in {@code QueryServiceAutoConfiguration} that
 * stops it registering a query service is not caught here.
 */
class KnowledgeAutoConfigurationTest {

    private static final Schema SCHEMA =
            new Schema(new OntologyVersion(1), "urn:sequeless:test:ontology", Optional.empty(), Map.of(), List.of());

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(KnowledgeAutoConfiguration.class));

    @Test
    void registers_an_answer_question_when_the_query_service_schema_source_and_inference_port_exist() {
        runner.withBean(QueryService.class, UnusedQueryService::new)
                .withBean(SchemaSource.class, () -> tenant -> SCHEMA)
                .withBean(InferencePort.class, () -> FakeInferencePort.replying("reply"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AnswerQuestion.class);
                });
    }

    @Test
    void registers_no_answer_question_and_still_starts_when_the_inference_port_is_absent() {
        runner.withBean(QueryService.class, UnusedQueryService::new)
                .withBean(SchemaSource.class, () -> tenant -> SCHEMA)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AnswerQuestion.class);
                });
    }

    @Test
    void registers_no_answer_question_when_the_query_service_is_absent() {
        runner.withBean(SchemaSource.class, () -> tenant -> SCHEMA)
                .withBean(InferencePort.class, () -> FakeInferencePort.replying("reply"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AnswerQuestion.class);
                });
    }

    @Test
    void registers_no_answer_question_and_still_starts_when_the_schema_source_is_absent() {
        runner.withBean(QueryService.class, UnusedQueryService::new)
                .withBean(InferencePort.class, () -> FakeInferencePort.replying("reply"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AnswerQuestion.class);
                });
    }

    @Test
    void backs_off_and_keeps_the_host_supplied_answer_question_when_all_three_beans_exist() {
        AnswerQuestion host = request -> Answer.noAnswer();

        runner.withBean(QueryService.class, UnusedQueryService::new)
                .withBean(SchemaSource.class, () -> tenant -> SCHEMA)
                .withBean(InferencePort.class, () -> FakeInferencePort.replying("reply"))
                .withBean(AnswerQuestion.class, () -> host)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AnswerQuestion.class)).isSameAs(host);
                });
    }

    /** A Query side that the context tests never call: the bean is only wired, not used. */
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
