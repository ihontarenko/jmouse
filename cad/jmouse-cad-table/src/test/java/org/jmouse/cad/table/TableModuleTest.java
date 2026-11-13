package org.jmouse.cad.table;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetField;
import org.jmouse.sheet.SheetReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TableModuleTest {

    private static final DocumentLanguage LANGUAGE = DocumentLanguage.of(new TableModule());

    /** Every kind of line a table holds: texts, words, pairs, numbers with units, a range, a citation, entries in entries. */
    private static final String BODY = """
                # Whose the drawings are, and their terms.
                holder "KiCad Libraries Team"
                license "CC-BY-SA-4.0 WITH KiCad-libraries-exception"
                provenance-fields source = Source, authors = Author, license = License
                symbol-tags electronics, schematic, kicad
                model-scales vrml = 2.54
                area-step 0.005 mm
                model-file "${KICAD8_3DMODEL_DIR}/%s.wrl"
                transient 20 ms
                supply-voltage 2 V .. 6 V source page 7 table 6.3
                mark-symbols ground = ground-symbol, common = common-symbol
                silkscreen "What is printed on the pcb, front and back" {
                    kicad "F.SilkS", "B.SilkS"
                    sides {
                        front top
                    }
                }
            """;

    private static final String AS_SHEET = "sheet \"KiCad libraries\" as kicad-libraries {\n" + BODY + "}\n";

    private static final String AS_TABLE = """
            table:kicad-libraries("KiCad libraries") {
                meta {
                    summary: "Who holds KiCad's own libraries, their licence, and what each KiCad layer is for."
                }
            """ + BODY + "}\n";

    @Test
    void readsAsTheSheetLanguageReadsTheSameLines() {
        Sheet expected = SheetReader.read("kicad-libraries.sht", AS_SHEET);
        DocumentNode table = LANGUAGE.readOne(AS_TABLE, "kicad-libraries.table.jm");
        Sheet read = TableModule.sheetOf(table);

        assertEquals("kicad-libraries", read.slug());
        assertEquals("KiCad libraries", read.title());
        assertEquals(written(expected.fields()), written(read.fields()));
        assertEquals(entries(expected.entries()), entries(read.entries()));
    }

    @Test
    void writesBackAsItWasRead() {
        DocumentNode table = LANGUAGE.readOne(AS_TABLE, "kicad-libraries.table.jm");
        DocumentNode again = LANGUAGE.readOne(LANGUAGE.write(table), "kicad-libraries.table.jm");
        assertEquals(written(TableModule.sheetOf(table).fields()), written(TableModule.sheetOf(again).fields()));
        assertEquals(entries(TableModule.sheetOf(table).entries()), entries(TableModule.sheetOf(again).entries()));
    }

    @Test
    void refusesWhatTheSheetLanguageRefuses() {
        String twice = "table:twice(\"Twice\") {\n    holder \"A\"\n    holder \"B\"\n}\n";
        assertThrows(RuntimeException.class, () -> TableModule.sheetOf(LANGUAGE.readOne(twice, "twice.table.jm")));
    }

    private static List<String> written(List<SheetField> fields) {
        return fields.stream().map(field -> field.name() + " " + field.value().written()
                + field.source().map(source -> " " + source.written()).orElse("")).toList();
    }

    private static List<String> entries(List<SheetEntry> entries) {
        return entries.stream().map(entry -> entry.id() + entry.title().map(title -> " \"" + title + "\"").orElse("") + " {"
                + String.join("; ", written(entry.fields())) + " | " + String.join("; ", entries(entry.entries())) + "}").toList();
    }
}
