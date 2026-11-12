package org.jmouse.altium;

import org.jmouse.altium.AltiumPcbLibrary.Arc;
import org.jmouse.altium.AltiumPcbLibrary.Body;
import org.jmouse.altium.AltiumPcbLibrary.Expansion;
import org.jmouse.altium.AltiumPcbLibrary.Fill;
import org.jmouse.altium.AltiumPcbLibrary.Footprint;
import org.jmouse.altium.AltiumPcbLibrary.Text;
import org.jmouse.altium.AltiumPcbLibrary.HoleShape;
import org.jmouse.altium.AltiumPcbLibrary.Model;
import org.jmouse.altium.AltiumPcbLibrary.Pad;
import org.jmouse.altium.AltiumPcbLibrary.PadShape;
import org.jmouse.altium.AltiumPcbLibrary.Point;
import org.jmouse.altium.AltiumPcbLibrary.Region;
import org.jmouse.altium.AltiumPcbLibrary.Rotation;
import org.jmouse.altium.AltiumPcbLibrary.Track;
import org.jmouse.altium.AltiumPcbLibrary.Via;
import org.jmouse.compound.CompoundFile;
import org.jmouse.compound.CompoundFileException;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * An Altium PCB library read into its model ({@link AltiumPcbLibrary}). The file is a compound file: a storage for each
 * footprint — its {@code Parameters} and its {@code Data}, the primitives it is drawn with one after another, each behind
 * a byte of its kind —, and {@code Library}, which lists them, embeds the 3D models zipped as they were and names the
 * kind of each mechanical layer.
 *
 * <p>Every field is read at the offset Altium writes it at, as the libraries this reading was checked against write it
 * (MIT-licensed libraries of Maxime Clement, 2021). A primitive of a kind it does not know cannot be stepped over — the
 * kind says how many records it has —, so it is refused, with where.</p>
 */
public final class AltiumPcbLibraryReader {

    private static final String LIBRARY = "Library";
    private static final String DATA = "Data";
    private static final String PARAMETERS = "Parameters";
    private static final String MODELS = LIBRARY + "/Models/";
    private static final String LAYER_KINDS = LIBRARY + "/LayerKindMapping/" + DATA;

    /** A storage beside the footprints that is none. */
    private static final Set<String> NOT_FOOTPRINTS = Set.of(LIBRARY, "FileVersionInfo");


    /** The primitives a footprint is drawn with, by the byte Altium writes their kind with, and how many records each has. */
    private enum Primitive {
        ARC(1, 1), PAD(2, 6), VIA(3, 1), TRACK(4, 1), TEXT(5, 2), FILL(6, 1), REGION(11, 1), BODY(12, 1);

        private final int number;
        private final int records;

        Primitive(int number, int records) {
            this.number = number;
            this.records = records;
        }

        static Optional<Primitive> of(int number) {
            for (Primitive primitive : values()) {
                if (primitive.number == number) {
                    return Optional.of(primitive);
                }
            }
            return Optional.empty();
        }
    }

    /** Where Altium writes each field of a primitive's record, in bytes from its start. */
    private static final int LAYER = 0;
    private static final int FLAGS = 1;
    private static final int AT_X = 13;
    private static final int AT_Y = 17;

    /** A pad's main record. */
    private static final int PAD_WIDTH = 21;
    private static final int PAD_HEIGHT = 25;
    private static final int PAD_HOLE = 45;
    private static final int PAD_SHAPE = 49;
    private static final int PAD_ROTATION = 52;
    private static final int PAD_PLATED = 60;
    private static final int PAD_PASTE_MARGIN = 86;
    private static final int PAD_MASK_MARGIN = 90;
    private static final int PAD_PASTE_MODE = 101;
    private static final int PAD_MASK_MODE = 102;
    private static final int TENTED_TOP = 0x20;
    private static final int TENTED_BOTTOM = 0x40;

    /** A pad's size-and-shape record, where it has one: 29 inner sizes and shapes, then its hole, then a shape for each layer. */
    private static final int HOLE_SHAPE = 262;
    private static final int SLOT_LENGTH = 263;
    private static final int SLOT_ROTATION = 267;
    private static final int LAYER_SHAPE = 532;
    private static final int LAYER_CORNER = 564;
    private static final int ROUNDED_RECTANGLE = 9;

    /** A via, a track, an arc, a fill. */
    private static final int VIA_DIAMETER = 21;
    private static final int VIA_HOLE = 25;
    private static final int END_X = 21;
    private static final int END_Y = 25;
    private static final int TRACK_WIDTH = 29;
    private static final int ARC_RADIUS = 21;
    private static final int ARC_START = 25;
    private static final int ARC_END = 33;
    private static final int ARC_WIDTH = 41;
    private static final int FILL_ROTATION = 29;
    private static final int TEXT_HEIGHT = 21;
    private static final int TEXT_ROTATION = 27;
    private static final int TEXT_MIRRORED = 35;
    private static final int TEXT_STROKE = 36;

    /** A region and a body: their hole count and their properties. */
    private static final int REGION_HOLES = 14;
    private static final int PROPERTIES = 18;

    private AltiumPcbLibraryReader() {
    }

    /** @throws AltiumReadException when the bytes are no Altium PCB library, or one broken past reading */
    public static AltiumPcbLibrary read(String name, byte[] bytes) {
        if (!CompoundFile.isCompoundFile(bytes)) {
            throw new AltiumReadException(name + " is no Altium PCB library: it is not a compound file.");
        }
        CompoundFile file;
        try {
            file = CompoundFile.read(bytes);
        } catch (CompoundFileException broken) {
            throw new AltiumReadException(name + ": " + broken.getMessage(), broken);
        }
        if (file.stream(LIBRARY + "/" + DATA).isEmpty()) {
            throw new AltiumReadException(name + " is a compound file with no " + LIBRARY + "/" + DATA + " stream: not an Altium PCB library.");
        }
        List<Footprint> footprints = new ArrayList<>();
        for (String storage : footprintStorages(file)) {
            footprints.add(footprint(name + " › " + storage, file, storage));
        }
        return new AltiumPcbLibrary(footprints, models(name, file), mechanicalKinds(name, file));
    }

    /** The storages that hold a footprint: every one beside {@code Library} with its data and its parameters. */
    private static List<String> footprintStorages(CompoundFile file) {
        Set<String> storages = new LinkedHashSet<>();
        for (String stream : file.streams()) {
            int slash = stream.indexOf('/');
            if (slash > 0 && stream.substring(slash + 1).equals(DATA)) {
                String storage = stream.substring(0, slash);
                if (!NOT_FOOTPRINTS.contains(storage) && file.stream(storage + "/" + PARAMETERS).isPresent()) {
                    storages.add(storage);
                }
            }
        }
        return List.copyOf(storages);
    }

    private static Footprint footprint(String where, CompoundFile file, String storage) {
        Map<String, String> parameters = AltiumRecords.properties(new AltiumBinaryCursor(where, file.stream(storage + "/" + PARAMETERS)
                .orElseThrow()).record(PARAMETERS).remaining());
        AltiumBinaryCursor data = new AltiumBinaryCursor(where, file.stream(storage + "/" + DATA).orElseThrow());
        String written = data.namedRecord("its name");
        // A storage's name is cut at 31 characters; the footprint's whole name is its pattern.
        String name = parameters.getOrDefault("PATTERN", written.isBlank() ? storage : written);
        List<Pad> pads = new ArrayList<>();
        List<Via> vias = new ArrayList<>();
        List<Track> tracks = new ArrayList<>();
        List<Arc> arcs = new ArrayList<>();
        List<Fill> fills = new ArrayList<>();
        List<Region> regions = new ArrayList<>();
        List<Body> bodies = new ArrayList<>();
        List<Text> texts = new ArrayList<>();
        Map<String, Integer> leftOut = new TreeMap<>();
        while (data.hasMore()) {
            int number = data.unsignedByte();
            Primitive primitive = Primitive.of(number).orElseThrow(() -> new AltiumReadException(where
                    + ": a primitive of kind " + number + ", which this reading does not know and cannot step over."));
            switch (primitive) {
                case PAD -> pads.add(pad(data));
                case VIA -> vias.add(via(data.record("a via").fields()));
                case TRACK -> tracks.add(track(data.record("a track").fields()));
                case ARC -> arcs.add(arc(data.record("an arc").fields()));
                case FILL -> fills.add(fill(data.record("a fill").fields()));
                case REGION -> regions.add(region(data.record("a region")));
                case BODY -> bodies.add(body(data.record("a body").fields()));
                case TEXT -> texts.add(text(data.record("a text").fields(), data.record("a text's words")));
            }
        }
        return new Footprint(name, parameters.getOrDefault("DESCRIPTION", ""), AltiumMeasures.units(parameters.get("HEIGHT")), pads, vias, tracks,
                arcs, fills, regions, bodies, texts, leftOut);
    }

    /** A pad's six records: its name, three Altium leaves unexplained, its main record, and its size and shape. */
    private static Pad pad(AltiumBinaryCursor data) {
        String name = data.namedRecord("a pad's name");
        for (int unexplained = 0; unexplained < 3; unexplained++) {
            data.record("a pad");
        }
        AltiumBinaryCursor.Fields main = data.record("a pad's main record").fields();
        AltiumBinaryCursor.Fields shape = data.record("a pad's size and shape").fields();
        boolean shaped = shape.length() > LAYER_CORNER;
        PadShape written = PadShape.of(main.unsignedByte(PAD_SHAPE));
        PadShape padShape = shaped && shape.unsignedByte(LAYER_SHAPE) == ROUNDED_RECTANGLE ? PadShape.ROUNDED_RECTANGLE : written;
        int flags = main.unsignedByte(FLAGS);
        return new Pad(name, main.unsignedByte(LAYER), point(main, AT_X, AT_Y), main.integer(PAD_WIDTH), main.integer(PAD_HEIGHT),
                padShape, main.real(PAD_ROTATION), main.integer(PAD_HOLE),
                shaped ? HoleShape.of(shape.unsignedByte(HOLE_SHAPE)) : HoleShape.ROUND,
                shaped ? shape.integer(SLOT_LENGTH) : 0, shaped ? shape.real(SLOT_ROTATION) : 0,
                main.unsignedByte(PAD_PLATED) != 0, shaped ? shape.unsignedByte(LAYER_CORNER) : 0,
                Expansion.of(main.unsignedByte(PAD_PASTE_MODE)), main.integer(PAD_PASTE_MARGIN),
                Expansion.of(main.unsignedByte(PAD_MASK_MODE)), main.integer(PAD_MASK_MARGIN),
                (flags & TENTED_TOP) != 0, (flags & TENTED_BOTTOM) != 0);
    }

    private static Via via(AltiumBinaryCursor.Fields fields) {
        return new Via(point(fields, AT_X, AT_Y), fields.integer(VIA_DIAMETER), fields.integer(VIA_HOLE));
    }

    private static Track track(AltiumBinaryCursor.Fields fields) {
        return new Track(fields.unsignedByte(LAYER), point(fields, AT_X, AT_Y), point(fields, END_X, END_Y), fields.integer(TRACK_WIDTH));
    }

    /** A text: its fields — where, how high, turned, mirrored, how thick a stroke —, then its words, a name of their own. */
    private static Text text(AltiumBinaryCursor.Fields fields, AltiumBinaryCursor words) {
        return new Text(fields.unsignedByte(LAYER), point(fields, AT_X, AT_Y), fields.integer(TEXT_HEIGHT), fields.real(TEXT_ROTATION),
                fields.unsignedByte(TEXT_MIRRORED) != 0, fields.integer(TEXT_STROKE), words.hasMore() ? words.name() : "");
    }

    private static Arc arc(AltiumBinaryCursor.Fields fields) {
        return new Arc(fields.unsignedByte(LAYER), point(fields, AT_X, AT_Y), fields.integer(ARC_RADIUS), fields.real(ARC_START),
                fields.real(ARC_END), fields.integer(ARC_WIDTH));
    }

    private static Fill fill(AltiumBinaryCursor.Fields fields) {
        return new Fill(fields.unsignedByte(LAYER), point(fields, AT_X, AT_Y), point(fields, END_X, END_Y), fields.real(FILL_ROTATION));
    }

    /** A region: its fields, its properties, its outline's points as reals, then each hole's. */
    private static Region region(AltiumBinaryCursor record) {
        AltiumBinaryCursor.Fields fields = record.fields();
        Map<String, String> properties = fields.properties(PROPERTIES);
        int holeCount = fields.unsignedShort(REGION_HOLES);
        int at = PROPERTIES + 4 + fields.integer(PROPERTIES);
        List<Point> outline = new ArrayList<>();
        at = points(fields, at, outline);
        List<List<Point>> holes = new ArrayList<>();
        for (int hole = 0; hole < holeCount; hole++) {
            List<Point> points = new ArrayList<>();
            at = points(fields, at, points);
            holes.add(points);
        }
        return new Region(fields.unsignedByte(LAYER), Integer.parseInt(properties.getOrDefault("KIND", "0").strip()), outline, holes);
    }

    /** A count, then that many points as pairs of reals; where the next field starts. */
    private static int points(AltiumBinaryCursor.Fields fields, int at, List<Point> points) {
        int count = fields.integer(at);
        int next = at + 4;
        for (int point = 0; point < count; point++) {
            points.add(new Point(fields.real(next), fields.real(next + 8)));
            next += 16;
        }
        return next;
    }

    private static Body body(AltiumBinaryCursor.Fields fields) {
        return new Body(fields.unsignedByte(LAYER), fields.properties(PROPERTIES));
    }

    private static Point point(AltiumBinaryCursor.Fields fields, int x, int y) {
        return new Point(fields.integer(x), fields.integer(y));
    }

    /** The models the library embeds: their properties one after another, each one's file zipped in a stream of its number. */
    private static List<Model> models(String name, CompoundFile file) {
        Optional<byte[]> written = file.stream(MODELS + DATA);
        if (written.isEmpty()) {
            return List.of();
        }
        AltiumBinaryCursor cursor = new AltiumBinaryCursor(name + " › models", written.get());
        List<Model> models = new ArrayList<>();
        int number = 0;
        while (cursor.hasMore()) {
            Map<String, String> properties = cursor.properties("a model");
            Optional<byte[]> zipped = file.stream(MODELS + number);
            number++;
            if (zipped.isEmpty() || !"TRUE".equalsIgnoreCase(properties.getOrDefault("EMBED", "TRUE"))) {
                continue;
            }
            models.add(new Model(properties.getOrDefault("ID", ""), properties.getOrDefault("NAME", ""),
                    new Rotation(AltiumMeasures.degrees(properties.get("ROTX")), AltiumMeasures.degrees(properties.get("ROTY")), AltiumMeasures.degrees(properties.get("ROTZ"))),
                    AltiumMeasures.units(properties.get("DZ")), unzipped(name + " › model " + (number - 1), zipped.get())));
        }
        return models;
    }

    private static byte[] unzipped(String where, byte[] zipped) {
        Inflater inflater = new Inflater();
        inflater.setInput(zipped);
        ByteArrayOutputStream output = new ByteArrayOutputStream(zipped.length * 4);
        byte[] chunk = new byte[1 << 16];
        try {
            while (!inflater.finished()) {
                int inflated = inflater.inflate(chunk);
                if (inflated == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    throw new AltiumReadException(where + " ends before its file does.");
                }
                output.write(chunk, 0, inflated);
            }
        } catch (DataFormatException broken) {
            throw new AltiumReadException(where + " is not zipped as Altium zips a model: " + broken.getMessage(), broken);
        } finally {
            inflater.end();
        }
        return output.toByteArray();
    }

    /**
     * The kind of each mechanical layer: a version written in UTF-16, four bytes Altium leaves unexplained, a count, then
     * that many pairs of a layer's number and its kind's.
     */
    private static Map<Integer, Integer> mechanicalKinds(String name, CompoundFile file) {
        Optional<byte[]> written = file.stream(LAYER_KINDS);
        if (written.isEmpty()) {
            return Map.of();
        }
        AltiumBinaryCursor cursor = new AltiumBinaryCursor(name + " › layer kinds", written.get());
        cursor.wideText();
        cursor.integer();
        int count = cursor.integer();
        Map<Integer, Integer> kinds = new LinkedHashMap<>();
        for (int pair = 0; pair < count; pair++) {
            kinds.put(cursor.integer(), cursor.integer());
        }
        return kinds;
    }
}
