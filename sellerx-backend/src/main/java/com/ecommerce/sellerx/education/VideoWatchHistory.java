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
@Table(name = "video_watch_history", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "video_id"})
    },
    indexes = {
        @Index(name = "idx_video_watch_user", columnList = "user_id"),
        @Index(name = "idx_video_watch_video", columnList = "video_id")
    })
public class VideoWatchHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private EducationVideo video;

    @Column(name = "watched_at", nullable = false)
    private LocalDateTime watchedAt;

    @Column(name = "watched_duration")
    private Integer watchedDuration; // Saniye cinsinden, ileride progress tracking için

    @PrePersist
    protected void onCreate() {
        if (watchedAt == null) {
            watchedAt = LocalDateTime.now();
        }
    }
}
