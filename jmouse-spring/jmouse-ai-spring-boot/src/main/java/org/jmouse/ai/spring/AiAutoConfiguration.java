package org.jmouse.ai.spring;

import org.jmouse.ai.GuardRoster;
import org.jmouse.ai.ToolCatalog;
import org.jmouse.ai.ToolDefinition;
import org.jmouse.ai.ToolDispatcher;
import org.jmouse.ai.guard.CeilingGuard;
import org.jmouse.ai.guard.ConfirmationGuard;
import org.jmouse.ai.guard.DeduplicationGuard;
import org.jmouse.ai.guard.EmptyDestructionGuard;
import org.jmouse.ai.guard.GuardChain;
import org.jmouse.ai.guard.GuardSettings;
import org.jmouse.ai.guard.InMemoryConfirmationStore;
import org.jmouse.ai.guard.InMemoryDuplicateCallStore;
import org.jmouse.ai.guard.InvocationGuard;
import org.jmouse.ai.guard.RateLimitGuard;
import org.jmouse.ai.guard.TokenBucketCallerRateLimiter;
import org.jmouse.ai.spi.CallerRateLimiter;
import org.jmouse.ai.spi.CallerResolver;
import org.jmouse.ai.spi.ConfirmationStore;
import org.jmouse.ai.spi.DuplicateCallStore;
import org.jmouse.ai.spi.InvocationTrace;
import org.jmouse.ai.spi.PermissionVocabulary;
import org.jmouse.ai.spi.ScopeResolver;
import org.jmouse.ai.spi.ToolAuthorizer;
import org.jmouse.ai.view.ProviderRegistry;
import org.jmouse.ai.view.ToolCallHistory;
import org.jmouse.ai.view.ToolCatalogView;
import org.jmouse.ai.view.UsageTotals;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * The mechanism, wired.
 *
 * <p>Wiring and nothing but wiring. Every decision worth arguing about was made in {@code jmouse-ai};
 * this decides which Spring bean holds which piece of it, and every one of those decisions backs off to
 * the application's own bean where there is one.
 *
 * <h2>How a capability arrives</h2>
 *
 * <p><strong>A ninth capability costs a bean where the capability lives, and nothing central is
 * edited.</strong> {@link ToolDefinition} beans are collected wherever they are declared, so a feature
 * offers an action without importing a tool server and no class ever becomes the one place that knows
 * every feature's name. That direction is the whole arrangement, and it is why this reads the
 * definitions rather than being handed a list.
 *
 * <h2>Two defaults that are deliberately permissive, and are said out loud</h2>
 *
 * <p>⚠️ {@link CallerResolver#anonymous()} and {@link ToolAuthorizer#permitAll()} are the defaults,
 * because an application with one tool definition and no configuration has to start and dispatch — that
 * is what makes this adoptable in an afternoon. But an application that reaches production still holding
 * them has an assistant nobody is authorizing, so {@link AiDiagnostics} says so at every startup, at
 * warning level, naming both. A default that is dangerous and silent is a different thing from a default
 * that is dangerous and announced.
 *
 * <p>The one default that is <em>not</em> permissive is {@link ScopeResolver#refusing()}, and for the
 * matching reason: an action declared as running inside a scope, in an application with no way to
 * resolve one, must not quietly run everywhere.
 */
@AutoConfiguration
@ConditionalOnClass(ToolCatalog.class)
@EnableConfigurationProperties(AiProperties.class)
public class AiAutoConfiguration {

    // ── The numbers ──────────────────────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    public GuardSettings aiGuardSettings(AiProperties properties) {
        AiProperties.Guards guards = properties.getGuards();

        return new GuardSettings(
                guards.getPerCallCeiling(),
                guards.getConfirmationThreshold(),
                guards.getConfirmationLifetime(),
                guards.getDeduplicationWindow());
    }

    // ── What the guards remember ─────────────────────────────────────────────────

    /**
     * ⚠️ In memory unless something else supplies one — which means a preview issued by one instance
     * cannot be redeemed by another, and every outstanding preview is lost on restart. Both are
     * survivable (the caller previews again) and both are surprising in a cluster, which is why
     * {@code jmouse-ai-jpa} exists.
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfirmationStore aiConfirmationStore(GuardSettings settings) {
        return new InMemoryConfirmationStore(settings.confirmationLifetime());
    }

    @Bean
    @ConditionalOnMissingBean
    public DuplicateCallStore aiDuplicateCallStore(GuardSettings settings) {
        return new InMemoryDuplicateCallStore(settings.deduplicationWindow());
    }

    /**
     * The bucket. Whether anything <em>consults</em> it is {@code jmouse.ai.rate-limit.enabled}, which
     * governs the guard rather than this — one switch, in one place, and a roster that can then report
     * the guard as missing when a product asked for it and turned it off in the same breath.
     */
    @Bean
    @ConditionalOnMissingBean
    public CallerRateLimiter aiCallerRateLimiter(AiProperties properties) {
        AiProperties.RateLimit rateLimit = properties.getRateLimit();

        return new TokenBucketCallerRateLimiter(rateLimit.getAllowance(), rateLimit.getPeriod());
    }

    // ── The chain ────────────────────────────────────────────────────────────────

    /*
     * Each guard is its own bean rather than five built inside one chain factory, so that a product can
     * replace exactly one of them — its own confirmation store's guard, a ceiling that reads a plan —
     * without restating the other four and inheriting whichever of them changes next release.
     */

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "jmouse.ai.rate-limit.enabled", havingValue = "true",
                           matchIfMissing = true)
    public RateLimitGuard aiRateLimitGuard(CallerRateLimiter rateLimiter) {
        return new RateLimitGuard(rateLimiter);
    }

    @Bean
    @ConditionalOnMissingBean
    public CeilingGuard aiCeilingGuard(GuardSettings settings) {
        return new CeilingGuard(settings);
    }

    @Bean
    @ConditionalOnMissingBean
    public EmptyDestructionGuard aiEmptyDestructionGuard() {
        return new EmptyDestructionGuard();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfirmationGuard aiConfirmationGuard(
            ConfirmationStore confirmations, GuardSettings settings) {

        return new ConfirmationGuard(confirmations, settings);
    }

    @Bean
    @ConditionalOnMissingBean
    public DeduplicationGuard aiDeduplicationGuard(DuplicateCallStore duplicateCalls) {
        return new DeduplicationGuard(duplicateCalls);
    }

    /**
     * Every contributed guard, in the order each declares for itself.
     *
     * <p>⚠️ {@code stream()} rather than a {@code List<InvocationGuard>} parameter, so that a product
     * contributing a sixth guard is contributing rather than replacing. The chain sorts by
     * {@link InvocationGuard#order()}, and the gaps between the shipped orders are wide enough to slot
     * one between any two.
     */
    @Bean
    @ConditionalOnMissingBean
    public GuardChain aiGuardChain(ObjectProvider<InvocationGuard> guards) {
        return new GuardChain(guards.orderedStream().toList());
    }

    // ── The seams ────────────────────────────────────────────────────────────────

    /**
     * What permissions exist, defaulting to "I cannot enumerate them".
     *
     * <p>Not to "none of them exist" — see {@link PermissionVocabulary#unchecked()}. An empty vocabulary
     * would make every action's permission unknown and refuse the whole catalogue at startup.
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionVocabulary aiPermissionVocabulary() {
        return PermissionVocabulary.unchecked();
    }

    @Bean
    @ConditionalOnMissingBean
    public CallerResolver aiCallerResolver() {
        return new PermissiveDefaults.AnonymousCallers();
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolAuthorizer aiToolAuthorizer() {
        return new PermissiveDefaults.PermitEverything();
    }

    @Bean
    @ConditionalOnMissingBean
    public ScopeResolver aiScopeResolver() {
        return ScopeResolver.refusing();
    }

    @Bean
    @ConditionalOnMissingBean
    public InvocationTrace aiInvocationTrace() {
        return new PermissiveDefaults.NoTrace();
    }

    // ── The catalogue, and the one door to a handler ─────────────────────────────

    /**
     * Everything a caller can reach, vetted as a whole set.
     *
     * <p>Building it is what runs the seven startup refusals, so this bean existing is the guarantee
     * that every registered action has a permission, a schema, a description, a name a protocol will
     * accept, and — where it destroys something — a way to say what.
     *
     * @param guards asked only for its names, so that a guard this installation <em>believes</em> it has
     *               and does not is refused at startup rather than discovered from a bill
     */
    @Bean
    @ConditionalOnMissingBean
    public ToolCatalog aiToolCatalog(
            ObjectProvider<ToolDefinition> definitions,
            PermissionVocabulary           vocabulary,
            GuardChain                     guards,
            AiProperties                   properties) {

        List<ToolDefinition> contributed = definitions.orderedStream().toList();
        GuardRoster          roster      = new GuardRoster(
                properties.getGuards().getRequired(), guards.names());

        return ToolCatalog.of(contributed, vocabulary, roster);
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolDispatcher aiToolDispatcher(
            ToolCatalog     catalog,
            CallerResolver  callerResolver,
            ToolAuthorizer  authorizer,
            ScopeResolver   scopeResolver,
            GuardChain      guards,
            InvocationTrace trace) {

        return new ToolDispatcher(catalog, callerResolver, authorizer, scopeResolver, guards, trace);
    }

    // ── What a screen may read ───────────────────────────────────────────────────

    /*
     * The read ports, always present so a product's own management screen can be written against them
     * whether or not it takes the optional controllers. Two of the four have no default implementation
     * that could say anything true, and answer empty rather than pretending.
     */

    @Bean
    @ConditionalOnMissingBean
    public ToolCatalogView aiToolCatalogView(ToolCatalog catalog) {
        return ToolCatalogView.over(catalog);
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolCallHistory aiToolCallHistory() {
        return ToolCallHistory.none();
    }

    @Bean
    @ConditionalOnMissingBean
    public UsageTotals aiUsageTotals() {
        return UsageTotals.none();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProviderRegistry aiProviderRegistry() {
        return ProviderRegistry.none();
    }

    // ── One line at startup ──────────────────────────────────────────────────────

    @Bean
    public AiDiagnostics aiDiagnostics(
            ToolCatalog       catalog,
            GuardChain        guards,
            CallerResolver    callerResolver,
            ToolAuthorizer    authorizer,
            ConfirmationStore confirmations,
            InvocationTrace   trace,
            ProviderRegistry  providers,
            AiProperties      properties) {

        return new AiDiagnostics(
                catalog, guards, callerResolver, authorizer, confirmations, trace, providers, properties);
    }
}
