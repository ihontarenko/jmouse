package org.jmouse.storage.s3;

import org.jmouse.core.MediaType;
import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceSegment;
import org.jmouse.http.ContentDisposition;
import org.jmouse.http.Range;
import org.jmouse.storage.Content;
import org.jmouse.storage.ContentTypes;
import org.jmouse.storage.DirectLink;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.ObjectDescription;
import org.jmouse.storage.Presentation;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.StoredObject;
import org.jmouse.storage.configuration.S3Settings;
import org.jmouse.storage.configuration.BackendSettings;
import org.jmouse.storage.exception.ObjectNotFoundException;
import org.jmouse.storage.exception.StorageException;
import org.jmouse.storage.support.ByteRanges;
import org.jmouse.storage.support.ContentDigests;
import org.jmouse.storage.support.Digested;
import org.jmouse.storage.support.TemporaryFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/**
 * 🪣 The {@link FileStore} for every S3-compatible provider: AWS S3, self-hosted MinIO and Supabase
 * Storage.
 *
 * <p>One class, not three. The providers speak the same protocol and differ only in endpoint,
 * addressing style and region — all of which arrive as configuration with a per-provider default,
 * so adding a fourth is an entry in {@link org.jmouse.storage.configuration.StorageProvider} rather
 * than a branch in here. There is deliberately no {@code if (provider == …)} anywhere below.</p>
 *
 * <p>Object keys are the {@link StorageKey} verbatim, so a bucket's layout mirrors what the local
 * backend would have written to disk and an object can move between the two without rewriting the
 * key already persisted against it. Key safety is not re-checked here because it cannot be
 * bypassed: {@code StorageKey} validates on construction, and an object store — which has no notion
 * of {@code ..} and will cheerfully create an object literally named {@code ../../etc/passwd} —
 * needs that guarantee at least as much as local disk does.</p>
 *
 * <p>Serving happens through {@link #resolveDirectLink}: clients are redirected to a presigned URL
 * and fetch bytes from the provider, which keeps large downloads out of this application's heap and
 * threads. {@link #read} remains for callers that genuinely need the bytes here.</p>
 *
 * <p>Holds an SDK client and a presigner, both of which own connection pools — hence
 * {@link AutoCloseable}. Close it with whatever context constructed it.</p>
 */
public class S3FileStore implements FileStore, AutoCloseable {

    private static final Logger LOGGER              = LoggerFactory.getLogger(S3FileStore.class);
    private static final String TEMPORARY_PREFIX    = "jmouse-storage-s3-";
    private static final String TEMPORARY_SUFFIX    = ".upload";
    private static final String RANGE_HEADER_FORMAT = "bytes=%d-%d";
    private static final int    NOT_FOUND           = 404;

    private final String      backendName;
    private final S3Client    client;
    private final S3Presigner presigner;
    private final String      bucket;
    private final Duration    linkTimeToLive;

    /**
     * 🏗️ Connect to the object store a backend describes.
     *
     * <p>Settings are validated before anything is built, so a deployment missing a bucket or a
     * secret key fails here — naming the setting — rather than hours later on the first upload,
     * where the same mistake surfaces as an opaque SDK error.</p>
     *
     * <p>The backend's name is what every object written here records, and what routes a later
     * read back to this store rather than to whichever store happens to be the default. Two object
     * stores — a live bucket and an archive one — therefore coexist without either shadowing the
     * other's objects.</p>
     *
     * @param backend the backend definition
     * @throws StorageException when the configuration could not possibly work
     */
    public S3FileStore(BackendSettings backend) {
        backend.validate();

        S3Settings s3 = backend.s3();

        this.backendName    = backend.name();
        this.bucket         = s3.bucket();
        this.linkTimeToLive = s3.linkTimeToLive();

        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3.accessKey(), s3.secretKey()));
        Region          region         = Region.of(backend.resolveRegion());
        String          endpoint       = backend.resolveEndpoint();
        String          publicEndpoint = backend.resolvePublicEndpoint();
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(backend.resolvePathStyleAccess())
                .build();

        S3ClientBuilder clientBuilder = S3Client.builder()
                .region(region).credentialsProvider(credentials).serviceConfiguration(serviceConfiguration);
        if (endpoint != null) {
            clientBuilder.endpointOverride(URI.create(endpoint));
        }

        // The presigner signs against the endpoint the BROWSER will reach, which may differ from the
        // one this application talks to (internal Docker hostname vs. public URL). The host is part
        // of the signature, so a link cannot be rewritten afterwards — it has to be signed for the
        // public host up front or every redirect points clients at a host they cannot resolve.
        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(region).credentialsProvider(credentials).serviceConfiguration(serviceConfiguration);
        if (publicEndpoint != null) {
            presignerBuilder.endpointOverride(URI.create(publicEndpoint));
        }

        this.client    = clientBuilder.build();
        this.presigner = presignerBuilder.build();

        LOGGER.info("Storage backend '{}' provider={} bucket={} region={} endpoint={} publicEndpoint={} "
                            + "pathStyle={} linkTimeToLive={}",
                    backendName, backend.provider(), bucket, region,
                    (endpoint != null) ? endpoint : "<sdk default>",
                    (publicEndpoint != null) ? publicEndpoint : "<same as endpoint>",
                    backend.resolvePathStyleAccess(), linkTimeToLive);
    }

    @Override
    public String backendName() {
        return backendName;
    }

    /**
     * 📝 Write content, spooling it to a temporary file first.
     *
     * <p>The spool is not incidental: a put must declare its content length up front and a stream
     * cannot answer that. Spooling buys the exact length, gives the SDK a repeatable source it can
     * retry an upload from, and costs one disk round-trip. The digest is taken in the same pass, so
     * content identity costs no second read either.</p>
     */
    @Override
    public StoredObject write(StorageKey key, Content content) {
        MediaType contentType = ContentTypes.resolve(content, key);
        Path      temporary   = null;

        try {
            temporary = Files.createTempFile(TEMPORARY_PREFIX, TEMPORARY_SUFFIX);

            Digested digested = ContentDigests.copyTo(content, temporary);

            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key.value())
                            .contentType(contentType.toString())
                            .contentLength(digested.sizeBytes())
                            .build(),
                    RequestBody.fromFile(temporary));

            return new StoredObject(key, digested.sizeBytes(), contentType, digested.sha256(), backendName);
        } catch (IOException | SdkException exception) {
            throw new StorageException("Failed to write '%s': %s".formatted(key, exception.getMessage()), exception);
        } finally {
            TemporaryFiles.discard(temporary);
        }
    }

    @Override
    public Resource read(StorageKey key) {
        ResponseInputStream<GetObjectResponse> objectStream = fetch(
                GetObjectRequest.builder().bucket(bucket).key(key.value()), key);

        return new S3ObjectResource(key, objectStream, objectStream.response().contentLength());
    }

    /**
     * ✂️ Fetch only the requested bytes.
     *
     * <p>A real ranged request rather than a whole object with most of it discarded — the point of
     * a range against an object store being that the bytes never cross the network. The object's
     * length is established first, because a range is only meaningful against it and because the
     * segment has to report the whole object's length for a {@code Content-Range} header to be
     * truthful.</p>
     */
    @Override
    public ResourceSegment readRange(StorageKey key, Range range) {
        ByteRanges.ByteRange resolved = ByteRanges.resolve(range, key, describe(key).sizeBytes());

        ResponseInputStream<GetObjectResponse> objectStream = fetch(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key.value())
                        .range(RANGE_HEADER_FORMAT.formatted(resolved.start(), resolved.end())),
                key);

        S3ObjectResource resource = new S3ObjectResource(
                key, objectStream, resolved.sizeBytes(), resolved.start());

        return ResourceSegment.ofRange(resolved.start(), resource, resolved.length());
    }

    @Override
    public ObjectDescription describe(StorageKey key) {
        try {
            HeadObjectResponse head = client.headObject(
                    HeadObjectRequest.builder().bucket(bucket).key(key.value()).build());

            return new ObjectDescription(key, head.contentLength(), describeContentType(key, head.contentType()));
        } catch (SdkException exception) {
            throw translate("inspect", key, exception);
        }
    }

    @Override
    public Optional<DirectLink> resolveDirectLink(StorageKey key, Presentation presentation) {
        // The provider serves these bytes without consulting us, so the response headers a client
        // would have received from the application are signed into the URL itself. Presentation must
        // be identical whether a file arrives by redirect or by stream.
        GetObjectRequest getObject = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key.value())
                .responseContentType(headerValue(presentation.contentType()))
                .responseContentDisposition(headerValue(presentation.contentDisposition()))
                .build();

        try {
            URI location = presigner.presignGetObject(GetObjectPresignRequest.builder()
                                                              .signatureDuration(linkTimeToLive)
                                                              .getObjectRequest(getObject)
                                                              .build())
                    .url()
                    .toURI();

            return Optional.of(new DirectLink(location, linkTimeToLive));
        } catch (URISyntaxException | RuntimeException exception) {
            // A direct link is an optimisation, never a requirement. Streaming is always correct, so
            // a failed presign degrades the transfer rather than the request.
            LOGGER.warn("Could not presign a direct link for '{}', falling back to streaming", key, exception);
            return Optional.empty();
        }
    }

    /**
     * 🗑️ Remove the object, reporting failure only to the log.
     *
     * <p>Deleting an object that is already gone is a success here as it is on local disk — the
     * store answers a delete for a missing key with 204 either way. Nothing escapes: deletion is
     * what a caller runs while cleaning up after something else went wrong, and a cleanup that can
     * itself throw turns one failure into two.</p>
     */
    @Override
    public void delete(StorageKey key) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key.value()).build());
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not delete '{}' from bucket '{}'", key, bucket, exception);
        }
    }

    /**
     * 🔌 Release the client and the presigner, and their connection pools with them.
     *
     * <p>Both are closed even if the first one fails, because leaking a pool to report a problem
     * closing another one helps nobody.</p>
     */
    @Override
    public void close() {
        try {
            client.close();
        } finally {
            presigner.close();
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * 📥 Open an object, mapping a missing key onto the library's own exception.
     *
     * @param request builder for the object to fetch, ranged or whole
     * @param key     key being read, for the error message
     * @return the open response stream
     */
    private ResponseInputStream<GetObjectResponse> fetch(GetObjectRequest.Builder request, StorageKey key) {
        try {
            return client.getObject(request.build());
        } catch (SdkException exception) {
            throw translate("read", key, exception);
        }
    }

    /**
     * 🎨 Establish the content type of a stored object.
     *
     * <p>Unlike local disk, an object store records a type alongside the bytes, and that is the type
     * it will serve — so it answers first. It is ignored when it says nothing in particular, which
     * is what a store reports for an object some other tool uploaded without declaring one; the
     * key's extension is a better answer than "unknown" in that case.</p>
     *
     * @param key      key of the stored object
     * @param recorded type the store reported, possibly {@code null}
     * @return a content type, never {@code null}
     */
    private MediaType describeContentType(StorageKey key, String recorded) {
        MediaType reported = ContentTypes.parse(recorded);
        boolean   specific = reported != null
                && !ContentTypes.baseType(ContentTypes.DEFAULT).equals(ContentTypes.baseType(reported));

        return specific ? reported : ContentTypes.forKey(key);
    }

    /**
     * 🔁 Turn an SDK failure into the library's own vocabulary.
     *
     * @param operation the operation being attempted, for the message
     * @param key       key it was attempted against
     * @param exception the SDK failure
     * @return the exception to throw
     */
    private RuntimeException translate(String operation, StorageKey key, SdkException exception) {
        if (exception instanceof NoSuchKeyException) {
            return new ObjectNotFoundException(key);
        }

        // Older SDK versions, and some S3-compatible stores, answer a missing key on HEAD with a
        // bare 404 rather than the typed exception — the same fact, reported differently.
        if (exception instanceof S3Exception s3Exception && s3Exception.statusCode() == NOT_FOUND) {
            return new ObjectNotFoundException(key);
        }

        return new StorageException(
                "Failed to %s '%s': %s".formatted(operation, key, exception.getMessage()), exception);
    }

    /**
     * 🏷️ Render a content type for the signed link, or nothing when the caller stated none.
     *
     * @param contentType type the caller wants served, may be {@code null}
     * @return the header value, or {@code null}
     */
    private static String headerValue(MediaType contentType) {
        return (contentType == null) ? null : contentType.toString();
    }

    /**
     * 🏷️ Render a disposition for the signed link, or nothing when the caller stated none.
     *
     * @param contentDisposition disposition the caller wants served, may be {@code null}
     * @return the header value, or {@code null}
     */
    private static String headerValue(ContentDisposition contentDisposition) {
        return (contentDisposition == null) ? null : contentDisposition.toString();
    }
}
