package org.jmouse.cad.jpa.plugin;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * 🎚️ The value one plugin setting was changed to. The setting itself — its kind, its default — is its plugin's; a row
 * is the change alone, and a setting nobody changed has none.
 */
@Entity(name = "KeptPluginSetting")
@Table(name = "cad_plugin_settings")
public class KeptPluginSetting {

    /** Which setting of which plugin. */
    @Embeddable
    public static class Key implements Serializable {

        @Column(nullable = false, length = 80, updatable = false)
        private String plugin;

        @Column(name = "setting_key", nullable = false, length = 80, updatable = false)
        private String setting;

        protected Key() {
        }

        public Key(String plugin, String setting) {
            this.plugin = plugin;
            this.setting = setting;
        }

        public String getPlugin() {
            return plugin;
        }

        public String getSetting() {
            return setting;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && plugin.equals(key.plugin) && setting.equals(key.setting);
        }

        @Override
        public int hashCode() {
            return Objects.hash(plugin, setting);
        }
    }

    @EmbeddedId
    private Key key;

    @Column(name = "setting_value", nullable = false, length = 2000)
    private String value;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected KeptPluginSetting() {
    }

    public KeptPluginSetting(Key key, String value, Instant changedAt) {
        this.key = key;
        this.value = value;
        this.changedAt = changedAt;
    }

    public void change(String value, Instant changedAt) {
        this.value = value;
        this.changedAt = changedAt;
    }

    public Key getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
