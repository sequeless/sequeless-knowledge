package org.sequeless.knowledge.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sequeless.kernel.values.Value;
import org.sequeless.ontology.schema.api.Schema;
import org.sequeless.ontology.schema.api.TypeDefinition;
import org.sequeless.query.api.ObjectView;
import org.sequeless.query.api.PropertyRef;

/**
 * Builds the system and user prompts for one model call.
 *
 * <p>Object values are untrusted text. They are rendered raw, with no escaping and no length cap,
 * so a stored value can contain text that reads as an instruction to the model. The fixed
 * instructions in {@link #SYSTEM_INSTRUCTIONS} are the only guidance the model gets about the
 * objects.
 */
final class PromptBuilder {

    /** The citation instruction. Stated in the system prompt and again as the last user-prompt line. */
    private static final String SOURCES_LINE = "End your reply with one line: Sources: <id>, <id>";

    /** The fixed instruction text. Kept in one constant so tests need not match its wording. */
    static final String SYSTEM_INSTRUCTIONS = String.join(
            "\n",
            "You answer a question from a numbered list of stored objects.",
            "Answer only from the numbered objects.",
            "If they are not sufficient to answer, say so.",
            "Cite only the object ids listed in the user message.",
            SOURCES_LINE,
            "",
            "Types in this ontology (IRI - label):");

    private PromptBuilder() {}

    /**
     * @param schema the active ontology's Schema; never {@code null}
     * @return the system prompt: the fixed instructions, then one {@code <iri> - <label>} line per
     *     type of the schema
     * @throws NullPointerException if {@code schema} is {@code null}
     */
    static String system(Schema schema) {
        Objects.requireNonNull(schema, "schema must not be null");
        StringBuilder out = new StringBuilder(SYSTEM_INSTRUCTIONS);
        for (TypeDefinition type : schema.types()) {
            out.append('\n').append(type.iri()).append(" - ").append(type.label());
        }
        return out.toString();
    }

    /**
     * @param items the retrieved objects, in result order; never {@code null}
     * @param question the question to answer; never {@code null}
     * @return the user prompt: one numbered line per object, its property lines, the question, and
     *     the final instruction line
     * @throws NullPointerException if an argument is {@code null}
     */
    static String user(List<ObjectView> items, String question) {
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(question, "question must not be null");
        StringBuilder out = new StringBuilder();
        int number = 1;
        for (ObjectView item : items) {
            out.append('[')
                    .append(number++)
                    .append("] id=")
                    .append(item.id().value())
                    .append(" type=")
                    .append(item.type().iri())
                    .append(" state=")
                    .append(item.state().orElse(""))
                    .append('\n');
            for (Map.Entry<PropertyRef, Value> property : sortedByIri(item.properties())) {
                out.append("  ")
                        .append(property.getKey().iri())
                        .append('=')
                        .append(ValueRendering.render(property.getValue()))
                        .append('\n');
            }
        }
        out.append("Question: ").append(question).append('\n');
        out.append(SOURCES_LINE);
        return out.toString();
    }

    private static List<Map.Entry<PropertyRef, Value>> sortedByIri(Map<PropertyRef, Value> properties) {
        List<Map.Entry<PropertyRef, Value>> entries = new ArrayList<>(properties.entrySet());
        entries.sort(Comparator.comparing(
                (Map.Entry<PropertyRef, Value> e) -> e.getKey().iri()));
        return entries;
    }
}
