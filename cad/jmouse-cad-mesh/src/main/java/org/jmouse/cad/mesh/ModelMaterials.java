package org.jmouse.cad.mesh;

import org.jmouse.cad.table.ReadTable;
import org.jmouse.cad.table.TableSource;
import org.jmouse.cad.table.ShippedTables;
import org.jmouse.mesh.MeshMaterial;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🎨 What a model drawn from shapes is made of ({@value #TABLE}): each material by its id, as a shape names it, and how
 * finely a cylinder's round is drawn. Data, never constants: a new material is a new row of the table.
 *
 * @param colours each material's colour as the table writes it — sRGB, three shares of 1 —, for a format that takes a
 *                colour alone (STEP)
 * @param steps   how many flat sides the round of a cylinder is drawn with
 */
public record ModelMaterials(Map<String, MeshMaterial> materials, Map<String, List<Double>> colours, int steps) {

    /** The table's address in the catalogue. */
    public static final String TABLE = "model-materials";

    /** Where its module ships it. */
    public static final ShippedTables SHIPPED = ShippedTables.in(ModelMaterials.class, "jmouse-cad-mesh", "org/jmouse/cad/mesh/tables");

    /** The table's row that says how a drawn model is drawn, not a material. */
    private static final String DRAWING = "drawing";

    public ModelMaterials {
        materials = Map.copyOf(materials);
        colours = Map.copyOf(colours);
    }

    /**
     * The materials as this library ships them. Their names are words of the model language — a shape's line names one of
     * them —, so the grammar reads them from here; a material's values are read through the engine's table at use.
     */
    public static ModelMaterials shipped() {
        return Shipped.MATERIALS;
    }

    /**
     * As its source answers at each use — the engine's catalogue table, its curator's revision where one is kept —, read again
     * only when another revision answers.
     */
    public static ReadTable<ModelMaterials> readFrom(TableSource tables) {
        return new ReadTable<>(tables, TABLE, ModelMaterials::of);
    }

    /** The shipped materials, read the first time they are asked for. */
    private static final class Shipped {

        private static final ModelMaterials MATERIALS = readFrom(SHIPPED).current();
    }

    /** Every material's id, as a shape's line may name it. */
    public List<String> names() {
        return materials.keySet().stream().sorted().toList();
    }

    public Optional<MeshMaterial> material(String name) {
        return Optional.ofNullable(materials.get(name));
    }

    /** A material's colour in sRGB, three shares of 1. */
    public Optional<List<Double>> colour(String name) {
        return Optional.ofNullable(colours.get(name));
    }

    public static ModelMaterials of(Sheet table) {
        Map<String, MeshMaterial> materials = new LinkedHashMap<>();
        Map<String, List<Double>> colours = new LinkedHashMap<>();
        int steps = 0;
        for (SheetEntry entry : table.entries()) {
            if (entry.id().equals(DRAWING)) {
                steps = (int) number(entry, "steps");
                continue;
            }
            int colour = Integer.parseInt(text(entry, "colour").substring(1), 16);
            colours.put(entry.id(), List.of((colour >> 16 & 0xff) / 255.0, (colour >> 8 & 0xff) / 255.0, (colour & 0xff) / 255.0));
            materials.put(entry.id(), new MeshMaterial(entry.id(), linear(colour >> 16 & 0xff), linear(colour >> 8 & 0xff),
                    linear(colour & 0xff), number(entry, "opacity"), number(entry, "metallic"), number(entry, "roughness")));
        }
        if (steps < 3) {
            throw new IllegalStateException(TABLE + " says no round of at least three sides in its 'drawing' row");
        }
        return new ModelMaterials(materials, colours, steps);
    }

    /** An sRGB channel as glTF's linear factor. */
    private static double linear(int channel) {
        return MeshMaterial.linear(channel / 255.0);
    }

    private static double number(SheetEntry entry, String field) {
        return Double.parseDouble(text(entry, field));
    }

    private static String text(SheetEntry entry, String field) {
        SheetValue value = entry.value(field)
                .orElseThrow(() -> new IllegalStateException("The material " + entry.id() + " writes no " + field));
        return switch (value) {
            case SheetValue.Text text -> text.text();
            case SheetValue.Word word -> word.word();
            default -> value.written();
        };
    }
}
