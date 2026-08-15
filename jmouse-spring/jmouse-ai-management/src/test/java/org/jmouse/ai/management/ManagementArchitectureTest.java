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
 * Nothing here can invoke a tool, and that is structural rather than careful.
 *
 * <p>The property a reviewer will want to check, checked. Shipping controllers over a tool catalogue is
 * only safe because <strong>this module cannot become a second way into an action</strong> — and the
 * reason it cannot is not discipline, it is that there is nothing here to call. No dispatcher, no
 * catalogue, no action; the richest thing any endpoint can reach is a {@code PublishedTool}, which
 * carries no handler.
 *
 * <p>⚠️ <strong>One controller writes, and the rule had to be told the difference.</strong>
 * {@link ProviderAdministrationController} changes which model this application talks to, through
 * {@code org.jmouse.ai.administration} — a port that reaches settings and nothing that can act. So the
 * allowed list gained a package, and the rule below gained the type names that actually matter: widening
 * a package list is exactly how a guarantee gets weakened by accident, and naming the three types keeps
 * the claim honest independently of which packages happen to be permitted.
 *
 * <p>Without these rules the guarantee is a paragraph in a package javadoc, honoured by whoever remembers
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
    @DisplayName("every controller works through a port and nothing else")
    void everyControllerWorksThroughAPort() {
        classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "org.jmouse.ai",                     // PublishedTool, and nothing that can act
                        "org.jmouse.ai.view..",              // the four read ports
                        "org.jmouse.ai.administration..",    // the one write port — settings, never a tool
                        // Two more write ports, and they pass the same test as the one above: an agent
                        // and a connection are rows ABOUT who may call, and nothing in that package can
                        // run anything. Switching an agent off is a large power and a small capability.
                        "org.jmouse.ai.agent..",
                        "org.jmouse.ai.management",          // the route constants and the one exception
                        "org.springframework..",
                        "java..")
                .because("the dependency list of a controller here is the whole security argument for "
                       + "shipping controllers in a library at all. Anything outside it is a new "
                       + "capability arriving in a module whose claim is that it has none")
                .check(screens);
    }
}
