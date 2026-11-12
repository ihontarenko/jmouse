package org.jmouse.cad.store.plugin;

import org.jmouse.cad.store.CadRead;
import org.jmouse.cad.jpa.plugin.PluginSettingRecords;
import org.jmouse.plugin.PluginSettingStore;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** 🎚️ The engine's plugins' settings kept in its own tables: each read and each change one transaction. */
public class CadPluginSettingStore implements PluginSettingStore {

    private final PluginSettingRecords records;

    public CadPluginSettingStore(PluginSettingRecords records) {
        this.records = records;
    }

    @Override
    @CadRead
    public Map<String, String> changed(String plugin) {
        return records.changed(plugin);
    }

    @Override
    @Transactional
    public void keep(String plugin, Map<String, String> values) {
        records.keep(plugin, values);
    }
}
