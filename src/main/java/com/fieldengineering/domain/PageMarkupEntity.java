package com.fieldengineering.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "page_markups", indexes = {
    @Index(name = "idx_page_markups_drawing", columnList = "drawing_id, page_number")
})
public class PageMarkupEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", length = 64)
    public String id;

    @Column(name = "drawing_id", length = 64, nullable = false)
    public String drawingId;

    @Column(name = "page_number", nullable = false)
    public int pageNumber = 1;

    @Column(name = "payload", columnDefinition = "TEXT")
    public String payload;

    @Column(name = "version", nullable = false)
    public int version = 1;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    public static PageMarkupEntity findByDrawingAndPage(String drawingId, int pageNumber) {
        return find("drawingId = ?1 and pageNumber = ?2", drawingId, pageNumber).firstResult();
    }
}
