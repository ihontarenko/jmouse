package org.jmouse.files.management;

import org.jmouse.files.jpa.directory.StorageDirectory;
import org.jmouse.files.management.access.DirectoryVisibility;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.util.Map;

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
     * ⚠️ Which of the rows a listing returns this caller may actually see (JMF-278).
     *
     * <p>The guard outside authorizes the directory the caller <em>named</em>; nothing authorized the
     * rows handed back. So a caller who could read a root could enumerate every folder under it,
     * including ones an explicit deny closed to them — refused on opening, listed by name in every tree.
     */
    private final DirectoryVisibility visible;

    /**
     * 🏗️ Serve the tree.
     *
     * @param directories the tree's transactional surface
     * @param visible     which of its rows this caller may see
     */
    public DirectoryController(DirectoryManagement directories, DirectoryVisibility visible) {
        this.directories = directories;
        this.visible     = visible;
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
        return visible.readable(directories.roots(owner)).stream().map(DirectoryView::of).toList();
    }

    /**
     * 🌿 One directory and everything under it, in tree order.
     *
     * <p>One indexed range query, which is the whole reason the numbering exists.</p>
     *
     * <h2>⚠️ FILTERED, and it was not (JMF-278)</h2>
     *
     * <p>The guard authorizes the directory NAMED in the path. Every descendant then came back
     * unfiltered, so a caller who may read a root could enumerate every folder beneath it — names, paths
     * and all — including ones an explicit deny closed to them. That made a per-directory deny stop
     * somebody <em>opening</em> a folder while leaving its existence in every tree the product draws.</p>
     *
     * <p>⚠️ A folder the caller may not read takes its children with it, because the engine walks the
     * containing chain: a child of a closed folder is closed too, and is dropped by the same predicate
     * rather than by anything here knowing about parents.</p>
     *
     * @param directoryId the directory
     * @return the readable part of the subtree, itself first
     */
    @GetMapping(ManagementRoutes.DIRECTORY_SUBTREE)
    public List<DirectoryView> subtree(@PathVariable String directoryId) {
        return visible.readable(directories.subtree(directoryId)).stream().map(DirectoryView::of).toList();
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

    /**
     * 🌳 One folder, with what actually applies to it.
     *
     * <p>⚠️ The rule reported here is the <strong>effective</strong> one — after inheritance — and it
     * says where it came from: this folder, an ancestor (by path), or the installation. A screen cannot
     * derive that for itself, because from below an inherited rule and a folder's own look identical.</p>
     *
     * <p>⚠️ It also says whether the rule admits active content. That is a <em>fact</em>, not a verdict:
     * the library reserves no type and declined to refuse anything on an owner's behalf. What it did not
     * decline was to say so out loud, and a folder quietly admitting {@code .svg} because a rule three
     * levels up says so is hidden risk rather than understood risk.</p>
     *
     * <p>⚠️ And a folder whose rule was <em>narrowed</em> still contains what it accepted before — a rule
     * governs entry, never residence. A listing showing files the folder would now refuse is correct;
     * this is what lets a screen say so rather than look broken.</p>
     *
     * @param directoryId the folder
     * @return the folder and every rule that applies to it
     */
    @GetMapping(ManagementRoutes.DIRECTORY)
    public DirectoryView read(@PathVariable String directoryId) {
        return directories.describe(directoryId);
    }

    /**
     * 📋 Which kinds of configuration this folder carries a row of its own for.
     *
     * <p>What a screen draws "clear" as available or not from — and with a table, that is simply
     * whether the row exists rather than a sentinel value somebody has to interpret.</p>
     *
     * @param directoryId the folder
     * @return the kind names
     */
    @GetMapping(ManagementRoutes.DIRECTORY_CONFIGURATIONS)
    public List<String> configurations(@PathVariable String directoryId) {
        return directories.configurationKinds(directoryId);
    }

    /**
     * 🔎 What this folder itself says, of one kind.
     *
     * <p>⚠️ Its <strong>own</strong> row, never what it inherits — the effective rule and where it came
     * from are on the directory's view, because that is where a screen needs them together.</p>
     *
     * @param directoryId the folder
     * @param kind        which question
     * @return the configuration, or {@code 404} when the folder carries none of its own
     */
    @GetMapping(ManagementRoutes.DIRECTORY_CONFIGURATION)
    public ResponseEntity<Object> configuration(@PathVariable String directoryId,
                                                @PathVariable String kind) {
        return directories.configuration(directoryId, kind)
                .map(configuration -> ResponseEntity.ok((Object) configuration))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * ✏️ Say what this folder does, of one kind.
     *
     * <h2>⚠️ Its own right, and that is load-bearing rather than tidy</h2>
     *
     * <p>The library reserves no file type: a folder may be configured to admit {@code exe},
     * {@code jar}, {@code php}. So <em>who may change a folder's upload rule</em> is literally <em>who
     * may put an executable into this installation</em>, and it cannot ride on the permission that
     * renames a folder. {@code FilesAccessRules.configuringWith(...)} declares it, and a product that
     * leaves it unset keeps the type's write rule — the safe direction, and almost certainly not what
     * was meant.</p>
     *
     * <p>⚠️ An unknown kind is refused from the registry, and a document that will not bind as that
     * kind's record is refused here — never stored to explode at somebody's next upload.</p>
     *
     * @param directoryId the folder
     * @param kind        which question
     * @param document    the answer
     * @return the configuration as it now reads, normalised
     */
    @PutMapping(ManagementRoutes.DIRECTORY_CONFIGURATION)
    public Object configure(@PathVariable String directoryId, @PathVariable String kind,
                            @RequestBody Map<String, Object> document) {
        return directories.writeConfiguration(directoryId, kind, document);
    }

    /**
     * 🧹 Stop saying anything of this kind, and go back to inheriting.
     *
     * <p>⚠️ Reachable on purpose. A configuration that could be set and not removed would be a one-way
     * door on every folder — and "cleared" here is genuinely no row at all, which is the state the
     * resolver reads as "ask my parent".</p>
     *
     * @param directoryId the folder
     * @param kind        which question
     */
    @DeleteMapping(ManagementRoutes.DIRECTORY_CONFIGURATION)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearConfiguration(@PathVariable String directoryId, @PathVariable String kind) {
        directories.clearConfiguration(directoryId, kind);
    }
}
