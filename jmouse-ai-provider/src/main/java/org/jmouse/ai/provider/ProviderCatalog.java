package org.jmouse.ai.provider;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * Which providers this library ships, in one place.
 *
 * <p><strong>It was written out in three</strong> — the starter's model factory, every product's own
 * one, and every product's validation of what a stored row may name. Three copies of a list whose whole
 * job is to be exhaustive: adding a provider meant finding all of them, and the one that was missed
 * failed as "that is not a provider this installation can speak to" about a provider the library plainly
 * ships.
 *
 * <p>⚠️ <strong>Naming one is not the same as instantiating it.</strong> {@link #shipped()} says what
 * may be named; {@link #modelFor(String, ProviderSettingsSource)} builds one. A caller that only
 * validates should ask the first, because building a model opens an {@code HttpClient}.
 */
public final class ProviderCatalog {

    private static final List<String> SHIPPED = List.of(
            AnthropicChatModel.PROVIDER_NAME,
            OpenAiChatModel.PROVIDER_NAME,
            GatewayChatModel.PROVIDER_NAME);

    private ProviderCatalog() {
    }

    /** Every provider name this build can speak to, alphabetically. */
    public static List<String> shipped() {
        return SHIPPED.stream().sorted().toList();
    }

    /** Whether this build can speak to a provider by that name, spelling normalised. */
    public static boolean ships(String providerName) {
        return SHIPPED.contains(normalised(providerName));
    }

    /**
     * The implementation answering to that name, or empty where nothing does.
     *
     * <p>Empty rather than throwing: what an unknown name should do differs by caller — a startup
     * property refuses to boot, a stored row refuses the one request — and only the caller knows which
     * of those it is.
     */
    public static Optional<ChatModel> modelFor(String providerName, ProviderSettingsSource settings) {
        return builderFor(normalised(providerName)).map(build -> build.apply(settings));
    }

    /** A name as this catalogue compares it — trimmed and lower-cased. */
    public static String normalised(String providerName) {
        return providerName == null ? "" : providerName.trim().toLowerCase(Locale.ROOT);
    }

    private static Optional<Function<ProviderSettingsSource, ChatModel>> builderFor(String named) {
        return switch (named) {
            case AnthropicChatModel.PROVIDER_NAME -> Optional.of(AnthropicChatModel::new);
            case OpenAiChatModel.PROVIDER_NAME    -> Optional.of(OpenAiChatModel::new);
            case GatewayChatModel.PROVIDER_NAME   -> Optional.of(GatewayChatModel::new);
            default                               -> Optional.empty();
        };
    }
}
