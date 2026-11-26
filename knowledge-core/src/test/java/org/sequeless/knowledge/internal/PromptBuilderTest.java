package org.sequeless.knowledge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.sequeless.kernel.identity.TenantId;
import org.sequeless.kernel.values.ObjectId;
import org.sequeless.kernel.values.Value;
import org.sequeless.ontology.schema.api.DisplayHints;
import org.sequeless.ontology.schema.api.OntologyVersion;
import org.sequeless.ontology.schema.api.Schema;
import org.sequeless.ontology.schema.api.TypeDefinition;
import org.sequeless.query.api.Audit;
import org.sequeless.query.api.ObjectView;
import org.sequeless.query.api.PropertyRef;
import org.sequeless.query.api.TypeRef;

class PromptBuilderTest {

    private static final TenantId TENANT = new TenantId("tenant-a");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final ObjectId FIRST = new ObjectId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final ObjectId SECOND = new ObjectId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    private static final Schema SCHEMA = new Schema(
            new OntologyVersion(1),
            "https://example.org/onto",
            Optional.empty(),
            Map.of(),
            List.of(
                    new TypeDefinition(
                            "https://example.org/Invoice", "Invoice", List.of(), List.of(), DisplayHints.none(), false),
                    new TypeDefinition(
                            "https://example.org/Customer",
                            "Customer",
                            List.of(),
                            List.of(),
                            DisplayHints.none(),
                            false)));

    private static TypeDefinition type(String iri, String label) {
        return new TypeDefinition(iri, label, List.of(), List.of(), DisplayHints.none(), false);
    }

    /** Zero-padded so that the IRIs sort the same as their numbers. */
    private static String propertyIri(int number) {
        return String.format("https://example.org/p%02d", number);
    }

    private static ObjectView object(ObjectId id, Map<PropertyRef, Value> properties) {
        return new ObjectView(
                id,
                new TypeRef("https://example.org/Invoice"),
                TENANT,
                1L,
                Optional.of("open"),
                properties,
                new Audit(NOW, "alice", NOW, "alice"),
                false);
    }

    @Test
    void the_system_prompt_names_every_type_iri() {
        String system = PromptBuilder.system(SCHEMA);

        assertThat(system).contains("https://example.org/Invoice").contains("https://example.org/Customer");
        assertThat(system).contains("https://example.org/Invoice - Invoice");
    }

    @Test
    void the_system_prompt_lists_the_type_lines_in_schema_order() {
        Schema schema = new Schema(
                new OntologyVersion(1),
                "https://example.org/onto",
                Optional.empty(),
                Map.of(),
                List.of(
                        type("https://example.org/Zebra", "Zebra"),
                        type("https://example.org/Account", "Account"),
                        type("https://example.org/Mango", "Mango")));
        String[] typeLines = schema.types().stream()
                .map(type -> type.iri() + " - " + type.label())
                .toArray(String[]::new);

        assertThat(PromptBuilder.system(schema).lines().toList()).containsSubsequence(typeLines);
    }

    @Test
    void the_user_prompt_contains_every_retrieved_id_and_the_question() {
        List<ObjectView> items = List.of(object(FIRST, Map.of()), object(SECOND, Map.of()));

        String user = PromptBuilder.user(items, "Which invoices are open?");

        assertThat(user)
                .contains(FIRST.value().toString())
                .contains(SECOND.value().toString())
                .contains("Which invoices are open?");
    }

    @Test
    void numbers_the_objects_from_one_and_states_the_state_or_empty() {
        ObjectView withState = object(FIRST, Map.of());
        ObjectView withoutState = new ObjectView(
                SECOND,
                new TypeRef("https://example.org/Invoice"),
                TENANT,
                1L,
                Optional.empty(),
                Map.of(),
                new Audit(NOW, "alice", NOW, "alice"),
                false);

        String user = PromptBuilder.user(List.of(withState, withoutState), "q");

        assertThat(user)
                .contains("[1] id=" + FIRST.value() + " type=https://example.org/Invoice state=open")
                .contains("[2] id=" + SECOND.value() + " type=https://example.org/Invoice state=\n");
    }

    @Test
    void the_user_prompt_never_shows_java_record_rendering() {
        Map<PropertyRef, Value> properties = Map.of(
                new PropertyRef("https://example.org/ref"),
                Value.ref(SECOND),
                new PropertyRef("https://example.org/tags"),
                Value.list(List.of(Value.text("a"), Value.text("b"))));

        String user = PromptBuilder.user(List.of(object(FIRST, properties)), "q");

        assertThat(user).doesNotContain("Value[").doesNotContain("ObjectId[");
    }

    @Test
    void a_list_value_renders_as_bracketed_comma_separated_text() {
        Map<PropertyRef, Value> properties = Map.of(
                new PropertyRef("https://example.org/tags"), Value.list(List.of(Value.text("a"), Value.text("b"))));

        String user = PromptBuilder.user(List.of(object(FIRST, properties)), "q");

        assertThat(user).contains("  https://example.org/tags=[a, b]");
    }

    @Test
    void a_reference_value_renders_as_the_target_uuid_text() {
        Map<PropertyRef, Value> properties = Map.of(new PropertyRef("https://example.org/ref"), Value.ref(SECOND));

        String user = PromptBuilder.user(List.of(object(FIRST, properties)), "q");

        assertThat(user).contains("  https://example.org/ref=" + SECOND.value());
    }

    @Test
    void property_lines_are_sorted_by_property_iri() {
        // Twelve distinct IRIs. ObjectView copies the map with Map.copyOf, which drops insertion order,
        // so iteration follows a per-JVM random salt. Without a sort, the output matches the sorted order
        // only by chance, about 1 in 12! (1 in 479 million), so a missing sort fails almost every run.
        Map<PropertyRef, Value> properties = new LinkedHashMap<>();
        for (int i = 12; i >= 1; i--) {
            properties.put(new PropertyRef(propertyIri(i)), Value.text("v" + i));
        }

        String user = PromptBuilder.user(List.of(object(FIRST, properties)), "q");

        List<String> expected = IntStream.rangeClosed(1, 12)
                .mapToObj(i -> "  " + propertyIri(i) + "=v" + i)
                .toList();
        List<String> actual = user.lines().filter(line -> line.startsWith("  ")).toList();
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    void an_object_with_no_properties_has_no_property_lines() {
        String user = PromptBuilder.user(List.of(object(FIRST, Map.of())), "q");

        assertThat(user.lines()).noneMatch(line -> line.startsWith("  "));
    }

    @Test
    void the_scalar_value_variants_are_rendered_as_plain_text() {
        Map<PropertyRef, Value> properties = Map.of(
                new PropertyRef("https://example.org/text"),
                Value.text("t"),
                new PropertyRef("https://example.org/integer"),
                Value.integer(3),
                new PropertyRef("https://example.org/decimal"),
                Value.decimal(new BigDecimal("1.50")),
                new PropertyRef("https://example.org/bool"),
                Value.bool(true),
                new PropertyRef("https://example.org/datetime"),
                Value.dateTime(Instant.parse("2026-01-02T03:04:05Z")),
                new PropertyRef("https://example.org/date"),
                Value.date(LocalDate.of(2026, 1, 2)));

        String user = PromptBuilder.user(List.of(object(FIRST, properties)), "q");

        assertThat(user)
                .contains("  https://example.org/integer=3")
                .contains("  https://example.org/decimal=1.50")
                .contains("  https://example.org/bool=true")
                .contains("  https://example.org/datetime=2026-01-02T03:04:05Z")
                .contains("  https://example.org/date=2026-01-02")
                .doesNotContain("Value[")
                .doesNotContain("ObjectId[");
    }

    @Test
    void the_user_prompt_ends_with_the_final_sources_instruction() {
        String user = PromptBuilder.user(List.of(object(FIRST, Map.of())), "q");

        assertThat(user).endsWith("End your reply with one line: Sources: <id>, <id>");
    }
}
