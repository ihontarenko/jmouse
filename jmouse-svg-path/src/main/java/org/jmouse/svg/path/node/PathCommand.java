package org.jmouse.svg.path.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceWriter;

import java.util.List;

/**
 * One command of a path: its kind, whether it is relative to where the pen stands, its numbers, and whether its letter
 * is written — a command that only goes on with the numbers of the one before ({@code M0 0 10 10}) is not. It writes its
 * letter against what comes before it, as path data is usually written ({@code M-4 0H40}).
 */
public final class PathCommand extends AbstractExpression {

    private final PathCommandKind  kind;
    private final boolean          relative;
    private final boolean          lettered;
    private final List<PathNumber> numbers;

    public PathCommand(PathCommandKind kind, boolean relative, boolean lettered, List<PathNumber> numbers) {
        if (numbers.size() != kind.arity()) {
            throw new IllegalArgumentException("'" + kind.letter(relative) + "' takes " + kind.arity() + " numbers, not " + numbers.size());
        }
        this.kind = kind;
        this.relative = relative;
        this.lettered = lettered;
        this.numbers = List.copyOf(numbers);
    }

    public PathCommandKind kind() {
        return kind;
    }

    public boolean relative() {
        return relative;
    }

    /** Whether its letter is written, rather than its numbers going on after the command before it. */
    public boolean lettered() {
        return lettered;
    }

    public List<PathNumber> numbers() {
        return numbers;
    }

    /** One of its numbers, by place: an arc's {@code x} is {@code number(5)}. */
    public double number(int index) {
        return numbers.get(index).value();
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    /** Its letter against what comes before it, its first number against its letter, the rest a space apart. */
    @Override
    public void writeSource(SourceWriter writer) {
        int from = 0;
        if (lettered) {
            writer.join(String.valueOf(kind.letter(relative)));
            if (!numbers.isEmpty()) {
                writer.join(numbers.getFirst().written());
                from = 1;
            }
        }
        for (PathNumber number : numbers.subList(from, numbers.size())) {
            if (number.joined()) {
                writer.join(number.written());
            } else {
                writer.word(number.written());
            }
        }
    }

    @Override
    public String toString() {
        return (lettered ? String.valueOf(kind.letter(relative)) : "") + numbers;
    }
}
