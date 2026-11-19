package org.jmouse.cad.plugin.git;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.management.CadManagementRoutes;
import org.jmouse.cad.plugin.git.PlaceGit.CommitRequest;
import org.jmouse.git.local.BranchInfo;
import org.jmouse.git.local.CommitDetail;
import org.jmouse.git.local.CommitInfo;
import org.jmouse.git.local.CommitResult;
import org.jmouse.git.local.PushOutcome;
import org.jmouse.git.local.WorkingTreeDiff;
import org.jmouse.git.local.WorkingTreeStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 🌿 Git's routes, under the plugin's own prefix, by the place whose history they keep — `/mounts/{mount}`, `/projects/{project}`
 * ({@link GitPlace}) —. Every route is decided inside, by the place's owner.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX + "/plugins/" + GitPlugin.ID + "/{places}/{place}")
public class GitController {

    private final PlaceGit git;

    /** A branch by its name. */
    public record BranchRequest(String name) {
    }

    /** A file, and the version of it the reader was shown. */
    public record RestoreRequest(String path, String version) {
    }

    public GitController(PlaceGit git) {
        this.git = git;
    }

    @GetMapping("/status")
    @PublicEndpoint("for whoever may read the place, checked in the service")
    public WorkingTreeStatus status(@PathVariable String places, @PathVariable String place) {
        return git.status(GitPlace.of(places, place));
    }

    @PostMapping("/initialize")
    @PublicEndpoint("only where the reader may change the place, checked in the service")
    public WorkingTreeStatus initialize(@PathVariable String places, @PathVariable String place) {
        return git.initialize(GitPlace.of(places, place));
    }

    @GetMapping("/log")
    @PublicEndpoint("for whoever may read the place, checked in the service")
    public List<CommitInfo> log(@PathVariable String places, @PathVariable String place, @RequestParam(defaultValue = "100") int limit) {
        return git.log(GitPlace.of(places, place), limit);
    }

    @GetMapping("/diff")
    @PublicEndpoint("for whoever may read the place, checked in the service")
    public WorkingTreeDiff diff(@PathVariable String places, @PathVariable String place, @RequestParam String path) {
        return git.diff(GitPlace.of(places, place), path);
    }

    @PostMapping("/commit")
    @PublicEndpoint("only where the reader may change the place, checked in the service")
    public CommitResult commit(@PathVariable String places, @PathVariable String place, @RequestBody CommitRequest request) {
        return git.commit(GitPlace.of(places, place), request);
    }

    @GetMapping("/commits/{id}")
    @PublicEndpoint("for whoever may read the place, checked in the service")
    public CommitDetail commitOf(@PathVariable String places, @PathVariable String place, @PathVariable String id) {
        return git.commitOf(GitPlace.of(places, place), id);
    }

    @GetMapping("/commits/{id}/diff")
    @PublicEndpoint("for whoever may read the place, checked in the service")
    public WorkingTreeDiff commitDiff(@PathVariable String places, @PathVariable String place, @PathVariable String id, @RequestParam String path) {
        return git.commitDiff(GitPlace.of(places, place), id, path);
    }

    @GetMapping("/branches")
    @PublicEndpoint("for whoever may read the place, checked in the service")
    public List<BranchInfo> branches(@PathVariable String places, @PathVariable String place) {
        return git.branches(GitPlace.of(places, place));
    }

    @PostMapping("/branches")
    @PublicEndpoint("only where the reader may change the place, checked in the service")
    public WorkingTreeStatus createBranch(@PathVariable String places, @PathVariable String place, @RequestBody BranchRequest request) {
        return git.createBranch(GitPlace.of(places, place), request.name());
    }

    @PostMapping("/merge")
    @PublicEndpoint("only where the reader may change the place, checked in the service")
    public PlaceGit.Merged mergeBranch(@PathVariable String places, @PathVariable String place, @RequestBody BranchRequest request) {
        return git.mergeBranch(GitPlace.of(places, place), request.name());
    }

    @PostMapping("/switch")
    @PublicEndpoint("only where the reader may change the place, checked in the service")
    public WorkingTreeStatus switchBranch(@PathVariable String places, @PathVariable String place, @RequestBody BranchRequest request) {
        return git.switchBranch(GitPlace.of(places, place), request.name());
    }

    @GetMapping("/remote")
    @PublicEndpoint("only where the reader may read the mount, checked in the service")
    public PlaceGit.RemoteView remote(@PathVariable String places, @PathVariable String place) {
        return git.remote(GitPlace.of(places, place));
    }

    @PostMapping("/remote")
    @PublicEndpoint("only where the reader may change the place, checked in the service")
    public PlaceGit.RemoteView setRemote(@PathVariable String places, @PathVariable String place, @RequestBody PlaceGit.RemoteRequest request) {
        return git.setRemote(GitPlace.of(places, place), request.address());
    }

    @PostMapping("/pull")
    @PublicEndpoint("only where the reader may change the place, checked in the service")
    public WorkingTreeStatus pull(@PathVariable String places, @PathVariable String place) {
        return git.pull(GitPlace.of(places, place));
    }

    @PostMapping("/push")
    @PublicEndpoint("only where the reader may change the place, checked in the service")
    public PushOutcome push(@PathVariable String places, @PathVariable String place) {
        return git.push(GitPlace.of(places, place));
    }

    @PostMapping("/restore")
    @PublicEndpoint("only where the reader may change the place, checked in the service")
    public WorkingTreeStatus restore(@PathVariable String places, @PathVariable String place, @RequestBody RestoreRequest request) {
        return git.restore(GitPlace.of(places, place), request.path(), request.version());
    }
}
