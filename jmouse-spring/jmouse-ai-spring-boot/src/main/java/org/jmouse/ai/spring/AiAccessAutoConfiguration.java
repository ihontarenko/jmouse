package org.jmouse.ai.spring;

import org.jmouse.access.AccessEngine;
import org.jmouse.access.PermissionCatalog;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.enforcement.CurrentSubject;
import org.jmouse.ai.access.AccessToolAuthorizer;
import org.jmouse.ai.access.InvocationScopes;
import org.jmouse.ai.access.SubjectAgentOwners;
import org.jmouse.ai.agent.AgentOwners;
import org.jmouse.ai.spi.PermissionVocabulary;
import org.jmouse.ai.spi.ToolAuthorizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One policy, two entry points.
 *
 * <p>Where a product already runs {@code jmouse-access}, its tool authorization becomes the
 * <strong>same engine</strong> as its HTTP authorization rather than a parallel one that drifts. That is
 * worth more than it sounds: without it, every permission a tool costs would have to be kept in step by
 * hand with the permission the equivalent endpoint requires, and the two would agree until the afternoon
 * somebody changed one of them.
 *
 * <p>The bridge itself is two translations and lives in {@code jmouse-ai-access}. This only decides which
 * bean holds it.
 *
 * <h2>The permission vocabulary is the quiet win here</h2>
 *
 * <p>⚠️ Without an access engine the catalogue cannot check that an action's declared permission
 * <em>exists</em> — it can only insist one is present — so a typo produces an action no caller can ever
 * hold the permission for, permanently unreachable, reading as a broken tool rather than as a misspelt
 * word. With {@link PermissionCatalog} in the context that check becomes real and the typo stops the
 * application at startup, naming it.
 */
@AutoConfiguration(before = AiAutoConfiguration.class)
@ConditionalOnClass({AccessToolAuthorizer.class, AccessEngine.class})
public class AiAccessAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ScopeCatalog.class)
    public InvocationScopes aiInvocationScopes(ScopeCatalog scopes) {
        return new InvocationScopes(scopes);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({AccessEngine.class, ScopeCatalog.class})
    public ToolAuthorizer aiAccessToolAuthorizer(AccessEngine engine, InvocationScopes scopes) {
        return new AccessToolAuthorizer(engine, scopes);
    }

    /** Every permission this installation knows about, so the catalogue can refuse one that is not there. */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(PermissionCatalog.class)
    public PermissionVocabulary aiPermissionVocabulary(PermissionCatalog permissions) {
        return permissions::all;
    }

    /**
     * Whose agents these are, for the self-scoped screen.
     *
     * <p>⚠️ <strong>Contributed here rather than written once per product</strong>, because the answer is
     * the same sentence in both: an agent's owner reference is the identifier the engine keys that
     * person's grants on. A product whose two differ has a bug rather than a preference, and writing this
     * per product is how that bug stays hidden.
     *
     * <p>⚠️ Nested and {@code @ConditionalOnClass}, because {@code CurrentSubject} lives in the HTTP
     * enforcement module and this starter is used by applications that do not have it. A condition naming
     * a class that is absent is only safe when the class holding the method is guarded by one.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(CurrentSubject.class)
    public static class SelfScopedAgents {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(CurrentSubject.class)
        public AgentOwners aiSubjectAgentOwners(CurrentSubject subjects) {
            return new SubjectAgentOwners(subjects::get);
        }
    }
}
