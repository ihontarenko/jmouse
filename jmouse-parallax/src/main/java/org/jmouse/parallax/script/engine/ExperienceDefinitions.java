package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.writer.DocumentWriter;
import org.jmouse.parallax.object.ObjectDefinition;

/**
 * The two-way adapter between jMX documents and catalogue objects: a document is read into a {@link DefinedObject},
 * and a {@link DefinedObject} is written as a document, by the mapping in {@code JMX-MAPPING.md}.
 *
 * <p>The test of the mapping is the round trip: for every object, reading what was written gives the object back —
 * the definition, its sliders, properties, representations, presets, relations and tags. Expressions travel as the
 * definition holds them and are never reformatted, so a round trip keeps every character of every formula.</p>
 *
 * <p>A component is the one kind whose definition is a document already: it is written as its own source and read
 * from its text, since a parsed document no longer carries the text it was read from.</p>
 */
public final class ExperienceDefinitions {

    /**
     * A parameter with a range as the slider it is: value, bounds, step and listed values in SI, a bare number in its
     * range read in the unit its value is written in ({@code C = 1 µF in 1 nF .. 10}). The one reading of a parameter's
     * range, for a circuit's sliders and a live part's control alike.
     */
    /** A part line as a circuit reads it ({@link EngineSelection}); {@code what} names where it is written, for the messages. */
    public static org.jmouse.parallax.object.ObjectDefinition.CircuitElement element(org.jmouse.parallax.script.Block.Part part, String what) {
        return EngineSelection.element(part, what);
    }

    public static org.jmouse.parallax.object.ParameterDefinition slider(org.jmouse.parallax.script.Block.Parameter parameter) {
        return ObjectDocumentReader.slider(parameter);
    }


    private ExperienceDefinitions() {
    }

    /**
     * The object a document describes.
     *
     * @throws IllegalArgumentException when the document is not one the mapping reads, naming what is missing or
     *                                  wrong; and for a component, which is read with {@link #read(String, String)}
     */
    public static DefinedObject read(ExperienceDocument document) {
        if (document.kind() == Kind.COMPONENT) {
            throw new IllegalArgumentException("Component '" + document.slug() + "' is its own source: read it from "
                    + "its text, with read(source, name).");
        }
        return ObjectDocumentReader.read(document);
    }

    /**
     * The object one jMX text describes; a component keeps the text as its definition.
     *
     * @param documentName what the text is called in a refusal: a file name, a catalogue path
     */
    public static DefinedObject read(String source, String documentName) {
        ExperienceDocument document = ExperienceReader.readOne(source, documentName);
        if (document.kind() == Kind.COMPONENT) {
            return ObjectDocumentReader.readAround(document, new ObjectDefinition.Component(source));
        }
        return ObjectDocumentReader.read(document);
    }

    /**
     * The object as jMX text the reader accepts. A component is its source, returned as it is: its head and
     * {@code meta} are where its title, summary and tags already live.
     *
     * @throws IllegalArgumentException when a value cannot be written so that it reads back the same — a text with
     *                                  both kinds of quote on one line, a property named like a field jMX reads itself
     */
    public static String write(DefinedObject object) {
        if (object.definition() instanceof ObjectDefinition.Component component) {
            return component.source();
        }
        return DocumentWriter.write(ObjectDocumentBuilder.documentOf(object));
    }

    /**
     * Settings written the way a view writes them after a representation's name — {@code axis Y offset 0},
     * {@code azimuth 45 target [0, 0.5, -1.25]} — as the values JSON would carry, each keeping its type. Documents
     * that are not objects (a course's steps, their cameras) write their settings in the same notation.
     *
     * @throws IllegalArgumentException when the text is not {@code name value} pairs
     */
    public static java.util.Map<String, Object> readSettings(String written) {
        return SettingNotation.read(written);
    }

    /**
     * The component a part of a circuit is, and so the symbols it may be drawn with: a model's reference as written
     * ({@code lm358@3}), or the component a two-terminal part is drawn from ({@code resistor}).
     */
    public static String componentOf(org.jmouse.parallax.object.ObjectDefinition.CircuitElement element) {
        if (element.type() == org.jmouse.parallax.object.ObjectDefinition.CircuitElementType.MODEL) {
            return element.component();
        }
        return DefinitionWords.Primitive.of(element.type()).reference();
    }

    /**
     * What the schematic canvas drew, written into the circuit's text line by line: a part or a layout line changes only
     * when what it says changed, so comments and formatting survive a save from the canvas.
     *
     * @param parameters the circuit's sliders; one a new part value names, and the text does not declare, is added
     * @param title      the title, or null to keep it; likewise the summary and the section
     */
    public static String patchCircuit(String source, org.jmouse.parallax.object.ObjectDefinition.Circuit circuit,
                                      java.util.List<org.jmouse.parallax.object.ParameterDefinition> parameters,
                                      String title, String summary, String category) {
        return CanvasTexts.circuit(source, circuit, parameters, new CanvasTexts.Head(title, summary, category));
    }

    /** What the drawing canvas drew, written into the symbol's text line by line. */
    public static String patchSymbol(String source, String drawing, String title, String category) {
        return CanvasTexts.symbol(source, drawing, new CanvasTexts.Head(title, null, category));
    }

    /** The structure and what is derived from it, each with the line it follows from (DECISIONS §15). */
    public static ObjectExplanation explain(String source, DefinedObject defined) {
        return ObjectExplanation.of(source, defined);
    }
}
