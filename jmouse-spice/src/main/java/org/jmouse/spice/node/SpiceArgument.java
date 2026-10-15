package org.jmouse.spice.node;

import org.jmouse.spice.SpicePosition;

import java.util.List;

/**
 * One argument of a card as written, for what a node keeps without a field of its own: an element's area, an
 * {@code AC} specification, a controlled source's control, the words of a dot card the reader has no node for.
 */
public sealed interface SpiceArgument {

    SpicePosition position();

    /** The argument as SPICE writes it back. */
    String written();

    /** {@code out}, {@code 10uF}, {@code DC}, {@code params:}. */
    record Word(String text, SpicePosition position) implements SpiceArgument {
        @Override
        public String written() {
            return text;
        }
    }

    /** {@code {2*R1}}. */
    record Expression(SpiceExpression expression) implements SpiceArgument {
        @Override
        public SpicePosition position() {
            return expression.position();
        }

        @Override
        public String written() {
            return expression.written();
        }
    }

    /** {@code "models.lib"}: a text in double quotes. */
    record Text(String text, SpicePosition position) implements SpiceArgument {
        @Override
        public String written() {
            return "\"" + text + "\"";
        }
    }

    /** {@code IS=2.52n}, {@code W={2*L}}. */
    record Assignment(String name, SpiceArgument value, SpicePosition position) implements SpiceArgument {
        @Override
        public String written() {
            return name + "=" + value.written();
        }
    }

    /** {@code SIN(0 1 50)}, {@code POLY(2)}: a word and the arguments in the parentheses after it. */
    record Group(String name, List<SpiceArgument> arguments, SpicePosition position) implements SpiceArgument {
        public Group {
            arguments = List.copyOf(arguments);
        }

        @Override
        public String written() {
            return name + "(" + String.join(" ", arguments.stream().map(SpiceArgument::written).toList()) + ")";
        }
    }
}
