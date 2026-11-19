package org.jmouse.cad.plugin.git;

import org.jmouse.git.local.CommitAuthors;
import org.jmouse.cad.management.CadAccessRules;
import org.jmouse.cad.plugin.explorer.Explorer;
import org.jmouse.cad.store.project.CadProjects;
import org.jmouse.cad.plugin.explorer.ExplorerPluginConfiguration;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.git.local.JGitLocalGit;
import org.jmouse.settings.Settings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

/**
 * 🧩 The Git plugin, on the classpath beside the explorer's: its plugin, the service over the explorer's mounts and its
 * routes. A host that knows its people's names and addresses may give {@link CommitAuthors}; nothing else is asked.
 */
@AutoConfiguration(after = ExplorerPluginConfiguration.class)
public class GitPluginConfiguration {

    @Bean
    public GitPlugin cadGitPlugin() {
        return new GitPlugin();
    }

    @Bean
    public PlaceGit cadPlaceGit(Explorer explorer, CadProjects projects, CadAuthority authority, ObjectProvider<CommitAuthors> authors, Settings settings) {
        return new PlaceGit(new GitFolders(explorer, projects), new JGitLocalGit(), authority, Optional.ofNullable(authors.getIfAvailable()), settings.parameters(GitPlugin.ID, GitPlugin.HOSTS_SECTION),
                settings.parameters(GitPlugin.ID, GitPlugin.HISTORY_SECTION));
    }

    @Bean
    public GitController cadGitController(PlaceGit git) {
        CadAccessRules.requireDecidedInside(GitController.class);
        return new GitController(git);
    }
}
