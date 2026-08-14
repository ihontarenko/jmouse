package org.jmouse.ai.provider;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Talking to a model is a separate mechanism from being able to do things, and stays one.
 *
 * <p>⚠️ <strong>This module must not learn that a tool catalogue exists.</strong> The two mechanisms
 * meet in exactly one place — {@code jmouse-ai-conversation} — and that is what makes both of them
 * usable alone: an application serving the Model Context Protocol has tools and no chat model, because
 * the model is on the far end of the connection; an application summarising text has a chat model and no
 * tools. One import in this direction collapses both arrangements into one.
 */
class ProviderArchitectureTest {

    private static JavaClasses providers;

    @BeforeAll
    static void importTheProviders() {
        providers = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.jmouse.ai.provider");
    }

    @Test
    @DisplayName("a provider knows nothing about tools")
    void knowsNothingAboutTools() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.jmouse.ai", "org.jmouse.ai.guard..", "org.jmouse.ai.spi..",
                        "org.jmouse.ai.view..")
                .because("the two mechanisms meet in the conversation module and nowhere else. That is "
                       + "what lets a product take tools without a model provider, or a provider "
                       + "without tools — and one import here would make each of them pay for the other")
                .check(providers);
    }

    @Test
    @DisplayName("a provider knows nothing about Spring, persistence or the protocol")
    void staysPlainJava() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "io.modelcontextprotocol..",
                        "org.jmouse.access..")
                .because("a product on Boot 3 that cannot take the starter can still take this jar and "
                       + "declare the beans itself, which is roughly twenty lines. Every one of these "
                       + "imports would end that")
                .check(providers);
    }
}
