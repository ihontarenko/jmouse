package org.jmouse.cad.store.plugin;

import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.store.about.EngineAbout;
import org.jmouse.cad.simulation.AnalysisEngine;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

/**
 * The engine itself, as the first plugin: it opens the points the engine reads — the language's modules, the formats a
 * document is exchanged in, the engines beside its own, the places parts are looked for — and brings what the engine is made of, the language's own
 * modules. Every other plugin stands on it; with it alone, jmCAD is whole.
 */
public final class CadEnginePlugin implements Plugin {

    public static final String ID = "cad-engine";

    @Override
    public PluginDescriptor descriptor() {
        PluginDescriptor descriptor = PluginDescriptor.ofArtifact(ID, "jMouse CAD engine", CadEnginePlugin.class);
        // The engine is told to a reader by its own summary, not by the description of the module it is wired in.
        return descriptor.describedAs(EngineAbout.engineSummary(), descriptor.author()).drawnBy(CadEnginePlugin.class, "engine.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.declare(DocumentModule.POINT);
        registration.declare(DocumentImport.POINT);
        registration.declare(DocumentExport.POINT);
        registration.declare(AnalysisEngine.POINT);
        registration.declare(PartSearchProvider.POINT);
        CadLanguage.engineModules().forEach(module -> registration.contribute(DocumentModule.POINT, module));
    }
}
