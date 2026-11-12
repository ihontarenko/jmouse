package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.settings.SettingsOwner;
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

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/gerber/assembly. */
    public static final SettingsSection<AssemblyProfile> ASSEMBLY = new SettingsSection<>("plugin/gerber/assembly", "Assembly files", SettingsOwner.ENGINE_DATA, AssemblyProfile.class);

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/gerber/fabrication. */
    public static final SettingsSection<GerberSet.Settings> FABRICATION = new SettingsSection<>("plugin/gerber/fabrication", "Gerber fabrication", SettingsOwner.ENGINE_DATA, GerberSet.Settings.class);

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/gerber/files. */
    public static final SettingsSection<GerberSet.FileRole[]> FILES = new SettingsSection<>("plugin/gerber/files", "Gerber files", SettingsOwner.ENGINE_DATA, GerberSet.FileRole[].class);

    public static final String ID = "cad-gerber";

    /** The format's word on the exchange routes: {@code /exchange/gerber/export}. */
    static final String FORMAT = "gerber";

    private final GerberSet set;

    public GerberPlugin(GerberSet set) {
        this.set = set;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Gerber", GerberPlugin.class).drawnBy(GerberPlugin.class, "gerber.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(SettingsSection.POINT, ASSEMBLY);
        registration.contribute(SettingsSection.POINT, FABRICATION);
        registration.contribute(SettingsSection.POINT, FILES);
        registration.contribute(DocumentExport.POINT, new FabricationExport(FORMAT, "Gerber X2 · Excellon", set,
                CadSettings.read(GerberPlugin.ASSEMBLY)));
    }
}
