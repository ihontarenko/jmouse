package org.jmouse.core.mapping.plugin;

import java.util.List;

public class PluginBus {

    private final List<MappingPlugin> plugins;

    public PluginBus(List<MappingPlugin> plugins) {
        this.plugins = plugins == null ? List.of() : List.copyOf(plugins);
    }

    public void onStart(MappingCall mappingCall) {
        for (var mappingPlugin : plugins) {
            mappingPlugin.onStart(mappingCall);
        }
    }

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

    public void onFinish(MappingResult result) {
        for (var mappingPlugin : plugins) {
            mappingPlugin.onFinish(result);
        }
    }

    public void onError(MappingFailure failure) {
        for (var mappingPlugin : plugins) {
            mappingPlugin.onError(failure);
        }
    }

    public static final class Noop extends PluginBus {

        public static final Noop INSTANCE = new Noop();

        private Noop() {
            super(List.of());
        }

        @Override
        public void onStart(MappingCall call) {
        }

        @Override
        public void onFinish(MappingResult result) {
        }

        @Override
        public void onError(MappingFailure failure) {
        }
    }

}
