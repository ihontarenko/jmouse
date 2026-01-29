package org.jmouse.core.mapping.plugin;

import java.util.List;

/**
 * A lightweight dispatcher that propagates mapping lifecycle events to a list of {@link MappingPlugin}s. 🚌
 *
 * <p>{@code PluginBus} provides a single entry point for invoking plugins in a stable order.
 * It supports both "side-effect" hooks (start/finish/error) and value interception via {@link #onValue(MappingValue)}.</p>
 *
 * <h3>Execution order</h3>
 * <p>Plugins are executed sequentially in the order they are provided to the constructor.</p>
 *
 * <h3>Value pipeline</h3>
 * <p>For {@link #onValue(MappingValue)}, the current value is passed through all plugins.
 * The output of one plugin becomes the input of the next plugin.</p>
 *
 * <p>If the bus is created with {@code null} plugins, it behaves as an empty bus.</p>
 *
 * @see MappingPlugin
 */
public class PluginBus {

    private final List<MappingPlugin> plugins;

    /**
     * Create a new plugin bus.
     *
     * @param plugins plugins to execute; {@code null} is treated as an empty list
     */
    public PluginBus(List<MappingPlugin> plugins) {
        this.plugins = plugins == null ? List.of() : List.copyOf(plugins);
    }

    /**
     * Notify plugins that a mapping call is about to start.
     *
     * @param mappingCall mapping call descriptor
     */
    public void onStart(MappingCall mappingCall) {
        for (var mappingPlugin : plugins) {
            mappingPlugin.onStart(mappingCall);
        }
    }

    /**
     * Pass a mapped value through the plugin pipeline.
     *
     * <p>The returned value is the final value produced after applying all plugins.</p>
     *
     * @param value mapping value wrapper
     * @return transformed value after executing all plugins
     */
    public Object onValue(MappingValue value) {
        Object currentValue = value.current();

        for (var mappingPlugin : plugins) {
            currentValue = mappingPlugin.onValue(new MappingValue(
                    value.rootSource(),
                    currentValue,
                    value.targetType(),
                    value.path(),
                    value.context()
            ));
        }

        return currentValue;
    }

    /**
     * Notify plugins that a mapping call has completed successfully.
     *
     * @param result mapping result descriptor
     */
    public void onFinish(MappingResult result) {
        for (var mappingPlugin : plugins) {
            mappingPlugin.onFinish(result);
        }
    }

    /**
     * Notify plugins that a mapping call has failed.
     *
     * @param failure mapping failure descriptor
     */
    public void onError(MappingFailure failure) {
        for (var mappingPlugin : plugins) {
            mappingPlugin.onError(failure);
        }
    }

    /**
     * No-op implementation of {@link PluginBus}.
     *
     * <p>This bus performs no actions and is intended as a cheap default when plugins are disabled.</p>
     */
    public static final class Noop extends PluginBus {

        /**
         * Shared singleton instance.
         */
        public static final Noop INSTANCE = new Noop();

        private Noop() {
            super(List.of());
        }

        /**
         * No-op.
         *
         * @param call mapping call descriptor
         */
        @Override
        public void onStart(MappingCall call) {
        }

        /**
         * No-op.
         *
         * @param result mapping result descriptor
         */
        @Override
        public void onFinish(MappingResult result) {
        }

        /**
         * No-op.
         *
         * @param failure mapping failure descriptor
         */
        @Override
        public void onError(MappingFailure failure) {
        }
    }

}
