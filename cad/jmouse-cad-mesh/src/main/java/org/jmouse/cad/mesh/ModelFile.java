package org.jmouse.cad.mesh;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.grammar.WordNode;
import org.jmouse.mesh.MeshFormats;

import java.util.List;
import java.util.Locale;

/**
 * One of a model document's files ({@link ModelLinesParser#FILE}).
 *
 * @param format the format's id in {@link MeshFormats}
 * @param scale  the millimetres one unit of the file is; null for its format's standard
 * @param up     the axis the file stands up along; null for its format's standard
 */
public record ModelFile(String name, String format, Double scale, MeshFormats.Axis up) {

    /** A file in its format's own convention. */
    public static ModelFile of(String name, String format) {
        return new ModelFile(name, format, null, null);
    }

    /** The files a model document names, in their order. */
    public static List<ModelFile> of(DocumentNode model) {
        return model.getExpressions().stream()
                .filter(statement -> statement instanceof ClauseLineNode line && line.grammar() == ModelLinesParser.FILE)
                .map(statement -> {
                    ClauseLineNode line = (ClauseLineNode) statement;
                    return new ModelFile(((WordNode) line.slot("name").orElseThrow()).word(), line.word("format").orElseThrow(),
                            line.quantity("scale").orElse(null),
                            line.word("up").map(axis -> MeshFormats.Axis.valueOf(axis.toUpperCase(Locale.ROOT))).orElse(null));
                })
                .toList();
    }

    /** {@code file "NAME" format FORMAT [scale s] [up y|z]}. */
    public ClauseLineNode line() {
        LineBuilder line = LineBuilder.line(ModelLinesParser.FILE).slot("name", Nodes.text(name))
                .clause("format", Nodes.word(format));
        if (scale != null) {
            line.clause("scale", Nodes.number(scale));
        }
        if (up != null) {
            line.clause("up", Nodes.word(up.name().toLowerCase(Locale.ROOT)));
        }
        return line.build();
    }
}
