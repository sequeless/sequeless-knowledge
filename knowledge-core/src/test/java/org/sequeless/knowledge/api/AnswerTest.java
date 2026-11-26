package org.sequeless.knowledge.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.sequeless.kernel.values.ObjectId;

class AnswerTest {

    private static final ObjectId ID = new ObjectId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    @Test
    void noAnswer_has_empty_text_no_citations_and_is_not_answered() {
        Answer noAnswer = Answer.noAnswer();

        assertThat(noAnswer.text()).isEmpty();
        assertThat(noAnswer.citations()).isEmpty();
        assertThat(noAnswer.answered()).isFalse();
    }

    @Test
    void an_answered_answer_keeps_its_text_and_citations() {
        Answer answer = new Answer("It is open. Sources: " + ID.value(), List.of(ID), true);

        assertThat(answer.answered()).isTrue();
        assertThat(answer.citations()).containsExactly(ID);
    }

    @Test
    void an_answered_answer_may_have_empty_text() {
        Answer answer = new Answer("", List.of(), true);

        assertThat(answer.answered()).isTrue();
    }

    @Test
    void citations_are_copied_so_later_changes_do_not_reach_the_answer() {
        List<ObjectId> citations = new ArrayList<>(List.of(ID));
        Answer answer = new Answer("text", citations, true);

        citations.clear();

        assertThat(answer.citations()).containsExactly(ID);
    }

    @Test
    void citations_are_unmodifiable() {
        Answer answer = new Answer("text", List.of(ID), true);

        assertThatThrownBy(() -> answer.citations().add(ID)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejects_null_text() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Answer(null, List.of(), false))
                .withMessageContaining("text");
    }

    @Test
    void rejects_null_citations() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Answer("text", null, true))
                .withMessageContaining("citations");
    }

    @Test
    void rejects_a_non_answer_with_text() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Answer("text", List.of(), false));
    }

    @Test
    void rejects_a_non_answer_with_citations() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Answer("", List.of(ID), false));
    }

    @Test
    void rejects_duplicate_citations() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Answer("It is open. Sources: " + ID.value(), List.of(ID, ID), true))
                .withMessage("the citations must be unique");
    }
}
