package org.jmouse.notifier;

import java.util.List;

/**
 * The product's half: turning a {@link Recipient} into the people it means.
 *
 * <p>⚠️ <strong>The library cannot supply one.</strong> Who "everyone in this household" is, which
 * accounts sit in a group, whose device that is — only the product knows, and a default that answered
 * with nobody would make every notification vanish silently.
 *
 * <p>{@link Recipient.Direct} never reaches a resolver; {@link Recipient.Except} does, so a product
 * that understands devices can honour it — {@link Notifier} drops addressees on the excluded device
 * afterwards in any case.
 */
@FunctionalInterface
public interface RecipientResolver {

    /**
     * @return everybody the recipient means; empty when it means nobody, never {@code null}
     */
    List<Addressee> resolve(Recipient recipient);
}
