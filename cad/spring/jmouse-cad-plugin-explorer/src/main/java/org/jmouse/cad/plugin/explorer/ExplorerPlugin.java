package org.jmouse.cad.plugin.explorer;

import org.jmouse.settings.Parameter;
import org.jmouse.settings.Section;
import org.jmouse.cad.store.catalogue.PlaceSource;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.List;

/**
 * The explorer as a plugin: it opens {@link ExplorerMount#POINT}, where the server's own folders are brought and any
 * other plugin may mount a store, {@link ExplorerReserved#POINT}, where a plugin keeps a name out of every folder (Git
 * its {@code .git}), and {@link ExplorerChange#TOPIC}, where every change it makes is told — so a product's
 * index of the files (the CAD engine's documents) follows them. What opens a file is the interface's.
 */
public final class ExplorerPlugin implements Plugin {

    public static final String ID = "cad-explorer";

    static final String MOST_FILES       = "most-files";
    static final String LARGEST_DOCUMENT = "largest-document";
    static final long   MEGABYTE         = 1024L * 1024;

    private static final String FOLDERS_AS_PROJECTS = "Folders as projects";
    /** Its section of how a folder is read as a project. */
    static final String FOLDERS = "folders";

    private final List<ExplorerMount> mounted;
    private final PlaceSource         documents;

    /** @param mounted the server's folders, as its settings name them */
    public ExplorerPlugin(List<ExplorerMount> mounted, PlaceSource documents) {
        this.mounted = List.copyOf(mounted);
        this.documents = documents;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Explorer", ExplorerPlugin.class).withIcon(ExplorerPlugin.class, "explorer.svg");
    }

    @Override
    public List<Section> settings() {
        return List.of(Section.fixed(FOLDERS, FOLDERS_AS_PROJECTS, null, List.of(
                Parameter.wholeNumber(MOST_FILES, "Entries looked at in a folder",
                        "How far a folder that belongs to a project is walked for its documents: a mount of a whole disk is not walked to its end.",
                        5000, 100, 200_000, "entries"),
                Parameter.wholeNumber(LARGEST_DOCUMENT, "Largest document read",
                        "A larger .jm file is shown as a file, and listed as too large to read as a document.", 8, 1, 256, "MB"))));
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.declare(ExplorerMount.POINT);
        registration.declare(ExplorerChange.TOPIC);
        registration.declare(ExplorerReserved.POINT);
        mounted.forEach(mount -> registration.contribute(ExplorerMount.POINT, mount));
        registration.contribute(PlaceSource.POINT, documents);
    }
}
