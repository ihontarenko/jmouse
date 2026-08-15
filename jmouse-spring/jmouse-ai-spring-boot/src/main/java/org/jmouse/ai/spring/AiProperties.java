package org.jmouse.ai.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Everything about this mechanism an installation gets to decide, in one place.
 *
 * <p><strong>The numbers here are settings rather than constants for one reason</strong>, and it is the
 * same reason throughout: what counts as "too many records", "too many calls" or "long enough to read a
 * preview" depends entirely on the installation. A workshop inventory of eighty parts and a catalogue of
 * nine thousand want different ceilings, and neither should need a release to say so.
 *
 * <p>Everything defaults, and the defaults are chosen to be <em>annoying rather than permissive</em>. A
 * ceiling that is too low produces a refusal naming the number, which somebody raises in a minute; one
 * that is too high produces a call nobody notices until the records are gone.
 *
 * <p>⚠️ Two of these switch a whole feature on and are deliberately off by default —
 * {@code protocol.enabled} and {@code management.enabled}. Serving the Model Context Protocol exposes
 * every tool to whatever can authenticate against that endpoint, and the management screens publish a
 * call history and a provider configuration. Neither is a thing to acquire by adding a dependency.
 */
@ConfigurationProperties(prefix = "jmouse.ai")
public class AiProperties {

    /**
     * Which application's rows this is, where provider settings live in a shared table.
     *
     * <p>Read only by the database-backed settings source, and only where one installation's table
     * serves more than one application.
     */
    private String application = "application";

    private final Guards        guards        = new Guards();
    private final RateLimit     rateLimit     = new RateLimit();
    private final Provider      provider      = new Provider();
    private final Conversation  conversation  = new Conversation();
    private final Protocol      protocol      = new Protocol();
    private final Management    management    = new Management();
    private final Migrations    migrations    = new Migrations();

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public Guards getGuards() {
        return guards;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Provider getProvider() {
        return provider;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public Management getManagement() {
        return management;
    }

    public Migrations getMigrations() {
        return migrations;
    }

    /** The four numbers the guards work from, plus the list a product checks itself against. */
    public static class Guards {

        /** The most records one call may affect. Past this the call is refused, never confirmed. */
        private long perCallCeiling = 100;

        /** Above how many affected records a non-destructive write still needs confirming. */
        private long confirmationThreshold = 5;

        /** How long a preview's token stays redeemable. */
        private Duration confirmationLifetime = Duration.ofMinutes(5);

        /** How long an identical call is treated as the same call. */
        private Duration deduplicationWindow = Duration.ofSeconds(60);

        /**
         * Which guards this installation believes it has.
         *
         * <p>⚠️ Naming them here is how a product finds out it is wrong. A typo, a bean that failed to
         * be contributed, a module left out of a build — each produces a running application whose
         * protection is quietly absent, and nothing logs it because nothing knows a guard was expected.
         * Every name listed here must exist at startup or the application does not start.
         */
        private Set<String> required = new LinkedHashSet<>();

        public long getPerCallCeiling() {
            return perCallCeiling;
        }

        public void setPerCallCeiling(long perCallCeiling) {
            this.perCallCeiling = perCallCeiling;
        }

        public long getConfirmationThreshold() {
            return confirmationThreshold;
        }

        public void setConfirmationThreshold(long confirmationThreshold) {
            this.confirmationThreshold = confirmationThreshold;
        }

        public Duration getConfirmationLifetime() {
            return confirmationLifetime;
        }

        public void setConfirmationLifetime(Duration confirmationLifetime) {
            this.confirmationLifetime = confirmationLifetime;
        }

        public Duration getDeduplicationWindow() {
            return deduplicationWindow;
        }

        public void setDeduplicationWindow(Duration deduplicationWindow) {
            this.deduplicationWindow = deduplicationWindow;
        }

        public Set<String> getRequired() {
            return required;
        }

        public void setRequired(Set<String> required) {
            this.required = required;
        }
    }

    /** One caller's allowance, refilling continuously. */
    public static class RateLimit {

        /**
         * Off produces a chain with no rate limit at all.
         *
         * <p>⚠️ Worth saying out loud rather than discovering: the rate limit is the only guard that
         * applies to reads, so turning it off leaves a read action with nothing between it and a caller
         * in a loop.
         */
        private boolean enabled = true;

        /** How many calls one caller may make per {@link #period}. */
        private long allowance = 60;

        private Duration period = Duration.ofMinutes(1);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getAllowance() {
            return allowance;
        }

        public void setAllowance(long allowance) {
            this.allowance = allowance;
        }

        public Duration getPeriod() {
            return period;
        }

        public void setPeriod(Duration period) {
            this.period = period;
        }
    }

    /** Which model is called, and how it authenticates. */
    public static class Provider {

        /** Where the settings come from. */
        public enum Source {

            /** From this file. What a product starts with. */
            PROPERTIES,

            /**
             * From {@code ai_provider_settings}, so a key can be rotated without a deploy.
             *
             * <p>⚠️ Requires {@code jmouse-ai-jpa}, and requires its entity package in the
             * application's {@code @EntityScan} — a missing package starts cleanly and dies on the
             * first query.
             */
            DATABASE
        }

        private Source source = Source.PROPERTIES;

        /** {@code anthropic}, {@code openai} or {@code gateway}. Empty means no model provider at all. */
        private String name;

        private String model;

        /** ⚠️ Belongs in an environment variable or a secret store, never in a checked-in file. */
        private String apiKey;

        /** Overrides the provider's own default address. For a proxy, or a compatible endpoint. */
        private String apiUrl;

        private int maximumTokens = 4_096;

        public Source getSource() {
            return source;
        }

        public void setSource(Source source) {
            this.source = source;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public int getMaximumTokens() {
            return maximumTokens;
        }

        public void setMaximumTokens(int maximumTokens) {
            this.maximumTokens = maximumTokens;
        }

        /** Whether a provider was named at all. Tools without a model is a supported arrangement. */
        public boolean isNamed() {
            return name != null && !name.isBlank();
        }
    }

    /** What one assistant conversation is allowed to spend before it is stopped. */
    public static class Conversation {

        /** How many model turns a conversation may take. */
        private int maximumRounds = 6;

        /** How many tokens it may spend across all of them. */
        private int maximumTokens = 120_000;

        public int getMaximumRounds() {
            return maximumRounds;
        }

        public void setMaximumRounds(int maximumRounds) {
            this.maximumRounds = maximumRounds;
        }

        public int getMaximumTokens() {
            return maximumTokens;
        }

        public void setMaximumTokens(int maximumTokens) {
            this.maximumTokens = maximumTokens;
        }
    }

    /** Serving the Model Context Protocol to clients outside this application. */
    public static class Protocol {

        /** ⚠️ Off by default. On, every tool in the catalogue is reachable from outside. */
        private boolean enabled = false;

        /** Where the protocol is served. */
        private String endpoint = "/mcp";

        /**
         * Where the product's authentication confines a credential to.
         *
         * <p>⚠️ Checked against {@link #endpoint} at startup and refused when they differ. A typo here
         * moves the whole protocol outside its security boundary and does so without failing, logging,
         * or looking wrong from either side.
         */
        private String credentialsConfinedTo = "/mcp";

        /** What this server calls itself in the protocol handshake. */
        private String serverName = "jmouse-ai";

        private String serverVersion = "1.0.0";

        /**
         * What this server is for, shown to a model once before it has seen a single tool.
         *
         * <p>Not a tool's description and not a substitute for one. A description answers <em>should I
         * call this one</em>; this answers <em>where do I start, and what is true of every call here</em>
         * — which action to call first, that every answer states where it acted, that a refusal is
         * worth reading rather than retrying.
         *
         * <p>The text is the product's, about a domain this library never learns. Left unset the server
         * says nothing and its tools speak for themselves.
         */
        private String instructions;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getCredentialsConfinedTo() {
            return credentialsConfinedTo;
        }

        public void setCredentialsConfinedTo(String credentialsConfinedTo) {
            this.credentialsConfinedTo = credentialsConfinedTo;
        }

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public String getServerVersion() {
            return serverVersion;
        }

        public void setServerVersion(String serverVersion) {
            this.serverVersion = serverVersion;
        }

        public String getInstructions() {
            return instructions;
        }

        public void setInstructions(String instructions) {
            this.instructions = instructions;
        }
    }

    /** The optional read-only screens, from {@code jmouse-ai-management}. */
    public static class Management {

        /**
         * ⚠️ Off by default, and having the module on the classpath is not enough.
         *
         * <p>These endpoints publish a call history and a provider configuration, and nothing in the
         * module guards them — the product mounts them behind its own gate. Switching them on by the
         * mere presence of a jar would publish both to anyone who could reach the application.
         */
        private boolean enabled = false;

        /** Where they answer. Also read by the controllers themselves, through the same property. */
        private String prefix = "/jmouse-ai";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }

    /** The library's own schema, migrated against its own history table. */
    public static class Migrations {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
