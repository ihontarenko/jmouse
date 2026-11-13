package org.jmouse.cad.store.authority;

import org.jmouse.cad.document.DocumentKindNames;
import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.folder.FolderLayout;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.core.text.Slugifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🗂️ The library folder in words (ADR 0006, amended 2026-11-13). Every document stands in its owner's folder, then the folder of
 * its kind — the plural the settings' kind names say, {@code circuits}, {@code 3d-models} —, then its category's, if it is filed
 * in one, then its file {@code <slug>.<kind>.jm}:
 *
 * <ul>
 *   <li>the shared catalogue's in {@code catalogue/};</li>
 *   <li>a host's owner under the host's word and by its name — {@code libraries/<handle>/}, {@code workspaces/<slug>/};</li>
 *   <li>a project in its own folder among its keeper's projects, its file beside what it holds:
 *       {@code libraries/owner/projects/kicad-control-board/kicad-control-board.project.jm} and
 *       {@code libraries/owner/projects/kicad-control-board/circuits/controls.circuit.jm}. Whose a project is the path says, as
 *       it says whose any document is; what the project holds is filed in no category — the project is its place.</li>
 * </ul>
 *
 * <p>No identifier is in a path: a person who opens the folder by hand reads where they are. An owner's name is asked of the
 * host once and kept, and a project's folder is learnt when its path is read or its file placed: an owner renamed is read
 * under its old folder until the engine opens the folder again.</p>
 */
public final class CadFolderLayout implements FolderLayout {

    /** The kind whose document is an owner of its own: a project, holding its sheets. */
    private static final String PROJECT_KIND = SimulationModule.PROJECT.word();

    private final CadHostPlaces           host;
    private final CadSettings             settings;
    private final Slugifier               slugifier      = Slugifier.standard();
    private final Map<DocumentOwner, String> names       = new ConcurrentHashMap<>();
    /** Each project's folder, by its slug: where its keeper's projects stand, and its slug. */
    private final Map<String, String>     projectFolders = new ConcurrentHashMap<>();

    public CadFolderLayout(CadHostPlaces host, CadSettings settings) {
        this.host = host;
        this.settings = settings;
    }

    @Override
    public String pathOf(Place place) {
        if (place.kind().equals(PROJECT_KIND)) {
            return projectFolder(place.slug(), place.owner()) + SEPARATOR + FolderLayout.fileOf(place);
        }
        return FolderLayout.standing(ownerFolder(place.owner()), kindFolderOf(place.kind()), place);
    }

    @Override
    public Optional<Placed> placedAt(List<String> words) {
        return ownerAt(words).flatMap(found -> {
            List<String> within = words.subList(found.words(), words.size());
            if (within.size() >= 3 && within.getFirst().equals(kindFolderOf(PROJECT_KIND))) {
                return projectPlacedAt(found.owner(), words.subList(0, found.words() + 2), within.subList(2, within.size()));
            }
            return FolderLayout.standingAt(found.owner(), within);
        });
    }

    /** An owner a path's first words name, and how many they are; the owner null for the shared catalogue's. */
    private record OwnerWords(DocumentOwner owner, int words) {
    }

    private Optional<OwnerWords> ownerAt(List<String> words) {
        if (words.getFirst().equals(CATALOGUE)) {
            return Optional.of(new OwnerWords(null, 1));
        }
        if (words.size() < 2) {
            return Optional.empty();
        }
        return host.ownerFolders().entrySet().stream().filter(folder -> folder.getValue().equals(words.getFirst())).findFirst()
                .flatMap(folder -> host.ownerNamed(folder.getKey(), words.get(1)))
                .map(owner -> {
                    names.put(owner, words.get(1));
                    return new OwnerWords(owner, 2);
                });
    }

    /**
     * What a path inside a project's folder says: its file is its keeper's document; anything else is the project's, standing in
     * the folder of its kind. A file directly in the project's folder that is not the project's own is no document's place.
     */
    private Optional<Placed> projectPlacedAt(DocumentOwner keeper, List<String> folderWords, List<String> inProject) {
        String project = folderWords.getLast();
        projectFolders.put(project, String.join(SEPARATOR, folderWords));
        if (inProject.size() == 1) {
            boolean own = inProject.getFirst().equals(FolderLayout.fileOf(new Place(keeper, PROJECT_KIND, null, project)));
            return own ? Optional.of(new Placed(keeper, null)) : Optional.empty();
        }
        return FolderLayout.standingAt(CadPlaces.projectOwner(project), inProject);
    }

    /** Where an owner's documents stand. */
    private String ownerFolder(DocumentOwner owner) {
        if (owner == null) {
            return CATALOGUE;
        }
        if (CadPlaces.isProject(owner)) {
            String folder = projectFolders.get(owner.id());
            if (folder == null) {
                throw new IllegalStateException("The project " + owner.id() + " has no folder: its file is not in the library folder");
            }
            return folder;
        }
        String word = host.ownerFolders().get(owner.type());
        if (word == null) {
            throw new IllegalArgumentException("The host keeps no folder for owners of type " + owner.type());
        }
        return word + SEPARATOR + names.computeIfAbsent(owner, host::nameOf);
    }

    /** A project's own folder, among its keeper's projects; learnt here for what the project holds. */
    private String projectFolder(String project, DocumentOwner keeper) {
        String folder = ownerFolder(keeper) + SEPARATOR + kindFolderOf(PROJECT_KIND) + SEPARATOR + project;
        projectFolders.put(project, folder);
        return folder;
    }

    /** The folder a kind's documents stand in within their owner's: {@code circuits}, {@code function-libraries}. */
    private String kindFolderOf(String kind) {
        DocumentKindNames.Names named = settings.read(DocumentKindNames.SECTION).kinds().get(kind);
        return slugifier.slug(named == null ? kind : named.plural());
    }
}
