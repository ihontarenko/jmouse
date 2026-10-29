package org.jmouse.svg.path.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceLayout;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.el.writer.TextSourceWriter;

import java.util.List;
import java.util.stream.Stream;

/**
 * SVG path data as its tree: the commands in order. The tree is the truth — a caller reads geometry from the commands
 * and makes a path with {@link PathBuilder}, never by writing text; the text is what it writes itself as.
 */
public final class PathData extends AbstractExpression {

    private final List<PathCommand> commands;

    public PathData(List<PathCommand> commands) {
        this.commands = List.copyOf(commands);
    }

    public List<PathCommand> commands() {
        return commands;
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }

    /** The commands of this path, then those of another: two figures drawn as one path. */
    public PathData followedBy(PathData other) {
        return new PathData(Stream.concat(commands.stream(), other.commands.stream()).toList());
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        commands.forEach(command -> command.writeSource(writer));
    }

    /** The path as SVG path data: {@code M-4 0H40M-8 -2H-4V2H-8Z}. */
    @Override
    public String toSource() {
        return TextSourceWriter.render(this, SourceLayout.WORDS).strip();
    }

    @Override
    public String toString() {
        return toSource();
    }
}
