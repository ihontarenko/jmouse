package org.jmouse.altium;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An Altium PCB library as its streams say it, in Altium's units — a ten-thousandth of a mil — with Y upwards: its
 * footprints, the 3D models it embeds, and what kind each of its mechanical layers is.
 *
 * @param footprints     every footprint, in the order the library lists them
 * @param models         the models it embeds, by the order it keeps them
 * @param mechanicalKind the kind of each mechanical layer it names one for, by layer number: Altium's own numbers, as
 *                       {@code Library/LayerKindMapping} writes them
 */
public record AltiumPcbLibrary(List<Footprint> footprints, List<Model> models, Map<Integer, Integer> mechanicalKind) {

    /** How many of Altium's units make a millimetre: ten thousand to a mil, a mil a thousandth of an inch. */
    public static final double UNITS_PER_MILLIMETRE = 10_000 / 0.0254;

    public AltiumPcbLibrary {
        footprints = List.copyOf(footprints);
        models = List.copyOf(models);
        mechanicalKind = Map.copyOf(mechanicalKind);
    }

    /** A length in Altium's units as millimetres. */
    public static double millimetres(double units) {
        return units / UNITS_PER_MILLIMETRE;
    }

    /** The model a body names by its identifier, if the library embeds it. */
    public Optional<Model> model(String identifier) {
        return models.stream().filter(model -> model.identifier().equalsIgnoreCase(identifier)).findFirst();
    }

    /** A point, in Altium's units. */
    public record Point(double x, double y) {
    }

    /**
     * One footprint: its name and what it says of itself, and what it is drawn with.
     *
     * @param height  the height its parameters give, in Altium's units; zero where they give none
     * @param leftOut what the reading does not hold, by kind, a count each
     */
    public record Footprint(String name, String description, double height, List<Pad> pads, List<Via> vias, List<Track> tracks,
                            List<Arc> arcs, List<Fill> fills, List<Region> regions, List<Body> bodies, List<Text> texts,
                            Map<String, Integer> leftOut) {

        public Footprint {
            pads = List.copyOf(pads);
            vias = List.copyOf(vias);
            tracks = List.copyOf(tracks);
            arcs = List.copyOf(arcs);
            fills = List.copyOf(fills);
            regions = List.copyOf(regions);
            bodies = List.copyOf(bodies);
            texts = List.copyOf(texts);
            leftOut = Map.copyOf(leftOut);
        }
    }

    /** The shape Altium draws a pad's copper as. */
    public enum PadShape {
        ROUND, RECTANGLE, OCTAGON, ROUNDED_RECTANGLE;

        /** A pad's main shape, as its record writes it; a round shape longer than wide is drawn as an oval. */
        static PadShape of(int written) {
            return switch (written) {
                case 2 -> RECTANGLE;
                case 3 -> OCTAGON;
                default -> ROUND;
            };
        }
    }

    /** The shape of a pad's hole. */
    public enum HoleShape {
        ROUND, SQUARE, SLOT;

        static HoleShape of(int written) {
            return switch (written) {
                case 1 -> SQUARE;
                case 2 -> SLOT;
                default -> ROUND;
            };
        }
    }

    /**
     * How a pad's mask or paste opening is sized: by the board's rules, or by the pad's own figure. Altium writes no
     * override as 0 or 1 alike, and only 2 for a figure of its own.
     */
    public enum Expansion {
        RULE, MANUAL;

        static Expansion of(int written) {
            return written == 2 ? MANUAL : RULE;
        }
    }

    /**
     * A pad as its top layer draws it — what a footprint lands with.
     *
     * @param layer         its layer: a copper layer for a surface pad, the multi-layer for a pad through the board
     * @param rotation      its turn, degrees counter-clockwise
     * @param hole          its hole's diameter, zero for a surface pad; a slot's width
     * @param slotLength    a slot's length, zero for a round or square hole
     * @param cornerPercent a rounded rectangle's corner, as a share of half its shorter side, in percent
     * @param tentedTop     whether the top mask covers it
     * @param tentedBottom  whether the bottom mask covers it
     */
    public record Pad(String name, int layer, Point at, double width, double height, PadShape shape, double rotation,
                      double hole, HoleShape holeShape, double slotLength, double slotRotation, boolean plated, int cornerPercent,
                      Expansion pasteExpansion, double pasteMargin, Expansion maskExpansion, double maskMargin,
                      boolean tentedTop, boolean tentedBottom) {
    }

    /** A via a footprint carries: a plated hole and its ring, joining no pin. */
    /**
     * A text a footprint writes: on a layer, its baseline's start, its letters' height and stroke, turned and maybe mirrored.
     * What it writes may be a special string standing for what the part placed says — {@code .Designator}, {@code .Comment}.
     */
    public record Text(int layer, Point at, double height, double rotation, boolean mirrored, double stroke, String text) {

        /** The special string standing for the placed part's designator. */
        public static final String DESIGNATOR = ".Designator";
        /** The special string standing for the placed part's comment — its value. */
        public static final String COMMENT = ".Comment";
    }

    public record Via(Point at, double diameter, double hole) {
    }

    /** A straight line on a layer. */
    public record Track(int layer, Point start, Point end, double width) {
    }

    /** An arc on a layer: its centre, radius and the angles it runs between, degrees counter-clockwise from the X axis. */
    public record Arc(int layer, Point centre, double radius, double startAngle, double endAngle, double width) {
    }

    /** A filled rectangle between two corners, turned about its centre. */
    public record Fill(int layer, Point first, Point second, double rotation) {
    }

    /**
     * A filled area: its outline and the holes in it.
     *
     * @param kind what the area is: copper ({@code 0}), a cutout, a board cutout… — as its {@code KIND} property writes it
     */
    public record Region(int layer, int kind, List<Point> outline, List<List<Point>> holes) {

        public Region {
            outline = List.copyOf(outline);
            holes = holes.stream().map(List::copyOf).toList();
        }
    }

    /**
     * A 3D body: the model it shows, where, and how high it stands.
     *
     * @param properties what its record writes of it: {@code MODELID}, {@code MODEL.2D.X}, {@code MODEL.3D.ROTX},
     *                   {@code OVERALLHEIGHT}…, each as written
     */
    public record Body(int layer, Map<String, String> properties) {

        public Body {
            properties = Map.copyOf(properties);
        }

        /** The identifier of the embedded model it shows, if it shows one. */
        public Optional<String> modelIdentifier() {
            return Optional.ofNullable(properties.get("MODELID")).filter(identifier -> !identifier.isBlank());
        }
    }

    /**
     * A model the library embeds: its identifier, the file it was — empty where the library kept no name —, its own turn, and
     * the file's bytes as they were.
     *
     * @param rotation its turn about X, Y and Z, degrees, before any body places it
     * @param lift     how far it is raised, in Altium's units
     */
    public record Model(String identifier, String fileName, Rotation rotation, double lift, byte[] content) {
    }

    /** A turn about X, Y and Z, degrees. */
    public record Rotation(double x, double y, double z) {
    }
}
