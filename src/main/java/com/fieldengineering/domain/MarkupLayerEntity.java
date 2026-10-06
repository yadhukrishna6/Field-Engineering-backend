package com.fieldengineering.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "markup_layers")
public class MarkupLayerEntity extends PanacheEntityBase {
    @Id
    @Column(length = 64)
    public String id;

    @Column(name = "revision_id")
    public String revisionId;

    @Column(name = "drawing_id")
    public String drawingId;

    @Column(nullable = false)
    public String name = "Default Markup";

    @Column(nullable = false)
    public String category = "Field Note";

    @Column(name = "owner_id")
    public String ownerId;

    @Column(nullable = false)
    public boolean visible = true;

    @Column(nullable = false)
    public boolean locked = false;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}
