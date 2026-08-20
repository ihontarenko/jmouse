package org.jmouse.files.management;

import org.jmouse.files.OwnerReference;

/**
 * 🧭 The two things about an upload that the <strong>server</strong> answers, never the client.
 *
 * <h3>⚠️ Why these two stopped being request parameters</h3>
 *
 * <p>{@link FileController#upload} used to take {@code namespace} and {@code uploadedBy} off the query
 * string. Both are answers about the caller and the installation, and letting the caller give them is
 * two holes wearing the shape of convenience:</p>
 *
 * <ul>
 *   <li><strong>{@code uploadedBy} is an identity claim.</strong> A product resolving a file's access
 *       target reads it to answer <em>whose row is this</em> — which is exactly how a permission held
 *       at an own-rows scope is honoured. Taken from the request, anybody could write their own name
 *       onto somebody else's upload and hold it as their own.</li>
 *   <li><strong>{@code namespace} is where the bytes land.</strong> Given by the caller, one product's
 *       upload can be addressed into another's storage prefix — and in an installation where several
 *       products share a bucket, that is a write into a neighbour's tree.</li>
 * </ul>
 *
 * <p>So the route asks this instead. A product implementing it says <em>attachments live under
 * {@code tessera/attachments}, and the uploader is the member behind this request</em>; nothing a client
 * sends can disagree.</p>
 *
 * <h3>The default is deliberately dull</h3>
 *
 * <p>Where a product declares no bean, the autoconfiguration supplies one that reads a single configured
 * namespace and reports no uploader at all. That is right for a product with one kind of file and no
 * notion of ownership, and it is honest rather than convenient for every other: an absent uploader shows
 * up as an unowned file, which is visible, where a guessed one would not be.</p>
 */
public interface FileManagementContext {

    /**
     * 📁 Where an upload filed against this owner should be stored.
     *
     * @param owner what will hold the file
     * @return the storage namespace
     */
    String namespaceFor(OwnerReference owner);

    /**
     * 👤 Who is uploading, as the product identifies people.
     *
     * @return the uploader's identifier, or {@code null} where the product tracks none
     */
    default String uploader() {
        return null;
    }
}
