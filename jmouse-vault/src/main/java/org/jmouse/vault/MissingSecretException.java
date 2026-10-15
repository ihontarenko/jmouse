package org.jmouse.vault;

import java.util.List;

/** 🚫 A required secret no source holds. The message names every place that was looked at. */
public class MissingSecretException extends RuntimeException {

    private final SecretName name;

    public MissingSecretException(SecretName name, List<String> lookedAt) {
        super("The secret '%s' is not configured. Looked at: %s.".formatted(name, String.join(", ", lookedAt)));
        this.name = name;
    }

    public SecretName getName() {
        return name;
    }
}
