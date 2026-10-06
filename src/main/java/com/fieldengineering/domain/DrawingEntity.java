package com.fieldengineering.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "drawing_files")
public class DrawingEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", length = 64)
    public String id;

    @Column(name = "name", length = 255, nullable = false)
    public String name;

    @Column(name = "file_key", length = 512, nullable = false)
    public String fileKey;

    @Column(name = "file_type", length = 32, nullable = false)
    public String fileType; // PDF | IMAGE

    @Column(name = "page_count", nullable = false)
    public int pageCount = 1;

    @Column(name = "file_size", nullable = false)
    public long fileSize = 0;

    @Column(name = "owner_id", length = 255)
    public String ownerId;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    public static List<DrawingEntity> listAllOrdered() {
        return list("order by createdAt desc");
    }
}
