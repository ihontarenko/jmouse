package org.jmouse.el.grammar;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.el.writer.SourceLayout;
import org.jmouse.el.writer.SourceWritable;
import org.jmouse.el.writer.TextSourceWriter;
import org.jmouse.units.Quantity;

import java.util.List;

/**
 * The value nodes a program makes a line of, rather than writing it: a word, a text, a number, a point, a size, a list.
 * What a program builds is the tree; the tree writes itself, in whichever spelling.
 */
public final class Nodes {

    private Nodes() {
    }

    /** One word, hyphens and all: {@code through-hole}. */
    public static WordNode word(String word) {
        return new WordNode(word, false);
    }

    /** A text, in quotes when written. */
    public static WordNode text(String text) {
        return new WordNode(text, true);
    }

    /** A name as a line holds it, with no index: {@code leads}, {@code R1}. */
    public static NameNode name(String name) {
        return new NameNode(name, name, null, null);
    }

    /** An expression made of a node — a word, a number —, as the formula language reads it: {@code silver}, {@code 0.6}. */
    public static FormulaNode formula(SourceWritable value) {
        return new FormulaNode(TextSourceWriter.render(value, SourceLayout.WORDS));
    }

    /** {@code = value}: what a property is given. */
    public static AssignedNode assigned(SourceWritable value) {
        return new AssignedNode(formula(value));
    }

    /** A plain number, in as few digits as read it back. */
    public static QuantityLiteralNode number(double value) {
        return new QuantityLiteralNode(Quantity.plain(value));
    }

    /** {@code x, y}. */
    public static PointNode point(double x, double y) {
        return new PointNode(number(x), number(y));
    }

    /** {@code width x height}. */
    public static ExtentNode extent(double width, double height) {
        return new ExtentNode(number(width), number(height));
    }

    /**
     * Groups of words between commas, the words of a group between spaces — {@code 1 7, 2}, {@code clk edge, data} —,
     * as numbers and items are read.
     */
    public static ListNode groups(List<List<String>> groups) {
        return new ListNode(groups.stream().map(group -> (Expression) new ListNode(group.stream().map(Nodes::word)
                .map(Expression.class::cast).toList(), false)).toList());
    }

    /** Plain numbers between commas: {@code 0.25, 0.4}. */
    public static ListNode numbers(List<Double> values) {
        return new ListNode(values.stream().map(Nodes::number).map(Expression.class::cast).toList());
    }

    /** Words between commas: {@code vcc, 0}. */
    public static ListNode words(List<String> words) {
        return new ListNode(words.stream().map(Nodes::word).map(Expression.class::cast).toList());
    }
}
