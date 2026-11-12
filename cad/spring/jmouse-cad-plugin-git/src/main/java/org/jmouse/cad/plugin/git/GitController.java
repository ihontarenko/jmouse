package org.jmouse.cad.plugin.git;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.management.CadManagementRoutes;
import org.jmouse.cad.plugin.git.ExplorerGit.CommitRequest;
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
 * 🌿 Git's routes, under the plugin's own prefix and by the explorer's mount ids. Every route is decided inside, by the
 * explorer's decision on the mount.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX + "/plugins/" + GitPlugin.ID + "/mounts/{mount}")
public class GitController {

    private final ExplorerGit git;

    /** A branch by its name. */
    public record BranchRequest(String name) {
    }

    /** A file, and the version of it the reader was shown. */
    public record RestoreRequest(String path, String version) {
    }

    public GitController(ExplorerGit git) {
        this.git = git;
    }

    @GetMapping("/status")
    @PublicEndpoint("for whoever may read the mount, checked in the service")
    public WorkingTreeStatus status(@PathVariable String mount) {
        return git.status(mount);
    }

    @PostMapping("/initialize")
    @PublicEndpoint("only where the reader may change the mount, checked in the service")
    public WorkingTreeStatus initialize(@PathVariable String mount) {
        return git.initialize(mount);
    }

    @GetMapping("/log")
    @PublicEndpoint("for whoever may read the mount, checked in the service")
    public List<CommitInfo> log(@PathVariable String mount, @RequestParam(defaultValue = "100") int limit) {
        return git.log(mount, limit);
    }

    @GetMapping("/diff")
    @PublicEndpoint("for whoever may read the mount, checked in the service")
    public WorkingTreeDiff diff(@PathVariable String mount, @RequestParam String path) {
        return git.diff(mount, path);
    }

    @PostMapping("/commit")
    @PublicEndpoint("only where the reader may change the mount, checked in the service")
    public CommitResult commit(@PathVariable String mount, @RequestBody CommitRequest request) {
        return git.commit(mount, request);
    }

    @GetMapping("/commits/{id}")
    @PublicEndpoint("for whoever may read the mount, checked in the service")
    public CommitDetail commitOf(@PathVariable String mount, @PathVariable String id) {
        return git.commitOf(mount, id);
    }

    @GetMapping("/commits/{id}/diff")
    @PublicEndpoint("for whoever may read the mount, checked in the service")
    public WorkingTreeDiff commitDiff(@PathVariable String mount, @PathVariable String id, @RequestParam String path) {
        return git.commitDiff(mount, id, path);
    }

    @GetMapping("/branches")
    @PublicEndpoint("for whoever may read the mount, checked in the service")
    public List<BranchInfo> branches(@PathVariable String mount) {
        return git.branches(mount);
    }

    @PostMapping("/branches")
    @PublicEndpoint("only where the reader may change the mount, checked in the service")
    public WorkingTreeStatus createBranch(@PathVariable String mount, @RequestBody BranchRequest request) {
        return git.createBranch(mount, request.name());
    }

    @PostMapping("/switch")
    @PublicEndpoint("only where the reader may change the mount, checked in the service")
    public WorkingTreeStatus switchBranch(@PathVariable String mount, @RequestBody BranchRequest request) {
        return git.switchBranch(mount, request.name());
    }

    @GetMapping("/remote")
    @PublicEndpoint("only where the reader may read the mount, checked in the service")
    public ExplorerGit.RemoteView remote(@PathVariable String mount) {
        return git.remote(mount);
    }

    @PostMapping("/remote")
    @PublicEndpoint("only where the reader may change the mount, checked in the service")
    public ExplorerGit.RemoteView setRemote(@PathVariable String mount, @RequestBody ExplorerGit.RemoteRequest request) {
        return git.setRemote(mount, request.address());
    }

    @PostMapping("/pull")
    @PublicEndpoint("only where the reader may change the mount, checked in the service")
    public WorkingTreeStatus pull(@PathVariable String mount) {
        return git.pull(mount);
    }

    @PostMapping("/push")
    @PublicEndpoint("only where the reader may change the mount, checked in the service")
    public PushOutcome push(@PathVariable String mount) {
        return git.push(mount);
    }

    @PostMapping("/restore")
    @PublicEndpoint("only where the reader may change the mount, checked in the service")
    public WorkingTreeStatus restore(@PathVariable String mount, @RequestBody RestoreRequest request) {
        return git.restore(mount, request.path(), request.version());
    }
}
