package org.jmouse.cad.store.library;

import java.util.function.Supplier;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.StoredDocuments;
import org.jmouse.cad.table.TableModule;
import org.jmouse.cad.table.TableSource;
import org.jmouse.sheet.Sheet;

/**
 * 📋 The engine's tables: the catalogue's table documents by slug — the one a library ships, or its curator's revision kept in
 * the library folder —, each one's text read once per revision and its sheet made from that tree. A table saved is a new revision, so the next value asked of it is the
 * new one; a program never reads a jar's file of a table while the engine runs.
 */
public class CatalogueTables implements TableSource {

    /**
     * The catalogue, asked for when a table is first read: the plugins that read tables are made before the language the
     * catalogue reads with, which their modules are part of.
     */
    private final Supplier<CadCatalogue> catalogue;

    public CatalogueTables(Supplier<CadCatalogue> catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public Sheet table(String slug) {
        StoredDocuments shared = catalogue.get().scope(null);
        var document = shared.addressed(slug).filter(found -> found.getKind().equals(TableModule.TABLE.word()))
                .orElseThrow(() -> new IllegalStateException("The catalogue holds no table '" + slug + "': it ships with the module that "
                        + "reads it, so the module or its plugin is not running"));
        return TableModule.sheetOf(shared.head(document).document());
    }
}
