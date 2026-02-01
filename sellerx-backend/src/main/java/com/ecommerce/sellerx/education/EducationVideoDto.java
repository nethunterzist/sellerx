package com.ecommerce.sellerx.education;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationVideoDto {
    private UUID id;
    private String title;
    private String description;
    private VideoCategory category;
    private String duration;
    private String videoUrl;
    private String thumbnailUrl;
    private VideoType videoType;
    private Integer order;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
