package org.jmouse.kicad.schematic;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadPlacement;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.KiCadProperty;
import org.jmouse.sexpression.node.SExpressionList;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A symbol placed on a sheet: {@code (symbol (lib_id "Device:R") (at 62.23 69.85 0) (mirror y) (unit 1) …)}.
 *
 * @param libraryId   the symbol it is: {@code Device:R} — the key of the schematic's {@code lib_symbols} copy
 * @param placement   where it stands and how it is turned
 * @param unit        which unit of a symbol of several it is
 * @param uuid        its identity on the sheet
 * @param properties  Reference, Value, Footprint … as written on the sheet
 * @param written     where the sheet writes each property it shows, by its name; a hidden one is not there
 * @param references  its reference in each instance of the sheet, by the instance's path, where a hierarchy names them
 * @param simulated   whether it takes part in a simulation ({@code exclude_from_sim no})
 * @param fitted      whether it is fitted to the board ({@code dnp no})
 */
public record KiCadSymbolInstance(String libraryId, KiCadPlacement placement, int unit, String uuid,
                                  Map<String, String> properties, Map<String, KiCadPoint> written, Map<String, String> references, boolean simulated,
                                  boolean fitted) {

    public KiCadSymbolInstance {
        properties = Map.copyOf(properties);
        written = Map.copyOf(written);
        references = Map.copyOf(references);
    }

    static KiCadSymbolInstance of(SExpressionList symbol) {
        SExpressionList at = symbol.requireList(AT);
        KiCadPlacement placement = new KiCadPlacement(KiCadPoint.of(at), at.size() > 3 ? (int) at.number(3) : 0,
                KiCadPlacement.Mirror.of(symbol.wordOf(MIRROR).orElse("")));
        Map<String, String> properties = new LinkedHashMap<>();
        Map<String, KiCadPoint> written = new LinkedHashMap<>();
        symbol.lists(PROPERTY).forEach(property -> {
            properties.put(property.word(1), property.word(2));
            boolean hidden = property.list("effects").map(effects -> effects.says("hide") || effects.list("hide").isPresent()).orElse(false);
            if (!hidden) {
                property.list(AT).ifPresent(place -> written.put(property.word(1), KiCadPoint.of(place)));
            }
        });
        Map<String, String> references = new LinkedHashMap<>();
        symbol.list(INSTANCES).ifPresent(instances -> instances.lists(PROJECT).forEach(project ->
                project.lists(PATH).forEach(path -> path.wordOf(REFERENCE)
                        .ifPresent(reference -> references.put(path.word(1), reference)))));
        return new KiCadSymbolInstance(symbol.requireList(LIBRARY_ID).word(1), placement,
                symbol.list(UNIT).map(unit -> (int) unit.number(1)).orElse(1), symbol.wordOf(UUID).orElse(""),
                properties, written, references, !symbol.says(EXCLUDE_FROM_SIMULATION), !symbol.says(DO_NOT_POPULATE));
    }

    public String property(String name) {
        return properties.getOrDefault(name, "");
    }

    public String value() {
        return KiCadProperty.VALUE.in(properties);
    }

    /** Its reference in the sheet instance at that path — {@code /uuid/uuid} —, else as written on the sheet. */
    public String reference(String sheetPath) {
        return references.getOrDefault(sheetPath, KiCadProperty.REFERENCE.in(properties));
    }
}
