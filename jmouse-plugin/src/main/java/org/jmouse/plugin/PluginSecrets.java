package org.jmouse.plugin;

/**
 * 🔐 How an application keeps a plugin's {@link PluginSetting.Kind#SECRET secret} settings: sealed before they reach its
 * store, opened only for the plugin that uses one. Bound to the setting by {@code context}, so a sealed value copied onto
 * another setting does not open. An application without one keeps no secrets: changing one is refused, saying so.
 */
public interface PluginSecrets {

    String seal(String secret, String context);

    String open(String sealed, String context);
}
