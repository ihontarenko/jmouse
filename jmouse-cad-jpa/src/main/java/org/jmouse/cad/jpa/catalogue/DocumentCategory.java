package org.jmouse.cad.jpa.catalogue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 📚 A section of the catalogue: where a document is filed, and the order sections are shown in.
 *
 * <p>The library seeds none. Which sections exist is the installation's, written by the product's own import.</p>
 */
@Entity(name = "DocumentCategory")
@Table(name = "cad_categories")
public class DocumentCategory {

    @Id
    @Column(length = 40, nullable = false, updatable = false)
    private String code;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private int position;

    protected DocumentCategory() {
    }

    public DocumentCategory(String code, String title, String description, int position) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.position = position;
    }

    public void describe(String title, String description, int position) {
        this.title = title;
        this.description = description;
        this.position = position;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPosition() {
        return position;
    }
}
