package org.jmouse.libre.pcb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every LibrePCB project under a folder read whole: {@code -Dlibre.pcb.real.projects=<folder>}. Nothing of them is shipped
 * with the module, so it is off without the property.
 */
@EnabledIfSystemProperty(named = "libre.pcb.real.projects", matches = ".+")
class LibrePcbRealProjectsCheck {

    @Test
    void everyProjectReads() throws IOException {
        Path root = Path.of(System.getProperty("libre.pcb.real.projects"));
        List<Path> folders;
        try (Stream<Path> found = Files.walk(root)) {
            folders = found.filter(path -> LibrePcbFile.isProject(path.toString())).map(Path::getParent).toList();
        }
        assertFalse(folders.isEmpty(), "no project under " + root);
        for (Path folder : folders) {
            LibrePcbProject project = LibrePcbProject.read(filesOf(folder));
            LibrePcbBoard board = project.boards().getFirst();
            System.out.printf("%s: %d parts, %d nets, %d sheets, %d symbols, %d devices, %d on the board, %d segments, %d planes, inner %d%n",
                    project.name(), project.circuit().parts().size(), project.circuit().nets().size(), project.schematics().size(),
                    project.library().symbols().size(), project.library().devices().size(), board.devices().size(),
                    board.segments().size(), board.planes().size(), board.innerLayers());
            assertTrue(project.circuit().parts().values().stream().allMatch(part -> project.library().components().containsKey(part.libraryComponent())));
            assertTrue(board.devices().stream().allMatch(device -> project.library().devices().containsKey(device.device())));
        }
    }

    private static Map<String, String> filesOf(Path folder) throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        try (Stream<Path> found = Files.walk(folder)) {
            for (Path path : found.filter(file -> LibrePcbFile.isRead(file.toString()) && !LibrePcbFile.isProject(file.toString())).toList()) {
                files.put(folder.relativize(path).toString().replace('\\', '/'), Files.readString(path));
            }
        }
        return files;
    }
}
