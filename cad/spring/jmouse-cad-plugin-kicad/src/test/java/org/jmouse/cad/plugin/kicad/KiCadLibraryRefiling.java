package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.document.DocumentEditing;
import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.folder.RevisionOrigin;
import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.simulation.CategoryDocument;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.git.local.CommitAuthor;
import org.jmouse.git.local.CommitAuthors;
import org.jmouse.git.local.JGitLocalGit;
import org.jmouse.revisions.RevisionLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The one refiling of the KiCad documents a library folder held before every KiCad library was a category of its own:
 * each document an import filed under KiCad's category — in KiCad's own, or in a library category it wrote — is filed
 * where an import files it now ({@link KiCadLibraries#libraryCategory(Provenance)}), the category written beside it first
 * and held by the same owner; a library category an import wrote that nothing is filed in any more is taken away. Each
 * change is a revision of the import's ({@code Origin: IMPORTED}), so the next import of the same design still refreshes
 * a document nobody has written since. Run it again and it changes nothing. Run with the server stopped:
 *
 * <pre>
 * -Dcad.refile.folder=&lt;the library folder&gt; -Dcad.refile.zone=Europe/Kyiv
 * </pre>
 */
@EnabledIfSystemProperty(named = "cad.refile.folder", matches = ".+")
class KiCadLibraryRefiling {

    private record Held(StoredDocument document, DocumentNode read) {
    }

    @Test
    void fileEachKiCadDocumentWhereAnImportFilesItNow() {
        Path folder = Path.of(System.getProperty("cad.refile.folder"));
        ZoneId zone = ZoneId.of(System.getProperty("cad.refile.zone", ZoneId.systemDefault().getId()));
        KiCadLibraries libraries = KiCadLibraries.standard();
        String kicad = libraries.libraryCategory();
        CadLanguage cad = CadLanguage.shipped();
        DocumentLanguage language = cad.language();
        DocumentEditing editing = new DocumentEditing(language, cad.modules());
        DocumentStore store = storeOf(folder, zone, "jmCAD", "library@cad.jmouse.org");
        List<StoredDocument> opened = List.copyOf(store.open());
        // The library categories an import wrote: categories standing under KiCad's, held by an owner.
        Set<String> written = new HashSet<>();
        for (StoredDocument document : opened) {
            if (document.getKind().equals(SimulationModule.CATEGORY.word()) && document.owner().isPresent()
                && kicad.equals(document.getCategoryCode())) {
                written.add(document.getSlug());
            }
        }
        // What an import filed under KiCad's: in its category, in one of those, or — named by an earlier rule — in what is
        // no category at all. Nothing else is read.
        Set<String> categories = new HashSet<>();
        opened.stream().filter(document -> document.getKind().equals(SimulationModule.CATEGORY.word()))
                .forEach(document -> categories.add(document.getSlug()));
        Map<String, Held> held = new LinkedHashMap<>();
        for (StoredDocument document : opened) {
            String standing = document.getCategoryCode();
            if (kicad.equals(standing) || written.contains(standing) || (standing != null && !categories.contains(standing)
                                                                        && standing.startsWith(kicad + "-"))) {
                held.put(document.getSlug(), new Held(document, language.readOne(store.heldText(document), document.getSlug())));
            }
        }
        int refiled = 0;
        int created = 0;
        Set<String> wanted = new HashSet<>();
        for (Held found : List.copyOf(held.values())) {
            String standing = filedIn(found.read());
            Provenance provenance = Provenance.of(found.read());
            Optional<DocumentNode> category = libraries.libraryCategoryDocument(provenance);
            if (category.isEmpty() || found.read().kind().equals(SimulationModule.CATEGORY)) {
                continue;
            }
            String slug = category.get().slug();
            wanted.add(slug);
            Instant now = Instant.now();
            if (store.findBySlug(slug).isEmpty()) {
                write(store, new StoredDocument(slug, found.document().owner().orElse(null), null, now), language.write(category.get()),
                        category.get(), zone, now);
                created++;
            }
            if (!slug.equals(standing)) {
                String text = editing.reheaded(found.read(), new DocumentEditing.Head(null, null, slug));
                write(store, found.document(), text, language.readOne(text, found.document().getSlug()), zone, now);
                refiled++;
            }
        }
        int removed = 0;
        for (String slug : written) {
            if (!wanted.contains(slug) && !store.anyInCategory(slug)) {
                store.remove(store.findBySlug(slug).orElseThrow(), null);
                removed++;
            }
        }
        // Opened again, every KiCad document stands where an import files it now.
        DocumentStore reopened = storeOf(folder, zone, "check", "check@cad.jmouse.org");
        List<StoredDocument> after = List.copyOf(reopened.open());
        Set<String> standingCategories = new HashSet<>();
        after.stream().filter(document -> document.getKind().equals(SimulationModule.CATEGORY.word()))
                .forEach(document -> standingCategories.add(document.getSlug()));
        long astray = after.stream()
                .filter(document -> document.getCategoryCode() != null && document.getCategoryCode().startsWith(kicad))
                .filter(document -> !document.getKind().equals(SimulationModule.CATEGORY.word()))
                .map(document -> language.readOne(reopened.heldText(document), document.getSlug()))
                .filter(read -> libraries.libraryOf(Provenance.of(read)).isPresent())
                .filter(read -> !standingCategories.contains(filedIn(read))
                                || !Objects.equals(filedIn(read), libraries.libraryCategory(Provenance.of(read))))
                .count();
        assertEquals(0, astray);
        System.out.println("REFILED " + refiled + " documents, wrote " + created + " categories, removed " + removed + " in " + folder);
    }

    private static DocumentStore storeOf(Path folder, ZoneId zone, String name, String email) {
        return new DocumentStore(folder, new JGitLocalGit(), CommitAuthors.NONE, new CommitAuthor(name, email), () -> zone);
    }

    private static String filedIn(DocumentNode read) {
        return read.block(MetaNode.class).flatMap(meta -> meta.text(MetaNode.CATEGORY)).orElse(null);
    }

    /** One revision of the import's: the index taken from the text, then the text kept where its category puts it. */
    private static void write(DocumentStore store, StoredDocument document, String text, DocumentNode read, ZoneId zone, Instant now) {
        StoredDocument kept = store.save(document);
        int revision = kept.getRevision() + 1;
        RevisionLabel label = store.nextLabel(kept.getSlug(), LocalDate.ofInstant(now, zone));
        kept.indexRevision(revision, label, DocumentStore.checksumOf(text), read.kind().word(), read.title(), null,
                read.block(MetaNode.class).flatMap(meta -> meta.text(MetaNode.SUMMARY)).orElse(null), filedIn(read), false, now);
        store.appendSource(kept.getSlug(), revision, label, text, null, RevisionOrigin.IMPORTED, now);
    }
}
