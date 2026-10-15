package org.jmouse.notifier.jpa;

import org.jmouse.notifier.Address;
import org.jmouse.notifier.Recipient;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * How a {@link Recipient} is written into one column and read back.
 *
 * <pre>
 *   everyone            everyone:&lt;scope&gt;
 *   subject:&lt;id&gt;        group:&lt;id&gt;        device:&lt;id&gt;
 *   except:&lt;device&gt;|&lt;recipient&gt;
 *   direct:&lt;channel&gt;|&lt;value&gt;
 * </pre>
 *
 * <p>Every identifier is URL-encoded, so a {@code :} or a {@code |} inside one cannot be read as a
 * separator. ⚠️ This is a stored format: changing it orphans every row written in the old one.
 */
final class RecipientNotation {

    private RecipientNotation() {
    }

    static String write(Recipient recipient) {
        return switch (recipient) {
            case Recipient.Everyone everyone -> everyone.scope() == null ? "everyone" : "everyone:" + encode(everyone.scope());
            case Recipient.Subject subject -> "subject:" + encode(subject.id());
            case Recipient.Group group -> "group:" + encode(group.id());
            case Recipient.Device device -> "device:" + encode(device.id());
            case Recipient.Except except -> "except:" + encode(except.deviceId()) + "|" + write(except.audience());
            case Recipient.Direct direct -> "direct:" + encode(direct.address().channel()) + "|" + encode(direct.address().value());
        };
    }

    static Recipient read(String notation) {
        if ("everyone".equals(notation)) {
            return new Recipient.Everyone(null);
        }

        int    colon = notation.indexOf(':');
        String kind  = colon < 0 ? notation : notation.substring(0, colon);
        String rest  = colon < 0 ? "" : notation.substring(colon + 1);

        return switch (kind) {
            case "everyone" -> new Recipient.Everyone(decode(rest));
            case "subject" -> new Recipient.Subject(decode(rest));
            case "group" -> new Recipient.Group(decode(rest));
            case "device" -> new Recipient.Device(decode(rest));
            case "except" -> {
                int bar = rest.indexOf('|');
                yield new Recipient.Except(read(rest.substring(bar + 1)), decode(rest.substring(0, bar)));
            }
            case "direct" -> {
                int bar = rest.indexOf('|');
                yield new Recipient.Direct(new Address(decode(rest.substring(0, bar)), decode(rest.substring(bar + 1))));
            }
            default -> throw new IllegalStateException("A stored recipient this version cannot read: " + notation);
        };
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
