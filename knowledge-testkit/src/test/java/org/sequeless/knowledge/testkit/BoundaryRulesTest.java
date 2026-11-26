package org.sequeless.knowledge.testkit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Mechanically enforced boundaries for the testkit. The testkit exercises the SPI from outside, so
 * it depends on the knowledge contract and on the test frameworks an adapter's tests use, and
 * never on the implementation.
 */
@AnalyzeClasses(packages = "org.sequeless.knowledge.testkit", importOptions = ImportOption.DoNotIncludeTests.class)
class BoundaryRulesTest {

    @ArchTest
    static final ArchRule testkit_is_free_of_spring = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .as("the testkit is framework-neutral: no Spring dependency")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule testkit_depends_only_on_its_declared_packages = noClasses()
            .should()
            .dependOnClassesThat()
            .resideOutsideOfPackages(
                    "java..",
                    "org.sequeless.knowledge..",
                    "org.junit.jupiter..",
                    "org.opentest4j..",
                    "org.assertj..",
                    "org.apiguardian..")
            .as("the testkit depends on the JDK, the knowledge contract, JUnit, AssertJ and their annotations only")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule testkit_never_reaches_the_knowledge_implementation = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.sequeless.knowledge.internal..")
            .as("the testkit tests the SPI, not the implementation in org.sequeless.knowledge.internal")
            .allowEmptyShould(false);
}
