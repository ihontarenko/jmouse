package org.jmouse.kicad.parser.node;

/**
 * A name a list is found by — {@code wire}, {@code at}, {@code symbol}. KiCad reserves no word: {@code wire} names a
 * list in one place and is a net's name in another, so the lexer reads every atom alike and a name means something
 * only where a reader of a file kind looks a list up by it. Such a reader keeps its names in one vocabulary that
 * implements this, rather than as words scattered through its code.
 */
public interface KiCadName {

    /** The word as KiCad writes it. */
    String word();
}
