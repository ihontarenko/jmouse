package org.jmouse.cad.plugin.drawings;

import org.jmouse.pdf.PdfFont;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The fonts a document is set in, found among those installed on the server: the first family asked that it has. A document then
 * carries them, so it reads the same on any machine, and its words can be searched and copied. With none of them there, it is
 * drawn in the engine's stroke font.
 */
final class SheetFonts {

    /** How deep a font folder is looked through: some keep each family in its own. */
    private static final int DEPTH = 4;

    private static final List<String> ENDINGS = List.of(".ttf", ".otf");

    /** A family's two faces; a family with no bold of its own sets its headings in its regular. */
    record Faces(PdfFont regular, PdfFont bold, String family) {
    }

    private final Supplier<List<Path>>   folders;
    private final Supplier<List<String>> families;
    private List<Path>                   foldersRead;
    private List<String>                 familiesRead;
    private Optional<Faces>              found = Optional.empty();

    /**
     * @param folders  where fonts are installed, as the engine's settings say now
     * @param families the families to set documents in, the one wanted first
     */
    SheetFonts(Supplier<List<Path>> folders, Supplier<List<String>> families) {
        this.folders = folders;
        this.families = families;
    }

    /** The faces to set a document in; looked for once until the settings change. */
    synchronized Optional<Faces> faces() {
        List<Path> wantedFolders = folders.get();
        List<String> wantedFamilies = families.get();
        if (!wantedFolders.equals(foldersRead) || !wantedFamilies.equals(familiesRead)) {
            foldersRead = wantedFolders;
            familiesRead = wantedFamilies;
            found = lookFor(wantedFolders, wantedFamilies);
        }
        return found;
    }

    private static Optional<Faces> lookFor(List<Path> folders, List<String> families) {
        List<Path> files = folders.stream().flatMap(SheetFonts::fontFilesIn).toList();
        for (String family : families) {
            String word = family.toLowerCase(Locale.ROOT).replace(" ", "");
            PdfFont regular = null;
            PdfFont bold = null;
            // A family's files nearly always say its name, so only those are read; reading all takes seconds.
            List<Path> likely = files.stream().filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).replaceAll("[- _]", "").startsWith(word.substring(0, Math.min(word.length(), 4)))).toList();
            for (Path file : likely) {
                Optional<PdfFont> font = fontIn(file).filter(read -> read.family().equalsIgnoreCase(family));
                if (font.isPresent() && regular == null && font.get().regular()) {
                    regular = font.get();
                } else if (font.isPresent() && bold == null && font.get().bold() && !font.get().style().toLowerCase(Locale.ROOT).contains("italic")) {
                    bold = font.get();
                }
            }
            if (regular != null) {
                return Optional.of(new Faces(regular, bold == null ? regular : bold, family));
            }
        }
        return Optional.empty();
    }

    private static Stream<Path> fontFilesIn(Path folder) {
        if (!Files.isDirectory(folder)) {
            return Stream.empty();
        }
        try (Stream<Path> walked = Files.walk(folder, DEPTH)) {
            return walked.filter(file -> ENDINGS.stream().anyMatch(ending -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(ending))).toList().stream();
        } catch (IOException unreadable) {
            return Stream.empty();
        }
    }

    /** A file that is no font this can embed is simply not one to choose. */
    private static Optional<PdfFont> fontIn(Path file) {
        try {
            return Optional.of(PdfFont.read(Files.readAllBytes(file)));
        } catch (IOException | IllegalArgumentException notUsable) {
            return Optional.empty();
        }
    }
}
