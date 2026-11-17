package org.jmouse.cad.schematic.drawing;

import java.util.List;

/**
 * A value of a living drawing as the browser evaluates it — read once as jMF, handed on as a tree, so the browser only
 * evaluates and never parses: a number, a name — an input, or a word the sheet colours by its tokens —, an operation
 * over operands, a call, a choice, or words printed as they are.
 */
public sealed interface DrawingExpression {

    /**
     * A number.
     *
     * @param value the number
     */
    record Number(double value) implements DrawingExpression {
    }

    /**
     * An input of the drawing, or a word of the drawing's vocabulary, which the sheet colours by its tokens.
     *
     * @param name the input's name, or the word
     */
    record Name(String name) implements DrawingExpression {
    }

    /**
     * {@code operator} over its operands: {@code +}, {@code -} (one operand for a negation), {@code <}, {@code and},
     * {@code not} …, as jMF writes them.
     *
     * @param operator the operator, as jMF writes it
     * @param operands what it works on, in order
     */
    record Operation(String operator, List<DrawingExpression> operands) implements DrawingExpression {

        public Operation {
            operands = List.copyOf(operands);
        }
    }

    /**
     * A function of the drawing's vocabulary, or of mathematics, called on its arguments.
     *
     * @param function  the function, by name
     * @param arguments what it is called on, in order
     */
    record Call(String function, List<DrawingExpression> arguments) implements DrawingExpression {

        public Call {
            arguments = List.copyOf(arguments);
        }
    }

    /**
     * {@code if}: each condition with its value, in order, then what holds otherwise.
     *
     * @param branches  each condition with its value, in order
     * @param otherwise what holds when no condition does
     */
    record Choice(List<Branch> branches, DrawingExpression otherwise) implements DrawingExpression {

        public Choice {
            branches = List.copyOf(branches);
        }

        /**
         * A condition, and the value that holds when it does.
         *
         * @param condition when it holds
         * @param value     what holds then
         */
        public record Branch(DrawingExpression condition, DrawingExpression value) {
        }
    }

    /**
     * Words a glyph prints as they are: a caption the drawing always carries.
     *
     * @param words the words
     */
    record Words(String words) implements DrawingExpression {
    }
}
