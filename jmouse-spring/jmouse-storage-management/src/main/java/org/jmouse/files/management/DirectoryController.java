package org.jmouse.files.management;

import org.jmouse.files.jpa.directory.StorageDirectory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 🌳 The directory endpoints — a separate set from the file ones, deliberately.
 *
 * <p>A file and the place a file sits are different things, and a product that does not want a tree
 * should not meet these routes at all. Tessera's attachments are {@code ownerType = ISSUE}: they never
 * touch a directory, and shipping the file routes and the tree routes as one surface would have made
 * the tree look like part of the price of attaching a picture to a ticket.</p>
 *
 * <h2>⚠️ Same two rules as its neighbour</h2>
 *
 * <p>No {@code @RequiresAccess} here either — gated from outside through {@code ExternalAccessRules},
 * because an annotation would win over the product's own rule and make it silently unreachable. And the
 * routes ship switched off behind {@code jmouse.files.management.endpoints.enabled}, so that adding the
 * dependency cannot publish an unguarded tree API before anybody has declared rules for it.</p>
 *
 * <h2>⚠️ It talks to {@link DirectoryManagement}, never to the tree directly</h2>
 *
 * <p>It used to hold {@code StorageDirectories} itself, and every write answered <strong>500</strong>:
 * the tree renumbers in bulk statements, a bulk statement needs an active transaction, and a controller
 * opens none. {@link FileManagement} had that boundary from the start, which is exactly why the file
 * half of this module worked while the tree half never had.</p>
 *
 * <h2>⚠️ Roots are not made here</h2>
 *
 * <p>{@code POST} makes a folder inside another one. A <em>root</em> — {@code innoventa/files} — is
 * named in a product's own configuration and resolved by {@code StorageDirectories.requireRoot} at
 * startup, because its path is the storage-key namespace of everything beneath it. Letting somebody
 * create one through an API would mean a person could invent a namespace, and letting them rename one
 * would leave every key already written disagreeing with the tree. Both are refused by the service, not
 * merely absent from this controller.</p>
 */
@RestController
public class DirectoryController {

    private final DirectoryManagement directories;

    /**
     * 🏗️ Serve the tree.
     *
     * @param directories the tree's transactional surface
     */
    public DirectoryController(DirectoryManagement directories) {
        this.directories = directories;
    }

    /**
     * 🌱 Every root — which is every place a product declared.
     *
     * <h2>⚠️ Authorized ABOUT THE OWNER, and it was not always (JMF-48)</h2>
     *
     * <p>{@code storage_directories} is keyed {@code (owner_key, path)} so a product can give every
     * account a tree of its own. This route names the owner, so it has to be gated on the owner —
     * {@code FilesAccessRules} declares it exactly as it declares {@code list}. Before that it carried a
     * rule about nothing at all, and anybody could list anybody's roots by naming them in a query
     * parameter.</p>
     *
     * <p>⚠️ <strong>A product whose resolver cannot place the owner is now REFUSED rather than answered
     * with an empty list</strong> — including for the {@code *} default, which is a bare key rather than
     * a {@code <kind>:<id>} pair. That is the correction: an empty list reads as <em>"you have no
     * folders"</em>, which is a sentence about the data, when the truth was <em>"this route cannot see
     * your tree"</em>, which is a sentence about the route. A product with a real installation tree says
     * so by resolving {@code *} to its installation target.</p>
     *
     * @param owner whose tree — the installation's own unless the product scopes trees per person
     * @return the roots
     */
    @GetMapping(ManagementRoutes.DIRECTORIES)
    public List<DirectoryView> roots(@RequestParam(defaultValue = StorageDirectory.INSTALLATION) String owner) {
        return directories.roots(owner).stream().map(DirectoryView::of).toList();
    }

    /**
     * 🌿 One directory and everything under it, in tree order.
     *
     * <p>One indexed range query, which is the whole reason the numbering exists.</p>
     *
     * @param directoryId the directory
     * @return the subtree, itself first
     */
    @GetMapping(ManagementRoutes.DIRECTORY_SUBTREE)
    public List<DirectoryView> subtree(@PathVariable String directoryId) {
        return directories.subtree(directoryId).stream().map(DirectoryView::of).toList();
    }

    /**
     * 📁 Make a folder inside another.
     *
     * @param parentId where it goes
     * @param request  what to call it
     * @return the new directory
     */
    @PostMapping(ManagementRoutes.DIRECTORIES)
    @ResponseStatus(HttpStatus.CREATED)
    public DirectoryView create(@RequestParam String parentId,
                                @RequestBody SaveDirectoryRequest request) {
        return DirectoryView.of(directories.create(parentId, request.name()));
    }

    /**
     * 🏷️ Rename one.
     *
     * <p>⚠️ Refused on a root by the service: a root's path is the storage namespace of every object
     * filed beneath it.</p>
     *
     * @param directoryId the directory
     * @param request     the new name
     * @return the renamed directory
     */
    @PutMapping(ManagementRoutes.DIRECTORY)
    public DirectoryView rename(@PathVariable String directoryId,
                                @RequestBody SaveDirectoryRequest request) {
        return DirectoryView.of(directories.rename(directoryId, request.name()));
    }

    /**
     * 📦 Move one under another.
     *
     * <p>⚠️ <strong>Authorized at BOTH ends, and only one of them is reachable from a rule.</strong> The
     * route is gated at the directory being moved; the destination sits in the body, where no annotation
     * and no external rule can see it. A product wiring this up must check the destination itself —
     * without that, somebody who may write in their own branch can move a subtree into one they cannot
     * see, which publishes it to that branch's readers. This is the same shape as re-filing a file, and
     * it is the single most likely thing to be got wrong when adopting this module.</p>
     *
     * @param directoryId the directory to move
     * @param request     where it should go
     * @return the moved directory
     */
    @PutMapping(ManagementRoutes.DIRECTORY_PARENT)
    public DirectoryView move(@PathVariable String directoryId,
                              @RequestBody MoveDirectoryRequest request) {
        return DirectoryView.of(directories.move(directoryId, request.parentId()));
    }

    /**
     * 🗑️ Remove one.
     *
     * <p>⚠️ Refused while it still holds folders, unless {@code withSubtree} says otherwise. Deleting a
     * folder is one click and losing a subtree is not recoverable, so the default is the cautious one.</p>
     *
     * <p>⚠️ Files filed in it are <strong>not</strong> deleted, and neither are their bytes — a file may
     * be filed against several things, and the caller is choosing to remove a place rather than content.
     * A product that wants the contents gone deletes them first, deliberately.</p>
     *
     * @param directoryId the directory
     * @param withSubtree whether folders inside it go too
     */
    @DeleteMapping(ManagementRoutes.DIRECTORY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String directoryId,
                       @RequestParam(defaultValue = "false") boolean withSubtree) {
        directories.delete(directoryId, withSubtree);
    }
}
