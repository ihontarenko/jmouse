package org.jmouse.files.management;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.files.OwnerReference;
import org.jmouse.storage.delivery.DeliveryIntent;
import org.jmouse.storage.spring.DeliveryIntents;
import org.jmouse.storage.spring.DeliveryRenderer;
import org.jmouse.storage.spring.MultipartContent;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 📁 The file endpoints, shipped once.
 *
 * <p>Two products wrote the same six routes under different names — list, upload, read, rename, refile,
 * delete, plus the bytes — and a third was about to write them a third time for issue attachments. What
 * actually differed between them was the permission each route declared and the DTO it answered with;
 * the routes themselves were the same API twice.</p>
 *
 * <h2>⚠️ THIS CONTROLLER CARRIES NO AUTHORIZATION ANNOTATION, AND THAT IS DELIBERATE</h2>
 *
 * <p>It is gated from outside, through {@code ExternalAccessRules}, so that a product states what its
 * own permissions and scopes are for routes it did not write. ⚠️ <strong>An annotation would win over
 * that</strong> — external rules answer only where nothing is declared — so putting one here would make
 * the product's rule silently unreachable, which looks exactly like a rule that is being honoured.</p>
 *
 * <p>⚠️ <strong>Which is why these routes ship switched off.</strong> Adding this dependency must not
 * publish an unguarded file API by accident: the controller is only registered when a product sets
 * {@code jmouse.files.management.endpoints.enabled}, and turning it on is meant to happen in the same
 * change that declares the access rules for it. See {@code FilesManagementAutoConfiguration}.</p>
 *
 * <h2>The owner is in the query, not in the path</h2>
 *
 * <p>{@code ?owner=ISSUE:TES-42} rather than {@code /issues/TES-42/files}. A path segment would mean
 * this library knowing what an issue is, and a product adding a second kind of owner would need a second
 * route from the library rather than a constant of its own.</p>
 *
 * <p>⚠️ <strong>And ONE parameter rather than two, which is an authorization requirement rather than a
 * matter of taste.</strong> An access rule names exactly one parameter as the thing a route acts on, so
 * a route taking {@code ownerType} and {@code ownerId} separately could not be gated on its owner at
 * all — neither half identifies anything alone. See {@code OwnerReference.parse}.</p>
 */
@RestController
public class FileController {

    private final FileManagement        management;
    private final DeliveryRenderer      renderer;
    private final FileManagementContext context;

    /**
     * 🏗️ Serve files over the management surface.
     *
     * @param management what the routes actually do
     * @param renderer   turns a delivery plan into a response
     * @param context    where an upload goes and who is making it — the server’s answers, not the client’s
     */
    public FileController(FileManagement management, DeliveryRenderer renderer,
                          FileManagementContext context) {
        this.management = management;
        this.renderer   = renderer;
        this.context    = context;
    }

    /**
     * 📂 Everything filed against one owner.
     *
     * @param owner what holds them, written {@code KIND:id} — one value, because an access rule can
     *              name exactly one parameter as the thing a route acts on
     * @return the files
     */
    @GetMapping(ManagementRoutes.BASE)
    public List<FileView> list(@RequestParam String owner) {
        return management.listFiledUnder(OwnerReference.parse(owner))
                .stream()
                .map(FileView::of)
                .toList();
    }

    /**
     * 📥 Put a file against one owner.
     *
     * <p>⚠️ What may be uploaded is the installation's answer, not this route's — the acceptance policy
     * runs inside ingestion, before any bytes are written.</p>
     *
     * <p>⚠️ <strong>The namespace and the uploader are NOT taken from the request.</strong> They were
     * once, and both were a claim a caller could make about the server: where to write, and whose file
     * it is. {@link FileManagementContext} answers them instead — see its documentation for what each
     * one costs when a client gets to say it.</p>
     *
     * @param owner what should hold it, written {@code KIND:id}
     * @param file  the multipart body
     * @return the recorded file
     */
    @PostMapping(value = ManagementRoutes.BASE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileView upload(@RequestParam String owner, @RequestParam("file") MultipartFile file) {
        OwnerReference reference = OwnerReference.parse(owner);

        return FileView.of(management.upload(
            reference, context.namespaceFor(reference), MultipartContent.of(file),
            file.getOriginalFilename(), context.uploader()));
    }


    /**
     * 🌐 Fetch a file from a web address and file it.
     *
     * <p>⚠️ <strong>An import is the one path where the content is chosen by somebody who is not the
     * caller</strong>, which makes it the more dangerous of the two ways in rather than the less. It is
     * judged by the same acceptance policy as an upload, and the fetcher refuses an address that resolves
     * inside this network — see {@link RemoteFileFetcher}.</p>
     *
     * <p>⚠️ It makes no public link and cannot: this library has no notion of one. A product that mints
     * one on import does it from {@link FileManagementEvent.Uploaded}, where it can also decide not to.</p>
     *
     * @param owner   what should hold it, written {@code KIND:id}
     * @param request the address
     * @return the recorded file
     */
    @PostMapping(ManagementRoutes.IMPORT)
    @ResponseStatus(HttpStatus.CREATED)
    public FileView importFrom(@RequestParam String owner, @RequestBody ImportFileRequest request) {
        OwnerReference reference = OwnerReference.parse(owner);

        return FileView.of(management.importFrom(
                reference, context.namespaceFor(reference), request.url(), context.uploader()));
    }
    /**
     * 🔎 One file.
     *
     * @param fileId the file
     * @return the file
     */
    @GetMapping(ManagementRoutes.ONE)
    public FileView read(@PathVariable String fileId) {
        return FileView.of(management.read(fileId));
    }

    /**
     * 🚚 The bytes.
     *
     * <p>⚠️ The audience is {@code OWNER}, which decides the cache lifetime the planner applies. A
     * {@code PUBLIC} lifetime here would let a shared cache hand a file to somebody the engine refused —
     * and a route that genuinely is public needs to be the product's own, because what makes it safe is
     * a product-specific fact (a publication, a share token, a capability address).</p>
     *
     * @param fileId   the file
     * @param download whether to force a download rather than rendering inline
     * @param request  the request, for conditional and range headers
     * @return the bytes, or a redirect to them
     */
    @GetMapping(ManagementRoutes.CONTENT)
    public ResponseEntity<Resource> content(@PathVariable String fileId,
                                            @RequestParam(defaultValue = "false") boolean download,
                                            HttpServletRequest request) {
        DeliveryIntent intent = DeliveryIntents.of(request, download, DeliveryIntent.Audience.OWNER);

        return renderer.render(management.planDelivery(fileId, intent));
    }


    /**
     * 🙈 Hide it from ordinary listings, or stop hiding it.
     *
     * <p>⚠️ <strong>A flag this library stores and never interprets.</strong> What private actually means
     * — who may still see it, whether a share link overrides it — is the product's answer, asked of its
     * own access engine. Products with no notion of a private file never call this and never read it.</p>
     *
     * <p>⚠️ Refuses for a file something is holding: hiding an avatar breaks whatever is displaying it,
     * which is the whole point of {@code held_reason}.</p>
     *
     * @param fileId  the file
     * @param request whether it should be private
     * @return the file
     */
    @PatchMapping(ManagementRoutes.PRIVACY)
    public FileView setPrivate(@PathVariable String fileId, @RequestBody PrivacyRequest request) {
        return FileView.of(management.setPrivate(fileId, request.isPrivate()));
    }
    /**
     * 🏷️ Rename one.
     *
     * @param fileId  the file
     * @param request the new name
     * @return the renamed file
     */
    @PutMapping(ManagementRoutes.ONE)
    public FileView rename(@PathVariable String fileId, @RequestBody RenameFileRequest request) {
        return FileView.of(management.rename(fileId, request.name()));
    }

    /**
     * 📦 File it somewhere else.
     *
     * <p>⚠️ <strong>Authorized at BOTH ends, and only one of them is reachable from here.</strong> The
     * route is gated at the place the file is leaving; the place it is going to sits in the body, where
     * no rule can see it. A product wiring this route up must check the destination itself — otherwise
     * somebody who may write in their own folder can file a document into one they cannot see, which is
     * publishing it to that folder's readers.</p>
     *
     * @param fileId  the file
     * @param request where it should go
     * @return the file
     */
    @PutMapping(ManagementRoutes.BINDING)
    public FileView refile(@PathVariable String fileId, @RequestBody RefileFileRequest request) {
        return FileView.of(
            management.refile(fileId, OwnerReference.of(request.ownerType(), request.ownerId())));
    }

    /**
     * 🗑️ Remove it.
     *
     * <p>⚠️ Removes the file and its filings. The stored bytes stay — see {@code ManagedFiles}.</p>
     *
     * @param fileId the file
     */
    @DeleteMapping(ManagementRoutes.ONE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String fileId) {
        management.delete(fileId);
    }
}
