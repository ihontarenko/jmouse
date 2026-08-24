package org.jmouse.avatar;

import org.jmouse.storage.Content;
import org.jmouse.storage.ContentTypes;
import org.jmouse.storage.delivery.DeliveryIntent;
import org.jmouse.storage.delivery.DeliveryPlan;
import org.jmouse.storage.exception.UploadRejectedException;
import org.jmouse.storage.jpa.StoredFile;
import org.jmouse.storage.jpa.StoredFileDelivery;
import org.jmouse.storage.jpa.StoredFileIngestion;
import org.jmouse.storage.jpa.StoredFileRegistry;
import org.jmouse.storage.key.StorageKeyRequest;

import java.util.Locale;
import java.util.Set;

/**
 * 🙂 A member's face: choosing one, uploading one, dropping back to initials, serving the bytes.
 *
 * <p>Two products carried this as the same file — 178 lines in one, 208 in the other, and the whole
 * difference between them was a size check one had added. Everything about the bytes was already the
 * library's; what stayed behind was the part that decides which of three kinds is worn, and that part
 * turned out not to be product-specific at all.</p>
 *
 * <h3>⚠️ Nothing is ever deleted</h3>
 *
 * <p>Replacing or clearing a picture unbinds the owner from an object and stops there. Keys are
 * content-addressed, so two people who upload the same image share one row, and deleting it because one
 * of them moved on takes the other's face away too. Reclaiming what nothing points at is the sweeper's,
 * and it discovers the product's avatar column on its own.</p>
 *
 * <h3>⚠️ Two rules stated here rather than in configuration, on purpose</h3>
 *
 * <p>The installation's upload policy says what may enter storage at all. These two say what an
 * <strong>avatar</strong> may be, which is a strictly smaller thing — raster images only, and small.
 * Both matter: the type rule is the second half of what makes the unauthenticated byte route safe, and
 * the size rule stopped being implied by the installation's the moment a product raised its ceiling to
 * fit documents.</p>
 *
 * <h3>What a generated face is stored as</h3>
 *
 * <p>A descriptor, not a bare seed — {@link AvatarDescriptors} states the shape and the reason a bare
 * seed nonetheless stays valid forever.</p>
 */
public class AvatarService {

    /** Logical content class of avatars, and the prefix their keys sit under. */
    public static final String NAMESPACE = "avatars";

    /**
     * What an avatar may be.
     *
     * <p>⚠️ <strong>Raster only, and SVG is absent on purpose.</strong> It is an image by every
     * intuition and a script host by specification, and the public byte route serves whatever was
     * stored under the type it was stored as. If a script host could be uploaded, that route is where
     * it would be executed from.</p>
     */
    private static final Set<String> ACCEPTED_TYPES =
        Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    private final StoredFileIngestion ingestion;
    private final StoredFileRegistry  registry;
    private final StoredFileDelivery  delivery;
    private final long                maximumSizeBytes;

    /**
     * 🏗️ Build the face surface.
     *
     * @param ingestion        the write path into storage
     * @param registry         where a stored avatar is looked up by its address
     * @param delivery         the read path out of storage
     * @param maximumSizeBytes the largest picture a face may be
     */
    public AvatarService(StoredFileIngestion ingestion, StoredFileRegistry registry,
                         StoredFileDelivery delivery, long maximumSizeBytes) {
        this.ingestion        = ingestion;
        this.registry         = registry;
        this.delivery         = delivery;
        this.maximumSizeBytes = maximumSizeBytes;
    }

    /**
     * 🎲 Wear a generated face.
     *
     * <p>⚠️ The value is a <strong>descriptor</strong> — the strategy that draws the face, the seed it
     * is drawn from, and that strategy's settings. It used to be a bare seed, and a bare seed is still
     * accepted forever: see {@link AvatarDescriptors}.</p>
     *
     * @param owner      whose face
     * @param descriptor what draws it
     */
    public void choosePreset(AvatarOwner owner, String descriptor) {
        owner.wearsPreset(AvatarDescriptors.validated(descriptor));
    }

    /**
     * 🖼️ Wear an uploaded picture.
     *
     * <p>The owner is not part of the storage key beyond its namespace, because content-addressed keys
     * mean the key is a digest: two people who upload the same picture reach the same object and the
     * second upload writes nothing at all.</p>
     *
     * @param owner   whose face
     * @param content the picture
     */
    public void uploadPicture(AvatarOwner owner, Content content) {
        ensurePicture(content);
        ensureSmallEnough(content);

        StorageKeyRequest request = StorageKeyRequest.forContent(owner.avatarOwnerId(), content)
            .namespace(NAMESPACE)
            .build();

        owner.wearsPicture(ingestion.ingest(request, content));
    }

    /**
     * ✍️ Drop back to drawn initials.
     *
     * @param owner whose face
     */
    public void clear(AvatarOwner owner) {
        owner.wearsInitials();
    }

    /**
     * 🚚 How the bytes of a stored avatar should reach a client.
     *
     * <p>⚠️ Addressed by the <strong>stored object</strong> rather than by the person wearing it, which
     * is what makes the route cacheable forever and safe to serve unauthenticated. It also means a
     * picture somebody has since replaced keeps resolving, which is correct: a page rendered a moment
     * ago should not develop holes.</p>
     *
     * @param storedFileId the stored object's registry identifier
     * @param intent       what the client asked for
     * @return the delivery plan
     */
    public DeliveryPlan planDelivery(String storedFileId, DeliveryIntent intent) {
        StoredFile storedFile = registry.find(storedFileId)
            .orElseThrow(() -> new AvatarNotFoundException(storedFileId));

        return delivery.plan(storedFile, intent);
    }

    /**
     * 🖼️ Refuse anything that is not a raster image, before the bytes are written.
     *
     * <p>Judged on the declared type stripped of its parameters, which is what the library's own policy
     * compares too — {@code image/png; charset=binary} is a PNG.</p>
     */
    private void ensurePicture(Content content) {
        String declared = ContentTypes.baseType(content.declaredContentType());

        if (declared == null || !ACCEPTED_TYPES.contains(declared.toLowerCase(Locale.ROOT))) {
            throw new UploadRejectedException(
                "An avatar has to be a PNG, JPEG, WebP or GIF image — '%s' is not one."
                    .formatted(declared == null ? "unknown" : declared));
        }
    }

    /**
     * 📏 Refuse a picture larger than a face has any business being.
     *
     * <p>An avatar arrives already cropped and downscaled by the interface, so anything approaching this
     * is a client that skipped that step. Refused with the number rather than with "too large": somebody
     * looking at a 3 MB photograph cannot otherwise tell how much smaller it has to get.</p>
     *
     * <h4>⚠️ This is a pre-check, not the enforced ceiling</h4>
     *
     * <p>The size here is <strong>declared</strong> by whoever sent the content, and a client that lies
     * about it walks past this. What actually enforces a limit is the installation's ceiling inside
     * ingestion, which re-checks the bytes that really arrived — and that ceiling is the larger one,
     * because it was raised to fit documents. So the honest description of this rule is "it stops
     * ordinary clients uploading a photograph", not "an avatar cannot exceed this".</p>
     *
     * <p>Closing that gap properly means ingestion taking a per-call ceiling, which is its to own rather
     * than something to reimplement here by re-reading the stream.</p>
     */
    private void ensureSmallEnough(Content content) {
        if (!content.hasDeclaredSize()) {
            return;
        }

        long size = content.declaredSize();

        if (size > maximumSizeBytes) {
            throw new UploadRejectedException(
                ("A picture may be up to %d KB — this one is %d KB. It is meant to be cropped to a "
                 + "small square first.").formatted(maximumSizeBytes / 1024, size / 1024));
        }
    }

}
