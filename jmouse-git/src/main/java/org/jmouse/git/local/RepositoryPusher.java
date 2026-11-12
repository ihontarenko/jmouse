package org.jmouse.git.local;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Where a repository's work goes, and sending it there.
 *
 * <p>⚠️ <strong>A folder's own {@code .git} is not the reader's to trust.</strong> JGit runs a {@code pre-push} hook, and
 * a repository's configuration can move an address ({@code url.*.insteadOf}) or its transport ({@code http.*}): a token
 * handed to such a push reaches whoever wrote that file. So a repository with a hook or an {@code http} section of its
 * own is refused, and the token is asked for only after the address is what the configuration resolved it to.</p>
 */
final class RepositoryPusher {

    private static final String ORIGIN = Constants.DEFAULT_REMOTE_NAME;
    /** The user name a token goes under: a provider reads the token, whichever name stands beside it. */
    private static final String TOKEN_USER = "x-access-token";
    private static final String PRE_PUSH = "pre-push";
    private static final String HTTP_SECTION = "http";

    /** Where the repository pushes to, as its configuration resolves it; empty for none. */
    Optional<String> remote(Path directory) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            return pushAddress(repository).map(RepositoryPusher::withoutCredentials);
        } catch (IOException | URISyntaxException failed) {
            throw JGitLocalGit.storage("The repository's remote could not be read.");
        }
    }

    synchronized void setRemote(Path directory, String address) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            URIish uri = new URIish(address == null ? "" : address.strip());
            if (uri.getPass() != null || uri.getUser() != null) {
                throw new LocalGitException(LocalGitException.Reason.INVALID_PATH, "A remote's address holds no name or password.");
            }
            StoredConfig config = repository.getConfig();
            config.setString(ConfigConstants.CONFIG_REMOTE_SECTION, ORIGIN, ConfigConstants.CONFIG_KEY_URL, uri.toString());
            config.setString(ConfigConstants.CONFIG_REMOTE_SECTION, ORIGIN, "fetch", "+refs/heads/*:refs/remotes/" + ORIGIN + "/*");
            config.save();
        } catch (URISyntaxException unreadable) {
            throw new LocalGitException(LocalGitException.Reason.INVALID_PATH, "'" + address + "' is no address of a repository.");
        } catch (IOException failed) {
            throw JGitLocalGit.storage("The remote could not be kept.");
        }
    }

    /**
     * The current branch sent to the remote; a rewrite of its history there is refused, never forced.
     *
     * @param tokenFor the token for the host the repository really pushes to; empty to send none
     */
    synchronized PushOutcome push(Path directory, Function<String, Optional<String>> tokenFor) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            requireNoOwnPushSetup(repository);
            String branch = repository.getBranch();
            if (repository.exactRef(Constants.R_HEADS + branch) == null) {
                throw new LocalGitException(LocalGitException.Reason.UNSUPPORTED_STATE, "Commit something first: there is nothing to send.");
            }
            URIish address = pushAddress(repository).orElseThrow(() -> new LocalGitException(
                    LocalGitException.Reason.NOT_FOUND, "This repository has no remote to send to."));
            var command = Git.wrap(repository).push().setRemote(ORIGIN).setForce(false)
                    .setRefSpecs(new RefSpec(Constants.R_HEADS + branch + ":" + Constants.R_HEADS + branch));
            Optional<String> token = address.getHost() == null ? Optional.empty() : tokenFor.apply(address.getHost());
            token.ifPresent(secret -> command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(TOKEN_USER, secret)));
            return outcomeOf(branch, address, command.call());
        } catch (GitAPIException refused) {
            throw new LocalGitException(LocalGitException.Reason.CONFLICT, "The remote refused it: " + withoutToken(refused.getMessage()));
        } catch (IOException | URISyntaxException failed) {
            throw JGitLocalGit.storage("The branch could not be sent.");
        }
    }

    /**
     * The remote's commits of the current branch taken in, only by moving forward: a history that went two ways is refused,
     * and so is a change in hand that the commits would overwrite. Nothing is merged, so nothing here is ever a merge commit.
     *
     * @param tokenFor the token for the host the address really goes to; empty to send none
     */
    synchronized PullOutcome pull(Path directory, Function<String, Optional<String>> tokenFor) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            requireNoOwnPushSetup(repository);
            WorkingTreeChanger.requireNoOwnFilter(repository);
            String branch = repository.getBranch();
            URIish address = pushAddress(repository).orElseThrow(() -> new LocalGitException(
                    LocalGitException.Reason.NOT_FOUND, "This repository has no remote to take from."));
            String tracking = Constants.R_REMOTES + ORIGIN + "/" + branch;
            var fetch = Git.wrap(repository).fetch().setRemote(ORIGIN)
                    .setRefSpecs(new RefSpec("+" + Constants.R_HEADS + branch + ":" + tracking));
            Optional<String> token = address.getHost() == null ? Optional.empty() : tokenFor.apply(address.getHost());
            token.ifPresent(secret -> fetch.setCredentialsProvider(new UsernamePasswordCredentialsProvider(TOKEN_USER, secret)));
            fetch.call();
            ObjectId theirs = repository.resolve(tracking);
            if (theirs == null) {
                throw new LocalGitException(LocalGitException.Reason.NOT_FOUND, "The remote has no branch " + branch + ".");
            }
            MergeResult merged = Git.wrap(repository).merge().include(theirs).setFastForward(MergeCommand.FastForwardMode.FF_ONLY).call();
            return switch (merged.getMergeStatus()) {
                case ALREADY_UP_TO_DATE -> new PullOutcome(branch, withoutCredentials(address), PullOutcome.State.ALREADY_HERE);
                case FAST_FORWARD -> new PullOutcome(branch, withoutCredentials(address), PullOutcome.State.TAKEN);
                case CHECKOUT_CONFLICT -> throw new LocalGitException(LocalGitException.Reason.CONFLICT,
                        "A change in hand stands in the way of what the remote has: commit it or take it back first. Nothing was changed.");
                case ABORTED -> throw new LocalGitException(LocalGitException.Reason.CONFLICT,
                        "This branch and the remote's went two ways: bring them together with Git itself. Nothing was changed.");
                default -> throw new LocalGitException(LocalGitException.Reason.CONFLICT, "It could not be taken in: " + merged.getMergeStatus() + ".");
            };
        } catch (GitAPIException refused) {
            throw new LocalGitException(LocalGitException.Reason.CONFLICT, "The remote refused it: " + withoutToken(refused.getMessage()));
        } catch (IOException | URISyntaxException failed) {
            throw JGitLocalGit.storage("The branch could not be taken.");
        }
    }

    private static PushOutcome outcomeOf(String branch, URIish address, Iterable<PushResult> results) {
        for (PushResult result : results) {
            for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                switch (update.getStatus()) {
                    case OK -> {
                        return new PushOutcome(branch, withoutCredentials(address), PushOutcome.State.SENT);
                    }
                    case UP_TO_DATE -> {
                        return new PushOutcome(branch, withoutCredentials(address), PushOutcome.State.ALREADY_THERE);
                    }
                    case REJECTED_NONFASTFORWARD, REJECTED_REMOTE_CHANGED -> throw new LocalGitException(LocalGitException.Reason.CONFLICT,
                            "The remote has commits this branch has not: take them first. Nothing was sent.");
                    default -> throw new LocalGitException(LocalGitException.Reason.CONFLICT, "The remote refused it"
                            + (update.getMessage() == null ? "." : ": " + withoutToken(update.getMessage())));
                }
            }
        }
        throw new LocalGitException(LocalGitException.Reason.CONFLICT, "The remote answered nothing about this branch.");
    }

    /** What the configuration really pushes to, its rewrites of an address applied. */
    private static Optional<URIish> pushAddress(Repository repository) throws URISyntaxException {
        RemoteConfig remote = new RemoteConfig(repository.getConfig(), ORIGIN);
        List<URIish> addresses = remote.getPushURIs().isEmpty() ? remote.getURIs() : remote.getPushURIs();
        return addresses.stream().findFirst();
    }

    private static void requireNoOwnPushSetup(Repository repository) throws IOException {
        if (Files.exists(repository.getDirectory().toPath().resolve(Constants.HOOKS).resolve(PRE_PUSH))) {
            throw new LocalGitException(LocalGitException.Reason.UNSUPPORTED_STATE,
                    "This repository has a pre-push hook, a program Git would run here: use Git itself to send it.");
        }
        FileBasedConfig own = new FileBasedConfig(new File(repository.getDirectory(), Constants.CONFIG), FS.DETECTED);
        try {
            own.load();
        } catch (ConfigInvalidException unreadable) {
            throw new LocalGitException(LocalGitException.Reason.UNSUPPORTED_STATE, "The repository's own configuration does not read.");
        }
        if (own.getString(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_HOOKS_PATH) != null
                || !own.getNames(HTTP_SECTION).isEmpty()
                || !own.getSubsections(HTTP_SECTION).isEmpty()) {
            throw new LocalGitException(LocalGitException.Reason.UNSUPPORTED_STATE,
                    "This repository's own configuration sets hooks or how it connects: use Git itself to send it.");
        }
    }

    private static String withoutCredentials(URIish address) {
        return address.setUser(null).setPass(null).toString();
    }

    /** What a transport says may quote the address it was given; only a host's own words are repeated. */
    private static String withoutToken(String message) {
        return message == null ? "" : message.replaceAll("://[^/@\\s]+@", "://");
    }
}
