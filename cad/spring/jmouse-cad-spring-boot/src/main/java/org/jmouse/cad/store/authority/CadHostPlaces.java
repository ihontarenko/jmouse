package org.jmouse.cad.store.authority;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.store.problem.CadProblem;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * The host's own owners as places — jmCAD's accounts, Innoventa's workspaces —: whether one still exists, what it sits in,
 * and whether it takes new work. A project is never the host's: the engine keeps projects and answers for them itself
 * ({@link CadPlaces}).
 *
 * <p>A refusal is a {@link CadProblem}: {@link CadProblem.NotFound} for an owner that does not exist,
 * {@link CadProblem.Conflict} for one that takes nothing new.</p>
 */
public interface CadHostPlaces {

    /** The owner's whole chain, outermost first and ending with the owner itself. Refuses an owner that does not exist. */
    CadPlace requireOwner(DocumentOwner owner);

    /** Refuses an owner that takes nothing new. Asked after access. */
    void requireAcceptsNew(DocumentOwner owner);

    /**
     * The word the folder of the host's owners stands under in the library folder, by the owner type: {@code ACCOUNT} →
     * {@code libraries}. A person opening the folder reads it; it names no identifier.
     */
    Map<String, String> ownerFolders();

    /** What an owner is called in a path a person reads — an account's handle, a workspace's slug —, never its identifier. */
    String nameOf(DocumentOwner owner);

    /** The owner of a type the host keeps that a folder's name stands for; empty for none. */
    Optional<DocumentOwner> ownerNamed(String ownerType, String name);

    /** Something of the owner's changed at this moment; a host that keeps an owner's last change records it. */
    default void recordChange(DocumentOwner owner, Instant when) {
    }
}
