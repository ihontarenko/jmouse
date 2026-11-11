package org.jmouse.cad.library;

import org.jmouse.cad.board.BoardModule;
import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.document.LanguageSchema;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.cad.simulation.SimulationModule;

import java.util.List;

/**
 * The jM language made of its modules: what reads, writes and describes a document. The engine's own modules —
 * schematics, boards, simulation — make the {@link #standard() standard} language, which reads every document the
 * library ships; an application with plugins makes one of the modules they contribute as well
 * ({@link DocumentModule#POINT}).
 */
public final class CadLanguage {

    private static final CadLanguage STANDARD = of(engineModules());

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

    /** A language of these modules, in this order. */
    public static CadLanguage of(List<DocumentModule> modules) {
        if (modules.isEmpty()) {
            throw new IllegalArgumentException("A language needs at least one module to read documents with.");
        }
        return new CadLanguage(modules);
    }

    /** The engine's own modules, each a new instance: what the engine contributes to {@link DocumentModule#POINT}. */
    public static List<DocumentModule> engineModules() {
        return List.of(new SchematicModule(), new BoardModule(), new SimulationModule());
    }

    public DocumentLanguage language() {
        return language;
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
