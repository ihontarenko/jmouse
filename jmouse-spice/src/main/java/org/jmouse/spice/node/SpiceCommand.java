package org.jmouse.spice.node;

import org.jmouse.el.writer.SourceWritable;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.spice.SpicePosition;

import java.util.List;

/**
 * One line of a {@code .control} script — SPICE's interpreter, a language of its own: a line read, kept as written, or a
 * command a program builds — its word and its arguments, which write themselves —, so a program that runs a deck never
 * joins a script out of strings.
 */
public sealed interface SpiceCommand extends SourceWritable {

    /** A line of a script as read: the interpreter's, which this library does not read further. */
    record Written(String line) implements SpiceCommand {

        @Override
        public void writeSource(SourceWriter writer) {
            writer.verbatim(line);
        }
    }

    /** {@code tran 1e-05 0.01}, {@code set width=100000}, {@code print all}: a command and its arguments. */
    record Command(String word, List<SpiceArgument> arguments) implements SpiceCommand {

        public Command {
            arguments = List.copyOf(arguments);
        }

        /** A command of words alone: {@code print all}, {@code echo @@section tran}. */
        public static Command of(String word, String... arguments) {
            return new Command(word, List.of(arguments).stream()
                    .map(argument -> (SpiceArgument) new SpiceArgument.Word(argument, SpicePosition.NONE))
                    .toList());
        }

        @Override
        public void writeSource(SourceWriter writer) {
            writer.word(word).each(arguments).endLine();
        }
    }
}
