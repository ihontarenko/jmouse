package org.jmouse.cad.library;

import org.jmouse.cad.packages.footprint.LandPatternRules;
import org.jmouse.cad.packages.pcb.PcbRules;
import org.jmouse.cad.table.ReadTable;
import org.jmouse.cad.table.ShippedTables;
import org.jmouse.cad.table.TableSource;

/**
 * The standards the library ships as tables of the catalogue: the land pattern rules its footprints are generated with, which an
 * editor checks a footprint against, and the ways a pcb can be made, which a pcb names and is checked against. Read through a
 * {@link TableSource} each time they are asked for — the engine's answers with the catalogue's table, its curator's revision
 * where one is kept, so a table saved applies at once —; what a table reads to is kept while the table is the same.
 */
public final class CadStandards {

    /** Where the library ships its tables, as resources. */
    public static final String TABLES = "org/jmouse/cad/library/tables";

    public static final String LAND_PATTERN_RULES = "land-pattern-rules";
    public static final String PCB_RULES          = "pcb-rules";

    private static final CadStandards SHIPPED = new CadStandards(ShippedTables.in(CadStandards.class, ShippedTables.ENGINE_LIBRARY, TABLES));

    private final ReadTable<LandPatternRules> landPatterns;
    private final ReadTable<PcbRules>         pcbRules;

    public CadStandards(TableSource tables) {
        this.landPatterns = new ReadTable<>(tables, LAND_PATTERN_RULES, LandPatternRules::of);
        this.pcbRules = new ReadTable<>(tables, PCB_RULES, PcbRules::of);
    }

    /** The standards as the library ships them: for a program run without the engine, and for the shipped library itself. */
    public static CadStandards shipped() {
        return SHIPPED;
    }

    public LandPatternRules landPatternRules() {
        return landPatterns.current();
    }

    public PcbRules pcbRules() {
        return pcbRules.current();
    }
}
