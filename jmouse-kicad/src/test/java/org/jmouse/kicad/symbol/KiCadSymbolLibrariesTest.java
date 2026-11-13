package org.jmouse.kicad.symbol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KiCadSymbolLibrariesTest {

    /** A symbol a schematic keeps a copy of is KiCad's only when the installed library of its name holds a symbol of its name. */
    @Test
    void holdsWhatTheLibraryOfItsNameHolds(@TempDir Path folder) throws IOException {
        Files.writeString(folder.resolve("Transistor_FET.kicad_sym"), """
                (kicad_symbol_lib (version 20231120) (generator "kicad_symbol_editor")
                  (symbol "2N7002" (property "Reference" "Q") (property "Value" "2N7002"))
                  (symbol "BSS138" (property "Reference" "Q") (property "Value" "BSS138")))
                """);
        KiCadSymbolLibraries installed = KiCadSymbolLibraries.in(List.of(folder.resolve("absent"), folder));

        assertTrue(installed.holds("Transistor_FET:2N7002"));
        assertFalse(installed.holds("Transistor_FET:IRF540"), "the library holds no symbol of that name");
        assertFalse(installed.holds("AD5292BRUZ-100-RL7:2N7002"), "a design's own library is no installed one, whatever it holds");
        assertFalse(installed.holds("2N7002"), "a name naming no library is nobody's library");
        assertFalse(KiCadSymbolLibraries.NONE.holds("Transistor_FET:2N7002"));
    }
}
