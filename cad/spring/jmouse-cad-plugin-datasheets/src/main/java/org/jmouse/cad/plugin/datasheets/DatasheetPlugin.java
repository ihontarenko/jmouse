package org.jmouse.cad.plugin.datasheets;

import org.jmouse.settings.Parameter;
import org.jmouse.settings.Section;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.exchange.AcceptedFiles;
import org.jmouse.cad.store.files.DocumentFiles;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.List;

/**
 * 📄 Datasheets in jmCAD: a part's PDF kept beside its document, and accepted as an upload. Without it the engine knows no
 * PDF: the upload is refused, and a part's inspector has no such section.
 */
public final class DatasheetPlugin implements Plugin {

    public static final String ID = "cad-datasheets";

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/cad-datasheets/format. */
    public static final SettingsSection<DatasheetFormat> FORMAT = SettingsSection.ofPlugin(ID, "format", "Datasheet format", DatasheetFormat.class);

    static final String LARGEST  = "largest";
    /** Its section of limits. */
    static final String LIMITS   = "limits";
    static final long   MEGABYTE = 1024L * 1024;

    private final CadSettings settings;

    /** @param settings the installation's settings: the format's files are accepted as it has them at the start */
    public DatasheetPlugin(CadSettings settings) {
        this.settings = settings;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Datasheets", DatasheetPlugin.class).withIcon(DatasheetPlugin.class, "datasheets.svg");
    }

    @Override
    public List<Section> settings() {
        return List.of(Section.fixed(LIMITS, "Limits", null, List.of(Parameter.wholeNumber(LARGEST, "Largest datasheet",
                "A larger PDF is refused as it is added.", 32, 1, DocumentFiles.LARGEST_FILE / MEGABYTE, "MB"))),
                CadSettings.declarationOf(FORMAT));
    }

    @Override
    public void register(PluginRegistration registration) {
        DatasheetFormat format = settings.read(FORMAT);
        registration.contribute(AcceptedFiles.POINT, new AcceptedFiles("Datasheets",
                format.extensions().stream().map(extension -> "*." + extension).toList(), List.of(format.mediaType())));
    }
}
