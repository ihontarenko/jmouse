package org.jmouse.cad.plugin.drawings;

import org.jmouse.cad.library.CadStandards;
import org.jmouse.settings.Parameter;
import org.jmouse.settings.Section;
import org.jmouse.cad.management.CadAccessRules;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.strokefont.StrokeFont;
import org.jmouse.cad.store.exchange.DocumentExchange;
import org.jmouse.settings.ParameterValues;
import org.jmouse.settings.Settings;
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
    /** Its section of fonts. */
    private static final String FONTS = "fonts";

    @Bean
    public DrawingsExport cadDrawingsExport(CadSettings sections, Settings settings, CadStandards standards) {
        ParameterValues values = settings.parameters(DrawingsPlugin.ID, FONTS);
        return new DrawingsExport(sections, StrokeFont.newStroke(), new SheetFonts(() -> values.folders(FONT_FOLDERS),
                () -> familiesOf(values.text(FONT_FAMILY), sections.read(DrawingsPlugin.SHEETS))), standards);
    }

    /** Its settings: where fonts are, and which — seeded from the sheets its module ships —, and the sheets themselves. */
    @Bean
    public DrawingsPlugin cadDrawingsPlugin(DrawingsExport drawings) {
        List<Section> settings = new ArrayList<>(settingsOf(CadSettings.shipped().read(DrawingsPlugin.SHEETS)));
        settings.add(CadSettings.declarationOf(DrawingsPlugin.SHEETS));
        return new DrawingsPlugin(drawings, settings);
    }

    @Bean
    public DrawingsController cadDrawingsController(DrawingsExport drawings, DocumentExchange exchange, CadSettings sections) {
        CadAccessRules.requireDecidedInside(DrawingsController.class);
        return new DrawingsController(drawings, exchange, sections);
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

    private static List<Section> settingsOf(SheetSettings sheets) {
        List<Path> installed = sheets.fontFolders().stream().map(Path::of).filter(Files::isDirectory).toList();
        return List.of(Section.fixed(FONTS, "Fonts", "The fonts a document is set in.", List.of(
                Parameter.folders(FONT_FOLDERS, "Font folders", "Where fonts are installed on the server: a document is set in them, and carries them with it.", installed),
                Parameter.text(FONT_FAMILY, "Document font", "The family a document is set in; left empty, the first of "
                                                                 + String.join(", ", sheets.fontFamilies()) + " that is installed.", ""))));
    }
}
