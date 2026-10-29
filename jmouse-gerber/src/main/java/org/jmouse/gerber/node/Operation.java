package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

import java.util.OptionalLong;

/**
 * {@code X1500Y-200I0J500D01*}: an operation at a point — a draw, a move or a flash. Each coordinate is the integer
 * the {@link FormatSpecification} reads; one left out keeps the pen's. An older file may open it with its interpolation
 * ({@code G01X…D01*}), which is kept.
 */
public final class Operation extends GerberCommand {

    private final Interpolation interpolation;
    private final OptionalLong  x;
    private final OptionalLong  y;
    private final OptionalLong  i;
    private final OptionalLong  j;
    private final OperationKind kind;

    /** @param interpolation the interpolation written in front of it; null for none */
    public Operation(Interpolation interpolation, OptionalLong x, OptionalLong y, OptionalLong i, OptionalLong j,
                     OperationKind kind, GerberPosition position) {
        super(position);
        this.interpolation = interpolation;
        this.x = x;
        this.y = y;
        this.i = i;
        this.j = j;
        this.kind = kind;
    }

    /** An operation at a point a program writes. */
    public static Operation at(long x, long y, OperationKind kind) {
        return new Operation(null, OptionalLong.of(x), OptionalLong.of(y), OptionalLong.empty(), OptionalLong.empty(),
                kind, GerberPosition.NONE);
    }

    /** A draw along an arc to a point, its centre given as the offset from where the pen is. */
    public static Operation arcTo(long x, long y, long i, long j) {
        return new Operation(null, OptionalLong.of(x), OptionalLong.of(y), OptionalLong.of(i), OptionalLong.of(j),
                OperationKind.DRAW, GerberPosition.NONE);
    }

    public OptionalLong x() {
        return x;
    }

    public OptionalLong y() {
        return y;
    }

    /** The arc's centre, across from the start. */
    public OptionalLong i() {
        return i;
    }

    /** The arc's centre, up from the start. */
    public OptionalLong j() {
        return j;
    }

    public OperationKind kind() {
        return kind;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        block(writer, words -> {
            if (interpolation != null) {
                words.word(interpolation.code());
            }
            coordinate(words, "X", x);
            coordinate(words, "Y", y);
            coordinate(words, "I", i);
            coordinate(words, "J", j);
            words.word(kind.code());
        });
    }

    private static void coordinate(SourceWriter words, String letter, OptionalLong value) {
        if (value.isPresent()) {
            words.word(letter).number(value.getAsLong());
        }
    }
}
