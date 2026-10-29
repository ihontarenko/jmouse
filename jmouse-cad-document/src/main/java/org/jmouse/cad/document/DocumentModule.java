package org.jmouse.cad.document;

import org.jmouse.el.grammar.BlockParser;

import org.jmouse.el.extension.Extension;
import org.jmouse.plugin.Contributions;

import java.util.List;

/**
 * What a part of the CAD engine brings to the document language: its kinds of document and the blocks they hold — the
 * schematic's symbols, the board's footprints, the simulation's circuits. A module is an extension of the expression
 * engine like any other: the parsers its blocks' lines are read with are its {@link #getParsers() parsers}.
 */
public interface DocumentModule extends Extension {

    /** Where the language finds its modules: the engine's own are contributed here first, a plugin's after them. */
    Contributions<DocumentModule> POINT = new Contributions<>("cad.language.modules", DocumentModule.class);

    List<DocumentKind> kinds();

    /** The blocks it reads; a document's body asks them by their word, never the engine's dispatch. */
    default List<BlockParser<?>> blocks() {
        return List.of();
    }
}
