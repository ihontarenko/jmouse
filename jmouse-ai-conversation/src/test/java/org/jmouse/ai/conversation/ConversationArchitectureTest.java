package org.jmouse.ai.conversation;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The one place the two mechanisms meet, and the rule is that it stays the only one.
 *
 * <p>This module is allowed to see both a catalogue and a chat model — that is its entire reason for
 * existing, and the rule in {@code jmouse-ai-provider} depends on this one being where the joining
 * happens. What it must not acquire is everything else: Spring, persistence, the protocol SDK, an
 * authorization engine. A conversation loop is plain Java, and a product declaring these beans by hand
 * is the documented way onto an older Boot.
 */
class ConversationArchitectureTest {

    private static JavaClasses conversation;

    @BeforeAll
    static void importTheConversation() {
        conversation = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.jmouse.ai.conversation");
    }

    @Test
    @DisplayName("this is where the two mechanisms meet")
    void thisIsWhereTheyMeet() {
        classes()
                .that().haveSimpleName("ConversationRunner")
                .should().dependOnClassesThat().resideInAPackage("org.jmouse.ai")
                .andShould().dependOnClassesThat().resideInAPackage("org.jmouse.ai.provider")
                .because("the rule forbidding jmouse-ai-provider from seeing the catalogue only means "
                       + "something because the joining happens here. If this stopped being true, that "
                       + "rule would be forbidding something nobody was doing anyway")
                .check(conversation);
    }

    @Test
    @DisplayName("...and it stays plain Java while doing it")
    void staysPlainJava() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "io.modelcontextprotocol..",
                        "org.jmouse.access..")
                .because("the conversation loop is one of the three modules a product on Boot 3 takes "
                       + "and wires by hand. Any of these imports turns twenty lines of wiring into a "
                       + "framework migration")
                .check(conversation);
    }
}
