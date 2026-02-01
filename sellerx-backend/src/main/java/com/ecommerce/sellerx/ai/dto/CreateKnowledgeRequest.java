package com.ecommerce.sellerx.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateKnowledgeRequest {
    @NotBlank(message = "Kategori zorunludur")
    private String category;

    @NotBlank(message = "Başlık zorunludur")
    private String title;

    @NotBlank(message = "İçerik zorunludur")
    private String content;

    private List<String> keywords;
    private Integer priority;
}
