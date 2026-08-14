package org.jmouse.ai.access;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * A bridge, and it stays a bridge.
 *
 * <p>Two translations is the whole of this module: asking the engine whether the caller holds the
 * action's permission at the invocation's scope, and turning one vocabulary's idea of a place into the
 * other's. Everything it is tempted to grow — a cache, a policy opinion, a second authorizer for the
 * case where the engine says no — belongs on one side or the other, because both sides already have
 * somewhere for it and a bridge that has opinions is a third source of authorization decisions.
 */
class AccessBridgeArchitectureTest {

    private static JavaClasses bridge;

    @BeforeAll
    static void importTheBridge() {
        bridge = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.jmouse.ai.access");
    }

    @Test
    @DisplayName("the bridge knows nothing about Spring, persistence or the protocol")
    void staysPlainJava() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "io.modelcontextprotocol..")
                .because("which bean holds this is the starter's business, and it is a starter that "
                       + "wires it in twelve lines. A module that needed a container to be useful would "
                       + "be unusable by the products this exists for")
                .check(bridge);
    }

    @Test
    @DisplayName("the bridge cannot reach a handler")
    void cannotReachAHandler() {
        noClasses()
                .should().callMethod(org.jmouse.ai.ToolAction.class, "handler")
                .because("this answers a question about an action and never runs one. An authorizer "
                       + "that could invoke what it was asked about would be the strangest possible "
                       + "place for a second path into a domain service")
                .check(bridge);
    }
}
