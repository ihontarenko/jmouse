package org.jmouse.vault.credential;

import org.jmouse.vault.SecretName;

import java.util.List;
import java.util.Optional;

/** 📦 One place credential definitions come from; asked in order, like vault sources. */
public interface CredentialSource {

    String name();

    Optional<CredentialDefinition> find(SecretName name);

    List<CredentialDefinition> list();
}
