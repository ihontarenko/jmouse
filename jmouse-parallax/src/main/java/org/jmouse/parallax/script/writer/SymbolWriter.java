package org.jmouse.parallax.script.writer;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.SymbolElement;
import org.jmouse.parallax.script.Block.SymbolInput;
import org.jmouse.parallax.script.Block.SymbolProperty;

import java.util.List;

/**
 * How a part is drawn, in the form it was read in: a reference to symbol documents
 * ({@code symbol uses resistor-symbol { a terminal_one }}), a shape with pins by side ({@code symbol box { left d }}),
 * or a drawing of its own — inside {@code symbol { … }} in a component, or as the top level of a {@code symbol} document.
 */
final class SymbolWriter {

    private SymbolWriter() {
    }

    /** The {@code symbol} block of a component or a circuit. */
    static void symbol(Block.Symbol symbol, SourceLines lines) {
        if (!symbol.uses().isEmpty()) {
            reference(symbol, lines);
        } else if (symbol.shape().isPresent()) {
            lines.block("symbol " + symbol.shape().get(), inner -> symbol.sides().forEach((side, entries) ->
                    inner.line(side + " " + String.join(", ", entries))));
        } else {
            lines.block("symbol", inner -> drawing(symbol, inner));
        }
    }

    /** {@code symbol uses a-symbol, b-symbol}, with the ports it maps and the inputs it binds in braces when it has any. */
    private static void reference(Block.Symbol symbol, SourceLines lines) {
        String opening = "symbol uses " + Values.names(symbol.uses());
        if (symbol.ports().isEmpty() && symbol.bindings().isEmpty()) {
            lines.line(opening);
            return;
        }
        lines.block(opening, inner -> {
            symbol.ports().forEach((port, pin) -> inner.line(port + " " + pin));
            symbol.bindings().forEach((input, expression) -> inner.line(input + " = " + expression.text()));
        });
    }

    /** A drawing's lines: its size, paths, inputs, elements, pins, labels and indicators. */
    static void drawing(Block.Symbol symbol, SourceLines lines) {
        if (symbol.width() != 0 || symbol.height() != 0) {
            lines.line("size " + Values.number(symbol.width()) + " x " + Values.number(symbol.height()));
        }
        symbol.paths().forEach(path -> lines.line("path " + Values.text(path)));
        symbol.upright().forEach(path -> lines.line("path " + Values.text(path) + " upright"));
        symbol.inputs().forEach(input -> lines.line(input(input)));
        symbol.elements().forEach(element -> element(element, lines));
        symbol.pins().forEach(pin -> lines.line("pin " + pin.pin() + " at " + Values.point(pin.x(), pin.y())
                + Values.described(pin.label())));
        symbol.labels().forEach(label -> lines.line("label " + label.subject() + " at " + Values.point(label.x(), label.y())));
        symbol.indicators().forEach(indicator -> lines.line("indicator " + indicator.name() + " from "
                + Values.point(indicator.left(), indicator.top()) + " to " + Values.point(indicator.right(), indicator.bottom())));
    }

    /** {@code input closed}, {@code input current 0 .. 20 mA}, {@code input screen picture}. */
    private static String input(SymbolInput input) {
        String line = "input " + input.name();
        if (input.picture()) {
            return line + " picture";
        }
        if (input.range().isPresent()) {
            return line + " " + input.range().get().text();
        }
        if (input.low() != 0 || input.high() != 1) {
            return line + " " + Values.number(input.low()) + " .. " + Values.number(input.high());
        }
        return line;
    }

    /**
     * {@code [live] shape [name] [at x, y] [size w x h] [radius r] [from x, y] [to x, y] [pivot x, y] ["path data"]}, or
     * {@code live when <condition>}, with its properties, what it hides and what it holds in braces.
     */
    private static void element(SymbolElement element, SourceLines lines) {
        StringBuilder opening = new StringBuilder(element.live() ? "live " : "");
        if (element.shape().equals("when")) {
            opening.append("when ").append(element.condition().written().text());
        } else {
            opening.append(element.shape());
            if (!element.name().isEmpty()) {
                opening.append(' ').append(element.name());
            }
            element.geometry().forEach((word, values) -> opening.append(' ').append(word).append(' ')
                    .append(geometry(word, values)));
            if (!element.path().isEmpty()) {
                opening.append(' ').append(Values.text(element.path()));
            }
        }
        boolean braced = element.shape().equals("group") || element.shape().equals("when")
                || !element.properties().isEmpty() || !element.hidden().isEmpty() || !element.children().isEmpty();
        if (!braced) {
            lines.line(opening.toString());
            return;
        }
        lines.block(opening.toString(), inner -> {
            element.properties().forEach(property -> inner.line(property(property)));
            if (!element.hidden().isEmpty()) {
                inner.line("hide " + Values.names(element.hidden()));
            }
            element.children().forEach(child -> element(child, inner));
        });
    }

    /** {@code w x h} for a size, {@code r} for a radius, {@code x, y} for a point. */
    private static String geometry(String word, List<Double> values) {
        return switch (word) {
            case "size" -> Values.number(values.get(0)) + " x " + Values.number(values.get(1));
            case "radius" -> Values.number(values.getFirst());
            default -> Values.point(values.get(0), values.get(1));
        };
    }

    /** {@code [live | animate] name = value[, value]}. */
    private static String property(SymbolProperty property) {
        String motion = switch (property.motion()) {
            case LIVE -> "live ";
            case ANIMATED -> "animate ";
            case STILL -> "";
        };
        return motion + property.name() + " = " + property.written().text();
    }
}
