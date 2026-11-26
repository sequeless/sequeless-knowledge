package org.sequeless.knowledge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.sequeless.kernel.identity.Principal;
import org.sequeless.kernel.identity.Scope;
import org.sequeless.kernel.identity.TenantId;
import org.sequeless.kernel.values.ObjectId;
import org.sequeless.knowledge.api.Answer;
import org.sequeless.knowledge.api.AnswerRequest;
import org.sequeless.knowledge.spi.InferencePort;
import org.sequeless.ontology.schema.api.DisplayHints;
import org.sequeless.ontology.schema.api.OntologyVersion;
import org.sequeless.ontology.schema.api.Schema;
import org.sequeless.ontology.schema.api.TypeDefinition;
import org.sequeless.ontology.schema.spi.NoConsistentOntologyException;
import org.sequeless.ontology.schema.spi.SchemaSource;
import org.sequeless.query.api.Audit;
import org.sequeless.query.api.BrowseQuery;
import org.sequeless.query.api.BrowseResult;
import org.sequeless.query.api.IndexPreparationResult;
import org.sequeless.query.api.InvalidQueryException;
import org.sequeless.query.api.ObjectView;
import org.sequeless.query.api.Page;
import org.sequeless.query.api.QueryService;
import org.sequeless.query.api.ReadRequest;
import org.sequeless.query.api.TypeRef;
import org.sequeless.query.field.api.ClassRef;
import org.sequeless.query.field.api.FieldScopeRequest;
import org.sequeless.query.field.api.LocalizedText;
import org.sequeless.query.field.api.OntologyVersionRef;
import org.sequeless.query.field.api.TraversalConfig;
import org.sequeless.query.field.api.VersionPolicy;

class DefaultAnswerQuestionTest {

    private static final TenantId TENANT = new TenantId("tenant-a");
    private static final Scope SCOPE = new Scope(TENANT, new Principal("alice", Set.of("reader")));
    private static final ClassRef ROOT = new ClassRef(
            UUID.randomUUID(), "invoice", "https://example.org/Invoice", new LocalizedText("Invoice", "en"));
    private static final VersionPolicy POLICY = new VersionPolicy.Pinned(new OntologyVersionRef(2));
    private static final TraversalConfig TRAVERSAL = TraversalConfig.defaults();
    private static final Optional<Locale> LOCALE = Optional.of(Locale.FRANCE);
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String QUESTION = "Which invoices are open?";

    private static final ObjectId FIRST = new ObjectId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final ObjectId SECOND = new ObjectId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final ObjectId UNKNOWN = new ObjectId(UUID.fromString("99999999-9999-9999-9999-999999999999"));

    private static final Schema SCHEMA = new Schema(
            new OntologyVersion(2),
            "https://example.org/onto",
            Optional.empty(),
            Map.of(),
            List.of(new TypeDefinition(
                    "https://example.org/Invoice", "Invoice", List.of(), List.of(), DisplayHints.none(), false)));

    private static AnswerRequest request(int maxObjects) {
        return new AnswerRequest(SCOPE, QUESTION, ROOT, POLICY, TRAVERSAL, LOCALE, maxObjects);
    }

    private static ObjectView object(ObjectId id) {
        return new ObjectView(
                id,
                new TypeRef("https://example.org/Invoice"),
                TENANT,
                1L,
                Optional.empty(),
                Map.of(),
                new Audit(NOW, "alice", NOW, "alice"),
                false);
    }

    private static List<ObjectView> objects(ObjectId... ids) {
        List<ObjectView> items = new ArrayList<>();
        for (ObjectId id : ids) {
            items.add(object(id));
        }
        return items;
    }

    private static BrowseResult result(ObjectId... ids) {
        List<ObjectView> items = objects(ids);
        return new BrowseResult(items, items.size(), Map.of());
    }

    private static SchemaSource schemaOf(Schema schema) {
        return tenant -> schema;
    }

    /** The model got exactly one call: the schema's system prompt and the given retrieved objects. */
    private static void assertOneModelCall(RecordingInference inference, ObjectId... retrieved) {
        assertThat(inference.calls).hasSize(1);
        RecordingInference.Call call = inference.calls.get(0);
        assertThat(call.system()).isEqualTo(PromptBuilder.system(SCHEMA));
        assertThat(call.user()).isEqualTo(PromptBuilder.user(objects(retrieved), QUESTION));
    }

    @Test
    void forwards_the_request_scope_and_locale_to_the_browse() {
        RecordingQueryService query = new RecordingQueryService(result(FIRST));
        RecordingInference inference = new RecordingInference("ok");
        DefaultAnswerQuestion useCase = new DefaultAnswerQuestion(query, schemaOf(SCHEMA), inference);

        useCase.answer(request(5));

        assertThat(query.queries).hasSize(1);
        FieldScopeRequest forwarded = query.requests.get(0);
        assertThat(forwarded.tenantId()).isEqualTo(TENANT);
        assertThat(forwarded.root()).isEqualTo(ROOT);
        assertThat(forwarded.versionPolicy()).isEqualTo(POLICY);
        assertThat(forwarded.traversal()).isEqualTo(TRAVERSAL);
        assertThat(forwarded.principal()).isEqualTo(Optional.of(SCOPE.principal()));
        assertThat(forwarded.locale()).isEqualTo(LOCALE);
        assertOneModelCall(inference, FIRST);
    }

    @Test
    void browses_with_no_filter_text_sort_or_facet_and_the_requested_page() {
        RecordingQueryService query = new RecordingQueryService(result(FIRST));
        DefaultAnswerQuestion useCase =
                new DefaultAnswerQuestion(query, schemaOf(SCHEMA), new RecordingInference("ok"));

        useCase.answer(request(7));

        assertThat(query.queries).hasSize(1);
        BrowseQuery browse = query.queries.get(0);
        assertThat(browse.filter()).isEmpty();
        assertThat(browse.text()).isEmpty();
        assertThat(browse.sorts()).isEmpty();
        assertThat(browse.facets()).isEmpty();
        assertThat(browse.page()).isEqualTo(new Page(0, 7));
    }

    @Test
    void an_empty_browse_returns_no_answer_and_never_calls_the_model() {
        RecordingQueryService query = new RecordingQueryService(result());
        RecordingInference inference = new RecordingInference("should not be used");
        DefaultAnswerQuestion useCase = new DefaultAnswerQuestion(query, schemaOf(SCHEMA), inference);

        Answer answer = useCase.answer(request(5));

        assertThat(query.queries).hasSize(1);
        assertThat(answer).isEqualTo(Answer.noAnswer());
        assertThat(inference.calls).isEmpty();
    }

    @Test
    void the_model_gets_one_call_with_the_schema_prompt_and_the_retrieved_objects() {
        RecordingQueryService query = new RecordingQueryService(result(FIRST, SECOND));
        RecordingInference inference = new RecordingInference("Both are open. Sources: " + FIRST.value());
        DefaultAnswerQuestion useCase = new DefaultAnswerQuestion(query, schemaOf(SCHEMA), inference);

        useCase.answer(request(5));

        assertThat(query.queries).hasSize(1);
        assertOneModelCall(inference, FIRST, SECOND);
    }

    @Test
    void drops_a_cited_id_that_was_not_retrieved() {
        String reply = "The invoice is open. Sources: " + FIRST.value() + ", " + UNKNOWN.value();
        RecordingQueryService query = new RecordingQueryService(result(FIRST));
        RecordingInference inference = new RecordingInference(reply);
        DefaultAnswerQuestion useCase = new DefaultAnswerQuestion(query, schemaOf(SCHEMA), inference);

        Answer answer = useCase.answer(request(5));

        assertThat(query.queries).hasSize(1);
        assertOneModelCall(inference, FIRST);
        assertThat(answer.answered()).isTrue();
        assertThat(answer.citations()).containsExactly(FIRST);
    }

    @Test
    void a_retrieved_id_cited_twice_is_returned_once() {
        String reply = "Open, per " + SECOND.value() + ". Sources: " + SECOND.value() + ", " + SECOND.value();
        RecordingQueryService query = new RecordingQueryService(result(FIRST, SECOND));
        RecordingInference inference = new RecordingInference(reply);
        DefaultAnswerQuestion useCase = new DefaultAnswerQuestion(query, schemaOf(SCHEMA), inference);

        Answer answer = useCase.answer(request(5));

        assertThat(query.queries).hasSize(1);
        assertOneModelCall(inference, FIRST, SECOND);
        assertThat(answer.citations()).containsExactly(SECOND);
    }

    @Test
    void a_schema_read_failure_propagates_and_the_browse_is_not_called() {
        RecordingQueryService query = new RecordingQueryService(result(FIRST));
        SchemaSource failing = tenant -> {
            throw new NoConsistentOntologyException("no active ontology");
        };
        DefaultAnswerQuestion useCase = new DefaultAnswerQuestion(query, failing, new RecordingInference("ok"));

        assertThatThrownBy(() -> useCase.answer(request(5)))
                .isInstanceOf(NoConsistentOntologyException.class)
                .hasMessage("no active ontology");
        assertThat(query.requests).isEmpty();
    }

    @Test
    void a_schema_read_failure_with_an_empty_browse_propagates_and_the_model_is_not_called() {
        RecordingQueryService query = new RecordingQueryService(result());
        RecordingInference inference = new RecordingInference("should not be used");
        SchemaSource failing = tenant -> {
            throw new NoConsistentOntologyException("no active ontology");
        };
        DefaultAnswerQuestion useCase = new DefaultAnswerQuestion(query, failing, inference);

        assertThatThrownBy(() -> useCase.answer(request(5)))
                .isInstanceOf(NoConsistentOntologyException.class)
                .hasMessage("no active ontology");
        assertThat(query.queries).isEmpty();
        assertThat(inference.calls).isEmpty();
    }

    @Test
    void a_browse_failure_propagates_and_the_model_is_not_called() {
        IllegalStateException failure = new IllegalStateException("query unavailable");
        RecordingQueryService query = new RecordingQueryService(failure);
        RecordingInference inference = new RecordingInference("should not be used");
        DefaultAnswerQuestion useCase = new DefaultAnswerQuestion(query, schemaOf(SCHEMA), inference);

        assertThatThrownBy(() -> useCase.answer(request(5))).isSameAs(failure);
        assertThat(query.queries).hasSize(1);
        assertThat(inference.calls).isEmpty();
    }

    @Test
    void an_invalid_query_from_the_query_side_propagates_and_the_model_is_not_called() {
        InvalidQueryException rejection = new InvalidQueryException(List.of("sort path is not sortable"));
        RecordingQueryService query = new RecordingQueryService(rejection);
        RecordingInference inference = new RecordingInference("should not be used");
        DefaultAnswerQuestion useCase = new DefaultAnswerQuestion(query, schemaOf(SCHEMA), inference);

        assertThatThrownBy(() -> useCase.answer(request(5))).isSameAs(rejection);
        assertThat(query.queries).hasSize(1);
        assertThat(inference.calls).isEmpty();
    }

    @Test
    void an_inference_failure_propagates() {
        IllegalStateException failure = new IllegalStateException("model unavailable");
        RecordingQueryService query = new RecordingQueryService(result(FIRST));
        DefaultAnswerQuestion useCase =
                new DefaultAnswerQuestion(query, schemaOf(SCHEMA), new RecordingInference(failure));

        assertThatThrownBy(() -> useCase.answer(request(5))).isSameAs(failure);
        assertThat(query.queries).hasSize(1);
    }

    @Test
    void the_returned_text_is_the_reply_text() {
        String reply = "The invoice is open, but the objects do not say when it is due. Sources: " + FIRST.value();
        RecordingQueryService query = new RecordingQueryService(result(FIRST));
        RecordingInference inference = new RecordingInference(reply);
        DefaultAnswerQuestion useCase = new DefaultAnswerQuestion(query, schemaOf(SCHEMA), inference);

        Answer answer = useCase.answer(request(5));

        assertThat(query.queries).hasSize(1);
        assertOneModelCall(inference, FIRST);
        assertThat(answer.text()).isEqualTo(reply);
        assertThat(answer.answered()).isTrue();
    }

    /** Hand-written Query side: records each browse and returns the configured result or throws the configured failure. */
    private static final class RecordingQueryService implements QueryService {

        private final List<FieldScopeRequest> requests = new ArrayList<>();
        private final List<BrowseQuery> queries = new ArrayList<>();
        private final BrowseResult result;
        private final RuntimeException failure;

        RecordingQueryService(BrowseResult result) {
            this.result = result;
            this.failure = null;
        }

        RecordingQueryService(RuntimeException failure) {
            this.result = null;
            this.failure = failure;
        }

        @Override
        public BrowseResult browse(FieldScopeRequest request, BrowseQuery query) {
            requests.add(request);
            queries.add(query);
            if (failure != null) {
                throw failure;
            }
            return result;
        }

        @Override
        public Optional<ObjectView> read(Scope scope, ReadRequest request) {
            throw new UnsupportedOperationException("read is not used by AnswerQuestion");
        }

        @Override
        public IndexPreparationResult prepare(TenantId tenant, OntologyVersionRef version) {
            throw new UnsupportedOperationException("prepare is not used by AnswerQuestion");
        }
    }

    /** Hand-written model port: records each call and returns a scripted reply or throws a scripted failure. */
    private static final class RecordingInference implements InferencePort {

        /** One recorded call: the system prompt and the user prompt, in the order the port receives them. */
        private record Call(String system, String user) {}

        private final List<Call> calls = new ArrayList<>();
        private final String reply;
        private final RuntimeException failure;

        RecordingInference(String reply) {
            this.reply = reply;
            this.failure = null;
        }

        RecordingInference(RuntimeException failure) {
            this.reply = null;
            this.failure = failure;
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            calls.add(new Call(systemPrompt, userPrompt));
            if (failure != null) {
                throw failure;
            }
            return reply;
        }
    }
}
