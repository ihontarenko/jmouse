package org.jmouse.files.management;

import org.jmouse.files.jpa.directory.StorageDirectories;
import org.jmouse.files.jpa.directory.StorageDirectory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 🌳 The whole of what a directory endpoint does, minus the routing and minus the authorization.
 *
 * <p>The tree's counterpart to {@link FileManagement}, and it exists for exactly the reason that one
 * gives for itself: <em>somebody has to own the transaction, and when the controller is mounted the only
 * caller is a controller.</em></p>
 *
 * <h3>⚠️ Why this class had to exist at all</h3>
 *
 * <p>{@link StorageDirectories} is a nested set, so every write renumbers — inserting a folder widens
 * every range to its right and moving one shifts a whole block. Both are done in bulk statements rather
 * than by walking entities, and a bulk statement without an active transaction does not fail quietly: it
 * throws {@code TransactionRequiredException}. Its own Javadoc says so and leaves demarcation to the
 * caller, which is right for a product calling from its own service and is nothing at all for a product
 * that merely mounts {@link DirectoryController}.</p>
 *
 * <p>Before this class, {@code POST /directories} answered <strong>500</strong> for every caller of the
 * mounted controller, and so did every move and every delete — the whole write half of the tree API.
 * {@link FileManagement} had the boundary from the beginning, which is why the file half worked and only
 * the tree did not.</p>
 *
 * <h3>⚠️ Not on the controller, and not on StorageDirectories</h3>
 *
 * <p>Not on the controller, because a transactional web layer holds a connection open across argument
 * binding, the handler and whatever an interceptor does around it — and {@code FileManagement} already
 * ruled that "a transactional controller is worse than this".</p>
 *
 * <p>Not on {@link StorageDirectories}, because its contract is deliberately annotation-free: a product
 * driving the tree from inside its own transaction must be able to, and an inner {@code REQUIRED} that
 * silently joins is fine while an annotation on a class also wraps every read.</p>
 *
 * <h3>⚠️ Nothing here asks who is calling</h3>
 *
 * <p>The same rule as its neighbour. Routes are gated before a handler runs, by one engine, against one
 * policy; a second opinion at this level is a rule somebody edits in one place. In particular the
 * destination of a {@link #move} is <strong>not</strong> checked here — it sits in the request body,
 * where no external rule can see it, and the product wiring this module up is told to check it. See
 * {@link DirectoryController#move}.</p>
 */
public class DirectoryManagement {

    private final StorageDirectories directories;

    /**
     * 🏗️ Own the tree's transactions.
     *
     * @param directories the tree
     */
    public DirectoryManagement(StorageDirectories directories) {
        this.directories = directories;
    }

    /**
     * 🌱 Every root a product declared for one owner.
     *
     * @param owner whose tree
     * @return the roots
     */
    @Transactional(readOnly = true)
    public List<StorageDirectory> roots(String owner) {
        return directories.roots(owner);
    }

    /**
     * 🌿 One directory and everything under it, in tree order.
     *
     * @param directoryId the directory
     * @return the subtree, itself first
     */
    @Transactional(readOnly = true)
    public List<StorageDirectory> subtree(String directoryId) {
        return directories.subtreeOf(directories.require(directoryId));
    }

    /**
     * 📁 Make a folder inside another.
     *
     * <p>⚠️ Resolving the parent and inserting under it are one unit: the insert renumbers against the
     * parent's range, and a parent read outside the boundary is a range that may already have moved.</p>
     *
     * @param parentId where it goes
     * @param name     what to call it
     * @return the new directory
     */
    @Transactional
    public StorageDirectory create(String parentId, String name) {
        return directories.create(directories.require(parentId), name);
    }

    /**
     * 🏷️ Rename one. Refused on a root by the tree itself.
     *
     * @param directoryId the directory
     * @param name        the new name
     * @return the renamed directory
     */
    @Transactional
    public StorageDirectory rename(String directoryId, String name) {
        return directories.rename(directories.require(directoryId), name);
    }

    /**
     * 📦 Move one under another.
     *
     * <p>⚠️ Both ends are resolved inside the boundary, and they must be: a move shifts two ranges, and
     * reading either of them before the transaction opens is reading a number that the other end's shift
     * is about to invalidate.</p>
     *
     * @param directoryId the directory to move
     * @param parentId    where it should go
     * @return the moved directory
     */
    @Transactional
    public StorageDirectory move(String directoryId, String parentId) {
        StorageDirectory directory   = directories.require(directoryId);
        StorageDirectory destination = directories.require(parentId);

        return directories.moveTo(directory, destination);
    }

    /**
     * 🗑️ Remove one, and optionally the folders under it.
     *
     * @param directoryId the directory
     * @param withSubtree whether folders inside it go too
     */
    @Transactional
    public void delete(String directoryId, boolean withSubtree) {
        directories.delete(directories.require(directoryId), withSubtree);
    }
}
