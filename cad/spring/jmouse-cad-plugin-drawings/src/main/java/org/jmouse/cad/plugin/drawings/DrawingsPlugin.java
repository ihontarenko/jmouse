package org.jmouse.cad.plugin.drawings;

import org.jmouse.settings.Section;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.OutputJob;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.List;

/** The drawing a factory is sent beside a pcb's files: fabrication and assembly sheets, as PDF and DXF. */
public final class DrawingsPlugin implements Plugin {

    public static final String ID = "cad-drawings";

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/cad-drawings/sheets. */
    public static final SettingsSection<SheetSettings> SHEETS = SettingsSection.ofPlugin(ID, "sheets", "Drawing sheets", SheetSettings.class);

    /** The job of the files and the drawing together; it is offered only while every format of it runs. */
    private static final OutputJob WITH_DRAWING = new OutputJob("factory-with-drawing", "For the factory, with its drawing",
            List.of("gerber", "step", DrawingsExport.FORMAT));

    private final DrawingsExport      drawings;
    private final List<Section> settings;

    DrawingsPlugin(DrawingsExport drawings, List<Section> settings) {
        this.drawings = drawings;
        this.settings = List.copyOf(settings);
    }

    @Override
    public List<Section> settings() {
        return settings;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Factory drawings", DrawingsPlugin.class).markedBy(DrawingsPlugin.class, "drawings.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(DocumentExport.POINT, drawings);
        registration.contribute(OutputJob.POINT, WITH_DRAWING);
    }
}
