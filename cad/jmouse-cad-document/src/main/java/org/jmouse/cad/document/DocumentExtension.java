package org.jmouse.cad.document;


import org.jmouse.el.extension.Extension;
import org.jmouse.el.parser.Parser;

import java.util.List;

/**
 * What the document language adds to the expression engine, whatever modules it is made of: the parsers of a text, of a
 * document and of its body, and the lines every document may write ({@code meta}, the declarations). The values its lines
 * hold are the grammar library's ({@link org.jmouse.el.grammar.GrammarExtension}).
 */
final class DocumentExtension implements Extension {

    private final DocumentGrammar grammar;

    DocumentExtension(DocumentGrammar grammar) {
        this.grammar = grammar;
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(
                new DocumentsParser(),
                new DocumentParser(grammar),
                new DocumentBodyParser(grammar),
                new MetaParser.MetaLinesParser(),
                new MetaLineParser(),
                new WrittenLinesParser(),
                new WrittenLineParser());
    }
}
