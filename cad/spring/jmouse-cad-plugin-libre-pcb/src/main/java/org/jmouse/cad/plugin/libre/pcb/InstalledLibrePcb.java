package org.jmouse.cad.plugin.libre.pcb;

import org.jmouse.cad.document.part.InstalledLibraryParts;
import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.libre.pcb.InstalledLibrePcbParts;
import org.jmouse.cad.libre.pcb.LibrePcbFiling;
import org.jmouse.cad.table.ReadTable;
import org.jmouse.settings.ParameterValues;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * The LibrePCB libraries installed beside the server, in the folders the plugin's settings name now: made anew — and read again —
 * when the folders or the filing table change.
 */
final class InstalledLibrePcb {

    static final String LIBRARY_FOLDERS = "library-folders";

    private record Made(List<Path> folders, LibrePcbFiling filing, PartSearchProvider parts) {
    }

    private final ParameterValues settings;
    private final ReadTable<LibrePcbFiling> filings;
    private final Executor reading;
    private Made current;

    /** @param reading where the libraries are read: a thread of their own */
    InstalledLibrePcb(ParameterValues settings, ReadTable<LibrePcbFiling> filings, Executor reading) {
        this.settings = settings;
        this.filings = filings;
        this.reading = reading;
    }

    /** The installed libraries as a place parts are looked for: the ones the settings name now. */
    PartSearchProvider parts() {
        return PartSearchProvider.following(InstalledLibrePcbParts.ID, InstalledLibrePcbParts.NAME, () -> current().parts());
    }

    private synchronized Made current() {
        List<Path> folders = settings.folders(LIBRARY_FOLDERS);
        LibrePcbFiling filing = filings.current();
        if (current == null || !current.folders().equals(folders) || current.filing() != filing) {
            current = new Made(folders, filing, new InstalledLibraryParts(new InstalledLibrePcbParts(folders, filing), reading));
        }
        return current;
    }
}
