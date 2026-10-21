package org.jmouse.taxonomy.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.taxonomy.management.TaxonomyViews.AssignmentRequest;
import org.jmouse.taxonomy.management.TaxonomyViews.AssignmentView;
import org.jmouse.taxonomy.management.TaxonomyViews.CategoryChange;
import org.jmouse.taxonomy.management.TaxonomyViews.CategoryRequest;
import org.jmouse.taxonomy.management.TaxonomyViews.CategoryView;
import org.jmouse.taxonomy.management.TaxonomyViews.EntityCategoryView;
import org.jmouse.taxonomy.management.TaxonomyViews.FiledEntityView;
import org.jmouse.taxonomy.management.TaxonomyViews.MoveRequest;
import org.jmouse.taxonomy.management.TaxonomyViews.TaxonomyView;
import org.jmouse.taxonomy.management.TaxonomyViews.TreeView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 🗂️ Taxonomies over HTTP. A category is addressed by its path, sent as a parameter ({@code ?path=kicad/device}) so its
 * slashes never meet the router's.
 *
 * <p>Reading is open: every answer is narrowed through the entity types, which leave out what the reader may not see
 * ({@link TaxonomyManagement}). Writing is gated by the rules a product declares ({@link TaxonomyAccessRules}).</p>
 */
@RestController
@RequestMapping(TaxonomyRoutes.PREFIX)
public class TaxonomyController {

    private static final String WHY_OPEN = "a taxonomy's shape is not private, and every entity in an answer is one its "
            + "entity type says the reader may see";

    private final TaxonomyManagement management;

    public TaxonomyController(TaxonomyManagement management) {
        this.management = management;
    }

    @GetMapping("/taxonomies")
    @PublicEndpoint(WHY_OPEN)
    public List<TaxonomyView> taxonomies() {
        return management.taxonomies();
    }

    /**
     * @param entityType count only entities of this type; absent, every type
     */
    @GetMapping("/taxonomies/{code}")
    @PublicEndpoint(WHY_OPEN)
    public TreeView tree(@PathVariable String code, @RequestParam(required = false) String entityType) {
        return management.tree(code, entityType);
    }

    @GetMapping("/taxonomies/{code}/assignments")
    @PublicEndpoint(WHY_OPEN)
    public List<AssignmentView> assignments(@PathVariable String code, @RequestParam(required = false) String entityType) {
        return management.assignments(code, entityType);
    }

    @GetMapping("/taxonomies/{code}/entities")
    @PublicEndpoint(WHY_OPEN)
    public List<FiledEntityView> entities(@PathVariable String code, @RequestParam String path,
                                          @RequestParam(required = false) String entityType,
                                          @RequestParam(defaultValue = "true") boolean subtree) {
        return management.entities(code, path, entityType, subtree);
    }

    @GetMapping("/entities/{entityType}/{entityId}/categories")
    @PublicEndpoint(WHY_OPEN)
    public List<EntityCategoryView> categoriesOf(@PathVariable String entityType, @PathVariable String entityId) {
        return management.categoriesOf(entityType, entityId);
    }

    @PostMapping("/taxonomies/{code}/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryView createCategory(@PathVariable String code, @RequestBody CategoryRequest request) {
        return management.createCategory(code, request.parent(), request.title(), request.description());
    }

    @PutMapping("/taxonomies/{code}/categories")
    public CategoryView changeCategory(@PathVariable String code, @RequestParam String path,
                                       @RequestBody CategoryChange change) {
        return management.changeCategory(code, path, change.title(), change.description());
    }

    @PostMapping("/taxonomies/{code}/categories/move")
    public CategoryView moveCategory(@PathVariable String code, @RequestBody MoveRequest request) {
        return management.moveCategory(code, request.path(), request.destination());
    }

    @DeleteMapping("/taxonomies/{code}/categories")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable String code, @RequestParam String path,
                               @RequestParam(defaultValue = "false") boolean subtree) {
        management.deleteCategory(code, path, subtree);
    }

    @PutMapping("/taxonomies/{code}/assignments")
    public AssignmentView assign(@PathVariable String code, @RequestBody AssignmentRequest request) {
        return management.assign(code, request.path(), request.entityType(), request.entityId());
    }

    @DeleteMapping("/taxonomies/{code}/assignments")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable String code, @RequestParam String path, @RequestParam String entityType,
                         @RequestParam String entityId) {
        management.unassign(code, path, entityType, entityId);
    }
}
