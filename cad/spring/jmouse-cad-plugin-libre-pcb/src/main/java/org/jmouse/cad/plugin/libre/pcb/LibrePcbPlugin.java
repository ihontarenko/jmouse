package org.jmouse.cad.plugin.libre.pcb;

import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.libre.pcb.LibrePcbFiling;
import org.jmouse.cad.table.ShippedTable;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;
import org.jmouse.settings.Parameter;
import org.jmouse.settings.Section;

import java.nio.file.Path;
import java.util.List;

/**
 * LibrePCB in jmCAD: a project read with its sheets and its board, brought to the core's point; and the libraries LibrePCB installs
 * beside the server, a place the part search looks in.
 */
public final class LibrePcbPlugin implements Plugin {

    public static final String ID = "cad-libre-pcb";

    /** The format's word on the exchange routes: {@code /exchange/libre-pcb/read}. */
    static final String FORMAT = "libre-pcb";

    static final String TITLE = "LibrePCB";

    /** Its section of installed libraries. */
    static final String LIBRARIES_SECTION = "libraries";
    /** What a reader finds the folders under. */
    private static final String LIBRARIES = "Installed libraries";

    private final DocumentImport designs;
    private final InstalledLibrePcb installed;
    private final List<Path> usualLibraryFolders;

    /**
     * @param installed           the libraries installed beside the server, in the folders the settings declared here name
     * @param usualLibraryFolders the folders those settings start from, as the deployment gives them
     */
    LibrePcbPlugin(DocumentImport designs, InstalledLibrePcb installed, List<Path> usualLibraryFolders) {
        this.designs = designs;
        this.installed = installed;
        this.usualLibraryFolders = List.copyOf(usualLibraryFolders);
    }

    @Override
    public List<Section> settings() {
        return List.of(Section.fixed(LIBRARIES_SECTION, LIBRARIES, null, List.of(
                Parameter.folders(InstalledLibrePcb.LIBRARY_FOLDERS, "Library folders",
                        "Folders of .lplib libraries, as LibrePCB's library manager installs them — a workspace's data/libraries/remote "
                                + "and data/libraries/local: the part search reads them.", usualLibraryFolders))));
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, TITLE, LibrePcbPlugin.class).drawnBy(LibrePcbPlugin.class, "libre-pcb.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        // The tables its module reads, as pages of the library.
        registration.contribute(ShippedTable.POINT, LibrePcbFiling.SHIPPED.shipping(LibrePcbFiling.TABLE));
        registration.contribute(DocumentImport.POINT, designs);
        registration.contribute(PartSearchProvider.POINT, installed.parts());
    }
}
