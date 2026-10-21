package org.jmouse.parallax.store.kicad;

import org.jmouse.parallax.kicad.KiCadSchematicExport;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;
import org.jmouse.parallax.store.catalogue.ComponentModels;

/**
 * KiCad out: a jMX circuit — the text an editor holds, saved or not — as a KiCad schematic, its parts drawn with their
 * own symbols under the library ids the library map gives them back. Nothing is stored; what the components the circuit
 * names say is read as the reader may see them.
 */
public class KiCadExports {

    private final ComponentModels components;
    private final KiCadLibraryMaps maps;

    public KiCadExports(ComponentModels components, KiCadLibraryMaps maps) {
        this.components = components;
        this.maps = maps;
    }

    /**
     * @throws ParallaxProblem when the text is no circuit
     */
    public KiCadSchematicExport.Exported exportDocument(String source) {
        DefinedObject defined = ExperienceDefinitions.read(source, "circuit.jmx");
        if (!(defined.definition() instanceof ObjectDefinition.Circuit circuit)) {
            throw ParallaxProblem.invalid("A KiCad schematic is made of a circuit; this document is no circuit.");
        }
        return KiCadSchematicExport.of(defined.slug(), defined.title(), circuit, components.forDrawing(), maps.read());
    }
}
