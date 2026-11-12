package org.jmouse.cad.store.plugin;

import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbLayers;
import org.jmouse.cad.exchange.sheet.SheetChecks;
import org.jmouse.cad.pcb.layout.PcbLayout;
import org.jmouse.cad.pcb.physics.CopperPhysics;
import org.jmouse.cad.pcb.physics.StackPhysics;
import org.jmouse.cad.pcb.routing.RoutingSettings;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.settings.SettingsOwner;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.document.part.FetchCompletion;
import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.exchange.AcceptedFiles;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.DocumentFlattening;
import org.jmouse.cad.exchange.OutputJob;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.simulation.AnalysisEngine;
import org.jmouse.cad.store.catalogue.PlaceSource;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;
import org.jmouse.plugin.PluginSetting;
import org.jmouse.cad.store.CadProperties;

import java.util.List;

/**
 * The engine itself, as the first plugin: it opens the points the engine reads — the language's modules, the formats a
 * document is exchanged in, the engines beside its own, the places parts are looked for — and brings what the engine is made of, the language's own
 * modules. Every other plugin stands on it; with it alone, jmCAD is whole.
 */
public final class CadEnginePlugin implements Plugin {

    public static final String ID = "cad-engine";

    /**
     * The output jobs any installation offers.
     *
     * @param jobs every job: a name, the outputs it writes, and how each is written
     */
    public record OutputJobs(List<OutputJob> jobs) {
    }

    /** Its section of the settings tree (ADR 0004): the output jobs every installation offers. */
    public static final SettingsSection<OutputJobs> OUTPUT_JOBS = new SettingsSection<>("output/jobs", "Output jobs", SettingsOwner.ENGINE_DATA, OutputJobs.class);

    /**
     * The core's sections of the settings tree: what the engine itself computes and makes with. A plugin contributes its own the
     * same way.
     */
    static final List<SettingsSection<?>> CORE_SECTIONS = List.of(PcbLayout.SECTION, RoutingSettings.SECTION, CopperPhysics.SECTION,
            StackPhysics.SECTION, PcbArtwork.SECTION, PcbLayers.SECTION, SheetChecks.SECTION, OUTPUT_JOBS);

    private final List<PluginSetting> settings;

    /** @param properties what the engine's settings start from */
    public CadEnginePlugin(CadProperties properties) {
        this.settings = EngineConfiguration.settingsFrom(properties);
    }

    @Override
    public List<PluginSetting> settings() {
        return settings;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "jMouse CAD engine", CadEnginePlugin.class).drawnBy(CadEnginePlugin.class, "engine.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.declare(DocumentModule.POINT);
        registration.declare(DocumentImport.POINT);
        registration.declare(DocumentExport.POINT);
        registration.declare(DocumentFlattening.POINT);
        registration.declare(OutputJob.POINT);
        CadSettings.read(OUTPUT_JOBS).jobs().forEach(job -> registration.contribute(OutputJob.POINT, job));
        registration.declare(SettingsSection.POINT);
        CORE_SECTIONS.forEach(section -> registration.contribute(SettingsSection.POINT, section));
        registration.declare(AnalysisEngine.POINT);
        registration.declare(PartSearchProvider.POINT);
        registration.declare(FetchCompletion.POINT);
        registration.declare(AcceptedFiles.POINT);
        registration.declare(PlaceSource.POINT);
        registration.contribute(AcceptedFiles.POINT, new AcceptedFiles("jM documents", List.of("*" + DocumentInput.FILE_SUFFIX), List.of()));
        CadLanguage.engineModules().forEach(module -> registration.contribute(DocumentModule.POINT, module));
    }
}
