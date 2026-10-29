package org.jmouse.spice.node;

import org.jmouse.el.writer.SourceWritable;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.el.writer.TextSourceWriter;
import org.jmouse.spice.SpicePosition;

import java.util.List;

/**
 * One argument of a card as written, for what a node keeps without a field of its own: an element's area, an
 * {@code AC} specification, a controlled source's control, the words of a dot card the reader has no node for. It
 * writes itself as SPICE writes it back.
 */
public sealed interface SpiceArgument extends SourceWritable {

    SpicePosition position();

    /** The argument as SPICE writes it back, on its own. */
    default String written() {
        return TextSourceWriter.render(this, SpiceNode.LAYOUT);
    }

    /** {@code out}, {@code 10uF}, {@code DC}, {@code params:}. */
    record Word(String text, SpicePosition position) implements SpiceArgument {
        @Override
        public void writeSource(SourceWriter writer) {
            writer.word(text);
        }
    }

    /** {@code {2*R1}}. */
    record Expression(SpiceExpression expression) implements SpiceArgument {
        @Override
        public SpicePosition position() {
            return expression.position();
        }

        @Override
        public void writeSource(SourceWriter writer) {
            writer.write(expression);
        }
    }

    /** {@code "models.lib"}: a text in double quotes. */
    record Text(String text, SpicePosition position) implements SpiceArgument {
        @Override
        public void writeSource(SourceWriter writer) {
            writer.text(text);
        }
    }

    /** {@code IS=2.52n}, {@code W={2*L}}. */
    record Assignment(String name, SpiceArgument value, SpicePosition position) implements SpiceArgument {
        @Override
        public void writeSource(SourceWriter writer) {
            writer.word(name).opening("=").write(value);
        }
    }

    /** {@code SIN(0 1 50)}, {@code POLY(2)}: a word and the arguments in the parentheses after it. */
    record Group(String name, List<SpiceArgument> arguments, SpicePosition position) implements SpiceArgument {
        public Group {
            arguments = List.copyOf(arguments);
        }

        @Override
        public void writeSource(SourceWriter writer) {
            // A group without a name stands as a word of its own; a named one opens right after its name.
            if (name.isEmpty()) {
                writer.openingWord("(");
            } else {
                writer.word(name).opening("(");
            }
            writer.each(arguments).join(")");
        }
    }
}
