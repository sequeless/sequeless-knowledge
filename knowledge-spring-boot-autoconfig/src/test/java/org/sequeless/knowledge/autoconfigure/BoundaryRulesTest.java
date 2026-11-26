package org.sequeless.knowledge.autoconfigure;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Mechanically enforced boundaries for the autoconfiguration. It is the host's wiring: it names the
 * knowledge api, the Query api and the schema and kernel types a bean needs, and never the
 * knowledge implementation or the Query side's SPI.
 *
 * <p>The rules analyse test classes too, because the Query SPI rule covers the whole module. The
 * main-code rule selects classes by their source location, so test classes, which sit in the same
 * package, are left out of it.
 */
@AnalyzeClasses(packages = "org.sequeless.knowledge.autoconfigure")
class BoundaryRulesTest {

    /** Main-code classes: those not compiled from {@code src/test}. */
    private static final DescribedPredicate<JavaClass> MAIN_CODE = DescribedPredicate.describe(
            "main code",
            javaClass -> javaClass
                    .getSource()
                    .map(source -> !source.getUri().getPath().contains("/test-classes/"))
                    .orElse(false));

    @ArchTest
    static final ArchRule main_code_depends_only_on_its_declared_packages = noClasses()
            .that(MAIN_CODE)
            .should()
            .dependOnClassesThat()
            .resideOutsideOfPackages(
                    "java..",
                    "org.springframework..",
                    "org.sequeless.knowledge..",
                    "org.sequeless.query.api..",
                    "org.sequeless.ontology.schema.api..",
                    "org.sequeless.ontology.schema.spi..",
                    "org.sequeless.kernel.identity..",
                    "org.sequeless.kernel.values..")
            .as("the autoconfiguration depends on the JDK, Spring, the knowledge api and spi, and the declared "
                    + "Query, schema and kernel contracts only")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule autoconfiguration_does_not_reach_the_knowledge_implementation = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.sequeless.knowledge.internal..")
            .as(
                    "the autoconfiguration wires the use case through the api factory, never org.sequeless.knowledge.internal")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule no_class_reaches_the_query_side_spi = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.sequeless.query.spi..", "org.sequeless.query.field.spi..")
            .as("no class in this module depends on the Query side's driven ports (query.spi, query.field.spi)")
            .allowEmptyShould(false);
}
