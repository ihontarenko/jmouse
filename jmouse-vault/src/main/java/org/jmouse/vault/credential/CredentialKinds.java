package org.jmouse.vault.credential;

import org.jmouse.vault.credential.CredentialKind.Field;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 📚 The kinds an application knows: the standard ones below, plus whatever its libraries and the
 * product itself declare. A later declaration of the same name replaces an earlier one.
 */
public final class CredentialKinds {

    public static final String GENERIC = "generic";

    private final Map<String, CredentialKind> kinds = new LinkedHashMap<>();

    public CredentialKinds(Collection<CredentialKind> kinds) {
        kinds.forEach(kind -> this.kinds.put(kind.name(), kind));
    }

    /** 🧰 The shapes almost every product needs, before any library adds its own. */
    public static List<CredentialKind> standard() {
        return List.of(
                new CredentialKind(GENERIC, "Generic", "Any set of values; each one is marked secret or plain.",
                        List.of(), true),
                new CredentialKind("http-api", "HTTP API", "A service reached at a URL with a key.",
                        List.of(Field.plain("base-url", "Base URL", false, "Leave empty for the service's default."),
                                Field.secret("api-key", "API key", true, null)),
                        true),
                new CredentialKind("oauth-client", "OAuth client", "A client registered with an authorization server.",
                        List.of(Field.plain("client-id", "Client ID", true, null),
                                Field.secret("client-secret", "Client secret", true, null),
                                Field.plain("token-url", "Token URL", false, null)),
                        false),
                new CredentialKind("s3", "S3 storage", "An S3-compatible bucket.",
                        List.of(Field.plain("endpoint", "Endpoint", false, "Empty for AWS."),
                                Field.plain("region", "Region", false, null),
                                Field.plain("bucket", "Bucket", true, null),
                                Field.secret("access-key", "Access key", true, null),
                                Field.secret("secret-key", "Secret key", true, null)),
                        false),
                new CredentialKind("telegram-bot", "Telegram bot", "A bot token from @BotFather.",
                        List.of(Field.plain("api-base", "API base", false, "A self-hosted Bot API server; empty for Telegram's."),
                                Field.secret("token", "Bot token", true, null)),
                        false),
                new CredentialKind("smtp", "SMTP server", "Outgoing mail.",
                        List.of(Field.plain("host", "Host", true, null),
                                Field.plain("port", "Port", true, null),
                                Field.plain("username", "Username", false, null),
                                Field.secret("password", "Password", false, null)),
                        false));
    }

    public Optional<CredentialKind> find(String name) {
        return Optional.ofNullable(kinds.get(name));
    }

    public CredentialKind require(String name) {
        return find(name).orElseThrow(() -> new IllegalArgumentException(
                "'%s' is not a credential kind here; known: %s.".formatted(name, kinds.keySet())));
    }

    public List<CredentialKind> list() {
        return List.copyOf(kinds.values());
    }
}
