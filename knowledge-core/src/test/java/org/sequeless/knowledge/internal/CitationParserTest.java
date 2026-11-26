package org.sequeless.knowledge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.sequeless.kernel.values.ObjectId;

class CitationParserTest {

    private static final ObjectId A = new ObjectId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final ObjectId B = new ObjectId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
    private static final ObjectId C = new ObjectId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
    private static final ObjectId UNKNOWN = new ObjectId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));

    private static Set<ObjectId> retrieved(ObjectId... ids) {
        return new LinkedHashSet<>(List.of(ids));
    }

    @Test
    void returns_citations_in_order_of_first_appearance() {
        String reply = "B first, then A, then C. Sources: " + B.value() + ", " + A.value() + ", " + C.value();

        List<ObjectId> cited = CitationParser.cited(reply, retrieved(A, B, C));

        assertThat(cited).containsExactly(B, A, C);
    }

    @Test
    void an_id_cited_more_than_once_appears_once() {
        String reply = "A says A. Sources: " + A.value() + ", " + A.value();

        List<ObjectId> cited = CitationParser.cited(reply, retrieved(A, B));

        assertThat(cited).containsExactly(A);
    }

    @Test
    void drops_ids_that_were_not_retrieved() {
        String reply = "UNKNOWN is not in the set. Sources: " + UNKNOWN.value() + ", " + B.value();

        List<ObjectId> cited = CitationParser.cited(reply, retrieved(A, B));

        assertThat(cited).containsExactly(B);
    }

    @Test
    void a_uuid_embedded_in_a_longer_hex_token_is_not_cited() {
        ObjectId prefix = new ObjectId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        // The first 36 characters are a retrieved id, but the token continues with hex digits.
        assertThat(CitationParser.cited("Sources: x11111111-1111-1111-1111-1111111111119", retrieved(prefix)))
                .isEmpty();
        assertThat(CitationParser.cited("Sources: f" + prefix.value(), retrieved(prefix)))
                .isEmpty();
    }

    @Test
    void a_retrieved_uuid_wrapped_in_punctuation_is_cited() {
        String reply = "The invoice is open (" + A.value() + ").";

        assertThat(CitationParser.cited(reply, retrieved(A))).containsExactly(A);
    }

    @Test
    void accepts_uppercase_uuid_text_and_matches_it_to_the_retrieved_id() {
        // Decision: uppercase hex is accepted. UUID.fromString ignores case and ObjectId equality is
        // by UUID value, so an uppercase token matches the lowercase retrieved id.
        String reply = "Sources: " + A.value().toString().toUpperCase();

        List<ObjectId> cited = CitationParser.cited(reply, retrieved(A));

        assertThat(cited).containsExactly(A);
    }

    @Test
    void an_empty_reply_gives_an_empty_list() {
        assertThat(CitationParser.cited("", retrieved(A, B))).isEmpty();
    }

    @Test
    void a_reply_with_no_uuid_gives_an_empty_list() {
        assertThat(CitationParser.cited("No objects answer this.", retrieved(A)))
                .isEmpty();
    }

    @Test
    void the_returned_list_is_unmodifiable() {
        List<ObjectId> cited = CitationParser.cited("Sources: " + A.value(), retrieved(A));

        assertThatThrownBy(() -> cited.add(B)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejects_a_null_reply() {
        assertThatNullPointerException().isThrownBy(() -> CitationParser.cited(null, retrieved(A)));
    }

    @Test
    void rejects_null_retrieved_ids() {
        assertThatNullPointerException().isThrownBy(() -> CitationParser.cited("text", null));
    }
}
