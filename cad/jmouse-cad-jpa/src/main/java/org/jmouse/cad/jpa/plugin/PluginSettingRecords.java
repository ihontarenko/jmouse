package org.jmouse.cad.jpa.plugin;

import jakarta.persistence.EntityManager;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🎚️ The values the engine's plugins' settings were changed to, as rows of {@code cad_plugin_settings}.
 *
 * <p>It checks nothing — a value is checked against its setting before it reaches here — and transaction demarcation is
 * the caller's throughout.</p>
 */
public class PluginSettingRecords {

    private final EntityManager entityManager;
    private final Clock         clock;

    public PluginSettingRecords(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    /** The values a plugin's settings were changed to, by key. */
    public Map<String, String> changed(String plugin) {
        Map<String, String> values = new LinkedHashMap<>();
        entityManager.createQuery("select kept from KeptPluginSetting kept where kept.key.plugin = :plugin",
                                  KeptPluginSetting.class)
                .setParameter("plugin", plugin)
                .getResultList()
                .forEach(kept -> values.put(kept.getKey().getSetting(), kept.getValue()));
        return values;
    }

    /** Keeps each value a setting is changed to; a null value deletes the setting's row. */
    public void keep(String plugin, Map<String, String> values) {
        for (Map.Entry<String, String> value : values.entrySet()) {
            KeptPluginSetting.Key key = new KeptPluginSetting.Key(plugin, value.getKey());
            KeptPluginSetting kept = entityManager.find(KeptPluginSetting.class, key);
            if (value.getValue() == null) {
                if (kept != null) {
                    entityManager.remove(kept);
                }
            } else if (kept == null) {
                entityManager.persist(new KeptPluginSetting(key, value.getValue(), clock.instant()));
            } else {
                kept.change(value.getValue(), clock.instant());
            }
        }
    }
}
