package org.jmouse.storage.configuration;

import org.jmouse.core.binding.BindDefault;

import java.time.Duration;

/**
 * ⏳ How long clients may hold on to what storage serves them.
 *
 * @param streamedPrivateMaxAge owner-only downloads served through the application — never shared,
 *                              so never public
 * @param streamedPublicMaxAge  publicly shared files served through the application
 * @param redirectSafetyMargin  held back from a redirect's advertised lifetime, so a link cached
 *                              just before its signature expires is not handed out already broken
 */
public record CacheSettings(@BindDefault(CacheSettings.DEFAULT_STREAMED_PRIVATE_MAX_AGE) Duration streamedPrivateMaxAge,
                            @BindDefault(CacheSettings.DEFAULT_STREAMED_PUBLIC_MAX_AGE) Duration streamedPublicMaxAge,
                            @BindDefault(CacheSettings.DEFAULT_REDIRECT_SAFETY_MARGIN) Duration redirectSafetyMargin) {

    /**
     * ⏳ One hour for owner-only downloads.
     */
    public static final String DEFAULT_STREAMED_PRIVATE_MAX_AGE = "PT1H";

    /**
     * ⏳ A year for publicly shared files.
     */
    public static final String DEFAULT_STREAMED_PUBLIC_MAX_AGE = "P365D";

    /**
     * ⏳ One minute held back from every redirect's advertised lifetime.
     */
    public static final String DEFAULT_REDIRECT_SAFETY_MARGIN = "PT1M";

    /**
     * 🏗️ Fill in whatever configuration omitted.
     */
    public CacheSettings {
        streamedPrivateMaxAge = (streamedPrivateMaxAge == null)
                ? Duration.parse(DEFAULT_STREAMED_PRIVATE_MAX_AGE) : streamedPrivateMaxAge;
        streamedPublicMaxAge  = (streamedPublicMaxAge == null)
                ? Duration.parse(DEFAULT_STREAMED_PUBLIC_MAX_AGE) : streamedPublicMaxAge;
        redirectSafetyMargin  = (redirectSafetyMargin == null)
                ? Duration.parse(DEFAULT_REDIRECT_SAFETY_MARGIN) : redirectSafetyMargin;
    }

    /**
     * 🏗️ The shipped defaults.
     *
     * @return default cache lifetimes
     */
    public static CacheSettings defaults() {
        return new CacheSettings(null, null, null);
    }
}
