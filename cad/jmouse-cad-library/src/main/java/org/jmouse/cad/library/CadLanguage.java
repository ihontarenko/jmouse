package org.jmouse.cad.library;

import java.util.function.Function;
import java.util.Optional;
import java.util.Map;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.document.LanguageSchema;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.cad.simulation.SimulationModule;

import java.util.ArrayList;
import java.util.List;

/**
 * The jM language made of its modules: what reads, writes and describes a document. The engine's own modules —
 * schematics, pcbs, simulation — make the {@link #standard() standard} language; with the 3D models besides, which a plugin
 * brings to an application, they make the {@link #shipped() shipped} one, which reads every document the library ships; an
 * application with plugins makes one of the modules they contribute as well ({@link DocumentModule#POINT}).
 */
public final class CadLanguage {

    private static final CadLanguage STANDARD = of(engineModules());

    private static final CadLanguage SHIPPED = of(shippedModules());

    private final List<DocumentModule> modules;
    private final DocumentLanguage     language;
    private final LanguageSchema       schema;

    private CadLanguage(List<DocumentModule> modules) {
        this.modules = List.copyOf(modules);
        this.language = DocumentLanguage.of(this.modules.toArray(DocumentModule[]::new));
        this.schema = LanguageSchema.of(this.modules);
    }

    /** The language of the engine's own modules alone. */
    public static CadLanguage standard() {
        return STANDARD;
    }

    /**
     * The language every document the library ships is read with: the engine's own modules, and the 3D models the library's
     * footprints stand with — a kind the engine leaves to a plugin, but whose documents the library holds.
     */
    public static CadLanguage shipped() {
        return SHIPPED;
    }

    /** A language of these modules, in this order. */
    public static CadLanguage of(List<DocumentModule> modules) {
        if (modules.isEmpty()) {
            throw new IllegalArgumentException("A language needs at least one module to read documents with.");
        }
        return new CadLanguage(modules);
    }

    /** The engine's own modules, each a new instance: what the engine contributes to {@link DocumentModule#POINT}. */
    public static List<DocumentModule> engineModules() {
        return List.of(new SchematicModule(), new PcbModule(), new SimulationModule());
    }

    /** The modules of the {@link #shipped() shipped} language, each a new instance. */
    private static List<DocumentModule> shippedModules() {
        List<DocumentModule> modules = new ArrayList<>(engineModules());
        modules.add(new ModelModule());
        return modules;
    }

    public DocumentLanguage language() {
        return language;
    }

    /**
     * What a document is to the engine — a {@code Component}, a {@code SymbolDrawing}, a model's files —, as the module
     * that brings its kind reads it; empty for a kind no module keeps a model of — a function library, a category.
     */
    public Optional<Object> modelOf(DocumentNode document) {
        for (DocumentModule module : modules) {
            for (Map.Entry<DocumentKind, Function<DocumentNode, Object>> reader : module.models().entrySet()) {
                if (reader.getKey().word().equals(document.kind().word())) {
                    return Optional.of(reader.getValue().apply(document));
                }
            }
        }
        return Optional.empty();
    }

    /** The modules the language is made of. */
    public List<DocumentModule> modules() {
        return modules;
    }

    /** The language as an editor builds its forms from it. */
    public LanguageSchema schema() {
        return schema;
    }
}
