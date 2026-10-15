package org.jmouse.notifier.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jmouse.notifier.Level;
import org.jmouse.notifier.Notification;
import org.jmouse.notifier.Publication;
import org.jmouse.notifier.Sender;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One publication, persisted — what was said, by whom, to whom, once.
 *
 * <p>⚠️ Write-once. The idempotency key is unique in the table, so a second publish under the same key is
 * refused by the database rather than by a check that two processes could both pass.
 */
@Entity
@Table(name = NotifierPublication.TABLE_NAME)
public class NotifierPublication {

    public static final String TABLE_NAME = "notifier_publications";

    /** Separates a link's label from its address, and one link from the next — characters no text carries. */
    private static final char FIELD_SEPARATOR  = '\u001F';
    private static final char RECORD_SEPARATOR = '\u001E';

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "idempotency_key", length = 255, unique = true)
    private String idempotencyKey;

    /** {@code SYSTEM}, {@code RULE} or {@code PERSON}. */
    @Column(name = "sender_kind", nullable = false, length = 16)
    private String senderKind;

    /** The system's or rule's name, or the person's display name. */
    @Column(name = "sender_name", nullable = false, length = 255)
    private String senderName;

    /** The person's subject identifier; {@code null} for a system or a rule. */
    @Column(name = "sender_subject", length = 128)
    private String senderSubject;

    @Column(name = "recipient", nullable = false, length = 1000)
    private String recipient;

    @Column(name = "title", length = 255)
    private String title;

    /** TEXT in both dialects. ⚠️ Not @Lob: on PostgreSQL that maps a String to a large-object oid. */
    @Column(name = "body", nullable = false, length = 65535)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 16)
    private Level level;

    @Column(name = "links", length = 65535)
    private String links;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    protected NotifierPublication() {
        // Jakarta Persistence.
    }

    static NotifierPublication of(Publication publication) {
        NotifierPublication row          = new NotifierPublication();
        Notification        notification = publication.notification();

        row.id             = publication.id();
        row.idempotencyKey = publication.idempotencyKey();
        row.recipient      = RecipientNotation.write(publication.recipient());
        row.title          = notification.title();
        row.body           = notification.body();
        row.level          = notification.level();
        row.links          = writeLinks(notification.links());
        row.publishedAt    = publication.publishedAt();

        switch (publication.sender()) {
            case Sender.System system -> {
                row.senderKind = "SYSTEM";
                row.senderName = system.name();
            }
            case Sender.Rule rule -> {
                row.senderKind = "RULE";
                row.senderName = rule.name();
            }
            case Sender.Person person -> {
                row.senderKind    = "PERSON";
                row.senderName    = person.displayName();
                row.senderSubject = person.subjectId();
            }
        }

        return row;
    }

    Publication toPublication() {
        Sender sender = switch (senderKind) {
            case "RULE" -> new Sender.Rule(senderName);
            case "PERSON" -> new Sender.Person(senderSubject, senderName);
            default -> new Sender.System(senderName);
        };

        return new Publication(id, idempotencyKey, sender, RecipientNotation.read(recipient),
                               new Notification(title, body, level, readLinks(links)), publishedAt);
    }

    String getId() {
        return id;
    }

    private static String writeLinks(List<Notification.Link> links) {
        if (links.isEmpty()) {
            return null;
        }

        StringBuilder text = new StringBuilder();

        for (Notification.Link link : links) {
            if (!text.isEmpty()) {
                text.append(RECORD_SEPARATOR);
            }

            text.append(link.label()).append(FIELD_SEPARATOR).append(link.url());
        }

        return text.toString();
    }

    private static List<Notification.Link> readLinks(String text) {
        List<Notification.Link> links = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return links;
        }

        for (String record : text.split(String.valueOf(RECORD_SEPARATOR))) {
            int separator = record.indexOf(FIELD_SEPARATOR);
            links.add(new Notification.Link(record.substring(0, separator), record.substring(separator + 1)));
        }

        return links;
    }
}
