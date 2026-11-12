package org.jmouse.plugin;

import java.util.Map;

/**
 * 🗄️ Where an application keeps the values its plugins' settings were changed to: the values alone, never a setting's
 * declaration, which is the plugin's ({@link Plugin#settings()}). A setting nobody changed is not kept at all.
 */
public interface PluginSettingStore {

    /** The values a plugin's settings were changed to, by key; a setting never changed is absent. */
    Map<String, String> changed(String plugin);

    /**
     * Keeps the values a plugin's settings are changed to — all of them or none; a null value forgets the change, the
     * setting back at its default.
     */
    void keep(String plugin, Map<String, String> values);
}
