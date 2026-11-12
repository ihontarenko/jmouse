package org.jmouse.cad.plugin.datasheets;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.management.CadAccessRules;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.files.DocumentFiles;
import org.jmouse.plugin.PluginSettingValues;
import org.jmouse.plugin.PluginSettings;
import org.jmouse.plugin.spring.PluginAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** 🧩 The datasheets plugin, on the classpath: the plugin, what keeps a part's datasheets, and its routes. */
@AutoConfiguration(after = PluginAutoConfiguration.class)
public class DatasheetPluginConfiguration {

    @Bean
    public DatasheetFormat cadDatasheetFormat() {
        return CadSettings.read(DatasheetPlugin.FORMAT);
    }

    @Bean
    public DatasheetPlugin cadDatasheetPlugin(DatasheetFormat format) {
        return new DatasheetPlugin(format);
    }

    @Bean
    public Datasheets cadDatasheets(CadCatalogue catalogue, CadLanguage language, DocumentAccess access,
                                    DocumentFiles files, DatasheetFormat format, PluginSettings settings,
                                    CadAuthority authority, CadPlaces places) {
        PluginSettingValues values = settings.valuesOf(DatasheetPlugin.ID);
        return new Datasheets(catalogue, language, access, files, format,
                () -> values.wholeNumber(DatasheetPlugin.LARGEST) * DatasheetPlugin.MEGABYTE, authority, places);
    }

    @Bean
    public DatasheetController cadDatasheetController(Datasheets datasheets) {
        CadAccessRules.requireDecidedInside(DatasheetController.class);
        return new DatasheetController(datasheets);
    }
}
