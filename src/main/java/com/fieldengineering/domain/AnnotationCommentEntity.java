package com.fieldengineering.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "annotation_comments")
public class AnnotationCommentEntity extends PanacheEntityBase {
    @Id
    @Column(length = 64)
    public String id;

    @Column(name = "markup_id", nullable = false)
    public String markupId;

    @Column(name = "author_id")
    public String authorId;

    @Column(name = "author_name", nullable = false)
    public String authorName = "Field Engineer";

    @Column(nullable = false, columnDefinition = "TEXT")
    public String text;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
