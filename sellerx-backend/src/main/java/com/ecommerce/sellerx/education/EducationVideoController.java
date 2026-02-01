package com.ecommerce.sellerx.education;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/education/videos")
public class EducationVideoController {
    
    private final EducationVideoService videoService;
    
    @GetMapping
    public List<EducationVideoDto> getAllVideos() {
        return videoService.getAllVideos();
    }
    
    @GetMapping("/{id}")
    public EducationVideoDto getVideoById(@PathVariable UUID id) {
        return videoService.getVideoById(id);
    }
    
    @GetMapping("/category/{category}")
    public List<EducationVideoDto> getVideosByCategory(@PathVariable VideoCategory category) {
        return videoService.getVideosByCategory(category);
    }
    
    @GetMapping("/my-watch-status")
    public VideoWatchStatusDto getMyWatchStatus() {
        Long userId = getCurrentUserId();
        return videoService.getUserWatchStatus(userId);
    }
    
    @PostMapping("/{id}/watch")
    public ResponseEntity<Void> markVideoAsWatched(@PathVariable UUID id) {
        Long userId = getCurrentUserId();
        videoService.markAsWatched(id, userId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EducationVideoDto> createVideo(
            @Valid @RequestBody CreateVideoRequest request,
            UriComponentsBuilder uriBuilder) {
        Long userId = getCurrentUserId();
        EducationVideoDto video = videoService.createVideo(request, userId);
        var uri = uriBuilder.path("/api/education/videos/{id}").buildAndExpand(video.getId()).toUri();
        return ResponseEntity.created(uri).body(video);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EducationVideoDto updateVideo(@PathVariable UUID id, @RequestBody UpdateVideoRequest request) {
        return videoService.updateVideo(id, request);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID id) {
        videoService.deleteVideo(id);
        return ResponseEntity.noContent().build();
    }
    
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
