package org.jmouse.cad.store.authority;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.DocumentStatus;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.store.problem.CadProblem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Every owner as a place, the engine's projects and the host's owners alike: a project is answered here, from its own
 * document — its chain is the chain of whoever keeps it, then the project —; every other owner is the host's
 * ({@link CadHostPlaces}). The rules a design keeps are the engine's too: a circuit lives in a project, a pcb with its
 * circuit, a project in no project, and an archived project takes nothing new.
 *
 * <p>A refusal is a {@link CadProblem}: {@link CadProblem.NotFound} for an owner that does not exist,
 * {@link CadProblem.Conflict} for one that takes nothing new.</p>
 */
public final class CadPlaces {

    /** The type a project's documents are owned under: {@code PROJECT:<the project's slug>}. */
    public static final String PROJECT = "PROJECT";

    private final CadHostPlaces host;
    private final DocumentStore documents;

    public CadPlaces(CadHostPlaces host, DocumentStore documents) {
        this.host = host;
        this.documents = documents;
    }

    /** A project's documents as the store keeps them: owned by the project, by its slug. */
    public static DocumentOwner projectOwner(String projectSlug) {
        return DocumentOwner.of(PROJECT, projectSlug);
    }

    /** Whether an owner is a project of the engine's. */
    public static boolean isProject(DocumentOwner owner) {
        return owner != null && PROJECT.equals(owner.type());
    }

    /**
     * The owner's whole chain, outermost first and ending with the owner itself. Refuses an owner that does not exist — a
     * project kept nowhere, or one a library ships, which keeps nothing of its own.
     */
    public CadPlace requireOwner(DocumentOwner owner) {
        if (!isProject(owner)) {
            return host.requireOwner(owner);
        }
        StoredDocument project = requireKeptProject(owner.id());
        List<DocumentOwner> chain = new ArrayList<>(host.requireOwner(keeperOf(project)).chain());
        chain.add(owner);
        return new CadPlace(chain);
    }

    /** Refuses an owner that takes nothing new: an archived project, or whatever the host refuses. Asked after access. */
    public void requireAcceptsNew(DocumentOwner owner) {
        if (!isProject(owner)) {
            host.requireAcceptsNew(owner);
            return;
        }
        StoredDocument project = requireKeptProject(owner.id());
        if (project.getStatus() == DocumentStatus.ARCHIVED) {
            throw CadProblem.conflict("An archived project takes nothing new.");
        }
        host.requireAcceptsNew(keeperOf(project));
    }

    /**
     * Refuses a document of a kind this owner does not hold: a circuit lives in a project — one design of sheets, the Drafts
     * for a loose one —, never straight in its keeper's library, where it has no home, no address and no bench; a pcb lives
     * with its circuit; a project stands in no project.
     *
     * @param kind the document's kind word: {@code circuit}, {@code pcb}, {@code component}
     */
    public void requireHolds(DocumentOwner owner, String kind) {
        if (isProject(owner)) {
            if (kind.equals(SimulationModule.PROJECT.word())) {
                throw CadProblem.conflict("A project stands in no project.");
            }
            return;
        }
        if (kind.equals(SimulationModule.CIRCUIT.word())) {
            throw CadProblem.conflict("A circuit lives in a project: put it in Drafts or another project.");
        }
        if (kind.equals(PcbModule.PCB.word())) {
            throw CadProblem.conflict("A pcb lives in the project its circuit lives in.");
        }
    }

    /**
     * Something of the owner's changed at this moment. A project keeps no record of it — when it last changed is read off its
     * documents —; the host is told of its own owners.
     */
    public void recordChange(DocumentOwner owner, Instant when) {
        if (!isProject(owner)) {
            host.recordChange(owner, when);
        }
    }

    /** A project kept in the library folder — the one kind of project that owns documents —; refused by its address otherwise. */
    private StoredDocument requireKeptProject(String slug) {
        return documents.find(slug)
                .filter(document -> SimulationModule.PROJECT.word().equals(document.getKind()))
                .filter(document -> !document.isShipped())
                .orElseThrow(() -> CadProblem.notFound("The project '" + slug + "'"));
    }

    /** Who keeps a project: the owner of its document. */
    private static DocumentOwner keeperOf(StoredDocument project) {
        return project.owner().orElseThrow(() -> new IllegalStateException("The project '" + project.getSlug() + "' is kept by nobody"));
    }
}
