package org.jmouse.datamap;

import org.jmouse.el.language.line.LineSyntaxException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataMapReaderTest {

    private static final String SHIFT_REGISTER = """
            # Texas Instruments SN74HC595, revision SCLS041I
            map "74HC595 — 8-bit shift register" as 74hc595 {
                manufacturer "Texas Instruments"
                revision "SCLS041I"
                package so-16

                supply-voltage "Supply voltage" source page 7 table 6.3 {
                    range 2 V .. 6 V
                    typical 5V
                }

                timing {
                    propagation-delay 19 ns source page 9 table "6.7 Switching Characteristics"
                    temperature -40 °C .. 85 °C   # the industrial range
                    rise 2.2µs

                    pins "Pin map" {
                        map 14 = data, 11 = shift-clock, 12 = latch-clock
                    }
                }

                supply-current-reference {
                    see 74hc595.jmd#supply-voltage/range
                    family logic/hc.jmd#levels
                }
            }
            """;

    @Test
    void readsAMap() {
        DataMap dataMap = DataMapReader.read("74hc595.jmd", SHIFT_REGISTER);
        assertEquals("74hc595", dataMap.slug());
        assertEquals("74HC595 — 8-bit shift register", dataMap.title());
        assertEquals(new DataMapValue.Text("Texas Instruments"), dataMap.field("manufacturer").orElseThrow().value());
        assertEquals(new DataMapValue.Word("so-16"), dataMap.field("package").orElseThrow().value());

        DataMapEntry supply = dataMap.entry("supply-voltage").orElseThrow();
        assertEquals("Supply voltage", supply.title().orElseThrow());
        assertEquals(7, supply.source().orElseThrow().page().orElseThrow());
        assertEquals("6.3", supply.source().orElseThrow().part("table").orElseThrow());
        DataMapValue.Range range = assertInstanceOf(DataMapValue.Range.class, supply.value("range").orElseThrow());
        assertEquals(2.0, ((DataMapValue.Measure) range.from()).quantity().value());
        assertEquals("V", ((DataMapValue.Measure) range.to()).quantity().unit());
        assertEquals(5.0, ((DataMapValue.Measure) supply.value("typical").orElseThrow()).quantity().value());
    }

    @Test
    void readsUnitsAsADatasheetWritesThem() {
        DataMapEntry timing = DataMapReader.read("74hc595.jmd", SHIFT_REGISTER).entry("timing").orElseThrow();
        DataMapField delay = timing.field("propagation-delay").orElseThrow();
        assertEquals(19e-9, ((DataMapValue.Measure) delay.value()).quantity().value(), 1e-21);
        assertEquals("6.7 Switching Characteristics", delay.source().orElseThrow().part("table").orElseThrow());
        DataMapValue.Range temperature = (DataMapValue.Range) timing.value("temperature").orElseThrow();
        assertEquals(-40.0, ((DataMapValue.Measure) temperature.from()).quantity().value());
        assertEquals("degC", ((DataMapValue.Measure) temperature.from()).quantity().unit());
        assertEquals(2.2e-6, ((DataMapValue.Measure) timing.value("rise").orElseThrow()).quantity().value(), 1e-18);
    }

    @Test
    void readsNestedEntriesAndPinMaps() {
        DataMap dataMap = DataMapReader.read("74hc595.jmd", SHIFT_REGISTER);
        DataMapEntry pins = dataMap.entry("pins").orElseThrow();
        DataMapValue.Sequence map = assertInstanceOf(DataMapValue.Sequence.class, pins.value("map").orElseThrow());
        assertEquals(new DataMapValue.Assignment("14", new DataMapValue.Word("data")), map.items().getFirst());
        assertEquals(new DataMapValue.Assignment("11", new DataMapValue.Word("shift-clock")), map.items().get(1));
        assertEquals(List.of("supply-voltage", "timing", "pins", "supply-current-reference"),
                dataMap.allEntries().stream().map(DataMapEntry::id).toList());
    }

    @Test
    void readsLinks() {
        DataMapEntry reference = DataMapReader.read("74hc595.jmd", SHIFT_REGISTER)
                .entry("supply-current-reference").orElseThrow();
        assertEquals(new DataMapValue.Link("74hc595.jmd", "supply-voltage", "range"), reference.value("see").orElseThrow());
        assertEquals(new DataMapValue.Link("logic/hc.jmd", "levels", ""), reference.value("family").orElseThrow());
    }

    @Test
    void writesWhatItReads() {
        DataMap dataMap = DataMapReader.read("74hc595.jmd", SHIFT_REGISTER);
        String written = DataMapWriter.write(dataMap);
        assertEquals(written, DataMapWriter.write(DataMapReader.read("written.jmd", written)));
        assertTrue(written.contains("    supply-voltage \"Supply voltage\" source page 7 table 6.3 {\n"), written);
        assertTrue(written.contains("propagation-delay 19 ns source page 9 table \"6.7 Switching Characteristics\""), written);
    }

    @Test
    void resolvesLinksAcrossALibrary() {
        String levels = """
                map "HC logic levels" as hc {
                    levels {
                        high-input 3.15 V source page 4
                    }
                }
                """;
        String shiftRegister = SHIFT_REGISTER.replace("logic/hc.jmd#levels", "../logic/hc.jmd#levels/high-input");
        DataMapLibrary library = DataMapLibrary.read(Map.of("parts/74hc595.jmd", shiftRegister, "logic/hc.jmd", levels));
        DataMapEntry reference = library.document("parts/74hc595.jmd").orElseThrow()
                .entry("supply-current-reference").orElseThrow();
        DataMapValue.Link family = (DataMapValue.Link) reference.value("family").orElseThrow();
        DataMapField highInput = library.field("parts/74hc595.jmd", family).orElseThrow();
        assertEquals(3.15, ((DataMapValue.Measure) highInput.value()).quantity().value());
        assertTrue(library.brokenLinks().isEmpty(), library.brokenLinks().toString());

        DataMapLibrary alone = DataMapLibrary.read(Map.of("parts/74hc595.jmd", shiftRegister));
        assertEquals(List.of("parts/74hc595.jmd: ../logic/hc.jmd#levels/high-input"), alone.brokenLinks());
    }

    @Test
    void refusesSayingWhere() {
        LineSyntaxException twice = assertThrows(LineSyntaxException.class, () -> DataMapReader.read("broken.jmd", """
                map "Broken" as broken {
                    one {
                    }
                    one {
                    }
                }
                """));
        assertEquals(4, twice.line());

        LineSyntaxException unclosed = assertThrows(LineSyntaxException.class,
                () -> DataMapReader.read("broken.jmd", "map \"Broken\" as broken {\n    value 5 V\n"));
        assertTrue(unclosed.detail().contains("'}'"), unclosed.detail());

        LineSyntaxException noValue = assertThrows(LineSyntaxException.class,
                () -> DataMapReader.read("broken.jmd", "map \"Broken\" as broken {\n    value 5 V sticks\n}\n"));
        assertEquals(2, noValue.line());

        LineSyntaxException joinedRange = assertThrows(LineSyntaxException.class,
                () -> DataMapReader.read("broken.jmd", "map \"Broken\" as broken {\n    value 2..6\n}\n"));
        assertTrue(joinedRange.detail().contains("2 V .. 6 V"), joinedRange.detail());
    }
}
