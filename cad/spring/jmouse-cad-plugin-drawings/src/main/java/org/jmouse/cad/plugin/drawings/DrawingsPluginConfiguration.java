package org.jmouse.cad.plugin.drawings;

import org.jmouse.cad.management.CadAccessRules;
import org.jmouse.cad.store.exchange.DocumentExchange;
import org.jmouse.plugin.PluginSetting;
import org.jmouse.plugin.PluginSettingValues;
import org.jmouse.plugin.PluginSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 🧩 The factory drawings plugin, on the classpath: where its fonts are looked for is the engine's settings'. */
@AutoConfiguration
public class DrawingsPluginConfiguration {

    private static final String FONT_FOLDERS = "font-folders";
    private static final String FONT_FAMILY = "font-family";

    @Bean
    public SheetSettings cadDrawingSheetSettings() {
        return DrawingsExport.shippedSettings();
    }

    @Bean
    public DrawingsExport cadDrawingsExport(SheetSettings sheets, PluginSettings settings) {
        PluginSettingValues values = settings.valuesOf(DrawingsPlugin.ID);
        return DrawingsExport.shipped(sheets, new SheetFonts(() -> values.folders(FONT_FOLDERS), () -> familiesOf(values.text(FONT_FAMILY), sheets)));
    }

    @Bean
    public DrawingsPlugin cadDrawingsPlugin(DrawingsExport drawings, SheetSettings sheets) {
        return new DrawingsPlugin(drawings, settingsOf(sheets));
    }

    @Bean
    public DrawingsController cadDrawingsController(DrawingsExport drawings, DocumentExchange exchange, SheetSettings sheets) {
        CadAccessRules.requireDecidedInside(DrawingsController.class);
        return new DrawingsController(drawings, exchange, sheets);
    }

    /** The family asked for, then those the sheets name, so a document is set in a real font wherever there is one. */
    private static List<String> familiesOf(String asked, SheetSettings sheets) {
        List<String> families = new ArrayList<>();
        if (asked != null && !asked.isBlank()) {
            families.add(asked.strip());
        }
        families.addAll(sheets.fontFamilies());
        return families;
    }

    private static List<PluginSetting> settingsOf(SheetSettings sheets) {
        List<Path> installed = sheets.fontFolders().stream().map(Path::of).filter(Files::isDirectory).toList();
        return List.of(
                PluginSetting.folders(FONT_FOLDERS, "Font folders", "Where fonts are installed on the server: a document is set in them, and carries them with it.", installed),
                PluginSetting.text(FONT_FAMILY, "Document font", "The family a document is set in; left empty, the first of "
                                                                 + String.join(", ", sheets.fontFamilies()) + " that is installed.", ""));
    }
}
