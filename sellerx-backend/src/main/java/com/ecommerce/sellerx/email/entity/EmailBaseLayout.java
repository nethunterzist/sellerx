package com.ecommerce.sellerx.email.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "email_base_layout")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EmailBaseLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "header_html", nullable = false, columnDefinition = "TEXT")
    private String headerHtml;

    @Column(name = "footer_html", nullable = false, columnDefinition = "TEXT")
    private String footerHtml;

    @Column(name = "styles", columnDefinition = "TEXT")
    private String styles;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "primary_color", nullable = false, length = 20)
    @Builder.Default
    private String primaryColor = "#2563eb";

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = OffsetDateTime.now();
    }
}
