package org.jmouse.parallax.script.writer;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.Criterion;
import org.jmouse.parallax.script.Block.Part;
import org.jmouse.parallax.script.Block.Placement;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Everything that is a netlist, in its one notation — a circuit's parts, a component's internal parts, an
 * expectation's bench — and the blocks around them: layout, nets, ports, probes, analysis, sheet, expectations.
 */
final class CircuitWriter {

    private CircuitWriter() {
    }

    /** {@code R1 resistor (in, out) resistance = R symbol resistor-iec}. */
    static String part(Part part) {
        StringBuilder line = new StringBuilder(part.id()).append(' ').append(part.reference());
        if (!part.positional().isEmpty()) {
            line.append(" (").append(Values.names(part.positional())).append(')');
        } else if (!part.named().isEmpty()) {
            List<String> connections = new ArrayList<>();
            part.named().forEach((pin, net) -> connections.add(pin + " = " + net));
            line.append(" (").append(String.join(", ", connections)).append(')');
        }
        List<String> settings = new ArrayList<>();
        part.settings().forEach((name, value) -> settings.add(value.isEmpty() ? name : name + " = " + value.text()));
        if (!settings.isEmpty()) {
            line.append(' ').append(String.join(", ", settings));
        }
        part.symbol().ifPresent(symbol -> line.append(" symbol ").append(symbol));
        return line.toString();
    }

    static void parts(String block, List<Part> parts, SourceLines lines) {
        lines.block(block, inner -> parts.forEach(part -> inner.line(part(part))));
    }

    // ─── how a circuit is drawn ─────────────────────────────────────────────────────────────────────

    /** The parts and marks where they stand, then the wires. */
    static void layout(Block.Layout layout, SourceLines lines) {
        lines.block("layout", inner -> {
            layout.items().forEach(item -> inner.line(item(item)));
            layout.wires().forEach(wire -> inner.line(wire(wire)));
        });
    }

    /** {@code R1 at 240, 200 rotate 90}, {@code U4.B at …}, {@code GND1 ground at …}, {@code A1 ground agnd at …}, {@code L1 label vp at …}. */
    static String item(Block.LayoutItem item) {
        StringBuilder line = new StringBuilder(item.id());
        item.unit().ifPresent(unit -> line.append('.').append(unit));
        item.mark().ifPresent(mark -> line.append(' ').append(mark));
        item.net().ifPresent(net -> line.append(' ').append(net));
        return line.append(" at ").append(placement(item.placement())).toString();
    }

    /** {@code x, y [rotate degrees] [mirror] [name at dx, dy] [shows rms graph]}. */
    private static String placement(Placement placement) {
        StringBuilder written = new StringBuilder(Values.point(placement.x(), placement.y()));
        if (placement.rotation() != 0) {
            written.append(" rotate ").append(Values.number(placement.rotation()));
        }
        if (placement.mirrored()) {
            written.append(" mirror");
        }
        placement.labels().forEach((label, point) -> written.append(' ').append(label).append(" at ")
                .append(Values.point(point.x(), point.y())));
        if (!placement.shows().isEmpty()) {
            written.append(" shows ").append(String.join(" ", placement.shows()));
        }
        return written.toString();
    }

    /** {@code W1 wire R1.negative to C1.positive via 240 120, 300 120}. */
    static String wire(Block.LayoutWire wire) {
        StringBuilder line = new StringBuilder(wire.id()).append(" wire ").append(wire.from()).append(" to ").append(wire.to());
        if (!wire.bends().isEmpty()) {
            line.append(" via ").append(String.join(", ", wire.bends().stream()
                    .map(bend -> Values.number(bend.x()) + " " + Values.number(bend.y())).toList()));
        }
        return line.toString();
    }

    static void nets(Block.Nets nets, SourceLines lines) {
        lines.block("nets", inner -> nets.nets().forEach(net -> inner.line(net(net))));
    }

    /** {@code name [ground] [global] [route x,y x,y …]}. */
    static String net(Block.Net net) {
        StringBuilder line = new StringBuilder(net.net());
        if (net.ground()) {
            line.append(" ground");
        }
        if (net.global()) {
            line.append(" global");
        }
        if (!net.route().isEmpty()) {
            line.append(" route ").append(String.join(" ", net.route().stream()
                    .map(point -> Values.number(point.x()) + "," + Values.number(point.y())).toList()));
        }
        return line.toString();
    }

    static void ports(Block.Ports ports, SourceLines lines) {
        lines.block("ports", inner -> ports.ports().forEach(port -> inner.line(port(port))));
    }

    /** {@code input in}, {@code output out}. */
    static String port(Block.Port port) {
        return port.direction().name().toLowerCase(Locale.ROOT) + " " + port.net();
    }

    /** The channels, then the time base the oscilloscope opens on. */
    static void probes(Block.Probes probes, SourceLines lines) {
        lines.block("probes", inner -> {
            probes.probes().forEach(probe -> inner.line(probe(probe)));
            probes.timePerDivision().ifPresent(time -> inner.line(timeBase(time)));
        });
    }

    /** {@code time 5 ms/div}. */
    static String timeBase(Quantity timePerDivision) {
        return "time " + timePerDivision.written() + "/div";
    }

    /** {@code CH1 out 2 V/div}, {@code CH2 out against ref}, {@code CH3 current R1}, {@code L1 level clock}. */
    static String probe(Block.Probe probe) {
        StringBuilder line = new StringBuilder(probe.id()).append(' ');
        switch (probe.measure()) {
            case 'I' -> line.append("current ");
            case 'L' -> line.append("level ");
            case 'P' -> line.append("power ");
            default -> {
            }
        }
        line.append(probe.target());
        probe.reference().ifPresent(reference -> line.append(" against ").append(reference));
        probe.scale().ifPresent(scale -> line.append(' ').append(scale.written()).append("/div"));
        probe.expression().ifPresent(expression -> line.append(" shows ").append(expression));
        return line.toString();
    }

    /** {@code analysis transient for 5 ms}, {@code analysis ac from 10 Hz to 1 MHz points 200}. */
    static String analysis(Block.Analysis analysis) {
        StringBuilder line = new StringBuilder("analysis ").append(analysis.kind().name().toLowerCase(Locale.ROOT));
        analysis.settings().forEach((key, value) -> line.append(' ').append(key).append(' ').append(value.text()));
        return line.toString();
    }

    /** {@code sheet 1200 x 760 grid 20}. */
    static String sheet(Block.Sheet sheet) {
        return "sheet " + Values.number(sheet.width()) + " x " + Values.number(sheet.height()) + " grid "
                + Values.number(sheet.grid());
    }

    // ─── expectations ───────────────────────────────────────────────────────────────────────────────

    /** {@code expect "title" [from "where"] { bench … { parts }  measure …  criterion }}. */
    static void expectation(Block.Expectation expectation, SourceLines lines) {
        String opening = "expect " + Values.text(expectation.title())
                + (expectation.reference() == null ? "" : " from " + Values.text(expectation.reference()));
        lines.block(opening, inner -> {
            if (hasBench(expectation)) {
                inner.block(benchHeader(expectation), bench -> expectation.bench().forEach(part -> bench.line(part(part))));
            }
            inner.line("measure " + measured(expectation.measure()));
            inner.line(criterion(expectation.criterion()));
        });
    }

    private static boolean hasBench(Block.Expectation expectation) {
        return !expectation.bench().isEmpty() || expectation.sharedBench() != null || expectation.duration() != 0
                || expectation.temperature() != null || expectation.realistic();
    }

    /** {@code bench [shared] [for duration] [at temperature] [realistic]}. */
    private static String benchHeader(Block.Expectation expectation) {
        StringBuilder header = new StringBuilder("bench");
        if (expectation.sharedBench() != null) {
            header.append(' ').append(expectation.sharedBench());
        }
        boolean timed = expectation.duration() != 0 || expectation.temperature() != null || expectation.realistic();
        if (timed) {
            header.append(" for ").append(Values.number(expectation.duration())).append(" s");
        }
        if (expectation.temperature() != null) {
            header.append(" at ").append(Values.number(expectation.temperature())).append(" °C");
        }
        if (expectation.realistic()) {
            header.append(" realistic");
        }
        return header.toString();
    }

    /** The words of a measurement, a word that was a text in quotes back in its quotes. */
    private static String measured(List<String> words) {
        return String.join(" ", words.stream().map(word -> word.isEmpty() || word.chars().anyMatch(Character::isWhitespace)
                ? Values.text(word) : word).toList());
    }

    /** {@code near 6.86 kHz +- 5%}, {@code within 26 V .. 30 V}, {@code is 1}. */
    static String criterion(Criterion criterion) {
        return switch (criterion.kind()) {
            case NEAR -> "near " + Values.amount(criterion.typical().orElseThrow()) + " +- "
                    + Values.amount(criterion.tolerance().orElseThrow());
            case WITHIN -> {
                StringBuilder line = new StringBuilder("within");
                criterion.minimum().ifPresent(minimum -> line.append(' ').append(Values.amount(minimum)));
                line.append(" ..");
                criterion.maximum().ifPresent(maximum -> line.append(' ').append(Values.amount(maximum)));
                yield line.toString();
            }
            case IS -> "is " + criterion.exact();
        };
    }

    /** {@code bench i2c-bus { parts }}: parts every expectation naming the bench stands on. */
    static void sharedBench(Block.Bench bench, SourceLines lines) {
        parts("bench " + bench.title(), bench.parts(), lines);
    }
}
