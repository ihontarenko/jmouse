package org.jmouse.libre.pcb;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** The files of a LibrePCB project by what they are, as they stand inside its folder. */
public enum LibrePcbFile {

    METADATA("project/metadata.lp"),
    CIRCUIT("circuit/circuit.lp"),
    SCHEMATICS("schematics/schematics.lp"),
    BOARDS("boards/boards.lp");

    /** What a project's marker is called: the one file named after the project, what a reading of it starts from. */
    public static final String PROJECT_SUFFIX = ".lpp";

    /** What a project packed into one file ends with: a zip of its folder. */
    public static final String ARCHIVE_SUFFIX = ".lppz";

    /** What a package's 3D model ends with. */
    public static final String MODEL_SUFFIX = ".step";

    /** What every file of a project that is read ends with. */
    public static final String TEXT_SUFFIX = ".lp";

    private final String path;

    LibrePcbFile(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    /** Whether a file's name is a project's marker. */
    public static boolean isProject(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(PROJECT_SUFFIX);
    }

    public static boolean isArchive(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(ARCHIVE_SUFFIX);
    }

    public static boolean isModel(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(MODEL_SUFFIX);
    }

    /** Where a package keeps a model of its: {@code library/pkg/<package>/<model>.step}. */
    public static String modelPath(String packageId, String modelId) {
        return Element.PACKAGE.folder + packageId + "/" + modelId + MODEL_SUFFIX;
    }

    /** Whether a file is one a reading takes: the marker, or a file of text. */
    public static boolean isRead(String name) {
        return isProject(name) || name.toLowerCase(Locale.ROOT).endsWith(TEXT_SUFFIX);
    }

    /** What a library holds, by the file each element of it is kept in: {@code library/sym/<uuid>/symbol.lp}. */
    public enum Element {

        SYMBOL("library/sym/", "symbol.lp"),
        COMPONENT("library/cmp/", "component.lp"),
        DEVICE("library/dev/", "device.lp"),
        PACKAGE("library/pkg/", "package.lp");

        private final String folder;
        private final String file;

        Element(String folder, String file) {
            this.folder = folder;
            this.file = file;
        }

        /** The element a path of the project keeps; empty for a path that keeps none. */
        static Optional<Element> of(String path) {
            return Arrays.stream(values()).filter(element -> path.startsWith(element.folder) && path.endsWith("/" + element.file)).findFirst();
        }
    }
}
