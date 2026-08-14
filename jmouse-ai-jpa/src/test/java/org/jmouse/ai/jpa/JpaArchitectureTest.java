package org.jmouse.ai.jpa;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Jakarta Persistence and nothing else.
 *
 * <p><strong>Whoever owns the table owns the mapping</strong> — the rule the other jMouse persistence
 * modules already set, and the reason the {@code ai_*} entities are here rather than in a product.
 * What follows from it is the rule below: this module maps tables and demarcates its own transactions,
 * and it must not acquire a framework for either. Transaction demarcation belongs to whoever calls;
 * Spring Data would decide the shape of every query a product might ever want to add beside these.
 */
class JpaArchitectureTest {

    private static JavaClasses persistence;

    @BeforeAll
    static void importThePersistence() {
        persistence = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.jmouse.ai.jpa");
    }

    @Test
    @DisplayName("persistence is Jakarta Persistence, not Spring")
    void staysJakartaPersistence() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "io.modelcontextprotocol..", "org.jmouse.access..")
                .because("transaction demarcation belongs to whoever calls, not to the library. A "
                       + "counter that joined the caller's transaction would be rolled back exactly "
                       + "when the call failed — which is when the count matters most")
                .check(persistence);
    }

    @Test
    @DisplayName("the entities are this module's, and live where the migrations put them")
    void theEntitiesLiveHere() {
        classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("org.jmouse.ai.jpa.entity")
                .because("⚠️ a consuming application names exactly this package in its @EntityScan, and "
                       + "Hibernate validates only what it was given — so an entity that drifted "
                       + "elsewhere would start cleanly and die on its first query")
                .check(persistence);
    }

    @Test
    @DisplayName("nothing here can reach a handler")
    void cannotReachAHandler() {
        noClasses()
                .should().callMethod(org.jmouse.ai.ToolAction.class, "handler")
                .because("this records what happened and remembers what a preview froze. Neither is a "
                       + "reason to be able to run anything")
                .check(persistence);
    }
}
