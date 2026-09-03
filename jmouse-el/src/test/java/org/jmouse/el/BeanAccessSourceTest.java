package org.jmouse.el;

import org.jmouse.el.node.Expression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <strong>Can an expression holding an {@code @} call be written back out?</strong>
 *
 * <p>It could not: {@link org.jmouse.el.node.expression.BeanAccessNode} had no {@code toSource()}, and
 * the default one throws. Anything that renders a tree back into text — a document writer, a
 * normalising rewrite, a diff of two revisions, an editor that reformats — hit an
 * {@code UnsupportedOperationException} naming a node the caller had never heard of, because what it
 * asked to render was a whole condition.</p>
 *
 * <p>⚠️ <strong>Round trips rather than string comparisons.</strong> A test asserting exact output
 * pins the spacing of a renderer nobody promised, and goes red on a harmless change. What has to hold
 * is that the text comes back as the same tree: render it, parse that, render again, and the two
 * renderings agree. Anything that drops or invents a token breaks it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
class BeanAccessSourceTest {

    private final ExpressionLanguage el = new ExpressionLanguage();

    /**
     * Renders an expression, parses what came out, and renders that.
     *
     * @param expression the source to round-trip
     * @return the second rendering, which must equal the first
     */
    private String roundTrip(String expression) {
        String once = el.compile(expression).toSource();
        return el.compile(once).toSource();
    }

    private void survives(String expression) {
        String once = el.compile(expression).toSource();

        assertEquals(once, roundTrip(expression), "'%s' does not survive a round trip".formatted(expression));
    }

    @Test
    @DisplayName("a method call with no arguments keeps its empty brackets")
    void emptyCall() {
        assertEquals("@store.open()", el.compile("@store.open()").toSource());
        survives("@store.open()");
    }

    @Test
    @DisplayName("⚠️ one null argument is not the same as no arguments")
    void nullArgument() {
        // The parser records "no arguments" as a null literal in the arguments slot, so a renderer that
        // simply asked it for its source would turn `open()` into `open(null)` — a document that still
        // parses and no longer says the same thing.
        assertEquals("@store.open(null)", el.compile("@store.open(null)").toSource());
        assertEquals("@store.open()", el.compile("@store.open()").toSource());
    }

    @Test
    @DisplayName("a method call keeps every argument it was given")
    void arguments() {
        assertEquals("@journal.record('open')", el.compile("@journal.record('open')").toSource());
        assertEquals("@alarm.arm('front', 30)", el.compile("@alarm.arm('front', 30)").toSource());

        survives("@alarm.arm('front', 30, true)");
    }

    @Test
    @DisplayName("field access and constant access keep their punctuation")
    void fieldAndConstant() {
        assertEquals("@player:$id", el.compile("@player:$id").toSource());
        assertEquals("@player#MAX", el.compile("@player#MAX").toSource());

        survives("@player:$id");
        survives("@player#MAX");
    }

    @Test
    @DisplayName("⚠️ an @ call nested in an operation renders — this is where it used to throw")
    void nested() {
        // The reported failure was never on a BeanAccessNode a caller was holding. It was a
        // BinaryOperation asking its right-hand side to render, three levels below anything named.
        survives("@store.pending() > 2");
        survives("@player.has('refinery') and @player.credits() >= 200");
        survives("@world.at(unit, spice) or @world.has_resource(unit)");
    }

    @Test
    @DisplayName("⚠️ a call with more than one argument parses at all")
    void severalArgumentsParse() {
        // Not a rendering test. `BeanAccessParser` used to hand the opening bracket to the expression
        // parser, which read it as a parenthesised expression and demanded a ')' where the first comma
        // was — so every multi-argument call in the language failed, on the default extension, with a
        // message about a token nobody wrote wrong.
        assertEquals("@alarm.arm('front', 30)", el.compile("@alarm.arm('front', 30)").toSource());
        assertEquals("@world.spawn('scout', 'ridge', 2, true)",
                     el.compile("@world.spawn('scout', 'ridge', 2, true)").toSource());
    }
}
