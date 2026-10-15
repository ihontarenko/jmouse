package org.jmouse.parallax.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.store.scene.Comparisons;
import org.jmouse.parallax.store.scene.Comparisons.ComparisonDraft;
import org.jmouse.parallax.store.scene.Comparisons.ComparisonView;
import org.jmouse.parallax.store.scene.SceneDocument;
import org.jmouse.parallax.store.scene.Scenes;
import org.jmouse.parallax.store.scene.Scenes.RevisionView;
import org.jmouse.parallax.store.scene.Scenes.SceneSummary;
import org.jmouse.parallax.store.scene.Scenes.SceneView;
import org.jmouse.parallax.store.scene.Scenes.SnapshotView;
import org.jmouse.parallax.store.support.ParallaxPage;
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
import java.util.Map;

/**
 * 🎬 Scenes — their documents, revisions, snapshots and forks — and the comparisons that lay several side by side.
 *
 * <p>A scene and a comparison belong to an owner (a project, in Parallax) and are seen in a realm (its workspace).
 * The product's rules decide each route at the scene, the comparison or the owner the address names, through its
 * access-target resolvers.</p>
 */
@RestController
@RequestMapping(ParallaxManagementRoutes.PREFIX)
public class SceneController {

    private final Scenes      scenes;
    private final Comparisons comparisons;

    public SceneController(Scenes scenes, Comparisons comparisons) {
        this.scenes      = scenes;
        this.comparisons = comparisons;
    }

    public record CreateSceneRequest(String title, String description, String instrumentCode, SceneDocument document) {
    }

    public record SaveSceneRequest(SceneDocument document, String message, int baseRevision) {
    }

    public record DescribeSceneRequest(String title, String description) {
    }

    /** A copy of a scene into another owner — a project, in Parallax; writing there is checked by {@code Scenes}. */
    public record ForkRequest(ParallaxOwner owner, Integer revision, String title) {
    }

    public record ArchiveRequest(boolean archived) {
    }

    public record SnapshotRequest(String title, double timelinePosition, Map<String, Object> camera,
                                  Map<String, Map<String, Double>> parameterValues) {
    }

    /** One owner's scenes that are not archived, most recently changed first. */
    @GetMapping(ParallaxManagementRoutes.OWNER + "/scenes")
    public ParallaxPage<SceneSummary> ownedScenes(@OwnerAddress String owner, @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        return scenes.owned(OwnerAddressArgumentResolver.ownerOf(owner), page, size);
    }

    @PostMapping(ParallaxManagementRoutes.OWNER + "/scenes")
    @ResponseStatus(HttpStatus.CREATED)
    public SceneView create(@OwnerAddress String owner, @RequestBody CreateSceneRequest request) {
        return scenes.create(OwnerAddressArgumentResolver.ownerOf(owner), request.title(), request.description(),
                request.instrumentCode(), request.document());
    }

    /** The newest scenes seen in one realm, whoever owns them. */
    @GetMapping(ParallaxManagementRoutes.REALM + "/scenes/recent")
    @PublicEndpoint("filtered per scene: only scenes the caller may read at their owner are listed")
    public List<SceneSummary> recent(@OwnerAddress(type = ParallaxManagementRoutes.REALM_TYPE,
                                                   id = ParallaxManagementRoutes.REALM_ID) String realm,
                                     @RequestParam(defaultValue = "12") int size) {
        return scenes.recentInRealm(OwnerAddressArgumentResolver.ownerOf(realm), size);
    }

    @GetMapping("/scenes/{sceneId}")
    public SceneView scene(@PathVariable String sceneId) {
        return scenes.get(sceneId);
    }

    @PutMapping("/scenes/{sceneId}/document")
    public SceneView save(@PathVariable String sceneId, @RequestBody SaveSceneRequest request) {
        return scenes.save(sceneId, request.document(), request.message(), request.baseRevision());
    }

    @PutMapping("/scenes/{sceneId}")
    public SceneView describe(@PathVariable String sceneId, @RequestParam long version,
                              @RequestBody DescribeSceneRequest request) {
        return scenes.describe(sceneId, request.title(), request.description(), version);
    }

    @GetMapping("/scenes/{sceneId}/revisions")
    public List<RevisionView> revisions(@PathVariable String sceneId) {
        return scenes.revisions(sceneId);
    }

    @GetMapping("/scenes/{sceneId}/revisions/{number}")
    public SceneDocument revision(@PathVariable String sceneId, @PathVariable int number) {
        return scenes.revision(sceneId, number);
    }

    @PostMapping("/scenes/{sceneId}/revisions/{number}/restore")
    public SceneView restore(@PathVariable String sceneId, @PathVariable int number) {
        return scenes.restore(sceneId, number);
    }

    /** A copy of a revision as a new scene; writing at the target is checked by {@code Scenes}. */
    @PostMapping("/scenes/{sceneId}/fork")
    @ResponseStatus(HttpStatus.CREATED)
    public SceneView fork(@PathVariable String sceneId, @RequestBody ForkRequest request) {
        if (request.owner() == null) {
            throw ParallaxProblem.invalid("Name the owner the copy goes to.");
        }

        return scenes.fork(sceneId, request.revision(), request.owner(), request.title());
    }

    @PutMapping("/scenes/{sceneId}/archived")
    public SceneView archive(@PathVariable String sceneId, @RequestBody ArchiveRequest request) {
        return scenes.setArchived(sceneId, request.archived());
    }

    @DeleteMapping("/scenes/{sceneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String sceneId) {
        scenes.delete(sceneId);
    }

    @GetMapping("/scenes/{sceneId}/snapshots")
    public List<SnapshotView> snapshots(@PathVariable String sceneId) {
        return scenes.snapshots(sceneId);
    }

    @PostMapping("/scenes/{sceneId}/snapshots")
    @ResponseStatus(HttpStatus.CREATED)
    public SnapshotView snapshot(@PathVariable String sceneId, @RequestBody SnapshotRequest request) {
        return scenes.snapshot(sceneId, request.title(), request.timelinePosition(), request.camera(),
                request.parameterValues());
    }

    @DeleteMapping("/scenes/{sceneId}/snapshots/{snapshotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSnapshot(@PathVariable String sceneId, @PathVariable String snapshotId) {
        scenes.deleteSnapshot(sceneId, snapshotId);
    }

    /** One owner's comparisons. */
    @GetMapping(ParallaxManagementRoutes.OWNER + "/comparisons")
    public List<ComparisonView> ownedComparisons(@OwnerAddress String owner) {
        return comparisons.owned(OwnerAddressArgumentResolver.ownerOf(owner));
    }

    @PostMapping(ParallaxManagementRoutes.OWNER + "/comparisons")
    @ResponseStatus(HttpStatus.CREATED)
    public ComparisonView createComparison(@OwnerAddress String owner, @RequestBody ComparisonDraft draft) {
        return comparisons.create(OwnerAddressArgumentResolver.ownerOf(owner), draft);
    }

    @GetMapping("/comparisons/{comparisonId}")
    public ComparisonView comparison(@PathVariable String comparisonId) {
        return comparisons.get(comparisonId);
    }

    @PutMapping("/comparisons/{comparisonId}")
    public ComparisonView updateComparison(@PathVariable String comparisonId, @RequestParam long version,
                                           @RequestBody ComparisonDraft draft) {
        return comparisons.update(comparisonId, draft, version);
    }

    @DeleteMapping("/comparisons/{comparisonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComparison(@PathVariable String comparisonId) {
        comparisons.delete(comparisonId);
    }
}
