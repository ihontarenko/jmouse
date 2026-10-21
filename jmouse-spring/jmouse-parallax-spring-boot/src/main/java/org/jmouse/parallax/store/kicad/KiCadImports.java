package org.jmouse.parallax.store.kicad;

import org.jmouse.kicad.KiCadProject;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.kicad.parser.KiCadParseException;
import org.jmouse.kicad.parser.KiCadParser;
import org.jmouse.kicad.schematic.KiCadSchematic;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.jpa.catalogue.ObjectStore;
import org.jmouse.parallax.kicad.KiCadProjectImport;
import org.jmouse.parallax.event.KiCadDesignImported;
import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;
import org.jmouse.parallax.script.writer.DocumentWriter;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.store.catalogue.ComponentModels;
import org.jmouse.parallax.store.catalogue.ObjectDraft;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

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
    private final KiCadLibraryMaps maps;
    private final KiCadFootprintLibraries installedFootprints;
    private final ApplicationEventPublisher events;

    /** @param installedFootprints the footprint libraries a design's own are searched before: KiCad's installed ones */
    public KiCadImports(Catalogue catalogue, ObjectStore objects, ComponentModels components, KiCadLibraryMaps maps,
                        KiCadFootprintLibraries installedFootprints, ApplicationEventPublisher events) {
        this.catalogue = catalogue;
        this.objects = objects;
        this.components = components;
        this.maps = maps;
        this.installedFootprints = installedFootprints;
        this.events = events;
    }

    /**
     * @param rootFile the root schematic's file name, {@code LINEAR_500W_PS-1.kicad_sch}
     * @param files    every sheet's text by its file name, the root's among them — what the reader chose —, and the
     *                 footprints of the project's own libraries as {@code LIBRARY.pretty/NAME.kicad_mod}; the server
     *                 reads no file of the design itself, only KiCad's installed libraries
     */
    @Transactional
    public Imported importDesign(ParallaxOwner owner, String rootFile, Map<String, String> files) {
        KiCadProject project = project(rootFile, files);
        String name = rootFile.replaceFirst("\\.kicad_sch$", "");
        KiCadProjectImport imported = KiCadProjectImport.of(project, name, maps.read(),
                new ModelsCatalogue(components), KiCadFootprintLibraries.among(files).then(installedFootprints));
        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        List<String> reused = new ArrayList<>();
        // The footprints first: a component made below names its footprint, and is stored only when that is there.
        imported.footprints().forEach(footprint -> storeMade(owner, footprint, created, updated, reused));
        imported.symbols().forEach(symbol -> storeMade(owner, symbol, created, updated, reused));
        // A component made from a KiCad symbol after the symbol it is drawn with: one the catalogue already holds — a
        // model written for it since, above all — is the one the circuits use.
        imported.components().forEach(component -> storeMade(owner, component, created, updated, reused));
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
        events.publishEvent(new KiCadDesignImported(owner, circuits.getFirst().slug(), imported.libraries(),
                circuits.stream().map(DefinedObject::slug).toList()));
        return new Imported(circuits.getFirst().slug(), created, updated, reused, imported.notes());
    }

    /**
     * A footprint, a symbol or a component the import makes, stored where nothing is called so. One an earlier import of this place made
     * and nobody has edited since — still its first revision — is written over, so a better drawing or pin order reaches
     * it; any other is kept as it is: a model written for it, a curator's edit, another place's or the catalogue's.
     * ⚠️ Being written over makes it a second revision, so it is refreshed once: the store keeps no record of who wrote a
     * revision, and an edit cannot be told from an import's rewrite otherwise.
     */
    private void storeMade(ParallaxOwner owner, ExperienceDocument made, List<String> created, List<String> updated,
                           List<String> reused) {
        Optional<MathematicalObject> existing = objects.findBySlug(made.slug());
        if (existing.isEmpty()) {
            catalogue.createOwned(owner, draft(DocumentWriter.write(made)));
            created.add(made.slug());
            return;
        }
        MathematicalObject stored = existing.get();
        boolean untouched = stored.owner().map(owner::equals).orElse(false) && stored.getRevision() == 1;
        if (!untouched) {
            reused.add(made.slug());
            return;
        }
        // Read before the update: the stored object is the managed entity the update changes.
        int before = stored.getRevision();
        int after = catalogue.update(stored.getId(), draft(DocumentWriter.write(made)), stored.getVersion()).summary()
                .definitionRevision();
        // An unchanged text writes nothing: the object is as this import would make it.
        (after == before ? reused : updated).add(made.slug());
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

    private static ObjectDraft draft(String source) {
        return new ObjectDraft(source, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
