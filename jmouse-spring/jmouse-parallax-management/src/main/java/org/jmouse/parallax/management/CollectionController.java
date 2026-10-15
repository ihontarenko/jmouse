package org.jmouse.parallax.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.jpa.collection.CollectionItem;
import org.jmouse.parallax.jpa.collection.ObjectCollection;
import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.store.collection.Collections;
import org.jmouse.parallax.store.collection.Collections.CollectionDraft;
import org.jmouse.parallax.store.collection.Collections.CollectionSummary;
import org.jmouse.parallax.store.collection.Collections.CollectionView;
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
 * 🗂️ Collections: the curated shelves, the formula sets, and a reader's own collections of objects and scenes.
 *
 * <p>Who may see and change one is decided per collection inside {@code Collections} — public, seen in a workspace, or
 * the owner's alone — so every route but creating one is a {@link PublicEndpoint}.</p>
 */
@RestController
@RequestMapping(ParallaxManagementRoutes.PREFIX)
public class CollectionController {

    private final Collections collections;

    public CollectionController(Collections collections) {
        this.collections = collections;
    }

    public record CreateCollectionRequest(ObjectCollection.Kind kind, String slug, CollectionDraft collection) {
    }

    public record ItemRequest(CollectionItem.Kind kind, String targetId, String note) {
    }

    public record ReorderRequest(List<String> itemIds) {
    }

    /** The curated shelves, or with {@code purpose=FORMULA_SET} the formula sets. */
    @GetMapping("/collections")
    @PublicEndpoint("curated collections are the portal's public shelves")
    public List<CollectionSummary> curated(@RequestParam(required = false) ObjectCollection.Purpose purpose) {
        return collections.curated(purpose);
    }

    @GetMapping("/me/collections")
    @PublicEndpoint("it lists only the caller's own collections, and refuses an anonymous caller")
    public List<CollectionSummary> mine() {
        return collections.mine();
    }

    @GetMapping("/collections/{collectionId}")
    @PublicEndpoint("Collections decides per collection: public, the workspace's, or the owner's alone")
    public CollectionView collection(@PathVariable String collectionId) {
        return collections.get(collectionId);
    }

    @PostMapping("/collections")
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionView create(@RequestBody CreateCollectionRequest request) {
        ObjectCollection.Kind kind = request.kind() == null ? ObjectCollection.Kind.PERSONAL : request.kind();
        if (request.collection() == null) {
            throw ParallaxProblem.invalid("Describe the collection: its title and visibility.");
        }

        return collections.create(kind, request.slug(), request.collection(), realmOf(request.collection()));
    }

    @PutMapping("/collections/{collectionId}")
    @PublicEndpoint("only the owner of a personal collection, or a curator of a curated one, changes it; Collections checks")
    public CollectionView update(@PathVariable String collectionId, @RequestParam long version,
                                 @RequestBody CollectionDraft draft) {
        return collections.update(collectionId, draft, version, realmOf(draft));
    }

    @DeleteMapping("/collections/{collectionId}")
    @PublicEndpoint("as for changing it")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String collectionId) {
        collections.delete(collectionId);
    }

    @PostMapping("/collections/{collectionId}/items")
    @PublicEndpoint("as for changing it; the item must also be something the caller can open")
    public CollectionView add(@PathVariable String collectionId, @RequestBody ItemRequest request) {
        return collections.add(collectionId, request.kind(), request.targetId(), request.note());
    }

    @DeleteMapping("/collections/{collectionId}/items/{itemId}")
    @PublicEndpoint("as for changing it")
    public CollectionView remove(@PathVariable String collectionId, @PathVariable String itemId) {
        return collections.remove(collectionId, itemId);
    }

    @PutMapping("/collections/{collectionId}/order")
    @PublicEndpoint("as for changing it")
    public CollectionView reorder(@PathVariable String collectionId, @RequestBody ReorderRequest request) {
        return collections.reorder(collectionId, request.itemIds());
    }

    /** The realm a draft names for a collection seen there, as Parallax stores it; null when it names none. */
    private static ParallaxOwner realmOf(CollectionDraft draft) {
        if (draft == null) {
            return null;
        }

        return draft.realm();
    }
}
