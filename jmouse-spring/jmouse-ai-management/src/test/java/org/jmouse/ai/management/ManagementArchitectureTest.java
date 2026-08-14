package org.jmouse.ai.management;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * These controllers read, and structurally cannot do anything else.
 *
 * <p>The property a reviewer will want to check, checked. Shipping controllers over a tool catalogue is
 * only safe because <strong>this module cannot become a second way into an action</strong> — and the
 * reason it cannot is not discipline, it is that there is nothing here to call. No dispatcher, no
 * catalogue, no action; the richest thing any endpoint can reach is a {@code PublishedTool}, which
 * carries no handler.
 *
 * <p>Without this rule the guarantee is a paragraph in a package javadoc, honoured by whoever remembers
 * it — and the first person to add a "run this tool from the screen" endpoint would be adding something
 * that looked entirely reasonable in a review.
 */
class ManagementArchitectureTest {

    private static JavaClasses screens;

    @BeforeAll
    static void importTheScreens() {
        screens = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.jmouse.ai.management");
    }

    @Test
    @DisplayName("nothing here holds a dispatcher, a catalogue or an action")
    void nothingHereCanAct() {
        noClasses()
                .should().dependOnClassesThat().haveFullyQualifiedName("org.jmouse.ai.ToolDispatcher")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.jmouse.ai.ToolCatalog")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.jmouse.ai.ToolAction")
                .because("a management screen that could reach a handler would run it with no "
                       + "permission check, no scope, no rate limit and no trace. Splitting the "
                       + "controllers off from the ports was for exactly this, and only this rule keeps "
                       + "the split meaning anything")
                .check(screens);
    }

    @Test
    @DisplayName("every controller reads through a port and nothing else")
    void everyControllerReadsThroughAPort() {
        classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "org.jmouse.ai",            // PublishedTool, and nothing that can act
                        "org.jmouse.ai.view..",     // the four read ports
                        "org.jmouse.ai.management", // the route constants and the one exception
                        "org.springframework..",
                        "java..")
                .because("the dependency list of a controller here is the whole security argument for "
                       + "shipping controllers in a library at all. Anything outside it is a new "
                       + "capability arriving in a module whose claim is that it has none")
                .check(screens);
    }
}
