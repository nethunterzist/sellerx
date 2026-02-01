package com.ecommerce.sellerx.qa;

import com.ecommerce.sellerx.ai.AiAnswerService;
import com.ecommerce.sellerx.ai.dto.AiGenerateResponse;
import com.ecommerce.sellerx.qa.dto.*;
import com.ecommerce.sellerx.users.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
public class QaController {

    private final TrendyolQaService qaService;
    private final AiAnswerService aiAnswerService;
    private final KnowledgeSuggestionService suggestionService;
    private final SeniorityService seniorityService;
    private final ConflictDetectionService conflictService;

    /**
     * Get questions for a store with pagination
     */
    @GetMapping("/stores/{storeId}/questions")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<Page<QuestionDto>> getQuestions(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        Page<QuestionDto> questions = qaService.getQuestions(storeId, status, pageable);
        return ResponseEntity.ok(questions);
    }

    /**
     * Get single question by ID
     */
    @GetMapping("/questions/{questionId}")
    public ResponseEntity<QuestionDto> getQuestion(@PathVariable UUID questionId) {
        QuestionDto question = qaService.getQuestion(questionId);
        return ResponseEntity.ok(question);
    }

    /**
     * Sync questions from Trendyol
     */
    @PostMapping("/stores/{storeId}/questions/sync")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<QaSyncResponse> syncQuestions(@PathVariable UUID storeId) {
        QaSyncResponse response = qaService.syncQuestions(storeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit manual answer to Trendyol
     * SAFETY: In development mode (TRENDYOL_SUBMIT_ENABLED not set),
     * this will save locally but NOT submit to Trendyol API
     */
    @PostMapping("/stores/{storeId}/questions/{questionId}/answer")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<Void> submitAnswer(
            @PathVariable UUID storeId,
            @PathVariable String questionId,
            @Valid @RequestBody SubmitAnswerRequest request,
            @AuthenticationPrincipal User user) {
        qaService.submitManualAnswer(storeId, questionId, request.getAnswerText(), user);
        return ResponseEntity.ok().build();
    }

    /**
     * Get Q&A statistics for a store
     */
    @GetMapping("/stores/{storeId}/stats")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<QaStatsDto> getStats(@PathVariable UUID storeId) {
        QaStatsDto stats = qaService.getStats(storeId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Generate AI answer for a question
     */
    @PostMapping("/questions/{questionId}/ai-generate")
    public ResponseEntity<AiGenerateResponse> generateAiAnswer(@PathVariable UUID questionId) {
        AiGenerateResponse response = aiAnswerService.generateAnswer(questionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve and submit AI-generated answer to Trendyol
     */
    @PostMapping("/questions/{questionId}/ai-approve")
    public ResponseEntity<AnswerDto> approveAndSubmitAiAnswer(
            @PathVariable UUID questionId,
            @RequestBody AiApproveRequest request,
            @AuthenticationPrincipal User user) {
        TrendyolAnswer answer = aiAnswerService.approveAndSubmit(
                questionId,
                request.getLogId(),
                request.getFinalAnswer(),
                user
        );
        return ResponseEntity.ok(toAnswerDto(answer));
    }

    private AnswerDto toAnswerDto(TrendyolAnswer answer) {
        return AnswerDto.builder()
                .id(answer.getId())
                .questionId(answer.getQuestion().getId())
                .answerText(answer.getAnswerText())
                .isSubmitted(answer.getIsSubmitted())
                .submittedAt(answer.getSubmittedAt())
                .trendyolAnswerId(answer.getTrendyolAnswerId())
                .createdAt(answer.getCreatedAt())
                .build();
    }

    // =====================================================
    // KNOWLEDGE SUGGESTIONS ENDPOINTS
    // =====================================================

    /**
     * Get knowledge suggestions for a store
     */
    @GetMapping("/stores/{storeId}/suggestions")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<List<KnowledgeSuggestionDto>> getSuggestions(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String status) {
        List<KnowledgeSuggestionDto> suggestions = suggestionService.getSuggestions(storeId, status);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Get pending suggestion count for a store
     */
    @GetMapping("/stores/{storeId}/suggestions/count")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<Long> getPendingSuggestionCount(@PathVariable UUID storeId) {
        long count = suggestionService.getPendingSuggestionCount(storeId);
        return ResponseEntity.ok(count);
    }

    /**
     * Approve a suggestion - creates knowledge base entry
     */
    @PostMapping("/suggestions/{suggestionId}/approve")
    public ResponseEntity<KnowledgeSuggestionDto> approveSuggestion(
            @PathVariable UUID suggestionId,
            @AuthenticationPrincipal User user) {
        KnowledgeSuggestionDto result = suggestionService.approveSuggestion(suggestionId, user);
        return ResponseEntity.ok(result);
    }

    /**
     * Reject a suggestion
     */
    @PostMapping("/suggestions/{suggestionId}/reject")
    public ResponseEntity<KnowledgeSuggestionDto> rejectSuggestion(
            @PathVariable UUID suggestionId,
            @RequestBody(required = false) RejectSuggestionRequest request,
            @AuthenticationPrincipal User user) {
        String reason = request != null ? request.getReason() : null;
        KnowledgeSuggestionDto result = suggestionService.rejectSuggestion(suggestionId, user, reason);
        return ResponseEntity.ok(result);
    }

    /**
     * Modify and approve a suggestion
     */
    @PostMapping("/suggestions/{suggestionId}/modify")
    public ResponseEntity<KnowledgeSuggestionDto> modifySuggestion(
            @PathVariable UUID suggestionId,
            @RequestBody ModifySuggestionRequest request,
            @AuthenticationPrincipal User user) {
        KnowledgeSuggestionDto result = suggestionService.modifySuggestion(
                suggestionId, user, request.getTitle(), request.getContent());
        return ResponseEntity.ok(result);
    }

    // =====================================================
    // SENIORITY / PATTERNS ENDPOINTS
    // =====================================================

    /**
     * Get patterns for a store
     */
    @GetMapping("/stores/{storeId}/patterns")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<List<QaPatternDto>> getPatterns(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String seniorityLevel,
            @RequestParam(required = false) Boolean autoSubmitOnly) {
        List<QaPatternDto> patterns = seniorityService.getPatterns(storeId, seniorityLevel, autoSubmitOnly);
        return ResponseEntity.ok(patterns);
    }

    /**
     * Get seniority statistics for a store
     */
    @GetMapping("/stores/{storeId}/patterns/stats")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<SeniorityStatsDto> getSeniorityStats(@PathVariable UUID storeId) {
        SeniorityStatsDto stats = seniorityService.getSeniorityStats(storeId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Manually promote a pattern
     */
    @PostMapping("/patterns/{patternId}/promote")
    public ResponseEntity<QaPatternDto> promotePattern(
            @PathVariable UUID patternId,
            @AuthenticationPrincipal User user) {
        QaPatternDto result = seniorityService.promotePattern(patternId, user);
        return ResponseEntity.ok(result);
    }

    /**
     * Manually demote a pattern
     */
    @PostMapping("/patterns/{patternId}/demote")
    public ResponseEntity<QaPatternDto> demotePattern(
            @PathVariable UUID patternId,
            @RequestBody(required = false) DemotePatternRequest request,
            @AuthenticationPrincipal User user) {
        String reason = request != null ? request.getReason() : null;
        QaPatternDto result = seniorityService.demotePattern(patternId, user, reason);
        return ResponseEntity.ok(result);
    }

    /**
     * Enable auto-submit for a pattern (skip waiting period)
     */
    @PostMapping("/patterns/{patternId}/enable-auto-submit")
    public ResponseEntity<QaPatternDto> enableAutoSubmit(
            @PathVariable UUID patternId,
            @AuthenticationPrincipal User user) {
        QaPatternDto result = seniorityService.enableAutoSubmit(patternId, user);
        return ResponseEntity.ok(result);
    }

    /**
     * Disable auto-submit for a pattern
     */
    @PostMapping("/patterns/{patternId}/disable-auto-submit")
    public ResponseEntity<QaPatternDto> disableAutoSubmit(
            @PathVariable UUID patternId,
            @RequestBody(required = false) DisableAutoSubmitRequest request,
            @AuthenticationPrincipal User user) {
        String reason = request != null ? request.getReason() : null;
        QaPatternDto result = seniorityService.disableAutoSubmit(patternId, user, reason);
        return ResponseEntity.ok(result);
    }

    // =====================================================
    // CONFLICT ALERTS ENDPOINTS
    // =====================================================

    /**
     * Get conflict alerts for a store
     */
    @GetMapping("/stores/{storeId}/conflicts")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<List<ConflictAlertDto>> getConflicts(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String status) {
        List<ConflictAlertDto> conflicts = conflictService.getAlerts(storeId, status);
        return ResponseEntity.ok(conflicts);
    }

    /**
     * Get active conflict count for a store
     */
    @GetMapping("/stores/{storeId}/conflicts/count")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<Long> getActiveConflictCount(@PathVariable UUID storeId) {
        long count = conflictService.getActiveAlertCount(storeId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get conflict statistics for a store
     */
    @GetMapping("/stores/{storeId}/conflicts/stats")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<ConflictStatsDto> getConflictStats(@PathVariable UUID storeId) {
        ConflictStatsDto stats = conflictService.getConflictStats(storeId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Resolve a conflict alert
     */
    @PostMapping("/conflicts/{conflictId}/resolve")
    public ResponseEntity<ConflictAlertDto> resolveConflict(
            @PathVariable UUID conflictId,
            @RequestBody(required = false) ResolveConflictRequest request,
            @AuthenticationPrincipal User user) {
        String notes = request != null ? request.getResolutionNotes() : null;
        ConflictAlertDto result = conflictService.resolveAlert(conflictId, user, notes);
        return ResponseEntity.ok(result);
    }

    /**
     * Dismiss a conflict alert
     */
    @PostMapping("/conflicts/{conflictId}/dismiss")
    public ResponseEntity<ConflictAlertDto> dismissConflict(
            @PathVariable UUID conflictId,
            @AuthenticationPrincipal User user) {
        ConflictAlertDto result = conflictService.dismissAlert(conflictId, user);
        return ResponseEntity.ok(result);
    }

    // =====================================================
    // REQUEST DTOs (Inner Classes)
    // =====================================================

    @lombok.Data
    public static class RejectSuggestionRequest {
        private String reason;
    }

    @lombok.Data
    public static class ModifySuggestionRequest {
        private String title;
        private String content;
    }

    @lombok.Data
    public static class DemotePatternRequest {
        private String reason;
    }

    @lombok.Data
    public static class DisableAutoSubmitRequest {
        private String reason;
    }

    @lombok.Data
    public static class ResolveConflictRequest {
        private String resolutionNotes;
    }
}
