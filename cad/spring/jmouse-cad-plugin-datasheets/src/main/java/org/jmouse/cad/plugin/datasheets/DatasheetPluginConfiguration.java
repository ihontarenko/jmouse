package org.jmouse.cad.plugin.datasheets;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.management.CadAccessRules;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.files.DocumentFiles;
import org.jmouse.settings.ParameterValues;
import org.jmouse.settings.Settings;
import org.jmouse.plugin.spring.PluginAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** 🧩 The datasheets plugin, on the classpath: the plugin, what keeps a part's datasheets, and its routes. */
@AutoConfiguration(after = PluginAutoConfiguration.class)
public class DatasheetPluginConfiguration {

    @Bean
    public DatasheetPlugin cadDatasheetPlugin(CadSettings settings) {
        return new DatasheetPlugin(settings);
    }

    @Bean
    public Datasheets cadDatasheets(CadCatalogue catalogue, CadLanguage language, DocumentAccess access,
                                    DocumentFiles files, CadSettings sections, Settings settings,
                                    CadAuthority authority, CadPlaces places) {
        ParameterValues values = settings.parameters(DatasheetPlugin.ID, DatasheetPlugin.LIMITS);
        return new Datasheets(catalogue, language, access, files, () -> sections.read(DatasheetPlugin.FORMAT),
                () -> values.wholeNumber(DatasheetPlugin.LARGEST) * DatasheetPlugin.MEGABYTE, authority, places);
    }

    @Bean
    public DatasheetController cadDatasheetController(Datasheets datasheets) {
        CadAccessRules.requireDecidedInside(DatasheetController.class);
        return new DatasheetController(datasheets);
    }
}
