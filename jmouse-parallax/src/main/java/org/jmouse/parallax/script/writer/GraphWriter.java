package org.jmouse.parallax.script.writer;

import org.jmouse.parallax.script.Block;

import java.util.Locale;

/** The blocks of a {@code graph}: its clock, its panes, its lines and the claims it checks. */
final class GraphWriter {

    private GraphWriter() {
    }

    /** {@code clock t [in a .. b] [speed s] [loop]}; a speed of 1 is the clock's own and is not written. */
    static String clock(Block.Clock clock) {
        StringBuilder line = new StringBuilder("clock ").append(clock.parameter());
        clock.range().ifPresent(range -> line.append(" in ").append(Values.range(range)));
        if (clock.speed() != 1) {
            line.append(" speed ").append(Values.number(clock.speed()));
        }
        if (clock.loop()) {
            line.append(" loop");
        }
        return line.toString();
    }

    /** {@code panes rows { wave plane x 0 .. 4 pi  y -1 .. 1;  sheet circuit uses bridge-rectifier over 0 .. 20 ms }}. */
    static void panes(Block.Panes panes, SourceLines lines) {
        lines.block("panes " + panes.layout().name().toLowerCase(Locale.ROOT), inner -> panes.panes().forEach(pane -> {
            StringBuilder line = new StringBuilder(pane.name()).append(' ').append(pane.kind().name().toLowerCase(Locale.ROOT));
            pane.axes().forEach((axis, range) -> line.append(' ').append(axis).append(' ').append(Values.range(range)));
            pane.uses().ifPresent(uses -> line.append(" uses ").append(uses));
            pane.over().ifPresent(over -> line.append(" over ").append(Values.range(over)));
            pane.camera().ifPresent(camera -> line.append(" camera ").append(camera));
            inner.line(line.toString());
        }));
    }

    /** {@code name expression [until u] [at p] [from pane] [in pane] [color n] [hidden]}; a label in quotes. */
    static void lines(Block.Lines graphLines, SourceLines lines) {
        lines.block("lines", inner -> graphLines.lines().forEach(line -> {
            StringBuilder written = new StringBuilder(line.name()).append(' ')
                    .append(line.label() ? Values.text(line.expression().text()) : line.expression().text());
            line.until().ifPresent(until -> written.append(" until ").append(until.text()));
            line.at().ifPresent(at -> written.append(" at ").append(at.text()));
            line.from().ifPresent(from -> written.append(" from ").append(from));
            line.pane().ifPresent(pane -> written.append(" in ").append(pane));
            line.color().ifPresent(color -> written.append(" color ").append(color));
            if (line.hidden()) {
                written.append(" hidden");
            }
            inner.line(written.toString());
        }));
    }

    /** {@code expect "what it claims" { measure …;  near … }}, its criterion as it was written when it was read. */
    static void check(Block.GraphCheck check, SourceLines lines) {
        lines.block("expect " + Values.text(check.title()), inner -> {
            inner.line(check.measure().isEmpty() ? "measure" : "measure " + check.measure().text());
            inner.line(check.written() == null ? CircuitWriter.criterion(check.criterion()) : check.written());
        });
    }
}
