package org.jmouse.cad.management;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jmouse.cad.Formula;
import org.jmouse.cad.Given;
import org.jmouse.cad.Text;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

/**
 * How the engine's data crosses this API as JSON, where the engine's own shape is not enough: the engine keeps no
 * JSON of its own — how its data is sent is the product's.
 */
final class CadJson {

    private CadJson() {
    }

    /**
     * A drawing's value is answered with its trees — what a browser evaluates — and read without them: they follow from
     * its words, which the server reads again, so an editor never has to send what it could not have written.
     */
    @JsonIgnoreProperties(value = "expressions", allowGetters = true)
    private interface ValueWithoutTrees {
    }

    /**
     * Where something of a sheet stands is known by what it says, as a reader of the answer knows it: a wire by its
     * {@code from} and {@code to}, a mark by its {@code symbol}, a placed part by the rest. Nothing names the type.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({@JsonSubTypes.Type(Placement.PlacedPart.class), @JsonSubTypes.Type(Placement.Mark.class),
                   @JsonSubTypes.Type(Placement.Wire.class)})
    private interface PlacementByFields {
    }

    /** A setting's value is a formula — {@code written} — or a text — {@code text} —, known by the field it carries. */
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({@JsonSubTypes.Type(Formula.class), @JsonSubTypes.Type(Text.class)})
    private interface GivenByFields {
    }

    static JacksonModule module() {
        SimpleModule module = new SimpleModule("jmouse-cad");
        module.setMixInAnnotation(SymbolDrawing.Value.class, ValueWithoutTrees.class);
        module.setMixInAnnotation(Placement.class, PlacementByFields.class);
        module.setMixInAnnotation(Given.class, GivenByFields.class);
        return module;
    }
}
