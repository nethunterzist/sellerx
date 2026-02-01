package com.ecommerce.sellerx.education;

import jakarta.persistence.*;
import lombok.*;
import com.ecommerce.sellerx.users.User;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "education_videos", indexes = {
    @Index(name = "idx_education_video_category", columnList = "category"),
    @Index(name = "idx_education_video_active_order", columnList = "is_active, video_order")
})
public class EducationVideo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private VideoCategory category;

    @Column(name = "duration", nullable = false)
    private String duration; // Format: "5:30"

    @Column(name = "video_url", nullable = false, columnDefinition = "TEXT")
    private String videoUrl; // YouTube embed URL veya dosya path

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "video_type", nullable = false)
    private VideoType videoType;

    @Column(name = "video_order", nullable = false)
    private Integer order;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
