package org.jmouse.cad.plugin.jlcpcb;

import org.jmouse.settings.Section;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.OutputJob;
import org.jmouse.cad.plugin.gerber.AssemblyProfile;
import org.jmouse.cad.plugin.gerber.FabricationExport;
import org.jmouse.cad.plugin.gerber.GerberPlugin;
import org.jmouse.cad.plugin.gerber.GerberSet;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.List;
import java.util.Set;

/**
 * JLCPCB as a pcb house: the Gerber set with its assembly tables in JLCPCB's columns, by the profile in
 * this jar ({@code settings/plugin/cad-jlcpcb/assembly}). It stands on the Gerber plugin; with that one off, this one is refused too.
 */
public final class JlcpcbPlugin implements Plugin {

    public static final String ID = "cad-jlcpcb";

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/cad-jlcpcb/assembly. */
    public static final SettingsSection<AssemblyProfile> ASSEMBLY = SettingsSection.ofPlugin(ID, "assembly", "JLCPCB assembly files", AssemblyProfile.class);

    /** The format's word on the exchange routes: {@code /exchange/jlcpcb/export}. */
    static final String FORMAT = "jlcpcb";

    private final GerberSet   set;
    private final CadSettings sections;

    public JlcpcbPlugin(GerberSet set, CadSettings sections) {
        this.set = set;
        this.sections = sections;
    }

    @Override
    public List<Section> settings() {
        return List.of(CadSettings.declarationOf(ASSEMBLY));
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "JLCPCB", JlcpcbPlugin.class)
                .markedBy(JlcpcbPlugin.class, "jlcpcb.svg").requiring(Set.of(GerberPlugin.ID));
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(DocumentExport.POINT, new FabricationExport(FORMAT, "JLCPCB", set, () -> sections.read(ASSEMBLY)));
        registration.contribute(OutputJob.POINT, new OutputJob(FORMAT, "JLCPCB order", List.of(FORMAT)));
    }
}
