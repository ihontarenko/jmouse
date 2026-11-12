package org.jmouse.cad.plugin.datasheets;

import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.settings.SettingsOwner;
import org.jmouse.cad.exchange.AcceptedFiles;
import org.jmouse.cad.store.files.DocumentFiles;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;
import org.jmouse.plugin.PluginSetting;

import java.util.List;

/**
 * 📄 Datasheets in jmCAD: a part's PDF kept beside its document, and accepted as an upload. Without it the engine knows no
 * PDF: the upload is refused, and a part's inspector has no such section.
 */
public final class DatasheetPlugin implements Plugin {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/datasheets/format. */
    public static final SettingsSection<DatasheetFormat> FORMAT = new SettingsSection<>("plugin/datasheets/format", "Datasheet format", SettingsOwner.ENGINE_DATA, DatasheetFormat.class);

    public static final String ID = "cad-datasheets";

    static final String LARGEST  = "largest";
    static final long   MEGABYTE = 1024L * 1024;

    private final DatasheetFormat format;

    public DatasheetPlugin(DatasheetFormat format) {
        this.format = format;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Datasheets", DatasheetPlugin.class).drawnBy(DatasheetPlugin.class, "datasheets.svg");
    }

    @Override
    public List<PluginSetting> settings() {
        return List.of(PluginSetting.wholeNumber(LARGEST, "Largest datasheet", "A larger PDF is refused as it is added.",
                32, 1, DocumentFiles.LARGEST_FILE / MEGABYTE, "MB"));
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(SettingsSection.POINT, FORMAT);
        registration.contribute(AcceptedFiles.POINT, new AcceptedFiles("Datasheets",
                format.extensions().stream().map(extension -> "*." + extension).toList(), List.of(format.mediaType())));
    }
}
