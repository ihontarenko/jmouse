package org.jmouse.core.mapping.plugin;

/**
 * Extension point for observing and transforming mapping execution. 🧩
 *
 * <p>{@code MappingPlugin} provides lifecycle callbacks that are invoked by the mapping engine
 * (typically via {@link PluginBus}) during a mapping invocation.</p>
 *
 * <p>Plugins may be used for cross-cutting concerns such as:</p>
 * <ul>
 *   <li>value normalization (e.g., trimming strings)</li>
 *   <li>auditing / tracing</li>
 *   <li>custom validation</li>
 *   <li>diagnostics and metrics</li>
 * </ul>
 *
 * <p>All methods have default no-op implementations.</p>
 */
public interface MappingPlugin {

    /**
     * Called once when a sourceRoot mapping invocation starts.
     *
     * @param call mapping call descriptor
     */
    default void onStart(MappingCall call) {
    }

    /**
     * Called when a raw value for a target "slot" is produced (explicit binding or fallback).
     *
     * <p>This hook can transform the value by returning a new value. The returned value will
     * be used by subsequent plugins and by the mapping engine.</p>
     *
     * @param value mapping value wrapper
     * @return transformed value (default: {@code value.current()})
     */
    default Object onValue(MappingValue value) {
        return value.current();
    }

    /**
     * Called once after the target object has been materialized successfully.
     *
     * @param result mapping result descriptor
     */
    default void onFinish(MappingResult result) {
    }

    /**
     * Called when mapping fails with an error.
     *
     * @param failure mapping failure descriptor
     */
    default void onError(MappingFailure failure) {
    }
}
