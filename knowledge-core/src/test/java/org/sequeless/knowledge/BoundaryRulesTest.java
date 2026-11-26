package org.sequeless.knowledge;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Mechanically enforced hexagonal boundaries for the knowledge core. The api and spi layers are the
 * published contract; internal is implementation detail. The module depends only on the JDK and on
 * the Sequeless contracts it is allowed to name.
 */
@AnalyzeClasses(
        packages = "org.sequeless.knowledge",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class BoundaryRulesTest {

    @ArchTest
    static final ArchRule module_is_free_of_spring = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .as("this module must stay transport-agnostic: no Spring dependency");

    @ArchTest
    static final ArchRule api_and_spi_types_are_public = classes()
            .that()
            .resideInAnyPackage("org.sequeless.knowledge.api..", "org.sequeless.knowledge.spi..")
            .and()
            .areTopLevelClasses()
            .and()
            .haveSimpleNameNotEndingWith("package-info")
            .should()
            .bePublic()
            .as("the published contract (api + spi) must be exported as public types")
            .allowEmptyShould(false);

    /**
     * The api and spi classes other than {@code AnswerQuestionFactory} never depend on internal. The
     * factory is excluded here and held to its own rule below.
     */
    @ArchTest
    static final ArchRule api_and_spi_never_depend_on_internal = noClasses()
            .that()
            .resideInAnyPackage("org.sequeless.knowledge.api..", "org.sequeless.knowledge.spi..")
            .and()
            .doNotHaveFullyQualifiedName("org.sequeless.knowledge.api.AnswerQuestionFactory")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.sequeless.knowledge.internal..")
            .as("api and spi must not depend on internal, which is implementation detail")
            .allowEmptyShould(false);

    /**
     * {@code AnswerQuestionFactory} is the one api class allowed to depend on internal: it wires the
     * default implementation, as query-core's {@code QueryServiceFactory} does. Its only internal
     * dependency may be {@code DefaultAnswerQuestion}.
     */
    @ArchTest
    static final ArchRule factory_reaches_internal_only_through_default_answer_question = classes()
            .that()
            .haveFullyQualifiedName("org.sequeless.knowledge.api.AnswerQuestionFactory")
            .should(dependOnInternalOnly("org.sequeless.knowledge.internal.DefaultAnswerQuestion"))
            .as("AnswerQuestionFactory may depend on internal only through DefaultAnswerQuestion")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule module_depends_only_on_its_declared_packages = classes()
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "java..",
                    "org.sequeless.knowledge..",
                    "org.sequeless.kernel.identity..",
                    "org.sequeless.kernel.values..",
                    "org.sequeless.ontology.schema.api..",
                    "org.sequeless.ontology.schema.spi..",
                    "org.sequeless.query.api..",
                    "org.sequeless.query.field.api..")
            .as("this module depends on the JDK, itself, and the declared Sequeless contracts only")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule module_never_reaches_query_or_ontology_internals = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.sequeless.query.spi..",
                    "org.sequeless.query.internal..",
                    "org.sequeless.query.field.spi..",
                    "org.sequeless.query.field.internal..",
                    "org.sequeless.ontology.behaviour..",
                    "org.sequeless.ontology.automation..")
            .as(
                    "this module uses the Query side only through its api, and never the ontology behaviour or automation halves");

    /**
     * Keeps the direct dependencies of the class that land in the internal package, and fails for
     * each one whose target is not {@code allowedTarget}.
     */
    private static ArchCondition<JavaClass> dependOnInternalOnly(String allowedTarget) {
        return new ArchCondition<JavaClass>("depend on internal only through " + allowedTarget) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String pkg = target.getPackageName();
                    boolean internal = pkg.equals("org.sequeless.knowledge.internal")
                            || pkg.startsWith("org.sequeless.knowledge.internal.");
                    if (internal && !target.getFullName().equals(allowedTarget)) {
                        events.add(SimpleConditionEvent.violated(
                                javaClass,
                                javaClass.getName() + " depends on internal class " + target.getFullName()
                                        + ", but only " + allowedTarget + " is allowed"));
                    }
                }
            }
        };
    }
}
