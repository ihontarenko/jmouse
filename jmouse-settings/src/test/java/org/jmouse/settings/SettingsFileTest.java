package org.jmouse.settings;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsFileTest {

    @Test
    void writesTheRowsAsKeptAndReadsThemBack() {
        Map<String, String> looks = new LinkedHashMap<>();
        looks.put("looks[drawing.theme]", "graphite");
        looks.put("looks[pcb.looks.outline.edge]", "#ffffff");
        List<SettingsFile.Block> blocks = List.of(
                new SettingsFile.Block(SettingsFile.Holding.READER, "cad-engine", "interface/looks", looks),
                new SettingsFile.Block(SettingsFile.Holding.INSTALLATION, "cad-engine", "library", Map.of("folders", "C:\\one\nD:\\two")));
        String file = SettingsFile.write("jmCAD settings · 2026-11-14", blocks);
        assertEquals("""
                # jmCAD settings · 2026-11-14
                [reader cad-engine interface/looks]
                looks[drawing.theme] = graphite
                looks[pcb.looks.outline.edge] = #ffffff
                [installation cad-engine library]
                folders = C:\\\\one\\nD:\\\\two
                """, file);
        assertEquals(blocks, SettingsFile.read(file));
    }

    @Test
    void readsRemarksEmptyLinesAndAValueWithItsOwnEquals() {
        List<SettingsFile.Block> blocks = SettingsFile.read("""
                # what this is

                [reader cad-engine interface/looks]
                # a remark between rows
                looks[editor.note] = a = b
                """);
        assertEquals(Map.of("looks[editor.note]", "a = b"), blocks.getFirst().rows());
    }

    @Test
    void readsARowOfAListSectionThatBeginsAndEndsWithABracket() {
        List<SettingsFile.Block> blocks = SettingsFile.read("""
                [installation cad-gerber files]
                [0].ending = -F_Cu.gbr
                [1] = [kept as written]
                """);
        assertEquals(1, blocks.size());
        assertEquals(Map.of("[0].ending", "-F_Cu.gbr", "[1]", "[kept as written]"), blocks.getFirst().rows());
    }

    @Test
    void refusesALineItCannotReadNamingIt() {
        SettingRefused headless = assertThrows(SettingRefused.class, () -> SettingsFile.read("dotPixels = 3\n"));
        assertTrue(headless.getMessage().startsWith("Line 1:"), headless.getMessage());
        SettingRefused whose = assertThrows(SettingRefused.class, () -> SettingsFile.read("[mine cad-engine x]\n"));
        assertTrue(whose.getMessage().contains("'mine'"), whose.getMessage());
        SettingRefused escape = assertThrows(SettingRefused.class, () -> SettingsFile.read("[reader a b]\nkey = one\\t\n"));
        assertTrue(escape.getMessage().startsWith("Line 2:"), escape.getMessage());
    }
}
