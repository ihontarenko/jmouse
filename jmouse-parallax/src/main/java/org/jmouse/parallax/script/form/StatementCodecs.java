package org.jmouse.parallax.script.form;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.Kind;

import java.util.List;
import java.util.Map;

/**
 * The statements at the top level of a body that are edited through fields: the declarations {@code use},
 * {@code extends} and {@code units}, and a component's {@code symbol uses …} header. Both are read by the reader, as a
 * line is.
 */
final class StatementCodecs {

    static final FieldSchema USE = FieldSchema.reference("use", "Use", FieldType.REFERENCES, true, List.of());
    static final FieldSchema EXTENDS = FieldSchema.reference("extends", "Extends", FieldType.REFERENCE, true,
            List.of(Kind.COMPONENT.keyword()));
    static final FieldSchema UNITS = FieldSchema.required("units", "Units", FieldType.NAMES);

    /** The declarations, as the schema lists them. */
    static final List<FieldSchema> DECLARATIONS = List.of(USE, EXTENDS, UNITS);

    /** {@code symbol uses resistor-symbol, resistor-iec}: the drawings a component may be shown with, the default first. */
    static final FieldSchema SYMBOL_USES = FieldSchema.reference("uses", "Drawn with", FieldType.REFERENCES, true,
            List.of(Kind.SYMBOL.keyword()));

    private StatementCodecs() {
    }

    static boolean isDeclaration(String word) {
        return Kind.DECLARATIONS.contains(word);
    }

    /** A declaration's one field and its value: {@code use lm358, ne555} → the references. */
    static List<FormField> readDeclaration(Kind kind, String statement) {
        ExperienceDocument document = LineCodecs.statement(kind, statement);
        String word = statement.strip().split("\\s+", 2)[0];
        return switch (word) {
            case "use" -> List.of(new FormField(USE, String.join(", ", document.uses())));
            case "extends" -> List.of(new FormField(EXTENDS, document.base().orElse("")));
            default -> List.of(new FormField(UNITS, String.join(", ", document.units())));
        };
    }

    static String writeDeclaration(String word, Map<String, String> values) {
        return word + " " + String.join(", ", LineCodecs.words(values, word));
    }

    /** Whether a {@code symbol} statement names drawings, rather than drawing the component itself. */
    static boolean namesSymbols(String header) {
        return header.strip().startsWith("uses");
    }

    static List<FormField> readSymbolHeader(Kind kind, String header) {
        Block.Symbol symbol = LineCodecs.statement(kind, "symbol " + header.strip()).block(Block.Symbol.class).orElseThrow();
        return List.of(new FormField(SYMBOL_USES, String.join(", ", symbol.uses())));
    }

    static String writeSymbolHeader(Map<String, String> values) {
        return "uses " + String.join(", ", LineCodecs.words(values, "uses"));
    }
}
