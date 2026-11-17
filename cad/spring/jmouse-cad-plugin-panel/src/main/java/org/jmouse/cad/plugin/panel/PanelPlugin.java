package org.jmouse.cad.plugin.panel;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.settings.Section;
import org.jmouse.cad.panel.PanelDefaults;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.exchange.DocumentFlattening;
import org.jmouse.cad.panel.PanelFlattening;
import org.jmouse.cad.panel.PanelModule;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.List;

/** Panels: one kind of document in the language, and how it is written, as the one pcb it makes, in every format of a pcb. */
public final class PanelPlugin implements Plugin {

    public static final String ID = "cad-panel";

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/cad-panel/defaults. */
    public static final SettingsSection<PanelDefaults> DEFAULTS = SettingsSection.ofPlugin(ID, "defaults", "Panel defaults", PanelDefaults.class);

    private final PanelFlattening flattening;

    PanelPlugin(PanelFlattening flattening) {
        this.flattening = flattening;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Panels", PanelPlugin.class).withIcon(PanelPlugin.class, "panel.svg");
    }

    @Override
    public List<Section> settings() {
        return List.of(CadSettings.declarationOf(DEFAULTS));
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(DocumentModule.POINT, new PanelModule());
        registration.contribute(DocumentFlattening.POINT, flattening);
    }
}
