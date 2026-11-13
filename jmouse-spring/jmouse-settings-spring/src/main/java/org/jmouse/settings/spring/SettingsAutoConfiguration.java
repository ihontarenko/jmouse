package org.jmouse.settings.spring;

import org.jmouse.settings.ParameterStore;
import org.jmouse.settings.SecretSealing;
import org.jmouse.settings.Section;
import org.jmouse.settings.SectionDeclarations;
import org.jmouse.settings.SectionReader;
import org.jmouse.settings.Settings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🎚️ The application's settings: one {@link Settings} over every {@link SectionDeclarations} bean, its values kept by the
 * application's {@link ParameterStore} when it has one, its secrets sealed by its {@link SecretSealing}, its structured sections read
 * by its {@link SectionReader} — and every value the installation holds none of yet seeded once every bean stands.
 */
@AutoConfiguration(after = SettingsJpaAutoConfiguration.class)
public class SettingsAutoConfiguration {

    /**
     * The settings. The declarations are read when a value is first asked for: a declaring part's own beans read their settings
     * through this, and could not wait for themselves. Two sources declaring one owner are refused: an owner's sections are one
     * list.
     */
    @Bean
    @ConditionalOnMissingBean
    public Settings jmouseSettings(ObjectProvider<SectionDeclarations> sources, ObjectProvider<ParameterStore> store,
                                   ObjectProvider<SecretSealing> secrets, ObjectProvider<SectionReader> reader) {
        return new Settings(() -> {
            Map<String, List<Section>> sections = new LinkedHashMap<>();
            sources.orderedStream().forEach(source -> source.sections().forEach((owner, owned) -> {
                if (sections.putIfAbsent(owner, owned) != null) {
                    throw new IllegalStateException("Two sources declare the settings of '" + owner + "'.");
                }
            }));
            return sections;
        }, Optional.ofNullable(store.getIfAvailable()), Optional.ofNullable(secrets.getIfAvailable()),
                Optional.ofNullable(reader.getIfAvailable()));
    }

    /**
     * Every value the installation holds none of yet kept, once every bean stands — the library's table migrated: at the first
     * start all of them, after a release the ones it brings. From then on the rows are the one source.
     */
    @Bean
    public SmartInitializingSingleton jmouseSettingsSeed(Settings settings) {
        return settings::seed;
    }
}
