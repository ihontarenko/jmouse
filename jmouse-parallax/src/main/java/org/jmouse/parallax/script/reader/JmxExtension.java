package org.jmouse.parallax.script.reader;

import org.jmouse.el.extension.Extension;
import org.jmouse.el.parser.Parser;
import org.jmouse.parallax.script.Kind;

import java.util.List;

import static org.jmouse.parallax.script.reader.JmxStatementParser.block;
import static org.jmouse.parallax.script.reader.JmxStatementParser.declaration;

/**
 * What jMX adds to the expression engine: the parser of a {@code .jmx} text and of its documents, and the statements
 * a document's braces hold — every word of the language and what reads it.
 *
 * <p>One extension per reading, because a reading carries the text a statement cuts its expressions out of, and the
 * types a document declares as it is read. It is imported beside jMS's own extension, which reads the bodies of
 * {@code behaviour} and {@code run}.</p>
 */
public final class JmxExtension implements Extension {

    private final JmxFileParser file;
    private final DocumentParser document;

    JmxExtension(Reading reading) {
        DeclarationReader declarations = new DeclarationReader(reading);
        CircuitReader circuits = new CircuitReader(reading);
        SymbolReader symbols = new SymbolReader(reading);
        PresentationReader presentation = new PresentationReader(reading);
        StructureReader structures = new StructureReader(reading, presentation);
        GraphReader graphs = new GraphReader(reading, circuits);
        ScriptBlocks scripts = new ScriptBlocks(reading);
        List<JmxStatementParser> statements = List.of(
                declaration("extends", keyword -> new DocumentStatement.Base(reading.wordAfter(keyword, "extends"))),
                declaration("units", keyword -> new DocumentStatement.Units(reading.namesAfter(keyword, "units"))),
                declaration("use", keyword -> new DocumentStatement.Uses(reading.namesAfter(keyword, "use"))),
                declaration("type", keyword -> {
                    declarations.type();
                    return new DocumentStatement.TypeDeclaration();
                }),
                JmxStatementParser.meta(keyword -> new DocumentStatement.MetaBlock(presentation.meta())),

                block("parameters", keyword -> declarations.parameters()),
                block("inputs", keyword -> declarations.inputs()),
                block("outputs", keyword -> declarations.outputs()),
                block("state", keyword -> declarations.state()),
                block("equations", keyword -> declarations.equations()),
                block("heat", keyword -> declarations.heat()),
                block("image", keyword -> declarations.image()),
                block("display", keyword -> declarations.display()),
                block("measure", keyword -> declarations.measure()),
                block("leads", keyword -> declarations.leads()),
                block("pins", keyword -> declarations.pins()),
                block("nodes", keyword -> declarations.nodes()),

                block("formulas", scripts::formulas),
                block("behaviour", keyword -> scripts.behaviour()),
                block("run", keyword -> scripts.run()),

                block("view", keyword -> presentation.view()),
                block("symbol", keyword -> symbols.symbol()),

                block("expect", kind -> kind == Kind.GRAPH, graphs::check),
                block("expect", kind -> kind != Kind.GRAPH, circuits::expectation),
                block("bench", circuits::sharedBench),
                block("parts", keyword -> circuits.parts()),
                block("nets", keyword -> circuits.nets()),
                block("layout", keyword -> circuits.layout()),
                block("ports", keyword -> circuits.ports()),
                block("probes", keyword -> circuits.probes()),
                block("analysis", circuits::analysis),
                block("sheet", circuits::sheet),

                block("maps", keyword -> structures.maps()),
                block("axiom", structures::axiom),
                block("rules", keyword -> structures.rules()),
                block("turtle", keyword -> structures.turtle()),
                block("steps", keyword -> structures.steps()),
                block("vertices", structures::vertices),
                block("edges", structures::edges),
                block("points", keyword -> structures.points()),
                block("items", structures::items),
                block("step", structures::courseStep),
                block("lesson", structures::lesson),
                block("section", structures::section),
                block("objects", structures::objectKinds),
                block("representations", structures::representations),
                block("opens", structures::opens),

                block("clock", graphs::clock),
                block("panes", graphs::panes),
                block("lines", keyword -> graphs.lines()));
        this.document = new DocumentParser(reading, declarations, symbols, new DocumentRules(reading), statements);
        this.file = new JmxFileParser(reading);
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(file, document);
    }
}
