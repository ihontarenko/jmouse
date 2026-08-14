package org.jmouse.ai.mcp;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.jmouse.ai.ToolDispatcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The one module allowed to see the protocol, and the rule that keeps the SDK inside it.
 *
 * <p>The corresponding rule is stated in every <em>other</em> module — none of them may import
 * {@code io.modelcontextprotocol} — and this is where the positive half lives: the SDK is used here, and
 * only the two classes that are supposed to touch it do.
 *
 * <p>⚠️ <strong>The rule that matters most is the last one.</strong> Both halves of the protocol go
 * through {@link ToolDispatcher} and neither may hold a {@code ToolAction}. A server that could reach a
 * handler would be a second way into an action with no permission check, no scope, no rate limit and no
 * trace — and the whole reason a transport is a transport is that it cannot be.
 */
class McpArchitectureTest {

    private static JavaClasses protocol;

    @BeforeAll
    static void importTheProtocolModule() {
        protocol = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.jmouse.ai.mcp");
    }

    /** The two classes that speak the protocol, named rather than matched by a suffix. */
    private static final String SERVING = "McpToolServer";
    private static final String CONNECTING = "McpSyncRemoteToolServer";

    @Test
    @DisplayName("the SDK is confined to the two classes that speak it")
    void theSdkIsConfined() {
        // ⚠️ Exact names, not haveSimpleNameNotEndingWith("ToolServer"). A suffix predicate also exempts
        // RemoteToolServer — the interface whose whole stated purpose is to be SDK-free — and would
        // hand SDK access to any future *ToolServer without anybody deciding to.
        noClasses()
                .that().doNotHaveSimpleName(SERVING)
                .and().doNotHaveSimpleName(CONNECTING)
                .should().dependOnClassesThat().resideInAPackage("io.modelcontextprotocol..")
                .because("the SDK is how bytes reach this mechanism and not the shape of it. Confining "
                       + "it to the server and the connection keeps the permission model, the "
                       + "registration rules and the failure story testable without a socket — which is "
                       + "most of why RemoteToolServer is an interface")
                .check(protocol);
    }

    @Test
    @DisplayName("...and something in here actually uses it")
    void somethingUsesIt() {
        classes()
                .that().haveSimpleName(SERVING).or().haveSimpleName(CONNECTING)
                .should().dependOnClassesThat().resideInAPackage("io.modelcontextprotocol..")
                .because("a confinement rule that matched no classes at all would pass by describing "
                       + "nothing")
                .check(protocol);
    }

    @Test
    @DisplayName("neither half of the protocol can reach a handler")
    void neitherHalfReachesAHandler() {
        noClasses()
                .should().callMethod(org.jmouse.ai.ToolAction.class, "handler")
                .because("a transport that held the work itself would run it with no permission check, "
                       + "no scope, no rate limit and no trace — and nothing in a review would look "
                       + "wrong. PublishedTool is what makes this hard; this is what makes it permanent")
                .check(protocol);
    }

    @Test
    @DisplayName("the protocol module knows nothing about Spring, persistence or an access engine")
    void staysPlainJava() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "org.jmouse.access..")
                .because("which servlet container serves the protocol, where a pending authorization is "
                       + "stored and what an account is are all the product's. This module speaks the "
                       + "protocol and holds none of those opinions")
                .check(protocol);
    }
}
