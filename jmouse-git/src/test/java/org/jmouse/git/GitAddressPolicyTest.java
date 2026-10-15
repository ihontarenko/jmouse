package org.jmouse.git;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Address rules, credential redaction and value validation; none of these touch the network. */
class GitAddressPolicyTest {

    private static final GitCredentials.AccessToken TOKEN = GitCredentials.accessToken("x-access-token", "ghp_secretTokenValue123");

    @Test
    void theDefaultPolicyAdmitsHttpsOnly() {
        assertEquals(URI.create("https://github.com/owner/name.git"),
                GitAddressPolicy.HTTPS.requireAllowed(GitRemote.anonymous(URI.create("https://github.com/owner/name.git"))));
        for (String address : new String[]{
                "http://github.com/owner/name.git",
                "file:///tmp/repository",
                "ssh://git@github.com/owner/name.git",
                "https://user:password@github.com/owner/name.git",
                "https://github.com/owner/name.git?ref=main",
                "https://github.com/owner/name.git#main",
                "https://github.com/",
                "github.com/owner/name.git"}) {
            assertRefused(GitAddressPolicy.HTTPS, GitRemote.anonymous(URI.create(address)), address);
        }
    }

    @Test
    void aTokenTravelsOverHttpsOnlyWhateverThePolicyAdmits() {
        GitAddressPolicy permissive = new GitAddressPolicy(Set.of(GitScheme.HTTPS, GitScheme.HTTP), false);
        GitRemote overHttp = new GitRemote(URI.create("http://git.example.test/owner/name.git"), TOKEN);

        GitException failure = assertThrows(GitException.class, () -> permissive.requireAllowed(overHttp));

        assertEquals(GitException.Reason.ADDRESS_NOT_ALLOWED, failure.reason());
        assertFalse(failure.getMessage().contains(TOKEN.token()));
        assertDoesNotThrow(() -> permissive.requireAllowed(GitRemote.anonymous(overHttp.address())));
    }

    @Test
    void aPublicOnlyPolicyRefusesPrivateAndLocalHosts() {
        for (String address : new String[]{
                "https://127.0.0.1/owner/name.git",
                "https://10.0.0.8/owner/name.git",
                "https://192.168.1.20/owner/name.git",
                "https://172.16.4.4/owner/name.git",
                "https://169.254.169.254/owner/name.git",
                "https://100.64.0.1/owner/name.git",
                "https://[::1]/owner/name.git",
                "https://[fd00::1]/owner/name.git"}) {
            assertRefused(GitAddressPolicy.PUBLIC_HTTPS, GitRemote.anonymous(URI.create(address)), address);
        }
        assertDoesNotThrow(() -> GitAddressPolicy.PUBLIC_HTTPS.requireAllowed(
                GitRemote.anonymous(URI.create("https://8.8.8.8/owner/name.git"))));
        GitAddressPolicy localButPublicOnly = new GitAddressPolicy(Set.of(GitScheme.FILE), true);
        assertRefused(localButPublicOnly, GitRemote.anonymous(URI.create("file:///tmp/repository")), "file under public-only");
    }

    @Test
    void publicAddressClassification() throws Exception {
        assertTrue(GitAddressPolicy.isPublic(InetAddress.getByName("140.82.112.3")));
        assertTrue(GitAddressPolicy.isPublic(InetAddress.getByName("2606:4700::1111")));
        assertFalse(GitAddressPolicy.isPublic(InetAddress.getByName("0.1.2.3")));
        assertFalse(GitAddressPolicy.isPublic(InetAddress.getByName("198.18.0.1")));
        assertFalse(GitAddressPolicy.isPublic(InetAddress.getByName("240.0.0.1")));
        assertFalse(GitAddressPolicy.isPublic(InetAddress.getByName("192.0.0.5")));
    }

    @Test
    void credentialsNeverShowTheirToken() {
        GitRemote remote = new GitRemote(URI.create("https://github.com/owner/name.git"), TOKEN);

        assertFalse(TOKEN.toString().contains(TOKEN.token()));
        assertTrue(TOKEN.toString().contains("x-access-token"));
        assertFalse(remote.toString().contains(TOKEN.token()));
        IllegalArgumentException badUsername = assertThrows(IllegalArgumentException.class,
                () -> GitCredentials.accessToken("user:name", TOKEN.token()));
        assertFalse(badUsername.getMessage().contains(TOKEN.token()));
        IllegalArgumentException badToken = assertThrows(IllegalArgumentException.class,
                () -> GitCredentials.accessToken("oauth2", "with space"));
        assertFalse(badToken.getMessage().contains("with space"));
        assertEquals("Anonymous", GitCredentials.anonymous().toString());
    }

    @Test
    void branchNamesAreTheOnesGitAccepts() {
        assertEquals("release/2.0", BranchSelection.named("release/2.0").name());
        for (String name : new String[]{"-rf", "a..b", "x.lock", "ends/", "double//slash", "", "has space", "~tilde", "a@{1}"}) {
            assertThrows(IllegalArgumentException.class, () -> BranchSelection.named(name), name);
        }
    }

    @Test
    void limitsAreExplicitAndBounded() {
        assertEquals(1, GitLimits.DEFAULT.depth());
        assertThrows(IllegalArgumentException.class, () -> GitLimits.DEFAULT.withDepth(0));
        assertThrows(IllegalArgumentException.class, () -> GitLimits.DEFAULT.withTimeout(Duration.ofMillis(10)));
        assertThrows(IllegalArgumentException.class, () -> GitLimits.DEFAULT.withMaximumBytes(10));
        assertThrows(IllegalArgumentException.class, () -> new CommitId("ABC"));
    }

    private static void assertRefused(GitAddressPolicy policy, GitRemote remote, String description) {
        GitException failure = assertThrows(GitException.class, () -> policy.requireAllowed(remote), description);
        assertEquals(GitException.Reason.ADDRESS_NOT_ALLOWED, failure.reason(), description);
    }
}
