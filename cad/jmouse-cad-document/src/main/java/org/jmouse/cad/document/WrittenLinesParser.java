package org.jmouse.cad.document;

import org.jmouse.el.grammar.LinesParser;

import org.jmouse.el.parser.Parser;

/** {@code { line … }} whose lines are kept whole. */
public class WrittenLinesParser extends LinesParser {

    @Override
    protected Class<? extends Parser> lineParser() {
        return WrittenLineParser.class;
    }
}
