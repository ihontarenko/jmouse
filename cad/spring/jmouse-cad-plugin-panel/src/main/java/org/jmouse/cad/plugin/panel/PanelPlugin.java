package org.jmouse.cad.plugin.panel;

import org.jmouse.cad.panel.PanelDefaults;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.settings.SettingsOwner;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.exchange.DocumentFlattening;
import org.jmouse.cad.panel.PanelFlattening;
import org.jmouse.cad.panel.PanelModule;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;
import org.jmouse.plugin.PluginSetting;

import java.util.List;

/** Panels: one kind of document in the language, and how it is written, as the one pcb it makes, in every format of a pcb. */
public final class PanelPlugin implements Plugin {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/panel/defaults. */
    public static final SettingsSection<PanelDefaults> DEFAULTS = new SettingsSection<>("plugin/panel/defaults", "Panel defaults", SettingsOwner.ENGINE_DATA, PanelDefaults.class);

    public static final String ID = "cad-panel";

    private final PanelFlattening     flattening;
    private final List<PluginSetting> settings;

    PanelPlugin(PanelFlattening flattening, List<PluginSetting> settings) {
        this.flattening = flattening;
        this.settings = List.copyOf(settings);
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Panels", PanelPlugin.class).markedBy(PanelPlugin.class, "panel.svg");
    }

    @Override
    public List<PluginSetting> settings() {
        return settings;
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(SettingsSection.POINT, DEFAULTS);
        registration.contribute(DocumentModule.POINT, new PanelModule());
        registration.contribute(DocumentFlattening.POINT, flattening);
    }
}
