package org.jmouse.cad.exchange;

import org.jmouse.plugin.Contributions;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Files a plugin keeps that no import reads — the engine's own documents, a part's 3D model —, named as a file chooser
 * filters them. What the installation accepts as an upload is these and every import's {@link DocumentImport#fileNames}:
 * a format whose plugin is off is not accepted, because nothing here knows it.
 *
 * @param title      what the files are, to a reader
 * @param fileNames  {@code *.jm}, {@code *.step}
 * @param mediaTypes what such a file is declared as, where it has a type of its own; none for one a browser sends as bytes
 */
public record AcceptedFiles(String title, List<String> fileNames, List<String> mediaTypes) {

    public static final Contributions<AcceptedFiles> POINT = new Contributions<>("cad.files.accepted", AcceptedFiles.class);

    private static final String ANY_NAME = "*.";

    public AcceptedFiles {
        fileNames = List.copyOf(fileNames);
        mediaTypes = List.copyOf(mediaTypes);
    }

    /** The extensions file names of the form {@code *.ext} name, lower case, without their dot; any other name names none. */
    public static Set<String> extensionsOf(List<String> fileNames) {
        return fileNames.stream().filter(name -> name.startsWith(ANY_NAME) && name.length() > ANY_NAME.length())
                .map(name -> name.substring(ANY_NAME.length()).toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    /** What every import reads and every plugin keeps: the extensions, then the media types. */
    public static Accepted of(List<DocumentImport> imports, List<AcceptedFiles> kept) {
        Set<String> extensions = Stream.concat(imports.stream().map(DocumentImport::fileNames), kept.stream().map(AcceptedFiles::fileNames))
                .flatMap(names -> extensionsOf(names).stream()).collect(Collectors.toSet());
        Set<String> mediaTypes = kept.stream().flatMap(files -> files.mediaTypes().stream()).collect(Collectors.toSet());
        return new Accepted(extensions, mediaTypes);
    }

    /** What the installation accepts besides its own settings. */
    public record Accepted(Set<String> extensions, Set<String> mediaTypes) {

        public Accepted {
            extensions = Set.copyOf(extensions);
            mediaTypes = Set.copyOf(mediaTypes);
        }
    }
}
