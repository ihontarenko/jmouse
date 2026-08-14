package org.jmouse.ai;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The rules that decay silently.
 *
 * <p>Everything else in this suite fails when somebody breaks it. These fail when somebody adds
 * something reasonable: one convenient import, one shortcut past the dispatcher, and the module is a
 * different module — with nothing about the change looking wrong in a review, because each individual
 * step is fine and only the destination is not.
 *
 * <p>Run through {@code ArchRule.check} from ordinary JUnit rather than through the ArchUnit engine, so
 * that a build reporting "no tests ran" cannot be the way these stop being enforced.
 */
class LibraryArchitectureTest {

    private static JavaClasses mechanism;

    @BeforeAll
    static void importTheMechanism() {
        mechanism = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.jmouse.ai");
    }

    @Test
    @DisplayName("the mechanism knows nothing about Spring")
    void knowsNothingAboutSpring() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                .because("this module is plain Java, and that is the entire premise of it. A product "
                       + "on Boot 3, on no Boot at all, or on something that is not Spring takes this "
                       + "jar unchanged — and one convenient import is all it takes for that to stop "
                       + "being true, permanently, without anything looking wrong in the review");

        rule.check(mechanism);
    }

    @Test
    @DisplayName("the mechanism knows nothing about persistence")
    void knowsNothingAboutPersistence() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "javax.sql..")
                .because("what the guards remember is behind ConfirmationStore and DuplicateCallStore, "
                       + "so a product with no database still gets every guard. An entity here would "
                       + "make persistence the price of a rate limit");

        rule.check(mechanism);
    }

    @Test
    @DisplayName("the mechanism knows nothing about the Model Context Protocol")
    void knowsNothingAboutTheProtocol() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAnyPackage("io.modelcontextprotocol..")
                .because("the protocol is one transport over this mechanism and not the shape of it. "
                       + "Every other module must be usable by a product that has never heard of it");

        rule.check(mechanism);
    }

    @Test
    @DisplayName("the mechanism knows nothing about any authorization engine")
    void knowsNothingAboutAccess() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAnyPackage("org.jmouse.access..")
                .because("what a permission MEANS is ToolAuthorizer's. This module insists one is "
                       + "present and never interprets it, which is what lets a product with its own "
                       + "authorization take this without taking a second engine beside its first");

        rule.check(mechanism);
    }

    @Test
    @DisplayName("only the dispatcher can resolve an action from the catalogue")
    void onlyTheDispatcherResolvesAnAction() {
        ArchRule rule = noClasses()
                .that().doNotHaveFullyQualifiedName(ToolDispatcher.class.getName())
                .should().callMethod(ToolCatalog.class, "find", String.class)
                .because("PublishedTool makes this hard and this rule makes it permanent. The one door "
                       + "to a handler is package-private so that every path to one passes identity, "
                       + "permission, scope and guards — and widening it would cost nothing at the "
                       + "moment it was done and everything the first time a transport found it "
                       + "convenient");

        rule.check(mechanism);
    }

    @Test
    @DisplayName("only the dispatcher reaches an action's handler")
    void onlyTheDispatcherReachesAHandler() {
        ArchRule rule = noClasses()
                .that().doNotHaveFullyQualifiedName(ToolDispatcher.class.getName())
                .and().doNotHaveFullyQualifiedName(ToolCatalog.class.getName())
                .should().callMethod(ToolAction.class, "handler")
                .because("the catalogue is allowed exactly one use of it — checking at startup that a "
                       + "handler is present — and nothing else in the module may hold the work itself. "
                       + "A transport that could would run it with no permission check, no scope, no "
                       + "rate limit and no trace");

        rule.check(mechanism);
    }

    /**
     * ⚠️ A negative rule that nothing could ever match passes for the wrong reason.
     *
     * <p>The two rules above say <em>nobody except the dispatcher</em> does these things. If ArchUnit
     * were not seeing these calls at all — a package-private method it skipped, a record accessor
     * resolved some other way — both would be green and both would be meaningless. This asserts the
     * positive half, so their silence is evidence rather than absence.
     */
    @Test
    @DisplayName("...and those two rules are looking at something")
    void theHandlerRulesAreLookingAtSomething() {
        classes()
                .that().haveFullyQualifiedName(ToolDispatcher.class.getName())
                .should().callMethod(ToolCatalog.class, "find", String.class)
                .andShould().callMethod(ToolAction.class, "handler")
                .because("if these calls were invisible to the importer, the rules forbidding everybody "
                       + "else from making them would pass by matching nothing")
                .check(mechanism);
    }

    @Test
    @DisplayName("the read ports cannot reach anything that acts")
    void theReadPortsOnlyRead() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("org.jmouse.ai.view..")
                .should().dependOnClassesThat().haveFullyQualifiedName(ToolDispatcher.class.getName())
                .because("a management screen is a reader. The moment a read port can hold the "
                       + "dispatcher, the module of controllers over these ports becomes a second way "
                       + "into an action — which is exactly the thing splitting them off was for");

        rule.check(mechanism);
    }
}
