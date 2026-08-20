package org.jmouse.storage.jpa;

import org.jmouse.storage.delivery.DeliverableFile;
import org.jmouse.storage.delivery.DeliveryIntent;
import org.jmouse.storage.delivery.DeliveryPlan;
import org.jmouse.storage.delivery.DeliveryPlanner;
import org.jmouse.storage.exception.StorageException;

/**
 * 📤 The read path's missing half: a registry row, planned for delivery.
 *
 * <p>{@link DeliverableFile} lives in the byte layer, which knows nothing about persistence, so it
 * cannot offer a factory taking a {@link StoredFile}. Every product filled that gap the same way —
 * six getters copied into a constructor call, immediately followed by a call to the planner — and
 * five copies of it existed across four products before this class did. Each one was a place where
 * a new field on the registry row would have to be remembered, and where forgetting it would show
 * up as a wrong header rather than as a compile error.</p>
 *
 * <h3>⚠️ The presented name is the binding's, not the registry's</h3>
 *
 * <p>Under content-addressed keys, two uploads of identical bytes share one registry row, and that
 * row's {@code originalName} is whoever uploaded them <em>first</em>. Serving it to everybody shows
 * one person another person's filename. So {@link #plan(StoredFile, DeliveryIntent)} is correct only
 * where the registry's own name is genuinely the one to present — an avatar, a file with no separate
 * binding row — and anything holding its own name uses
 * {@link #plan(StoredFile, String, DeliveryIntent)} instead.</p>
 *
 * <p>Both overloads exist so that this is a decision a call site makes visibly. A single method
 * taking the row alone would make the wrong answer the silent default.</p>
 *
 * <h3>What it deliberately does not do</h3>
 *
 * <p>It does not look a row up. A product reaching delivery has already loaded the row — through its
 * own binding, its own share token, its own authorization — and a lookup here would be a second
 * query answering a question that was already answered. Where only an identifier is in hand, ask
 * {@link StoredFileRegistry#find(String)} and raise the refusal the route should give: "no such
 * avatar" is a better answer than anything a storage library could word.</p>
 */
public class StoredFileDelivery {

    private final DeliveryPlanner deliveryPlanner;

    /**
     * 🏗️ Build the read path over the application's planner.
     *
     * @param deliveryPlanner decides between streaming and a presigned redirect, and for how long
     */
    public StoredFileDelivery(DeliveryPlanner deliveryPlanner) {
        this.deliveryPlanner = deliveryPlanner;
    }

    /**
     * 📄 A registry row, as the delivery layer needs to see it.
     *
     * <p>Static because it is a translation rather than a decision, so a caller already holding a
     * planner — or wanting to adjust the result before planning — does not need this class injected
     * to perform it.</p>
     *
     * @param storedFile the registry row
     * @return the deliverable file, presented under the registry's own name
     */
    public static DeliverableFile deliverable(StoredFile storedFile) {
        if (storedFile == null) {
            throw new StorageException("A stored file is required to deliver one.");
        }

        return new DeliverableFile(
            storedFile.getStorageKey(), storedFile.getBackend(), storedFile.getOriginalName(),
            storedFile.getContentType(), storedFile.getSizeBytes(), storedFile.getSha256());
    }

    /**
     * 🚚 How the bytes of this row should reach the client, under the registry's own name.
     *
     * @param storedFile the registry row
     * @param intent     what the client asked for
     * @return the delivery plan
     */
    public DeliveryPlan plan(StoredFile storedFile, DeliveryIntent intent) {
        return deliveryPlanner.plan(deliverable(storedFile), intent);
    }

    /**
     * 🚚 How the bytes of this row should reach the client, under the name its binding gives it.
     *
     * @param storedFile        the registry row
     * @param presentedFilename name this particular reference shows the user
     * @param intent            what the client asked for
     * @return the delivery plan
     */
    public DeliveryPlan plan(StoredFile storedFile, String presentedFilename, DeliveryIntent intent) {
        return deliveryPlanner.plan(deliverable(storedFile).presentedAs(presentedFilename), intent);
    }
}
