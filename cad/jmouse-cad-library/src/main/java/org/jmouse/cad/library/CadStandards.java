package org.jmouse.cad.library;

import org.jmouse.cad.packages.pcb.PcbRules;
import org.jmouse.cad.packages.footprint.LandPatternRules;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * The standards the library ships beside its documents, read once: the land pattern rules its footprints are generated
 * with, which an editor checks a footprint against, and the ways a pcb can be made, which a pcb names and is checked
 * against.
 */
public final class CadStandards {

    private static final String LAND_PATTERNS = "standards/footprint-standards/land-pattern-rules.sht";
    private static final String PCB_RULES   = "standards/pcb-standards/pcb-rules.sht";

    private CadStandards() {
    }

    public static LandPatternRules landPatternRules() {
        return Shipped.LAND_PATTERN_RULES;
    }

    public static PcbRules pcbRules() {
        return Shipped.PCB_RULES_READ;
    }

    /** Holds the tables, read the first time one is asked for. */
    private static final class Shipped {

        private static final LandPatternRules LAND_PATTERN_RULES = LandPatternRules.of(table(LAND_PATTERNS));
        private static final PcbRules       PCB_RULES_READ   = PcbRules.of(table(PCB_RULES));
    }

    private static Sheet table(String path) {
        try (InputStream stream = CadStandards.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("'" + path + "' is not beside " + CadStandards.class.getName());
            }
            return SheetReader.read(path, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
