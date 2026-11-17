package org.jmouse.settings.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** 🎚️ One value of a setting, as a row of {@code settings_parameters}: whose, of which owner's section, under which key. */
@Entity(name = "KeptParameter")
@Table(name = "settings_parameters")
public class KeptParameter {

    /** Whose value, of which owner's section, under which key. */
    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "holder_type", nullable = false, length = 40, updatable = false)
        private String holderType;

        @Column(name = "holder_id", nullable = false, length = 80, updatable = false)
        private String holderId;

        @Column(name = "owner", nullable = false, length = 80, updatable = false)
        private String owner;

        @Column(name = "section_path", nullable = false, length = 160, updatable = false)
        private String section;

        @Column(name = "parameter_key", nullable = false, length = 255, updatable = false)
        private String parameter;

        protected Key() {
        }

        public Key(String holderType, String holderId, String owner, String section, String parameter) {
            this.holderType = holderType;
            this.holderId = holderId;
            this.owner = owner;
            this.section = section;
            this.parameter = parameter;
        }

        public String getSection() {
            return section;
        }

        public String getParameter() {
            return parameter;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && holderType.equals(key.holderType) && holderId.equals(key.holderId)
                   && owner.equals(key.owner) && section.equals(key.section) && parameter.equals(key.parameter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(holderType, holderId, owner, section, parameter);
        }
    }

    @EmbeddedId
    private Key key;

    /**
     * Its value as text; null for a value that is null. ⚠️ The length is what makes it {@code LONGTEXT} on MySQL and {@code TEXT}
     * on PostgreSQL, as the migrations write it: unset, a {@code @Lob} is sized 255 and validation refuses to start.
     */
    @Lob
    @Column(name = "parameter_value", length = Integer.MAX_VALUE)
    private String value;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected KeptParameter() {
    }

    public KeptParameter(Key key, String value, Instant changedAt) {
        this.key = key;
        this.value = value;
        this.changedAt = changedAt;
    }

    public void change(String value, Instant changedAt) {
        this.value = value;
        this.changedAt = changedAt;
    }

    public Key getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
