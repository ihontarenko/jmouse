package org.jmouse.mesh;

import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetReader;
import org.jmouse.sheet.SheetValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The open formats a 3D body comes in ({@code mesh-formats.sht}), whoever drew it: what a file is told apart by, what it
 * is served as, and the unit and up axis its standard sets.
 */
public record MeshFormats(List<MeshFormat> formats) {

    private static final String RESOURCE = "/org/jmouse/mesh/mesh-formats.sht";

    /** Whether a format holds a body's exact surfaces, or triangles that approximate them. */
    public enum Geometry {
        EXACT, MESH
    }

    /** The axis a body stands up along. */
    public enum Axis {
        Y, Z
    }

    /**
     * @param id         how a format is named: {@code step}, {@code vrml}
     * @param title      how it is named to a reader
     * @param extensions a file's extensions in it, lower case, the usual first
     * @param mediaType  what it is served as
     * @param unit       the millimetres one unit of a file is, as its standard says
     * @param up         the axis a file stands up along, as its standard says
     */
    public record MeshFormat(String id, String title, List<String> extensions, String mediaType, Geometry geometry,
                             double unit, Axis up, String standard) {

        public MeshFormat {
            extensions = List.copyOf(extensions);
        }

        /**
         * Where a body in millimetres with Z up stands in a file of this format: in its unit, stood up along its axis — the
         * reverse of {@link Mesh#placed}, so a Z-up body written into a Y-up format has its Z become Y and its Y become −Z.
         */
        public Placement frame() {
            Placement scaled = Placement.scale(1 / unit, 1 / unit, 1 / unit);
            return up == Axis.Y ? scaled.then(new Placement(new double[] {1, 0, 0, 0, 0, 0, 1, 0, 0, -1, 0, 0})) : scaled;
        }
    }

    public MeshFormats {
        formats = List.copyOf(formats);
    }

    /** The formats this library ships. */
    public static MeshFormats standard() {
        try (InputStream stream = MeshFormats.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("'" + RESOURCE + "' is not beside " + MeshFormats.class.getName());
            }
            return of(SheetReader.read(RESOURCE, new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public static MeshFormats of(Sheet table) {
        return new MeshFormats(table.entries().stream().map(MeshFormats::format).toList());
    }

    /** The format of a file by its extension; empty for a file no format here is. */
    public Optional<MeshFormat> ofFile(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return Optional.empty();
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return formats.stream().filter(format -> format.extensions().contains(extension)).findFirst();
    }

    /** A format by its id. */
    public Optional<MeshFormat> byId(String id) {
        return formats.stream().filter(format -> format.id().equals(id)).findFirst();
    }

    /** Every format's id. */
    public List<String> ids() {
        return formats.stream().map(MeshFormat::id).toList();
    }

    /** Every extension a file in one of these formats may have, each once. */
    public List<String> extensions() {
        return formats.stream().flatMap(format -> format.extensions().stream()).distinct().toList();
    }

    private static MeshFormat format(SheetEntry entry) {
        List<SheetValue> extensions = entry.value("extensions")
                .map(value -> value instanceof SheetValue.Sequence sequence ? sequence.items() : List.of(value))
                .orElse(List.of());
        return new MeshFormat(entry.id(), entry.title().orElse(entry.id()), extensions.stream().map(MeshFormats::text).toList(),
                text(entry, "media-type"), Geometry.valueOf(upper(text(entry, "geometry"))),
                Double.parseDouble(text(entry, "unit")), Axis.valueOf(upper(text(entry, "up"))), text(entry, "standard"));
    }

    private static String upper(String word) {
        return word.toUpperCase(Locale.ROOT);
    }

    private static String text(SheetEntry entry, String field) {
        return entry.value(field).map(MeshFormats::text)
                .orElseThrow(() -> new IllegalStateException("The mesh format " + entry.id() + " writes no " + field));
    }

    private static String text(SheetValue value) {
        return switch (value) {
            case SheetValue.Text text -> text.text();
            case SheetValue.Word word -> word.word();
            default -> value.written();
        };
    }
}
