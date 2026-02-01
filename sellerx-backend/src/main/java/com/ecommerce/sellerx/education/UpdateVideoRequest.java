package com.ecommerce.sellerx.education;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVideoRequest {
    private String title;
    private String description;
    private VideoCategory category;
    private String duration;
    private String videoUrl;
    private String thumbnailUrl;
    private VideoType videoType;
    private Integer order;
    private Boolean isActive;
}
