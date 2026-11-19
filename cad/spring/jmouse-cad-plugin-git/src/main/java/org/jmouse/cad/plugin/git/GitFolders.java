package org.jmouse.cad.plugin.git;

import org.jmouse.cad.plugin.explorer.Explorer;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.cad.store.project.CadProjects;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The folder of a place whose history Git keeps, read or changed only where its owner lets the reader: a mount where the explorer
 * does, a project where the engine does. Git decides nothing of who may do what; a place that is no folder of the disk — a store a
 * plugin brings, a project shipped in a jar — has no Git.
 */
public final class GitFolders {

    private final Explorer    explorer;
    private final CadProjects projects;

    public GitFolders(Explorer explorer, CadProjects projects) {
        this.explorer = explorer;
        this.projects = projects;
    }

    /** The place's folder, for whoever may read it. */
    public Path readable(GitPlace place) {
        Optional<Path> folder = switch (place.kind()) {
            case MOUNTS -> explorer.folderForReading(place.id());
            case PROJECTS -> projects.folderForReading(place.id());
        };
        return folder.orElseThrow(() -> noFolder(place));
    }

    /** The place's folder, for whoever may change it. */
    public Path changeable(GitPlace place) {
        Optional<Path> folder = switch (place.kind()) {
            case MOUNTS -> explorer.folderForChanging(place.id());
            case PROJECTS -> projects.folderForChanging(place.id());
        };
        return folder.orElseThrow(() -> noFolder(place));
    }

    private static CadProblem noFolder(GitPlace place) {
        return CadProblem.invalid("'" + place.id() + "' is not a folder of the server's disk, so it has no Git.");
    }
}
