package org.sequeless.knowledge.autoconfigure;

import org.sequeless.knowledge.api.AnswerQuestion;
import org.sequeless.knowledge.api.AnswerQuestionFactory;
import org.sequeless.knowledge.spi.InferencePort;
import org.sequeless.ontology.schema.spi.SchemaSource;
import org.sequeless.query.api.QueryService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers the {@link AnswerQuestion} use case in a Spring Boot host.
 *
 * <p>The host supplies the {@link QueryService}, which query-core's autoconfiguration provides, and
 * its own {@link SchemaSource} and {@link InferencePort}. The bean is absent unless all three
 * exist. It runs after {@code QueryServiceAutoConfiguration}, so the query service's condition is
 * evaluated first.
 */
@AutoConfiguration(afterName = "org.sequeless.query.autoconfig.QueryServiceAutoConfiguration")
public class KnowledgeAutoConfiguration {

    @Bean
    @ConditionalOnBean({QueryService.class, SchemaSource.class, InferencePort.class})
    @ConditionalOnMissingBean(AnswerQuestion.class)
    AnswerQuestion answerQuestion(QueryService queryService, SchemaSource schemaSource, InferencePort inferencePort) {
        return AnswerQuestionFactory.create(queryService, schemaSource, inferencePort);
    }
}
