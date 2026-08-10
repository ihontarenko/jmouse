package org.jmouse.access.spring.policy;

import org.jmouse.access.PermissionCatalog;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.el.loader.PolicyLoader;
import org.jmouse.access.el.loader.PolicySources;
import org.jmouse.access.policy.CompositeGrantStore;
import org.jmouse.access.policy.ConditionCompiler;
import org.jmouse.access.policy.LivePolicy;
import org.jmouse.access.policy.PlaceholderResolver;
import org.jmouse.access.policy.PolicyBinder;
import org.jmouse.access.policy.PolicyException;
import org.jmouse.access.policy.PolicyGrantStore;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.spi.GrantStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns "there are {@code .jmp} files on the classpath" into a working {@link GrantStore}.
 *
 * <p>Load, parse, follow the includes, merge, bind, serve — once, at startup, and every step of it
 * before the application answers its first request.
 *
 * <h2>⚠️ A failure here stops the context, on purpose</h2>
 *
 * <p>Nothing below degrades. A location naming no file, an include pointing at nothing, a scope the
 * application does not register, a permission nobody declared, a placeholder with no property: all of
 * them throw out of a {@code @Bean} method and the application does not start. An installation whose
 * policy half-loaded has permissions that are neither what the files say nor what they said
 * yesterday, and there is no log line that makes that better than a refusal.
 *
 * <h2>Composed with, not instead of</h2>
 *
 * <p>Policy files fit <strong>structure</strong> — roles and their bundles, the bootstrap grants.
 * They do not fit <strong>assignments</strong>: "give Petro access to this workspace" must not need a
 * deploy. So where an application already has a store of its own, this contributes a second one
 * beside it rather than replacing it, and the engine cannot tell them apart.
 */
@AutoConfiguration
@ConditionalOnClass({PolicyLoader.class, PolicyGrantStore.class})
@ConditionalOnBean(ScopeCatalog.class)
@Conditional(OnPolicyLocationsCondition.class)
@EnableConfigurationProperties(AccessPolicyProperties.class)
public class AccessPolicyAutoConfiguration {

    /** The composed store's bean name — known here because composing has to leave itself out. */
    static final String COMPOSED_STORE = "accessGrantStore";

    /** The policy's own store, which is composed rather than composed with. */
    static final String POLICY_STORE = "livePolicy";

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessPolicyAutoConfiguration.class);

    /**
     * @param resources the context's own loader, wrapped so that {@code classpath:policy/*.jmp}
     *                  matches rather than being looked up as a file with a star in its name
     */
    @Bean
    @ConditionalOnMissingBean
    public PolicySources accessPolicySources(ResourceLoader resources) {
        return new SpringPolicySources(new PathMatchingResourcePatternResolver(resources));
    }

    @Bean
    @ConditionalOnMissingBean
    public PlaceholderResolver accessPolicyPlaceholders(Environment environment) {
        return new EnvironmentPlaceholders(environment);
    }

    /**
     * Everything the configured <strong>locations</strong> say, as one document.
     *
     * <p>⚠️ This is what was on disk at startup and stays that way for the life of the context. It is
     * not <em>the policy in force</em> — that is {@link LivePolicy#document()}, which starts as this
     * and moves whenever something adopts a candidate. A screen showing the current policy must read
     * the live one; a screen showing what shipped with the application reads this.
     */
    @Bean
    @ConditionalOnMissingBean
    public PolicyDocument accessPolicyDocument(PolicySources sources, AccessPolicyProperties properties) {
        PolicyDocument document = new PolicyLoader(sources).load(properties.getName(), properties.getLocations());

        LOGGER.info("Policy '{}' loaded from {}: {} role(s), {} subject(s)",
                document.name(), properties.getLocations(), document.roles().size(), document.subjects().size());

        return document;
    }

    /**
     * How a document becomes engine types, against the vocabulary this installation registered.
     *
     * <p>Exposed as a bean rather than kept inside the store because binding is needed twice more
     * after startup: once to <em>rehearse</em> a candidate policy somebody is about to save, and once
     * to adopt it.
     *
     * @param permissions ⚠️ required, and the reason the whole feature is safe to use. Without a
     *                    catalogue {@code @SPACE frm:write} loads, matches nothing, and says nothing
     */
    @Bean
    @ConditionalOnMissingBean
    public PolicyBinder accessPolicyBinder(
            ScopeCatalog                      scopes,
            ObjectProvider<PermissionCatalog> permissions,
            ObjectProvider<ConditionCompiler> conditions,
            PlaceholderResolver               placeholders) {

        PermissionCatalog declared = permissions.getIfAvailable(() -> {
            throw new PolicyException(
                    "Policy files are configured, and this application registers no PermissionCatalog. "
                    + "A permission is a bare string everywhere it is asked about, so without the "
                    + "catalogue a file writing 'frm:write' would load, match nothing, and say nothing "
                    + "— which is the one failure writing authorization down exists to prevent.");
        });

        return new PolicyBinder(scopes, declared, conditions.getIfAvailable(), placeholders);
    }

    /**
     * The bound policy, and the one reference in the application that may be replaced while it runs.
     *
     * <p>⚠️ Injected as {@link LivePolicy} rather than as {@link PolicyGrantStore} wherever the current
     * policy matters: the store behind it is swapped whole, so a component holding the store directly
     * would keep answering from the revision it was wired with.
     */
    @Bean(POLICY_STORE)
    @ConditionalOnMissingBean
    public LivePolicy livePolicy(PolicyDocument document, PolicyBinder binder) {
        return new LivePolicy(binder, document);
    }

    /**
     * The store the engine reads: the policy, plus whatever else this application keeps grants in.
     *
     * <p>⚠️ <strong>Assembled by bean name rather than by injecting every {@link GrantStore}.</strong>
     * Asking a container for all beans of a type while building a bean of that type asks it for this
     * one, which is a circular reference — so the two names this configuration owns are excluded and
     * the rest are collected. Ugly, and the alternative is an application that starts sometimes.
     *
     * <p>Order is presentation and not precedence: a store listed first does not win, because deny
     * already wins over every allow regardless of where either came from. Policy is listed first so
     * the structural half of an answer appears first in the control room.
     */
    @Bean(COMPOSED_STORE)
    @Primary
    @ConditionalOnMissingBean(name = COMPOSED_STORE)
    public GrantStore accessGrantStore(LivePolicy policy, ListableBeanFactory beans) {
        List<GrantStore> stores = new ArrayList<>();

        stores.add(policy);

        for (String name : beans.getBeanNamesForType(GrantStore.class, true, false)) {
            if (!COMPOSED_STORE.equals(name) && !POLICY_STORE.equals(name)) {
                stores.add(beans.getBean(name, GrantStore.class));
            }
        }

        if (stores.size() == 1) {
            return policy;
        }

        LOGGER.info("Access grants are read from {} stores, policy first", stores.size());

        return new CompositeGrantStore(stores);
    }
}
