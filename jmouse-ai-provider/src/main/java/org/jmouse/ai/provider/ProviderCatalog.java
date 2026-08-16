package org.jmouse.ai.provider;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Which providers this library ships, in one place.
 *
 * <p><strong>It was written out in three</strong> — the starter's model factory, every product's own
 * one, and every product's validation of what a stored row may name. Three copies of a list whose whole
 * job is to be exhaustive: adding a provider meant finding all of them, and the one that was missed
 * failed as "that is not a provider this installation can speak to" about a provider the library plainly
 * ships.
 *
 * <h2>⚠️ Most of these are one row in a table, not one class</h2>
 *
 * <p>Almost every provider worth having speaks OpenAI's chat-completions shape, including the free ones.
 * So they are not adapters — they are an <em>address</em> and a <em>name</em> handed to the adapter that
 * already exists. Writing a class per provider would have produced six files differing by one string
 * each, and the seventh would have been copied from the sixth with one string forgotten.
 *
 * <p>What genuinely differs gets a class: Anthropic's shape is its own, and a gateway is an address that
 * speaks the canonical shape rather than any provider's.
 *
 * <p>⚠️ <strong>Naming one is not the same as instantiating it.</strong> {@link #shipped()} says what
 * may be named; {@link #modelFor(String, ProviderSettingsSource)} builds one. A caller that only
 * validates should ask the first, because building a model opens an {@code HttpClient}.
 */
public final class ProviderCatalog {

    /**
     * One provider, as this catalogue knows it.
     *
     * @param defaultApiUrl where it answers when a configuration names no address of its own. ⚠️ Null
     *                      for a gateway, which is somewhere in particular and has nothing to guess
     * @param requiresKey   whether a configuration must carry a credential before it may be put in
     *                      force. ⚠️ False is not laxity — a model running on the same machine has no
     *                      credential to give, and demanding one would make the only free-in-every-sense
     *                      option unusable
     * @param note          one line a screen shows beside the name, so choosing does not require
     *                      knowing the landscape
     */
    public record Shipped(String name, String defaultApiUrl, boolean requiresKey, String note) {
    }

    /**
     * ⚠️ Ordered deliberately: the two whose shape is their own first, then the compatible ones with the
     * keyless local option leading them. A screen renders this order, and the order is the
     * recommendation.
     */
    private static final Map<String, Shipped> SHIPPED = shipped(
            new Shipped(AnthropicChatModel.PROVIDER_NAME, null, true,
                    "Claude. Its own request shape, adapted here."),

            new Shipped(OpenAiChatModel.PROVIDER_NAME, null, true,
                    "GPT. The shape every provider below borrows."),

            new Shipped(GatewayChatModel.PROVIDER_NAME, null, true,
                    "Not a provider — your own endpoint speaking the canonical shape, holding the keys "
                    + "for a whole installation. Give it an address."),

            new Shipped("ollama", "http://localhost:11434/v1/chat/completions", false,
                    "Runs on this machine. Free with no account and no key — the address is the default "
                    + "Ollama port, and the model is whatever you have pulled."),

            new Shipped("groq", "https://api.groq.com/openai/v1/chat/completions", true,
                    "Free tier, and the fastest of these by a distance. Open-weight models."),

            new Shipped("openrouter", "https://openrouter.ai/api/v1/chat/completions", true,
                    "One key, many models — including several that cost nothing. Name one with a ':free' "
                    + "suffix and it stays free."),

            new Shipped("mistral", "https://api.mistral.ai/v1/chat/completions", true,
                    "Free tier. European, if that matters to where the data may go."),

            new Shipped("google", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", true,
                    "Gemini, through its OpenAI-compatible endpoint. Generous free tier."),

            new Shipped("deepseek", "https://api.deepseek.com/chat/completions", true,
                    "Not free, but the cheapest here by an order of magnitude."));

    private ProviderCatalog() {
    }

    /** Every provider name this build can speak to, alphabetically. */
    public static List<String> shipped() {
        return SHIPPED.keySet().stream().sorted().toList();
    }

    /** The same, with what a screen needs to describe each one — in the order they are meant to be read. */
    public static List<Shipped> describeShipped() {
        return List.copyOf(SHIPPED.values());
    }

    /** Whether this build can speak to a provider by that name, spelling normalised. */
    public static boolean ships(String providerName) {
        return SHIPPED.containsKey(normalised(providerName));
    }

    /**
     * Whether a configuration naming this provider must carry a credential.
     *
     * <p>⚠️ Unknown names answer {@code true}. A provider this build does not ship cannot be put in force
     * anyway, and the cautious answer is the one that does not invent an exemption.
     */
    public static boolean requiresKey(String providerName) {
        Shipped known = SHIPPED.get(normalised(providerName));

        return known == null || known.requiresKey();
    }

    /**
     * The implementation answering to that name, or empty where nothing does.
     *
     * <p>Empty rather than throwing: what an unknown name should do differs by caller — a startup
     * property refuses to boot, a stored row refuses the one request — and only the caller knows which
     * of those it is.
     */
    public static Optional<ChatModel> modelFor(String providerName, ProviderSettingsSource settings) {
        Shipped known = SHIPPED.get(normalised(providerName));

        if (known == null) {
            return Optional.empty();
        }

        return Optional.of(switch (known.name()) {
            case AnthropicChatModel.PROVIDER_NAME -> new AnthropicChatModel(settings);
            case GatewayChatModel.PROVIDER_NAME   -> new GatewayChatModel(settings);
            // Everything else is OpenAI's shape at a different address, which is the whole reason there
            // is no class for any of them.
            default -> new OpenAiChatModel(settings, known.name(), known.defaultApiUrl());
        });
    }

    /** A name as this catalogue compares it — trimmed and lower-cased. */
    public static String normalised(String providerName) {
        return providerName == null ? "" : providerName.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * ⚠️ {@code unmodifiableMap} over a {@link LinkedHashMap}, and never {@code Map.copyOf} — that one
     * returns an unordered map, which would silently scramble the order this catalogue is written in and
     * a screen renders.
     */
    private static Map<String, Shipped> shipped(Shipped... providers) {
        Map<String, Shipped> byName = new LinkedHashMap<>();

        for (Shipped provider : providers) {
            byName.put(provider.name(), provider);
        }

        return Collections.unmodifiableMap(byName);
    }
}
