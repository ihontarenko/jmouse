package org.jmouse.files.management;

import org.jmouse.access.enforcement.ExternalAccessRules;
import org.jmouse.access.enforcement.ExternalAccessRules.Declaration;
import org.jmouse.files.OwnerReference;
import org.jmouse.files.jpa.ManagedFile;
import org.jmouse.files.jpa.directory.StorageDirectory;
import org.jmouse.storage.administration.StorageAdministrationController;

/**
 * 🔐 What this module's routes require, said in a product's own vocabulary.
 *
 * <h3>Why a builder rather than a paragraph of documentation</h3>
 *
 * <p>A product adopting these controllers has to declare eleven rules across two types it did not
 * write, in a vocabulary only it knows. Left to prose, that is eleven chances to forget one — and
 * ⚠️ <strong>a forgotten rule is not an error, it is an un-gated route</strong> that looks exactly like
 * a gated one from every angle except a request.</p>
 *
 * <pre>{@code
 * @Bean
 * ExternalAccessRules fileManagementAccess() {
 *     return FilesAccessRules.atScope(Scopes.SPACE)
 *             .reading(Permissions.FILE_READ)
 *             .writing(Permissions.FILE_WRITE)
 *             .inModule(Modules.FILES)
 *             .build();
 * }
 * }</pre>
 *
 * <h3>⚠️ The type carries the WRITE rule, and reads narrow it</h3>
 *
 * <p>External rules work the other way round from how it first reads: <em>a method rule narrows, it
 * does not replace</em>, and anything not named keeps the type's rule. So the type is declared with the
 * stricter permission and the read-only methods are named individually. The consequence is the one that
 * matters: a method added to these controllers in a later release, and not yet known to this builder,
 * is gated as a <strong>write</strong> rather than as nothing at all.</p>
 *
 * <h3>⚠️ What this cannot do for you</h3>
 *
 * <p><strong>Resolving a row to a place.</strong> The declarations name {@link ManagedFile},
 * {@link StorageDirectory} and {@link OwnerReference} as the things these routes act on; turning one of
 * those into the space, project or section it belongs to is the product's answer, registered as an
 * access-target resolver. Without those resolvers the engine has a requirement and no target, and
 * refuses — loudly, which is the correct direction.</p>
 *
 * <p><strong>The far end of a move.</strong> {@code refile} and {@code move} act on two places and only
 * the one in the path is reachable from a rule; the destination sits in the request body. A product must
 * check it in its own code. This is stated on both routes and is the single most likely thing to be got
 * wrong when adopting this module.</p>
 */
public final class FilesAccessRules {

    /** The widest scope, where a rule is about the installation rather than a place in it. */
    private static final String GLOBAL_SCOPE = "GLOBAL";

    private final String scope;
    private String       readPermission        = "";
    private String       writePermission       = "";
    private String       module                = "";
    private String       administerPermission  = "";
    private String       configuringPermission = "";

    private FilesAccessRules(String scope) {
        this.scope = scope;
    }

    /**
     * 🏗️ Start declaring, at the scope these routes are answered in.
     *
     * @param scope the scope's name, as this product's catalogue spells it — {@code SPACE},
     *              {@code PROJECT}, {@code CATEGORY}
     * @return the builder
     */
    public static FilesAccessRules atScope(String scope) {
        return new FilesAccessRules(scope);
    }

    /**
     * 👁️ What listing and reading a file requires.
     *
     * @param permission the permission's name
     * @return the builder
     */
    public FilesAccessRules reading(String permission) {
        this.readPermission = permission;

        return this;
    }

    /**
     * ✍️ What uploading, renaming, re-filing and deleting requires.
     *
     * @param permission the permission's name
     * @return the builder
     */
    public FilesAccessRules writing(String permission) {
        this.writePermission = permission;

        return this;
    }

    /**
     * 🗄️ What the storage administration surface requires.
     *
     * <p>⚠️ <strong>Declare this at {@code GLOBAL} and with an ADMINISTRATIVE permission</strong>, not
     * with the one that gates ordinary file reads. Reading the registry lists every stored object's key
     * and name across the whole installation — it is a disclosure surface of its own, and it is not
     * scoped to a space or a project because it deliberately spans them.</p>
     *
     * <p>Leaving it unset means the administration controller keeps the type-level WRITE rule, which is
     * the safe direction but almost certainly not what was meant.</p>
     *
     * @param permission the permission's name, e.g. {@code storage:administer}
     * @return the builder
     */
    public FilesAccessRules administeringWith(String permission) {
        this.administerPermission = permission;

        return this;
    }

    /**
     * 🔧 What changing a folder's own configuration requires.
     *
     * <h3>⚠️ Its own right, and it is the only barrier there is</h3>
     *
     * <p>This library reserves no file type. A folder's upload configuration may admit {@code exe},
     * {@code jar}, {@code php}, {@code html} — that was decided deliberately, on the reading that
     * <em>access</em> rather than <em>acceptance</em> is what closes a dangerous branch. Which makes
     * this permission literally <em>who may put an executable into this installation</em>, and it
     * cannot be the one that renames a folder.</p>
     *
     * <p>⚠️ <strong>Grant it on a directory and it covers the subtree</strong>, like every other
     * directory grant — so "you may configure anything under {@code innoventa/files}" is expressible
     * without handing over the whole tree. Verify that: the permission axis does not consult the scope
     * hierarchy by itself, and a grant at a parent silently not reaching a child has bitten twice.</p>
     *
     * <p>⚠️ One right for every kind, because one kind exists. {@code upload} without a floor and a
     * future {@code retention} are not equally dangerous, and levelling them under one name is a
     * decision worth revisiting the day a second kind arrives — which is why the route is written
     * against the kind rather than against {@code upload}, and why this is a separate builder method
     * rather than a reused one.</p>
     *
     * <p>Leaving it unset keeps the type's WRITE rule — the safe direction, and almost certainly not
     * what was meant.</p>
     *
     * @param permission the permission's name, e.g. {@code directory:policy}
     * @return the builder
     */
    public FilesAccessRules configuringWith(String permission) {
        this.configuringPermission = permission;

        return this;
    }

    /**
     * 🎛️ The feature module these routes belong to, where the product has one.
     *
     * @param module the module's name
     * @return the builder
     */
    public FilesAccessRules inModule(String module) {
        this.module = module;

        return this;
    }

    /**
     * 🔐 The rules.
     *
     * @return rules covering both controllers, every method
     */
    public ExternalAccessRules build() {
        Declaration read  = declaration(readPermission);
        Declaration write = declaration(writePermission);

        // Unset means the type's write rule, which is the safe direction rather than the intended one.
        Declaration configure = configuringPermission.isBlank()
                ? write
                : declaration(configuringPermission);

        return ExternalAccessRules.builder()
                // ⚠️ The stricter rule on the type, so an unnamed method is gated as a write.
                .type(FileController.class, write.about(ManagedFile.class, "fileId"))
                .method(FileController.class, "list", read.about(OwnerReference.class, "owner"))
                .method(FileController.class, "upload", write.about(OwnerReference.class, "owner"))
                .method(FileController.class, "importFrom", write.about(OwnerReference.class, "owner"))
                .method(FileController.class, "read", read.about(ManagedFile.class, "fileId"))
                .method(FileController.class, "content", read.about(ManagedFile.class, "fileId"))

                .type(DirectoryController.class, write.about(StorageDirectory.class, "directoryId"))

                // ⚠️ ABOUT THE OWNER, and it used to be about nothing (JMF-48). `roots` takes an `owner`
                // — `storage_directories` is keyed `(owner_key, path)` precisely so a product can give
                // every account a tree of its own, and Innoventa does — so an unqualified rule let
                // anybody list anybody's roots by naming them in a query parameter. The rule reads like
                // `list`'s now, because it is the same question about the same argument.
                //
                // ⚠️ A product whose resolver cannot place the owner gets a REFUSAL where it used to get
                // an empty list, and that is the point of the change rather than a side effect of it: an
                // empty list is a sentence about the data — "you have no folders" — and the truth was a
                // sentence about the route. Innoventa read the first one and believed it.
                //
                // ⚠️ Which includes the DEFAULT. `owner` defaults to `StorageDirectory.INSTALLATION`, and
                // that is a bare `*` rather than a `<kind>:<id>` pair — so a resolver that only parses
                // owner references answers nothing for it and the installation tree is refused. That is
                // correct for a product with no installation tree, and a product that HAS one has to say
                // so by resolving `*` to its installation target. There is nowhere else that decision
                // could live: the library does not know whether this installation's root is a thing
                // anybody may list.
                .method(DirectoryController.class, "roots", read.about(OwnerReference.class, "owner"))
                .method(DirectoryController.class, "read",
                        read.about(StorageDirectory.class, "directoryId"))
                .method(DirectoryController.class, "subtree",
                        read.about(StorageDirectory.class, "directoryId"))
                .method(DirectoryController.class, "create",
                        write.about(StorageDirectory.class, "parentId"))

                // ⚠️ Reading a folder's own configuration is a READ of that folder. Writing one is
                // neither a read nor an ordinary write: with no reserved type anywhere in this library,
                // it is the decision about what may enter this installation, so it gets its own right.
                .method(DirectoryController.class, "configurations",
                        read.about(StorageDirectory.class, "directoryId"))
                .method(DirectoryController.class, "configuration",
                        read.about(StorageDirectory.class, "directoryId"))
                .method(DirectoryController.class, "configure",
                        configure.about(StorageDirectory.class, "directoryId"))
                .method(DirectoryController.class, "clearConfiguration",
                        configure.about(StorageDirectory.class, "directoryId"))

                // ⚠️ GLOBAL, and administrative. This surface spans every space and project by design,
                // so a scoped permission cannot express it and a read permission understates it.
                .type(StorageAdministrationController.class,
                      administerPermission.isBlank()
                              ? write
                              : Declaration.permission(administerPermission).atScope(GLOBAL_SCOPE))
                .build();
    }

    private Declaration declaration(String permission) {
        Declaration declared = Declaration.permission(permission).atScope(scope);

        return module.isBlank() ? declared : declared.inModule(module);
    }
}
