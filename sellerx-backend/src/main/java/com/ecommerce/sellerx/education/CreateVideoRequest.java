package com.ecommerce.sellerx.education;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVideoRequest {
    @NotBlank
    private String title;
    
    private String description;
    
    @NotNull
    private VideoCategory category;
    
    @NotBlank
    private String duration;
    
    @NotBlank
    private String videoUrl;
    
    private String thumbnailUrl;
    
    @NotNull
    private VideoType videoType;
    
    @NotNull
    private Integer order;
    
    private Boolean isActive;
}
