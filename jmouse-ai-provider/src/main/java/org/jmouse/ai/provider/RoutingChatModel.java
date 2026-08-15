package org.jmouse.ai.provider;

import java.util.Map;
import java.util.TreeMap;

/**
 * Whichever provider the settings in force name, asked afresh on every turn.
 *
 * <p><strong>This is what makes the provider a screen and not a redeploy.</strong> The starter's own
 * model picks its implementation from a property, which means the choice of vendor is a file and a
 * deploy. Where the settings come from a row, the row carries the provider name too — and this reads it
 * per call, so switching provider takes effect on the next request rather than at the next restart
 * nobody knew was needed.
 *
 * <p>⚠️ <strong>A dispatcher and not a fourth provider.</strong> Each implementation checks that the
 * settings it was handed name <em>it</em> before sending anything, so routing by that same name is the
 * only arrangement in which that check can never fire.
 *
 * <p>⚠️ <strong>The delegates are built once and kept.</strong> Every one owns an {@code HttpClient}, so
 * constructing one per call would leak a selector thread and an executor apiece.
 *
 * <p>It was written out identically in two products before it was written here.
 */
public final class RoutingChatModel implements ChatModel {

    private final ProviderSettingsSource settingsSource;
    private final Map<String, ChatModel> byProviderName;

    public RoutingChatModel(ProviderSettingsSource settingsSource, Map<String, ChatModel> byProviderName) {
        this.settingsSource = settingsSource;
        this.byProviderName = Map.copyOf(byProviderName);
    }

    /** Over everything {@link ProviderCatalog} ships, which is what a row is allowed to name. */
    public static RoutingChatModel overShippedProviders(ProviderSettingsSource settingsSource) {
        Map<String, ChatModel> models = new TreeMap<>();

        ProviderCatalog.shipped().forEach(providerName ->
                ProviderCatalog.modelFor(providerName, settingsSource)
                        .ifPresent(model -> models.put(providerName, model)));

        return new RoutingChatModel(settingsSource, models);
    }

    @Override
    public String providerName() {
        return active().providerName();
    }

    @Override
    public ChatResponse converse(ChatRequest request) {
        return active().converse(request);
    }

    /**
     * ⚠️ The refusal is this library's own {@link ProviderException}, so a caller cannot tell an
     * unusable configuration from an absent one by exception type — and should not have to. The message
     * says which of the two it is.
     */
    private ChatModel active() {
        String    named = ProviderCatalog.normalised(settingsSource.settings().providerName());
        ChatModel model = byProviderName.get(named);

        if (model == null) {
            throw new ProviderException(
                    "The settings in force name the provider '" + named + "', which is not one this "
                    + "application can speak to. Change it to one of: "
                    + String.join(", ", byProviderName.keySet()) + ".");
        }

        return model;
    }
}
