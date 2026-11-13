package org.jmouse.cad.plugin.gerber;

import org.jmouse.settings.Section;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.settings.SettingsSection;

import java.util.List;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

/**
 * Gerber in the engine: a pcb written as the files a pcb house makes and assembles it from. It brings the plain set
 * to the core's exports; a pcb house's plugin requires it and builds its own set on {@link PcbFabrication},
 * {@link GerberSet} and {@link FabricationExport} with its own {@link AssemblyProfile}. Without it the core writes no pcb.
 */
public final class GerberPlugin implements Plugin {

    public static final String ID = "cad-gerber";

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/cad-gerber/assembly. */
    public static final SettingsSection<AssemblyProfile> ASSEMBLY = SettingsSection.ofPlugin(ID, "assembly", "Assembly files", AssemblyProfile.class);

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/cad-gerber/fabrication. */
    public static final SettingsSection<GerberSet.Settings> FABRICATION = SettingsSection.ofPlugin(ID, "fabrication", "Gerber fabrication", GerberSet.Settings.class);

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/cad-gerber/files. */
    public static final SettingsSection<GerberSet.FileRole[]> FILES = SettingsSection.ofPlugin(ID, "files", "Gerber files", GerberSet.FileRole[].class);

    /** The format's word on the exchange routes: {@code /exchange/gerber/export}. */
    static final String FORMAT = "gerber";

    private final GerberSet   set;
    private final CadSettings sections;

    public GerberPlugin(GerberSet set, CadSettings sections) {
        this.set = set;
        this.sections = sections;
    }

    @Override
    public List<Section> settings() {
        return List.of(CadSettings.declarationOf(ASSEMBLY), CadSettings.declarationOf(FABRICATION), CadSettings.declarationOf(FILES));
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Gerber", GerberPlugin.class).drawnBy(GerberPlugin.class, "gerber.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(DocumentExport.POINT, new FabricationExport(FORMAT, "Gerber X2 · Excellon", set, () -> sections.read(ASSEMBLY)));
    }
}
