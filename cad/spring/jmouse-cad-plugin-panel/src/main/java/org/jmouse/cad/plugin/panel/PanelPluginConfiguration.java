package org.jmouse.cad.plugin.panel;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.management.CadAccessRules;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.panel.PanelDefaults;
import org.jmouse.cad.panel.PanelFlattening;
import org.jmouse.plugin.PluginSetting;
import org.jmouse.plugin.PluginSettingValues;
import org.jmouse.plugin.PluginSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.util.List;

/** 🧩 The panels plugin, on the classpath: its defaults, shipped as data and set per installation in the engine's settings. */
@AutoConfiguration
public class PanelPluginConfiguration {

    private static final String MILLIMETRES = "mm";
    private static final BigDecimal SMALLEST = new BigDecimal("0.1");
    private static final BigDecimal LARGEST = new BigDecimal("100");

    private static final String RAIL = "rail";
    private static final String BOARD_GAP = "board-gap";
    private static final String FIDUCIAL_DIAMETER = "fiducial-diameter";
    private static final String FIDUCIAL_OPENING = "fiducial-opening";
    private static final String TOOLING_DRILL = "tooling-drill";
    private static final String TAB_WIDTH = "tab-width";
    private static final String TAB_SPACING = "tab-spacing";
    private static final String BITE_DRILL = "bite-drill";
    private static final String BITE_PITCH = "bite-pitch";

    @Bean
    public PanelPlugin cadPanelPlugin(PluginSettings settings) {
        PanelDefaults shipped = CadSettings.read(PanelPlugin.DEFAULTS);
        PluginSettingValues values = settings.valuesOf(PanelPlugin.ID);
        return new PanelPlugin(new PanelFlattening(() -> new PanelDefaults(number(values, RAIL), number(values, BOARD_GAP),
                number(values, FIDUCIAL_DIAMETER), number(values, FIDUCIAL_OPENING), number(values, TOOLING_DRILL),
                number(values, TAB_WIDTH), number(values, TAB_SPACING), number(values, BITE_DRILL), number(values, BITE_PITCH))),
                settingsOf(shipped));
    }

    @Bean
    public PanelController cadPanelController(CadLanguage language) {
        CadAccessRules.requireDecidedInside(PanelController.class);
        return new PanelController(language);
    }

    private static double number(PluginSettingValues values, String key) {
        return values.number(key).doubleValue();
    }

    private static List<PluginSetting> settingsOf(PanelDefaults shipped) {
        return List.of(
                length(RAIL, "Rail width", "How wide a frame's rail is, where a panel does not say.", shipped.rail()),
                length(BOARD_GAP, "Gap between boards", "Between boards that are routed apart, and between them and the rails.", shipped.boardGap()),
                length(FIDUCIAL_DIAMETER, "Fiducial", "The copper of a fiducial on the frame.", shipped.fiducialDiameter()),
                length(FIDUCIAL_OPENING, "Fiducial's opening", "The opening in the mask round a fiducial.", shipped.fiducialOpening()),
                length(TOOLING_DRILL, "Tooling hole", "A tooling hole on the frame.", shipped.toolingDrill()),
                length(TAB_WIDTH, "Tab width", "How wide a mouse-bite tab is.", shipped.tabWidth()),
                length(TAB_SPACING, "Length per tab", "How much of a board's side one tab holds.", shipped.tabSpacing()),
                length(BITE_DRILL, "Mouse-bite hole", "A hole in a tab's row.", shipped.biteDrill()),
                length(BITE_PITCH, "Mouse-bite pitch", "Between the holes of a tab's row.", shipped.bitePitch()));
    }

    private static PluginSetting length(String key, String title, String description, double value) {
        return PluginSetting.number(key, title, description, BigDecimal.valueOf(value), SMALLEST, LARGEST, MILLIMETRES);
    }
}
