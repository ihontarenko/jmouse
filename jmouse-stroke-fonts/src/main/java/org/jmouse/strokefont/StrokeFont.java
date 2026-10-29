package org.jmouse.strokefont;

import org.jmouse.strokefont.node.FontoBeneDocument;
import org.jmouse.strokefont.node.FontoBeneLine;
import org.jmouse.strokefont.parser.FontoBeneParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A stroke font read from its FontoBene tree: every glyph by its codepoint, each a set of polylines in the font's units —
 * 9 is the cap height, Y up, the baseline at 0 —, its trailing space, and the spacing the font sets between letters and
 * lines. A reference to an earlier glyph is resolved here, so a glyph holds its own lines.
 */
public record StrokeFont(String id, String name, String license, double letterSpacing, double lineSpacing,
                         Map<Integer, Glyph> glyphs) {

    /** The units a cap is high in: what a height in a drawing is divided by. */
    public static final double CAP_HEIGHT = 9;

    /** A glyph: its polylines, the space after it, and how far left and right its strokes reach. */
    public record Glyph(List<List<FontoBeneLine.Vertex>> polylines, double trailing, double left, double right) {

        public Glyph {
            polylines = polylines.stream().map(List::copyOf).toList();
        }

        static Glyph of(List<List<FontoBeneLine.Vertex>> polylines, double trailing) {
            double left = polylines.stream().flatMap(List::stream).mapToDouble(FontoBeneLine.Vertex::x).min().orElse(0);
            double right = polylines.stream().flatMap(List::stream).mapToDouble(FontoBeneLine.Vertex::x).max().orElse(0);
            return new Glyph(polylines, trailing, left, right);
        }
    }

    public StrokeFont {
        glyphs = Collections.unmodifiableMap(new HashMap<>(glyphs));
    }

    /** NewStroke, the font every drawing is lettered in unless it names another: CC0, shipped in this library. */
    public static StrokeFont newStroke() {
        return Shipped.NEW_STROKE;
    }

    /** The font a FontoBene tree describes. */
    public static StrokeFont of(FontoBeneDocument document) {
        Map<String, String> header = new LinkedHashMap<>();
        Map<Integer, Glyph> glyphs = new HashMap<>();
        String section = "";
        Integer codepoint = null;
        List<List<FontoBeneLine.Vertex>> polylines = new ArrayList<>();
        double trailing = 0;
        for (FontoBeneLine line : document.lines()) {
            switch (line) {
                case FontoBeneLine.Section opened -> section = opened.name();
                // A key said twice — two authors — keeps its first value: nothing read here needs more.
                case FontoBeneLine.Entry entry -> header.putIfAbsent(section + "." + entry.key(), entry.value());
                case FontoBeneLine.Glyph declared -> {
                    close(glyphs, codepoint, polylines, trailing);
                    codepoint = declared.codepoint();
                    polylines = new ArrayList<>();
                    trailing = 0;
                }
                case FontoBeneLine.Blank ignored -> {
                    close(glyphs, codepoint, polylines, trailing);
                    codepoint = null;
                    polylines = new ArrayList<>();
                    trailing = 0;
                }
                case FontoBeneLine.Reference reference -> {
                    // Only backward references are allowed, so the glyph referred to is read already.
                    Glyph referred = glyphs.get(reference.codepoint());
                    if (referred == null) {
                        throw new FontoBeneParseException(reference.line(), "A reference to a glyph not defined before it");
                    }
                    polylines.addAll(referred.polylines());
                    trailing = referred.trailing();
                }
                case FontoBeneLine.Spacing spacing -> trailing = spacing.trailing();
                case FontoBeneLine.Polyline polyline -> polylines.add(polyline.vertices());
                default -> {
                }
            }
        }
        close(glyphs, codepoint, polylines, trailing);
        if (!"FontoBene".equals(header.get("format.format"))) {
            throw new FontoBeneParseException(1, "Not a FontoBene file");
        }
        return new StrokeFont(header.getOrDefault("font.id", ""), header.getOrDefault("font.name", ""),
                header.getOrDefault("font.license", ""), Double.parseDouble(header.getOrDefault("font.letter_spacing", "1.8")),
                Double.parseDouble(header.getOrDefault("font.line_spacing", "16")), glyphs);
    }

    private static void close(Map<Integer, Glyph> glyphs, Integer codepoint, List<List<FontoBeneLine.Vertex>> polylines,
                              double trailing) {
        if (codepoint != null) {
            glyphs.put(codepoint, Glyph.of(polylines, trailing));
        }
    }

    /** The fonts this library ships, read once, when first asked for. */
    private static final class Shipped {

        static final StrokeFont NEW_STROKE = read("newstroke.bene");

        private static StrokeFont read(String resource) {
            try (InputStream stream = StrokeFont.class.getResourceAsStream(resource)) {
                if (stream == null) {
                    throw new IllegalStateException("The stroke fonts library ships no " + resource);
                }
                return of(new FontoBeneParser().parse(resource, new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
            } catch (IOException unreadable) {
                throw new UncheckedIOException(unreadable);
            }
        }
    }
}
