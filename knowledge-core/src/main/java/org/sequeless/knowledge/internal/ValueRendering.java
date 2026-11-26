package org.sequeless.knowledge.internal;

import java.util.Objects;
import java.util.stream.Collectors;
import org.sequeless.kernel.values.BoolValue;
import org.sequeless.kernel.values.DateTimeValue;
import org.sequeless.kernel.values.DateValue;
import org.sequeless.kernel.values.DecimalValue;
import org.sequeless.kernel.values.IntegerValue;
import org.sequeless.kernel.values.ListValue;
import org.sequeless.kernel.values.ReferenceValue;
import org.sequeless.kernel.values.TextValue;
import org.sequeless.kernel.values.Value;

/**
 * Renders a {@link Value} as the plain text the model sees. The switch is exhaustive over the sealed
 * {@link Value} variants and has no default branch, so a new variant fails to compile here until it
 * is rendered.
 */
final class ValueRendering {

    private ValueRendering() {}

    /**
     * @param value the value to render; never {@code null}
     * @return the value's text: a reference renders as its target's UUID text, and a list as
     *     {@code [a, b]}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    static String render(Value value) {
        Objects.requireNonNull(value, "value must not be null");
        return switch (value) {
            case TextValue t -> t.value();
            case IntegerValue i -> Long.toString(i.value());
            case DecimalValue d -> d.value().toString();
            case BoolValue b -> Boolean.toString(b.value());
            case DateTimeValue dt -> dt.value().toString();
            case DateValue dv -> dv.value().toString();
            case ReferenceValue r -> r.target().value().toString();
            case ListValue l ->
                l.values().stream().map(ValueRendering::render).collect(Collectors.joining(", ", "[", "]"));
        };
    }
}
