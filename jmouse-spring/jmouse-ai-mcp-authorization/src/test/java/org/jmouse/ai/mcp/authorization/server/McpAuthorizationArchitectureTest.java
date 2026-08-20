package org.jmouse.ai.mcp.authorization.server;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * <strong>This module walks a protocol. It must never learn what an account is.</strong>
 *
 * <p>That sentence is the whole design, and it is the kind that decays quietly: the first product to
 * need something the seam does not expose can get it by widening a record by one field, and the change
 * looks like a convenience rather than the end of the arrangement. What the two products here disagree
 * about is not a detail — one issues a credential to a <em>sub-account</em> with its own permissions and
 * the other to the approving person themselves — so a vocabulary leaking in either direction makes the
 * module wrong for the other one.
 *
 * <p>No Spring context and no socket: it reads the compiled classes and nothing else.
 */
class McpAuthorizationArchitectureTest {

    /** The words a product uses for what a credential is issued against. None of them belong here. */
    private static final List<String> ACCOUNT_WORDS = List.of("account", "user", "member", "agent");

    private static JavaClasses module;

    @BeforeAll
    static void importTheModule() {
        module = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.jmouse.ai.mcp.authorization.server");
    }

    @Test
    @DisplayName("no type here names an account, a user, a member or an agent")
    void namesNothingAboutIdentity() {
        classes()
                .should(new ArchCondition<>("avoid every word for what a credential is issued against") {

                    @Override
                    public void check(JavaClass type, ConditionEvents events) {
                        String name = type.getSimpleName().toLowerCase(Locale.ROOT);

                        ACCOUNT_WORDS.stream()
                                .filter(name::contains)
                                .forEach(word -> events.add(SimpleConditionEvent.violated(type,
                                        type.getName() + " says '" + word + "'. What a credential is "
                                        + "issued against is the product's, and reaches this module only "
                                        + "as an opaque reference — see ApprovingSubject")));
                    }
                })
                .because("one product issues to a sub-account with its own permissions and the other to "
                       + "the approving person; a module that knew the difference would be wrong for one "
                       + "of them")
                .check(module);
    }

    /**
     * ⚠️ <strong>Narrowed, and the narrowing is the point.</strong> This used to name the whole module,
     * from back when nothing here was durable. Then a client's name turned out to need a table — losing
     * one on restart baked <em>An unnamed client</em> into a connection row, and in one product into an
     * agent's name — so {@code JpaClientNameRegistry} arrived and this rule was quietly false.
     *
     * <p>What it was actually protecting survives: the <em>flow</em> must not know where anything is kept.
     * A storage adapter that a product may replace with its own bean is the opposite arrangement, and
     * naming the classes it may not reach is what keeps the rule true instead of merely present.
     */
    @Test
    @DisplayName("the flow itself stores nothing, and knows where nothing is stored")
    void storesNothing() {
        noClasses()
                .that().haveSimpleNameNotEndingWith("ClientNameRegistry")
                .and().doNotHaveSimpleName("McpAuthorizationAutoConfiguration")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..", "org.springframework.data..", "javax.sql..")
                .because("where a one-time code lives is AuthorizationCodeStore's, and the two products "
                       + "answer it differently on purpose — a map that forgets on restart and a Redis "
                       + "key that does not are both correct")
                .check(module);
    }

    @Test
    @DisplayName("nothing here depends on a product")
    void dependsOnNoProduct() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage("net.innoventa..", "com.innoventa..")
                .because("a library that reached back into one of its consumers would stop being usable "
                       + "by the other, which is the entire failure this module was extracted to end")
                .check(module);
    }

    @Test
    @DisplayName("nothing here speaks the protocol itself")
    void speaksNoProtocol() {
        noClasses()
                .should().dependOnClassesThat().resideInAPackage("io.modelcontextprotocol..")
                .because("this is how a client gets a credential, not what it then does with one. The "
                       + "SDK is confined to jmouse-ai-mcp's two transport classes, and a dependency "
                       + "here would mean an installation could not host authorization without also "
                       + "hosting a tool server")
                .check(module);
    }
}
