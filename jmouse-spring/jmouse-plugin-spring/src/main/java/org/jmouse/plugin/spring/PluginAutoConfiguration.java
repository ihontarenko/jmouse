package org.jmouse.plugin.spring;

import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.Plugins;
import org.jmouse.settings.Section;
import org.jmouse.settings.SectionDeclarations;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🧩 The application's plugins: every {@link Plugin} bean, in the order the context orders them, started once — and their
 * settings, each plugin's sections owned by its id, declared to the application's settings (jmouse-settings).
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
        return Plugins.start(enabled(plugins, properties));
    }

    /**
     * Every switched-on plugin's sections, owned by its id. Read when a value is first asked for, so the plugins are asked only
     * then: a plugin's own beans read their settings, and could not wait for themselves.
     */
    @Bean
    public SectionDeclarations jmousePluginSections(ObjectProvider<Plugin> plugins, PluginProperties properties) {
        return () -> {
            Map<String, List<Section>> sections = new LinkedHashMap<>();
            enabled(plugins, properties).forEach(plugin -> sections.put(plugin.descriptor().id(), plugin.settings()));
            return sections;
        };
    }

    private static List<Plugin> enabled(ObjectProvider<Plugin> plugins, PluginProperties properties) {
        return plugins.orderedStream().filter(plugin -> properties.enabled(plugin.descriptor().id())).toList();
    }
}
