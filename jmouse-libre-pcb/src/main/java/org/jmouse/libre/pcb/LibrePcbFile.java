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

    /** What an installed library's folder is called by: {@code LibrePCB_Base.lplib}. */
    public static final String LIBRARY_SUFFIX = ".lplib";

    /** Where a project keeps the library it carries, inside its folder. */
    public static final String PROJECT_LIBRARY = "library/";

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

    /** Whether a folder's name is an installed library's. */
    public static boolean isLibrary(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(LIBRARY_SUFFIX);
    }

    /** Where a package a project carries keeps a model of its: {@code library/pkg/<package>/<model>.step}. */
    public static String modelPath(String packageId, String modelId) {
        return PROJECT_LIBRARY + Element.PACKAGE.folder + packageId + "/" + modelId + MODEL_SUFFIX;
    }

    /** Whether a file is one a reading takes: the marker, or a file of text. */
    public static boolean isRead(String name) {
        return isProject(name) || name.toLowerCase(Locale.ROOT).endsWith(TEXT_SUFFIX);
    }

    /**
     * What a library holds, by the file each element of it is kept in, inside the library's folder: {@code sym/<uuid>/symbol.lp} —
     * an installed library's, and a project's under {@link #PROJECT_LIBRARY}.
     */
    public enum Element {

        SYMBOL("sym/", "symbol.lp"),
        COMPONENT("cmp/", "component.lp"),
        DEVICE("dev/", "device.lp"),
        PACKAGE("pkg/", "package.lp"),
        /** A category components are filed in, under its parent: what a component says it is. */
        COMPONENT_CATEGORY("cmpcat/", "component_category.lp"),
        /** A category packages are filed in, under its parent. */
        PACKAGE_CATEGORY("pkgcat/", "package_category.lp");

        private final String folder;
        private final String file;

        Element(String folder, String file) {
            this.folder = folder;
            this.file = file;
        }

        /** The folder its elements stand in, one each, inside the library's: {@code sym/}. */
        public String folder() {
            return folder;
        }

        /** The file each element is kept in, inside its own folder: {@code symbol.lp}. */
        public String file() {
            return file;
        }

        /** The element a path inside a library's folder keeps; empty for a path that keeps none. */
        public static Optional<Element> inLibrary(String path) {
            return Arrays.stream(values()).filter(element -> path.startsWith(element.folder) && path.endsWith("/" + element.file)).findFirst();
        }
    }
}
