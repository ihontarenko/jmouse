package org.jmouse.parallax.store.kicad;

import org.jmouse.datamap.DataMapReader;
import org.jmouse.kicad.KiCadProject;
import org.jmouse.kicad.parser.KiCadParseException;
import org.jmouse.kicad.parser.KiCadParser;
import org.jmouse.kicad.schematic.KiCadSchematic;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.jpa.catalogue.ObjectStore;
import org.jmouse.parallax.kicad.KiCadLibraryMap;
import org.jmouse.parallax.kicad.KiCadProjectImport;
import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;
import org.jmouse.parallax.script.writer.DocumentWriter;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.store.catalogue.ComponentModels;
import org.jmouse.parallax.store.catalogue.ObjectDraft;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A KiCad design brought into an owner's place: its sheets read, imported as jMX circuits and symbols, and stored —
 * the symbols first, a symbol the catalogue already holds taken as it is, then the sheets each before the sheet that
 * places it, a block naming the circuit of the sheet it places by its slug. Importing a design again writes the next
 * revision of each of its circuits.
 */
public class KiCadImports {

    /**
     * What an import stored.
     *
     * @param root    the slug of the design's root circuit
     * @param created what was stored new, by slug
     * @param updated the circuits of an earlier import of the same design, written over with this one
     * @param reused  the symbols and components the catalogue already held
     * @param notes   what the import could not carry, part by part
     */
    public record Imported(String root, List<String> created, List<String> updated, List<String> reused,
                           List<String> notes) {
    }

    private final Catalogue catalogue;
    private final ObjectStore objects;
    private final ComponentModels components;
    private final ResourceLoader resources;
    private final KiCadProperties properties;

    public KiCadImports(Catalogue catalogue, ObjectStore objects, ComponentModels components, ResourceLoader resources,
                        KiCadProperties properties) {
        this.catalogue = catalogue;
        this.objects = objects;
        this.components = components;
        this.resources = resources;
        this.properties = properties;
    }

    /**
     * @param rootFile the root schematic's file name, {@code LINEAR_500W_PS-1.kicad_sch}
     * @param files    every sheet's text by its file name, the root's among them — what the reader chose; the server
     *                 reads no file itself
     */
    @Transactional
    public Imported importDesign(ParallaxOwner owner, String rootFile, Map<String, String> files) {
        KiCadProject project = project(rootFile, files);
        String name = rootFile.replaceFirst("\\.kicad_sch$", "");
        KiCadProjectImport imported = KiCadProjectImport.of(project, name, libraryMap(),
                new ModelsCatalogue(components));
        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        List<String> reused = new ArrayList<>();
        for (ExperienceDocument symbol : imported.symbols()) {
            if (objects.findBySlug(symbol.slug()).isPresent()) {
                reused.add(symbol.slug());
            } else {
                catalogue.createOwned(owner, draft(DocumentWriter.write(symbol)));
                created.add(symbol.slug());
            }
        }
        // A component made from a KiCad symbol after the symbol it is drawn with, and only where none is called so: one
        // the catalogue already holds — a model written for it since, above all — is the one the circuits use.
        for (ExperienceDocument component : imported.components()) {
            if (objects.findBySlug(component.slug()).isPresent()) {
                reused.add(component.slug());
            } else {
                catalogue.createOwned(owner, draft(DocumentWriter.write(component)));
                created.add(component.slug());
            }
        }
        List<DefinedObject> circuits = imported.circuits();
        // A sheet before the sheet that places it: a block names its sheet by bare slug, the sheet's head, and a circuit
        // is stored only when every part it names is there.
        for (DefinedObject circuit : circuits.reversed()) {
            String source = ExperienceDefinitions.write(circuit);
            Optional<MathematicalObject> existing = objects.findBySlug(circuit.slug());
            if (existing.isPresent()) {
                if (!existing.get().owner().map(owner::equals).orElse(false)) {
                    throw ParallaxProblem.conflict("An object is already called '" + circuit.slug() + "' and is not "
                            + "this place's: rename the KiCad project or its sheet's file.");
                }
                catalogue.update(existing.get().getId(), draft(source), existing.get().getVersion());
                updated.add(circuit.slug());
            } else {
                catalogue.createOwned(owner, draft(source));
                created.add(circuit.slug());
            }
        }
        return new Imported(circuits.getFirst().slug(), created, updated, reused, imported.notes());
    }

    private static KiCadProject project(String rootFile, Map<String, String> files) {
        if (rootFile == null || files == null || !files.containsKey(rootFile)) {
            throw ParallaxProblem.invalid("A KiCad design is its root schematic and the sheets it places: send the root's "
                    + "file among them.");
        }
        KiCadParser parser = new KiCadParser();
        Map<String, KiCadSchematic> read = new HashMap<>();
        try {
            return KiCadProject.of(rootFile, file -> read.computeIfAbsent(file, named -> {
                String text = files.get(named);
                if (text == null) {
                    throw ParallaxProblem.invalid("The design places the sheet " + named + ", which was not sent: choose "
                            + "the project's folder, every sheet of it.");
                }
                return KiCadSchematic.of(parser.parse(named, text));
            }));
        } catch (KiCadParseException unreadable) {
            throw ParallaxProblem.invalid("invalid-kicad", unreadable.getMessage());
        }
    }

    private KiCadLibraryMap libraryMap() {
        var resource = resources.getResource(properties.libraryMap());
        if (!resource.exists()) {
            return KiCadLibraryMap.EMPTY;
        }
        try {
            String text = resource.getContentAsString(StandardCharsets.UTF_8);
            return KiCadLibraryMap.of(DataMapReader.read(properties.libraryMap(), text));
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The KiCad library map " + properties.libraryMap() + " cannot be read", unreadable);
        }
    }

    private static ObjectDraft draft(String source) {
        return new ObjectDraft(source, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
