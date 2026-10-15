package org.jmouse.vault.spring;

import org.jmouse.vault.SecretReference;
import org.springframework.core.convert.converter.Converter;

/** 🔗 Binds a property to a {@link SecretReference}: {@code vault:<name>} or a literal. */
public final class SecretReferenceConverter implements Converter<String, SecretReference> {

    @Override
    public SecretReference convert(String source) {
        return SecretReference.parse(source);
    }
}
