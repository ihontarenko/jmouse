package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;
import org.jmouse.geo.Country;

import java.util.Optional;
import java.util.Set;

/**
 * 🌍 Converter from {@link String} to {@link Country}.
 *
 * <p>Tries to resolve an ISO code, name, or calling code into a {@code Country}.
 * Falls back to {@link #DEFAULT_COUNTRY} (🇺🇦 Ukraine) if resolution fails.</p>
 */
public class StringToCountryConverter implements GenericConverter<String, Country> {

    /** Default country if lookup fails (🇺🇦 Ukraine). */
    private static final Country DEFAULT_COUNTRY = Country.UA;

    /**
     * Converts a string into a {@link Country}.
     *
     * <p>Resolution is case-insensitive and supports multiple identifiers.</p>
     *
     * <pre>{@code
     * StringToCountryConverter c = new StringToCountryConverter();
     * // Ukraine
     * Country ua = c.convert("UA", String.class, Country.class);
     * // United States
     * Country us = c.convert("840", String.class, Country.class);
     * // Defaults to UA
     * Country defaultCountry = c.convert("???", String.class, Country.class);
     * }</pre>
     *
     * @param source     input string (ISO alpha-2, alpha-3, numeric, or name)
     * @param sourceType always {@code String.class}
     * @param targetType always {@code Country.class}
     * @return resolved country or {@link #DEFAULT_COUNTRY} if not found
     */
    @Override
    public Country convert(String source, Class<String> sourceType, Class<Country> targetType) {
        Country           country  = DEFAULT_COUNTRY;
        Optional<Country> optional = Country.of(source);

        if (optional.isPresent()) {
            country = optional.get();
        }

        return country;
    }

    /**
     * Returns the supported type mapping: {@code String → Country}.
     *
     * @return singleton set with {@link ClassPair#of(Class, Class)}
     */
    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                ClassPair.of(String.class, Country.class)
        );
    }

}
