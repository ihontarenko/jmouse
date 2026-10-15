package org.jmouse.parallax.jpa.catalogue;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A visualizer or a lab: which object kinds it opens, which representations it starts with, and the catalogue
 * object it opens by default. Every instrument is the same scene editor with a different starting point.
 */
@Entity(name = "Instrument")
@Table(name = "instruments")
public class Instrument {

    public enum Section { VISUALIZER, LAB }

    @Id
    @Column(length = 40, nullable = false, updatable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Section section;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(length = Length.LONG32)
    private String description;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(length = Length.LONG32, name = "default_representations", nullable = false)
    private String defaultRepresentations;

    @Column(name = "preset_object_id", length = 36)
    private String presetObjectId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int position;

    @ElementCollection
    @CollectionTable(name = "instrument_object_kinds", joinColumns = @JoinColumn(name = "instrument_code"))
    @Column(name = "object_kind", length = 40)
    private Set<String> objectKinds = new LinkedHashSet<>();

    protected Instrument() {
    }

    public Instrument(String code, Section section) {
        this.code = code;
        this.section = section;
        this.enabled = true;
    }

    public void describe(String title, String summary, String description, String defaultRepresentations,
                         String presetObjectId, boolean enabled, int position, Set<String> objectKinds) {
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.defaultRepresentations = defaultRepresentations;
        this.presetObjectId = presetObjectId;
        this.enabled = enabled;
        this.position = position;
        this.objectKinds.clear();
        this.objectKinds.addAll(objectKinds);
    }

    public String getCode() {
        return code;
    }

    public Section getSection() {
        return section;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultRepresentations() {
        return defaultRepresentations;
    }

    public String getPresetObjectId() {
        return presetObjectId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getPosition() {
        return position;
    }

    public Set<String> getObjectKinds() {
        return objectKinds;
    }
}
