package org.sequeless.knowledge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.sequeless.kernel.values.BoolValue;
import org.sequeless.kernel.values.DateTimeValue;
import org.sequeless.kernel.values.DateValue;
import org.sequeless.kernel.values.DecimalValue;
import org.sequeless.kernel.values.IntegerValue;
import org.sequeless.kernel.values.ListValue;
import org.sequeless.kernel.values.ObjectId;
import org.sequeless.kernel.values.ReferenceValue;
import org.sequeless.kernel.values.TextValue;

class ValueRenderingTest {

    @Test
    void renders_text_as_is() {
        assertThat(ValueRendering.render(new TextValue("hello world"))).isEqualTo("hello world");
    }

    @Test
    void renders_an_integer_in_decimal() {
        assertThat(ValueRendering.render(new IntegerValue(-42L))).isEqualTo("-42");
    }

    @Test
    void renders_a_decimal_with_its_canonical_string() {
        assertThat(ValueRendering.render(new DecimalValue(new BigDecimal("1.50"))))
                .isEqualTo("1.50");
    }

    @Test
    void renders_a_boolean_as_true_or_false() {
        assertThat(ValueRendering.render(new BoolValue(true))).isEqualTo("true");
        assertThat(ValueRendering.render(new BoolValue(false))).isEqualTo("false");
    }

    @Test
    void renders_a_date_time_as_an_iso_instant() {
        assertThat(ValueRendering.render(new DateTimeValue(Instant.parse("2026-01-02T03:04:05Z"))))
                .isEqualTo("2026-01-02T03:04:05Z");
    }

    @Test
    void renders_a_date_as_an_iso_local_date() {
        assertThat(ValueRendering.render(new DateValue(LocalDate.of(2026, 1, 2))))
                .isEqualTo("2026-01-02");
    }

    @Test
    void renders_a_reference_as_the_target_uuid_text_not_the_record_string() {
        UUID uuid = UUID.fromString("33333333-3333-3333-3333-333333333333");

        String rendered = ValueRendering.render(new ReferenceValue(new ObjectId(uuid)));

        assertThat(rendered).isEqualTo("33333333-3333-3333-3333-333333333333");
    }

    @Test
    void renders_a_list_in_brackets_joined_with_comma_space() {
        ListValue list = new ListValue(List.of(new TextValue("a"), new IntegerValue(2L)));

        assertThat(ValueRendering.render(list)).isEqualTo("[a, 2]");
    }

    @Test
    void a_list_with_a_reference_renders_the_reference_as_uuid_text_inside_the_brackets() {
        UUID uuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
        ListValue list = new ListValue(List.of(new ReferenceValue(new ObjectId(uuid)), new TextValue("x")));

        assertThat(ValueRendering.render(list)).isEqualTo("[33333333-3333-3333-3333-333333333333, x]");
    }

    @Test
    void renders_an_empty_list_as_empty_brackets() {
        assertThat(ValueRendering.render(new ListValue(List.of()))).isEqualTo("[]");
    }
}
