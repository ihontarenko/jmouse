package org.jmouse.plugin.spring;

import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginSettingStore;
import org.jmouse.plugin.PluginSecrets;
import org.jmouse.plugin.PluginSettings;
import org.jmouse.plugin.Plugins;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

/**
 * 🧩 The application's plugins: every {@link Plugin} bean, in the order the context orders them, started once.
 *
 * <p>⚠️ A plugin bean is made before any owner reads a point, so it may not stand on a bean that reads the
 * {@link Extensions}: what it contributes is values made by itself, and what it needs at run time it is given in
 * {@link Plugin#start}.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(PluginProperties.class)
public class PluginAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(Extensions.class)
    public Plugins jmousePlugins(ObjectProvider<Plugin> plugins, PluginProperties properties) {
        return Plugins.start(plugins.orderedStream().filter(plugin -> properties.enabled(plugin.descriptor().id())).toList());
    }

    /**
     * The plugins' settings, kept by the application's {@link PluginSettingStore} when it has one. A plugin's own beans
     * read their settings through this, so it reads the plugins only once a setting is first asked for.
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginSettings jmousePluginSettings(ObjectProvider<Plugin> plugins, ObjectProvider<PluginSettingStore> store, ObjectProvider<PluginSecrets> secrets,
                                               PluginProperties properties) {
        return new PluginSettings(() -> plugins.orderedStream().filter(plugin -> properties.enabled(plugin.descriptor().id()))
                .toList(), Optional.ofNullable(store.getIfAvailable()), Optional.ofNullable(secrets.getIfAvailable()));
    }
}
