package org.jmouse.kicad.symbol;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.parser.KiCadDocument;
import org.jmouse.kicad.parser.node.KiCadList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A symbol library — a {@code .kicad_sym} file —: its symbols by name, each one that {@code extends} another given its
 * parent's units and pins, as KiCad shows it.
 */
public final class KiCadSymbolLibrary {

    private final String name;
    private final Map<String, KiCadSymbolDefinition> symbols;

    private KiCadSymbolLibrary(String name, Map<String, KiCadSymbolDefinition> symbols) {
        this.name = name;
        this.symbols = symbols;
    }

    /**
     * The library a document holds.
     *
     * @throws IllegalArgumentException when it is not a symbol library, or a symbol extends one it does not hold
     */
    public static KiCadSymbolLibrary of(KiCadDocument document) {
        if (!document.root().is(SYMBOL_LIBRARY_FILE)) {
            throw new IllegalArgumentException(document.name() + " is a " + document.kind() + ", not a symbol library.");
        }
        Map<String, KiCadSymbolDefinition> written = new LinkedHashMap<>();
        for (KiCadList symbol : document.root().lists(SYMBOL)) {
            KiCadSymbolDefinition definition = KiCadSymbolDefinition.of(symbol);
            written.put(definition.name(), definition);
        }
        Map<String, KiCadSymbolDefinition> symbols = new LinkedHashMap<>();
        written.forEach((symbolName, definition) -> symbols.put(symbolName, derived(document.name(), definition, written)));
        return new KiCadSymbolLibrary(document.name(), symbols);
    }

    private static KiCadSymbolDefinition derived(String library, KiCadSymbolDefinition definition,
                                                 Map<String, KiCadSymbolDefinition> written) {
        if (definition.extendsName().isEmpty()) {
            return definition;
        }
        KiCadSymbolDefinition parent = written.get(definition.extendsName());
        if (parent == null) {
            throw new IllegalArgumentException(library + ": " + definition.name() + " extends " + definition.extendsName()
                    + ", which the library does not hold.");
        }
        return definition.derivedFrom(derived(library, parent, written));
    }

    public String name() {
        return name;
    }

    public List<KiCadSymbolDefinition> symbols() {
        return List.copyOf(symbols.values());
    }

    public Optional<KiCadSymbolDefinition> symbol(String symbolName) {
        return Optional.ofNullable(symbols.get(symbolName));
    }
}
