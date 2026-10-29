package org.jmouse.cad.schematic.drawing;

import java.util.List;

/**
 * A value of a living drawing as the browser evaluates it — read once as jMF, handed on as a tree, so the browser only
 * evaluates and never parses: a number, a name — an input, or a word the sheet colours by its tokens —, an operation
 * over operands, a call, a choice, or words printed as they are.
 */
public sealed interface DrawingExpression {

    record Number(double value) implements DrawingExpression {
    }

    /** An input of the drawing, or a word of {@link DrawingVocabulary#WORDS}. */
    record Name(String name) implements DrawingExpression {
    }

    /**
     * {@code operator} over its operands: {@code +}, {@code -} (one operand for a negation), {@code <}, {@code and},
     * {@code not} …, as jMF writes them.
     */
    record Operation(String operator, List<DrawingExpression> operands) implements DrawingExpression {

        public Operation {
            operands = List.copyOf(operands);
        }
    }

    /** A function of {@link DrawingVocabulary#FUNCTIONS} or {@link DrawingVocabulary#MATHEMATICS}. */
    record Call(String function, List<DrawingExpression> arguments) implements DrawingExpression {

        public Call {
            arguments = List.copyOf(arguments);
        }
    }

    /** {@code if}: each condition with its value, in order, then what holds otherwise. */
    record Choice(List<Branch> branches, DrawingExpression otherwise) implements DrawingExpression {

        public Choice {
            branches = List.copyOf(branches);
        }

        public record Branch(DrawingExpression condition, DrawingExpression value) {
        }
    }

    /** Words a glyph prints as they are: a caption the drawing always carries. */
    record Words(String words) implements DrawingExpression {
    }
}
