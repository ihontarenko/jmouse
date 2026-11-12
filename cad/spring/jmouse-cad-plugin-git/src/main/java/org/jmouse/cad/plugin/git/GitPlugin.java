package org.jmouse.cad.plugin.git;

import org.jmouse.cad.plugin.explorer.ExplorerPlugin;
import org.jmouse.cad.plugin.explorer.ExplorerReserved;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;
import org.jmouse.plugin.PluginSetting;

import java.util.List;
import java.util.Set;

/**
 * Git in jmCAD: a project is a folder of documents under Git, and this plugin gives the explorer's folders a history —
 * made a repository, changes reviewed and committed with a message, the log and a file's changes shown. It works only
 * in the explorer's mounts and through its decisions, so it stands on the explorer: without it, there is no Git.
 */
public final class GitPlugin implements Plugin {

    public static final String ID = "cad-git";

    static final String GITHUB_TOKEN = "github-token";
    static final String GITLAB_TOKEN = "gitlab-token";
    static final String FIRST_BRANCH = "first-branch";

    private static final String HOSTS   = "Hosts";
    private static final String HISTORY = "History";

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Git", GitPlugin.class).markedBy(GitPlugin.class, "git.svg")
                .requiring(Set.of(ExplorerPlugin.ID));
    }

    @Override
    public List<PluginSetting> settings() {
        return List.of(
                PluginSetting.secret(GITHUB_TOKEN, "GitHub token",
                        "Sent only to github.com, when a branch is pushed: a fine-grained token with Contents, read and write. A commit stays under the reader's own name.", null).inGroup(HOSTS),
                PluginSetting.secret(GITLAB_TOKEN, "GitLab token",
                        "Sent only to gitlab.com, when a branch is pushed: a project or personal token with write_repository.", null).inGroup(HOSTS),
                PluginSetting.text(FIRST_BRANCH, "First branch", "What a folder's new history starts on.", "main").inGroup(HISTORY));
    }

    @Override
    public void register(PluginRegistration registration) {
        // A repository's own workings are Git's alone: the explorer neither shows them nor lets them be touched.
        registration.contribute(ExplorerReserved.POINT, new ExplorerReserved("git-repository", ".git"));
    }
}
