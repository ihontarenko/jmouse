package org.jmouse.cad.store.authority;

import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.store.problem.CadProblem;

import java.time.Instant;

/**
 * The host's owners as places: jmCAD stores an owner as a pair and asks here whether it still exists, what it sits in,
 * and whether it takes new work.
 *
 * <p>A refusal is a {@link CadProblem}: {@link CadProblem.NotFound} for an owner that does not exist,
 * {@link CadProblem.Conflict} for one that takes nothing new.</p>
 */
public interface CadPlaces {

    /**
     * The owner's whole chain, outermost first and ending with the owner itself — a project gives its workspace and
     * then the project. Refuses an owner that does not exist.
     */
    CadPlace requireOwner(DocumentOwner owner);

    /** Refuses an owner that takes nothing new — an archived project. Asked after access. */
    void requireAcceptsNew(DocumentOwner owner);

    /**
     * Refuses a document of a kind this owner does not hold — a host whose circuits live in projects, never straight in a
     * workspace. Asked before an owner's new document is written; every kind anywhere by default.
     *
     * @param kind the document's kind word: {@code circuit}, {@code pcb}, {@code component}
     */
    default void requireHolds(DocumentOwner owner, String kind) {
    }

    /** Something of the owner's changed at this moment; a host that keeps an owner's last change records it. */
    void recordChange(DocumentOwner owner, Instant when);
}
