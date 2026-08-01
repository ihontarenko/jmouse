package org.jmouse.storage.configuration;

import org.jmouse.core.binding.BindDefault;

import java.time.Duration;

/**
 * 🪣 Everything an S3-compatible backend needs to connect, for every provider that speaks S3.
 *
 * <p>Pure data. Reading these fields <em>means</em> something only alongside the active
 * {@link StorageProvider} — which endpoint applies, which region to fall back to, whether
 * addressing is path-style — so all of that lives on {@link StorageSettings}, which holds both
 * halves. Splitting it the other way would have this record reaching into an enum it does not own.</p>
 *
 * <p>Lives in the core module although only the object-store backend reads it: settings are one
 * record so that adding a backend does not mean reopening the settings type, and none of these
 * fields drag an SDK onto anyone's classpath.</p>
 *
 * @param bucket             bucket holding every object
 * @param region             region to sign with; falls back to the provider's default
 * @param endpoint           service endpoint; required for MinIO, derived for Supabase, unused for AWS
 * @param publicEndpoint     endpoint direct links are signed against, when it differs from
 *                           {@link #endpoint}
 * @param accessKey          access key identifier
 * @param secretKey          secret access key
 * @param pathStyleAccess    overrides the provider's addressing style; leave unset
 * @param supabaseProjectUrl Supabase project URL, e.g. {@code https://abcdefgh.supabase.co}
 * @param linkTimeToLive     how long a direct link stays valid
 */
public record S3Settings(String bucket, String region, String endpoint, String publicEndpoint,
                         String accessKey, String secretKey, Boolean pathStyleAccess,
                         String supabaseProjectUrl,
                         @BindDefault(S3Settings.DEFAULT_LINK_TIME_TO_LIVE) Duration linkTimeToLive) {

    /**
     * ⏱️ How long a direct link stays valid unless configured otherwise.
     *
     * <p>Raising it weakens revocation; lowering it costs cache hits, because a redirect may never
     * be cached for longer than the signature it points at.</p>
     */
    public static final String DEFAULT_LINK_TIME_TO_LIVE = "PT15M";

    /**
     * 🏗️ Fill in whatever configuration omitted.
     */
    public S3Settings {
        linkTimeToLive = (linkTimeToLive == null) ? Duration.parse(DEFAULT_LINK_TIME_TO_LIVE) : linkTimeToLive;
    }

    /**
     * 🏗️ Empty settings, for a deployment that never touches an object store.
     *
     * @return unconfigured S3 settings
     */
    public static S3Settings none() {
        return new S3Settings(null, null, null, null, null, null, null, null, null);
    }
}
