package org.jmouse.access.spring.policy;

import org.jmouse.access.CapabilityCatalog;
import org.jmouse.access.CapabilityResolver;
import org.jmouse.access.PermissionCatalog;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.el.loader.PolicyLoader;
import org.jmouse.access.el.loader.PolicySources;
import org.jmouse.access.policy.CompositeEntitlementStore;
import org.jmouse.access.policy.CompositeGrantStore;
import org.jmouse.access.policy.ConditionCompiler;
import org.jmouse.access.policy.LivePolicy;
import org.jmouse.access.policy.PlaceholderResolver;
import org.jmouse.access.policy.PolicyBinder;
import org.jmouse.access.policy.PolicyException;
import org.jmouse.access.policy.LivePolicyEntitlements;
import org.jmouse.access.policy.PolicyEntitlementStore;
import org.jmouse.access.policy.PolicyEntitlementStore.SubjectHandleResolver;
import org.jmouse.access.policy.PolicyGrantStore;
import org.jmouse.access.policy.QuantityScale;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.spi.CapabilitySwitchStore;
import org.jmouse.access.spi.EntitlementStore;
import org.jmouse.access.spi.GrantStore;
import org.jmouse.access.spi.ScopeHierarchy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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

import java.time.Clock;
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

    /** The composed entitlement store's name, excluded from its own composition for the same reason. */
    static final String COMPOSED_ENTITLEMENTS = "accessEntitlementStore";

    /** The document's own capability grants, composed rather than composed with. */
    static final String POLICY_ENTITLEMENTS = "policyEntitlementStore";

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

    // ── The capability half ───────────────────────────────────────────────────

    /**
     * The document's {@code entitlements &#123; &#125;}, as a store.
     *
     * <p>⚠️ <strong>A block that parses and is never read is worse than no block.</strong> That is not
     * hypothetical: {@code PolicyEntitlementStore} existed, nothing wired it, and every entitlement
     * anybody wrote into a document was parsed, projected, and silently ignored at decision time. This
     * bean is what makes the block mean something, and it is why the requirement below refuses a
     * context rather than logging.
     *
     * <p>{@code SubjectHandleResolver} is the product's: a document says {@code @ORGANIZATION:acme}
     * because a slug is what a person can write down, and only the product knows what {@code acme} is.
     * Without one the handle is the identifier, which is right for an installation that writes
     * identifiers and wrong silently for one that does not — hence the default is the identity and the
     * javadoc says so rather than the code guessing.
     */
    @Bean(POLICY_ENTITLEMENTS)
    @ConditionalOnMissingBean(name = POLICY_ENTITLEMENTS)
    public EntitlementStore policyEntitlementStore(
            LivePolicy live,
            ScopeCatalog scopes,
            ObjectProvider<SubjectHandleResolver> handles,
            ObjectProvider<QuantityScale> scale) {

        // ⚠️ Over LivePolicy, never over the startup document. The reference moves when something
        // adopts a candidate, and a store built once would go on answering from the policy that was in
        // force at boot — see LivePolicyEntitlements for why that is the failure nobody would notice.
        return new LivePolicyEntitlements(
                live,
                scopes,
                handles.getIfAvailable(() -> (kind, handle) -> handle),
                scale.getIfAvailable(() -> QuantityScale.PLAIN));
    }

    /**
     * Every source of capability grants, read as one — {@link #accessGrantStore} for the other axis.
     *
     * <p>Assembled by bean name for the identical reason, and it is the same circularity: asking the
     * container for all {@code EntitlementStore} beans while building one asks it for this one.
     *
     * <p>⚠️ <strong>Primary, and that is not tidiness.</strong> A product contributing its own store
     * has two beans of the type before this makes a third, and every injection point is then ambiguous
     * — a context that refuses to start, which is the right failure and still a failure. A declared
     * preference is what fixes it once; a qualifier at each use would leave the next injection point
     * somebody adds silently reading one half of the answer.
     */
    @Bean(COMPOSED_ENTITLEMENTS)
    @Primary
    @ConditionalOnMissingBean(name = COMPOSED_ENTITLEMENTS)
    public EntitlementStore accessEntitlementStore(
            @Qualifier(POLICY_ENTITLEMENTS) EntitlementStore declared,
            ListableBeanFactory beans) {

        List<EntitlementStore> stores = new ArrayList<>();

        stores.add(declared);

        for (String name : beans.getBeanNamesForType(EntitlementStore.class, true, false)) {
            if (!COMPOSED_ENTITLEMENTS.equals(name) && !POLICY_ENTITLEMENTS.equals(name)) {
                stores.add(beans.getBean(name, EntitlementStore.class));
            }
        }

        if (stores.size() == 1) {
            return declared;
        }

        LOGGER.info("Capability grants are read from {} stores, the document first", stores.size());

        return new CompositeEntitlementStore(stores);
    }

    /**
     * Where a capability stands — the third axis, resolved by the engine.
     *
     * <p>⚠️ <strong>Registered unconditionally, and the first attempt to condition it on a
     * {@code CapabilityCatalog} was a mistake worth recording.</strong> {@code @ConditionalOnBean} over
     * a type the *product* declares is evaluated against whatever has been registered by the time this
     * auto-configuration is processed, and it silently does not match often enough that Spring's own
     * documentation warns against it. The failure mode is the worst available: no resolver, and a
     * context that dies naming a bean nobody can see why is missing.
     *
     * <p>Registering it always is also simply correct. A resolver needs a store and a containment, not
     * a catalogue — and over an empty store it answers {@code UNGRANTED} for everything, which nothing
     * asks, because a product with no capability vocabulary has no entitlement axis to ask. Opting in
     * happens by *declaring a vocabulary*, which is where it belongs; this bean costs an object.
     */
    @Bean
    @ConditionalOnMissingBean
    public CapabilityResolver capabilityResolver(
            @Qualifier(COMPOSED_ENTITLEMENTS) EntitlementStore entitlements,
            ObjectProvider<ScopeHierarchy> hierarchy,
            ObjectProvider<Clock> clock) {

        return new CapabilityResolver(
                entitlements,
                // ⚠️ Flat by default: an application whose authorization is "these people hold these
                // roles", with no places at all, must not have to write a class with two empty methods
                // to say so. What a product has not adopted costs it nothing.
                hierarchy.getIfAvailable(ScopeHierarchy::flat),
                clock.getIfAvailable(Clock::systemUTC));
    }

    /**
     * What a place has switched <strong>off</strong> — axis 4's seam, defaulted to nothing.
     *
     * <p>Default-on is the engine's assumption and the store names the exceptions, so an application
     * with no switches at all registers nothing and every capability it has is simply on. A store that
     * listed what was <em>on</em> would have to know the catalogue to be complete, and would answer
     * wrongly for every capability introduced after its rows were written.
     */
    @Bean
    @ConditionalOnMissingBean
    public CapabilitySwitchStore capabilitySwitchStore() {
        return CapabilitySwitchStore.allOn();
    }

    /**
     * ⚠️ <strong>A document that declares capabilities and has nowhere to resolve them refuses to
     * start.</strong>
     *
     * <p>The asymmetry with the paragraph above is deliberate and is the whole rule: <em>saying
     * nothing</em> is a product that has not adopted the axis, and <em>saying something nothing
     * reads</em> is a rule written down that does nothing. The first is fine. The second is the same
     * class of hole as a condition nobody evaluates, and it is the one that actually happened here.
     */
    @Bean
    public PolicyEntitlementRequirement policyEntitlementRequirement(
            PolicyDocument document, ObjectProvider<CapabilityCatalog> catalogue) {

        if (!document.capabilities().isEmpty() && catalogue.getIfAvailable() == null) {
            throw new PolicyException(
                    "'" + document.name() + "' declares " + document.capabilities().size()
                    + " capability/capabilities, and this application registers no CapabilityCatalog "
                    + "for them to be resolved against. A capabilities block with nothing reading it "
                    + "parses, projects, and decides nothing — which is worse than not writing one. "
                    + "Register a CapabilityCatalog bean, or remove the block.");
        }

        return new PolicyEntitlementRequirement();
    }

    /** Nothing but a hook for the check above to run inside — it holds no state and answers nothing. */
    public static final class PolicyEntitlementRequirement {
    }
}
