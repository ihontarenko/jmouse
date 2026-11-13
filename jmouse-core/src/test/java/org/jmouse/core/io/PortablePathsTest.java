package org.jmouse.core.io;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortablePathsTest {

    @Test
    void writesAFileBelowAFolderWithTheOneSeparator() {
        Path folder = Path.of("library");
        Path file = folder.resolve("components").resolve("passive").resolve("resistor.component.jm");
        assertEquals("components/passive/resistor.component.jm", PortablePaths.below(folder, file));
    }

    @Test
    void writesAFileDirectlyInTheFolderByItsName() {
        Path folder = Path.of("library");
        assertEquals("index.properties", PortablePaths.below(folder, folder.resolve("index.properties")));
    }

    @Test
    void refusesAFileOutsideTheFolder() {
        assertThrows(IllegalArgumentException.class, () -> PortablePaths.below(Path.of("library"), Path.of("elsewhere", "file.jm")));
    }

    @Test
    void refusesAnAbsolutePath() {
        assertThrows(IllegalArgumentException.class, () -> PortablePaths.written(Path.of("library").toAbsolutePath()));
    }
}
