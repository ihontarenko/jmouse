package org.jmouse.json;

import org.jmouse.core.access.TypedValue;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonMappingTest {

    public record Point(double x, double y) {
    }

    public record Part(String designator, Point at, List<String> tags, Kind kind) {
    }

    public enum Kind {
        RESISTOR, CAPACITOR
    }

    public static final class Supplier {

        private String name;
        private int    stock;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getStock() {
            return stock;
        }

        public void setStock(int stock) {
            this.stock = stock;
        }
    }

    /** A resistance written as its engineering text, the way a designer writes it. */
    public record Resistance(double ohms) {
    }

    /** Two resistances, each read and written by the resistance's own serializer and deserializer. */
    public record Divider(Resistance top, Resistance bottom) {
    }

    @Test
    void aRecordAndABeanAreWrittenByTheirProperties() {
        Supplier supplier = new Supplier();
        supplier.setName("Mouser");
        supplier.setStock(12);

        assertEquals("{\"designator\":\"R1\",\"at\":{\"x\":1.5,\"y\":2},\"tags\":[\"smd\"],\"kind\":\"RESISTOR\"}",
                     Json.write(new Part("R1", new Point(1.5, 2), List.of("smd"), Kind.RESISTOR)));
        assertEquals(Map.of("name", "Mouser", "stock", 12), Json.read(Json.write(supplier)));
    }

    @Test
    void aValueOfTheJdkIsWrittenAsItsText() {
        assertEquals("[\"2026-11-17\",null,7]", Json.write(List.of(LocalDate.of(2026, 11, 17), Optional.empty(), Optional.of(7))));
    }

    @Test
    void aTextIsReadIntoARecordABeanAndTypedCollections() {
        Part part = Json.read("{\"designator\": \"C3\", \"at\": {\"x\": 1, \"y\": 2.5}, \"tags\": [\"tht\", \"x7r\"], \"kind\": \"CAPACITOR\"}", Part.class);

        assertEquals(new Part("C3", new Point(1, 2.5), List.of("tht", "x7r"), Kind.CAPACITOR), part);

        Supplier supplier = Json.read("{\"name\": \"Digikey\", \"stock\": \"40\"}", Supplier.class);
        assertEquals("Digikey", supplier.getName());
        assertEquals(40, supplier.getStock());

        assertEquals(List.of(new Point(0, 0), new Point(3, 4)),
                     Json.read("[{\"x\": 0, \"y\": 0}, {\"x\": 3, \"y\": 4}]", TypedValue.ofList(Point.class)));
    }

    @Test
    void aSerializerReplacesTheStandardOneWhereverItsTypeStands() {
        JsonMapper mapper = Json.mapper().toBuilder()
                .serializer(Resistance.class, (resistance, output) -> output.text(resistance.ohms() / 1000 + "k"))
                .build();

        assertEquals("{\"top\":\"4.7k\",\"bottom\":\"1.0k\"}", mapper.write(new Divider(new Resistance(4700), new Resistance(1000))));
        assertEquals("[\"2.2k\"]", mapper.write(List.of(new Resistance(2200))));
    }

    @Test
    void aSerializerForASupertypeWritesEveryTypeBelowIt() {
        JsonMapper mapper = Json.mapper().toBuilder()
                .serializer(Number.class, (number, output) -> output.text("#" + number))
                .build();

        assertEquals("[\"#1\",\"#2.5\"]", mapper.write(List.of(1, 2.5)));
    }

    @Test
    void aDeserializerReadsItsTypeWhereverTheBindingMeetsIt() {
        JsonMapper mapper = Json.mapper().toBuilder()
                .deserializer(Resistance.class, (json, deserialization) -> {
                    String written = (String) json;
                    return new Resistance(Double.parseDouble(written.substring(0, written.length() - 1)) * 1000);
                })
                .build();

        assertEquals(new Resistance(4700), mapper.read("\"4.7k\"", Resistance.class));
        assertEquals(new Divider(new Resistance(4700), new Resistance(1000)), mapper.read("{\"top\": \"4.7k\", \"bottom\": \"1k\"}", Divider.class));
        assertEquals(List.of(new Resistance(1000), new Resistance(2200)), mapper.read("[\"1k\", \"2.2k\"]", TypedValue.ofList(Resistance.class)));
    }

    @Test
    void membersHoldingNullAreLeftOutWhereTheInclusionSaysSo() {
        JsonMapper mapper = Json.mapper().toBuilder().inclusion(JsonInclusion.NON_NULL).build();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("a", 1);
        value.put("b", null);

        assertEquals("{\"a\":1}", mapper.write(value));
        assertEquals("{\"a\":1,\"b\":null}", Json.write(value));
    }

    @Test
    void jsonThatCannotBeReadIntoTheTypeIsRefusedWithTheType() {
        assertThrows(JsonMappingException.class, () -> Json.read("{\"x\": \"far\", \"y\": 1}", Point.class));
    }

    @Test
    void aSerializerWritesAnObjectOfItsOwnShape() {
        JsonMapper mapper = JsonMapper.builder()
                .serializer(Part.class, (part, output) -> output.object(members -> members
                        .member("ref", part.designator())
                        .member("at", position -> position.array(elements -> elements
                                .element(part.at().x())
                                .element(part.at().y())))))
                .build();

        assertEquals("{\"ref\":\"R1\",\"at\":[1.5,2]}", mapper.write(new Part("R1", new Point(1.5, 2), List.of(), Kind.RESISTOR)));
    }

    public sealed interface Shape permits Circle, Rectangle {
    }

    public record Circle(double radius) implements Shape {
    }

    public record Rectangle(double width, double height) implements Shape {
    }

    @Test
    void aDeserializerChoosesTheTypeByADiscriminator() {
        JsonMapper mapper = JsonMapper.builder()
                .deserializer(Shape.class, (json, deserialization) -> {
                    Map<?, ?> object = (Map<?, ?>) json;
                    return switch ((String) object.get("kind")) {
                        case "circle" -> deserialization.read(object, Circle.class);
                        case "rectangle" -> deserialization.read(object, Rectangle.class);
                        default -> throw new JsonMappingException("No shape of kind " + object.get("kind"));
                    };
                })
                .build();

        assertEquals(List.of(new Circle(2), new Rectangle(3, 4)),
                     mapper.read("[{\"kind\": \"circle\", \"radius\": 2}, {\"kind\": \"rectangle\", \"width\": 3, \"height\": 4}]",
                                 TypedValue.ofList(Shape.class)));
    }
}
