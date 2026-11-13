package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.store.catalogue.CatalogueViews.SheetView;
import org.jmouse.cad.store.library.PartLibrary.DrawnDocument;
import org.jmouse.cad.store.project.CadProjects;
import org.jmouse.cad.store.project.ProjectView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 🗂️ The projects: the reader's own, kept by their place, and those the libraries ship — {@code JMOUSE_CAD_API.md} §2.3.
 *
 * <p>Every route decides inside the service, at the project's place or the reader's own, and says so: whose a project is is
 * read off its document, never off the request.</p>
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class ProjectsController {

    private final CadProjects projects;

    public ProjectsController(CadProjects projects) {
        this.projects = projects;
    }

    /** What a project is called and what it is. */
    public record ProjectDraft(String title, String summary) {
    }

    /** A project archived, or brought back. */
    public record ArchiveRequest(boolean archived) {
    }

    @GetMapping("/projects")
    @PublicEndpoint("the reader's own projects where they may read, then the shipped ones; decided in the service")
    public List<ProjectView> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return projects.list(includeArchived);
    }

    /** The reader's Drafts: their own place, beside {@code /projects/{slug}} so that no project's address is taken from it. */
    @GetMapping("/drafts")
    @PublicEndpoint("the reader's own Drafts, made on the first question; decided in the service at the reader's place")
    public ProjectView drafts() {
        return projects.drafts();
    }

    @PostMapping("/projects")
    @PublicEndpoint("a project of the reader's own; decided in the service at the reader's place")
    public ProjectView create(@RequestBody ProjectDraft draft) {
        return projects.create(draft.title(), draft.summary());
    }

    @GetMapping("/projects/{slug}")
    @PublicEndpoint("a project the reader may read; decided in the service at its place")
    public ProjectView project(@PathVariable String slug) {
        return projects.project(slug);
    }

    @PutMapping("/projects/{slug}")
    @PublicEndpoint("a project the reader may change; decided in the service at its place")
    public ProjectView describe(@PathVariable String slug, @RequestBody ProjectDraft draft, @RequestParam long version) {
        return projects.describe(slug, draft.title(), draft.summary(), version);
    }

    @PutMapping("/projects/{slug}/archived")
    @PublicEndpoint("a project the reader may change; decided in the service at its place")
    public ProjectView archive(@PathVariable String slug, @RequestBody ArchiveRequest request) {
        return projects.archive(slug, request.archived());
    }

    @GetMapping("/documents/{slug}/project")
    @PublicEndpoint("the project of a document the reader may read; decided in the service")
    public ProjectView holding(@PathVariable String slug) {
        return projects.holding(slug);
    }

    @GetMapping("/projects/{slug}/sheets")
    @PublicEndpoint("the sheets of a project the reader may read; decided in the service")
    public List<SheetView> sheets(@PathVariable String slug) {
        return projects.sheets(slug);
    }

    @GetMapping("/projects/{slug}/documents")
    @PublicEndpoint("what a project the reader may read holds, each document the reader may read; decided in the service")
    public List<DrawnDocument> documents(@PathVariable String slug, @RequestParam(required = false) String kind) {
        return projects.documents(slug, kind);
    }
}
