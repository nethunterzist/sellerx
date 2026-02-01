package com.ecommerce.sellerx.qa;

import com.ecommerce.sellerx.ai.StoreKnowledgeBase;
import com.ecommerce.sellerx.ai.StoreKnowledgeBaseRepository;
import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("ConflictDetectionService")
class ConflictDetectionServiceTest extends BaseUnitTest {

    @Mock
    private ConflictAlertRepository alertRepository;

    @Mock
    private StoreKnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private StoreRepository storeRepository;

    private ConflictDetectionService conflictDetectionService;

    private User testUser;
    private Store testStore;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        conflictDetectionService = new ConflictDetectionService(
                alertRepository, knowledgeBaseRepository, storeRepository);

        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testStore = TestDataBuilder.completedStore(testUser).build();
        storeId = UUID.randomUUID();
        testStore.setId(storeId);
    }

    @Nested
    @DisplayName("checkBeforeGeneration")
    class CheckBeforeGeneration {

        @Test
        @DisplayName("should block when legal risk keywords detected")
        void shouldBlockOnLegalKeywords() {
            // Given
            TrendyolQuestion question = buildQuestion("Bu urun sahte mi? Avukat ile gorusecegim");

            when(alertRepository.save(any(ConflictAlert.class))).thenAnswer(i -> {
                ConflictAlert saved = i.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // When
            ConflictDetectionService.ConflictCheckResult result =
                    conflictDetectionService.checkBeforeGeneration(question);

            // Then
            assertThat(result.shouldBlock).isTrue();
            assertThat(result.conflicts).isNotEmpty();

            ConflictAlert legalAlert = result.conflicts.stream()
                    .filter(a -> ConflictAlert.TYPE_LEGAL_RISK.equals(a.getConflictType()))
                    .findFirst()
                    .orElse(null);
            assertThat(legalAlert).isNotNull();
            assertThat(legalAlert.getSeverity()).isEqualTo(ConflictAlert.SEVERITY_CRITICAL);
            assertThat(legalAlert.getDetectedKeywords()).contains("sahte");
        }

        @Test
        @DisplayName("should not block but warn when health/safety keywords detected")
        void shouldWarnOnHealthKeywords() {
            // Given
            TrendyolQuestion question = buildQuestion("Bu urun hamile kadinlar icin uygun mu? Alerji yapar mi?");

            when(alertRepository.save(any(ConflictAlert.class))).thenAnswer(i -> {
                ConflictAlert saved = i.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // When
            ConflictDetectionService.ConflictCheckResult result =
                    conflictDetectionService.checkBeforeGeneration(question);

            // Then
            assertThat(result.shouldBlock).isFalse();
            assertThat(result.conflicts).isNotEmpty();

            ConflictAlert healthAlert = result.conflicts.stream()
                    .filter(a -> ConflictAlert.TYPE_HEALTH_SAFETY.equals(a.getConflictType()))
                    .findFirst()
                    .orElse(null);
            assertThat(healthAlert).isNotNull();
            assertThat(healthAlert.getSeverity()).isEqualTo(ConflictAlert.SEVERITY_HIGH);
            assertThat(healthAlert.getDetectedKeywords()).contains("hamile");
        }

        @Test
        @DisplayName("should block and detect both legal and health keywords in same question")
        void shouldDetectBothLegalAndHealthKeywords() {
            // Given - question containing both legal and health keywords
            TrendyolQuestion question = buildQuestion("Bu sahte urun hamile kadinlar icin tehlikeli, dava acacagim");

            when(alertRepository.save(any(ConflictAlert.class))).thenAnswer(i -> {
                ConflictAlert saved = i.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // When
            ConflictDetectionService.ConflictCheckResult result =
                    conflictDetectionService.checkBeforeGeneration(question);

            // Then
            assertThat(result.shouldBlock).isTrue(); // legal keywords trigger block
            assertThat(result.conflicts).hasSize(2); // one legal, one health
        }

        @Test
        @DisplayName("should return empty conflicts when no risk keywords found")
        void shouldReturnEmptyWhenNoKeywords() {
            // Given
            TrendyolQuestion question = buildQuestion("Bu urunun renk secenekleri nelerdir?");

            // When
            ConflictDetectionService.ConflictCheckResult result =
                    conflictDetectionService.checkBeforeGeneration(question);

            // Then
            assertThat(result.shouldBlock).isFalse();
            assertThat(result.conflicts).isEmpty();
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle null or empty question text gracefully")
        void shouldHandleNullQuestionText() {
            // Given
            TrendyolQuestion question = buildQuestion(null);

            // When
            ConflictDetectionService.ConflictCheckResult result =
                    conflictDetectionService.checkBeforeGeneration(question);

            // Then
            assertThat(result.shouldBlock).isFalse();
            assertThat(result.conflicts).isEmpty();
        }
    }

    @Nested
    @DisplayName("checkAfterGeneration")
    class CheckAfterGeneration {

        @Test
        @DisplayName("should detect knowledge conflict when numbers differ")
        void shouldDetectKnowledgeConflictWithDifferentNumbers() {
            // Given
            TrendyolQuestion question = buildQuestion("Bu urunun garanti suresi ne kadar?");

            // Knowledge base says 2 year warranty
            StoreKnowledgeBase knowledge = buildKnowledgeBase(
                    "Garanti Bilgisi", "Urunumuz 2 yil garanti kapsamindadir");

            when(knowledgeBaseRepository.findByStoreIdAndIsActiveTrueOrderByPriorityDesc(storeId))
                    .thenReturn(List.of(knowledge));
            when(alertRepository.save(any(ConflictAlert.class))).thenAnswer(i -> {
                ConflictAlert saved = i.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // AI response says 5 year warranty - conflicting
            String aiResponse = "Bu urunun garanti suresi 5 yil olmaktadir.";

            // When
            List<ConflictAlert> result = conflictDetectionService.checkAfterGeneration(question, aiResponse);

            // Then
            assertThat(result).isNotEmpty();
            // Should detect a knowledge conflict due to number mismatch (2 vs 5)
            boolean hasKnowledgeConflict = result.stream()
                    .anyMatch(a -> ConflictAlert.TYPE_KNOWLEDGE_VS_TRENDYOL.equals(a.getConflictType()));
            assertThat(hasKnowledgeConflict).isTrue();
        }

        @Test
        @DisplayName("should detect brand inconsistency for suspicious warranty period")
        void shouldDetectBrandInconsistencyForSuspiciousWarranty() {
            // Given
            TrendyolQuestion question = buildQuestion("Bu urunun garanti suresi nedir?");

            when(knowledgeBaseRepository.findByStoreIdAndIsActiveTrueOrderByPriorityDesc(storeId))
                    .thenReturn(List.of());
            when(alertRepository.save(any(ConflictAlert.class))).thenAnswer(i -> {
                ConflictAlert saved = i.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // AI response with suspicious 10 year warranty (> 5 triggers brand inconsistency)
            // Must use Turkish "yıl" (with dotless ı) for locale-aware matching
            String aiResponse = "Bu ürünün garanti süresi 10 yıl boyunca geçerlidir.";

            // When
            List<ConflictAlert> result = conflictDetectionService.checkAfterGeneration(question, aiResponse);

            // Then
            boolean hasBrandInconsistency = result.stream()
                    .anyMatch(a -> ConflictAlert.TYPE_BRAND_INCONSISTENCY.equals(a.getConflictType()));
            assertThat(hasBrandInconsistency).isTrue();
        }

        @Test
        @DisplayName("should return empty when no conflicts detected")
        void shouldReturnEmptyWhenNoConflicts() {
            // Given
            TrendyolQuestion question = buildQuestion("Bu urun hangi renklerde mevcut?");

            when(knowledgeBaseRepository.findByStoreIdAndIsActiveTrueOrderByPriorityDesc(storeId))
                    .thenReturn(List.of());

            // Response without brand keywords
            String aiResponse = "Bu urun siyah ve beyaz renklerde mevcuttur.";

            // When
            List<ConflictAlert> result = conflictDetectionService.checkAfterGeneration(question, aiResponse);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should skip knowledge entries with null content")
        void shouldSkipNullContentKnowledge() {
            // Given
            TrendyolQuestion question = buildQuestion("Kargo suresi ne kadar?");

            StoreKnowledgeBase nullContentKnowledge = buildKnowledgeBase("Title", null);

            when(knowledgeBaseRepository.findByStoreIdAndIsActiveTrueOrderByPriorityDesc(storeId))
                    .thenReturn(List.of(nullContentKnowledge));

            String aiResponse = "Kargo 3 gun icinde teslim edilir.";

            // When
            List<ConflictAlert> result = conflictDetectionService.checkAfterGeneration(question, aiResponse);

            // Then
            // Should not throw and should not create knowledge conflict for null content
            boolean hasKnowledgeConflict = result.stream()
                    .anyMatch(a -> ConflictAlert.TYPE_KNOWLEDGE_VS_TRENDYOL.equals(a.getConflictType()));
            assertThat(hasKnowledgeConflict).isFalse();
        }
    }

    @Nested
    @DisplayName("createAlert")
    class CreateAlertTests {

        @Test
        @DisplayName("should create alert with correct fields and save")
        void shouldCreateAlertWithCorrectFields() {
            // Given
            TrendyolQuestion question = buildQuestion("Test question");
            List<String> keywords = List.of("sahte", "avukat");

            when(alertRepository.save(any(ConflictAlert.class))).thenAnswer(i -> {
                ConflictAlert saved = i.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // When
            ConflictAlert result = conflictDetectionService.createAlert(
                    testStore, question,
                    ConflictAlert.TYPE_LEGAL_RISK,
                    ConflictAlert.SEVERITY_CRITICAL,
                    "CUSTOMER_QUESTION", "Test question content",
                    null, null,
                    keywords
            );

            // Then
            ArgumentCaptor<ConflictAlert> captor = ArgumentCaptor.forClass(ConflictAlert.class);
            verify(alertRepository).save(captor.capture());

            ConflictAlert saved = captor.getValue();
            assertThat(saved.getStore()).isEqualTo(testStore);
            assertThat(saved.getQuestion()).isEqualTo(question);
            assertThat(saved.getConflictType()).isEqualTo(ConflictAlert.TYPE_LEGAL_RISK);
            assertThat(saved.getSeverity()).isEqualTo(ConflictAlert.SEVERITY_CRITICAL);
            assertThat(saved.getStatus()).isEqualTo(ConflictAlert.STATUS_ACTIVE);
            assertThat(saved.getDetectedKeywords()).containsExactly("sahte", "avukat");
        }

        @Test
        @DisplayName("should truncate content longer than 500 characters")
        void shouldTruncateLongContent() {
            // Given
            TrendyolQuestion question = buildQuestion("Test question");
            String longContent = "x".repeat(600);

            when(alertRepository.save(any(ConflictAlert.class))).thenAnswer(i -> {
                ConflictAlert saved = i.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // When
            conflictDetectionService.createAlert(
                    testStore, question,
                    ConflictAlert.TYPE_LEGAL_RISK,
                    ConflictAlert.SEVERITY_HIGH,
                    "SOURCE_A", longContent,
                    "SOURCE_B", longContent,
                    List.of("keyword")
            );

            // Then
            ArgumentCaptor<ConflictAlert> captor = ArgumentCaptor.forClass(ConflictAlert.class);
            verify(alertRepository).save(captor.capture());

            ConflictAlert saved = captor.getValue();
            assertThat(saved.getSourceAContent()).hasSize(503); // 500 + "..."
            assertThat(saved.getSourceAContent()).endsWith("...");
        }
    }

    @Nested
    @DisplayName("resolveAlert")
    class ResolveAlert {

        @Test
        @DisplayName("should resolve alert with notes and resolved-by user")
        void shouldResolveAlert() {
            // Given
            UUID alertId = UUID.randomUUID();
            ConflictAlert alert = buildConflictAlert(ConflictAlert.STATUS_ACTIVE, ConflictAlert.TYPE_LEGAL_RISK);
            alert.setId(alertId);

            when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
            when(alertRepository.save(any(ConflictAlert.class))).thenAnswer(i -> i.getArgument(0));

            // When
            ConflictAlertDto result = conflictDetectionService.resolveAlert(
                    alertId, testUser, "Issue investigated and resolved");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ConflictAlert.STATUS_RESOLVED);
            assertThat(alert.getResolvedAt()).isNotNull();
            assertThat(alert.getResolvedBy()).isEqualTo(testUser);
            assertThat(alert.getResolutionNotes()).isEqualTo("Issue investigated and resolved");
            verify(alertRepository).save(alert);
        }

        @Test
        @DisplayName("should throw when alert not found")
        void shouldThrowWhenAlertNotFound() {
            // Given
            UUID alertId = UUID.randomUUID();
            when(alertRepository.findById(alertId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> conflictDetectionService.resolveAlert(alertId, testUser, "notes"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Conflict alert not found");
        }
    }

    @Nested
    @DisplayName("dismissAlert")
    class DismissAlert {

        @Test
        @DisplayName("should dismiss alert and set resolved-by user")
        void shouldDismissAlert() {
            // Given
            UUID alertId = UUID.randomUUID();
            ConflictAlert alert = buildConflictAlert(ConflictAlert.STATUS_ACTIVE, ConflictAlert.TYPE_HEALTH_SAFETY);
            alert.setId(alertId);

            when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
            when(alertRepository.save(any(ConflictAlert.class))).thenAnswer(i -> i.getArgument(0));

            // When
            ConflictAlertDto result = conflictDetectionService.dismissAlert(alertId, testUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ConflictAlert.STATUS_DISMISSED);
            assertThat(alert.getResolvedAt()).isNotNull();
            assertThat(alert.getResolvedBy()).isEqualTo(testUser);
            assertThat(alert.getResolutionNotes()).isNull(); // dismiss has no notes
            verify(alertRepository).save(alert);
        }

        @Test
        @DisplayName("should throw when alert not found for dismiss")
        void shouldThrowWhenAlertNotFoundForDismiss() {
            UUID alertId = UUID.randomUUID();
            when(alertRepository.findById(alertId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> conflictDetectionService.dismissAlert(alertId, testUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Conflict alert not found");
        }
    }

    @Nested
    @DisplayName("getActiveAlerts")
    class GetActiveAlerts {

        @Test
        @DisplayName("should return active alerts as DTOs")
        void shouldReturnActiveAlerts() {
            // Given
            ConflictAlert alert1 = buildConflictAlert(ConflictAlert.STATUS_ACTIVE, ConflictAlert.TYPE_LEGAL_RISK);
            ConflictAlert alert2 = buildConflictAlert(ConflictAlert.STATUS_ACTIVE, ConflictAlert.TYPE_HEALTH_SAFETY);

            when(alertRepository.findByStoreIdAndStatusOrderBySeverityDescCreatedAtDesc(
                    storeId, ConflictAlert.STATUS_ACTIVE))
                    .thenReturn(List.of(alert1, alert2));

            // When
            List<ConflictAlertDto> result = conflictDetectionService.getActiveAlerts(storeId);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no active alerts")
        void shouldReturnEmptyWhenNoActiveAlerts() {
            when(alertRepository.findByStoreIdAndStatusOrderBySeverityDescCreatedAtDesc(
                    storeId, ConflictAlert.STATUS_ACTIVE))
                    .thenReturn(List.of());

            List<ConflictAlertDto> result = conflictDetectionService.getActiveAlerts(storeId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAlerts")
    class GetAlerts {

        @Test
        @DisplayName("should return alerts filtered by status")
        void shouldReturnAlertsFilteredByStatus() {
            // Given
            ConflictAlert alert = buildConflictAlert(ConflictAlert.STATUS_RESOLVED, ConflictAlert.TYPE_LEGAL_RISK);
            when(alertRepository.findByStoreIdAndStatusOrderBySeverityDescCreatedAtDesc(
                    storeId, ConflictAlert.STATUS_RESOLVED))
                    .thenReturn(List.of(alert));

            // When
            List<ConflictAlertDto> result = conflictDetectionService.getAlerts(storeId, ConflictAlert.STATUS_RESOLVED);

            // Then
            assertThat(result).hasSize(1);
            verify(alertRepository).findByStoreIdAndStatusOrderBySeverityDescCreatedAtDesc(
                    storeId, ConflictAlert.STATUS_RESOLVED);
        }

        @Test
        @DisplayName("should return all alerts when status is null")
        void shouldReturnAllAlertsWhenStatusNull() {
            // Given
            when(alertRepository.findByStoreIdOrderByCreatedAtDesc(storeId))
                    .thenReturn(List.of());

            // When
            List<ConflictAlertDto> result = conflictDetectionService.getAlerts(storeId, null);

            // Then
            assertThat(result).isEmpty();
            verify(alertRepository).findByStoreIdOrderByCreatedAtDesc(storeId);
        }

        @Test
        @DisplayName("should return all alerts when status is empty string")
        void shouldReturnAllAlertsWhenStatusEmpty() {
            when(alertRepository.findByStoreIdOrderByCreatedAtDesc(storeId))
                    .thenReturn(List.of());

            conflictDetectionService.getAlerts(storeId, "");

            verify(alertRepository).findByStoreIdOrderByCreatedAtDesc(storeId);
        }
    }

    @Nested
    @DisplayName("getActiveAlertCount")
    class GetActiveAlertCount {

        @Test
        @DisplayName("should return active alert count")
        void shouldReturnActiveAlertCount() {
            when(alertRepository.countByStoreIdAndStatus(storeId, ConflictAlert.STATUS_ACTIVE))
                    .thenReturn(4L);

            assertThat(conflictDetectionService.getActiveAlertCount(storeId)).isEqualTo(4L);
        }
    }

    @Nested
    @DisplayName("hasCriticalConflicts")
    class HasCriticalConflicts {

        @Test
        @DisplayName("should return true when critical conflicts exist")
        void shouldReturnTrueWhenCriticalExist() {
            UUID questionId = UUID.randomUUID();
            when(alertRepository.hasCriticalAlertsForQuestion(questionId)).thenReturn(true);

            assertThat(conflictDetectionService.hasCriticalConflicts(questionId)).isTrue();
        }

        @Test
        @DisplayName("should return false when no critical conflicts")
        void shouldReturnFalseWhenNoCritical() {
            UUID questionId = UUID.randomUUID();
            when(alertRepository.hasCriticalAlertsForQuestion(questionId)).thenReturn(false);

            assertThat(conflictDetectionService.hasCriticalConflicts(questionId)).isFalse();
        }
    }

    // Helper methods

    private TrendyolQuestion buildQuestion(String questionText) {
        return TrendyolQuestion.builder()
                .id(UUID.randomUUID())
                .store(testStore)
                .questionId("TQ-" + UUID.randomUUID().toString().substring(0, 8))
                .customerQuestion(questionText)
                .questionDate(LocalDateTime.now())
                .status("PENDING")
                .isPublic(true)
                .answers(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ConflictAlert buildConflictAlert(String status, String conflictType) {
        ConflictAlert alert = new ConflictAlert();
        alert.setId(UUID.randomUUID());
        alert.setStore(testStore);
        alert.setQuestion(buildQuestion("Test question"));
        alert.setConflictType(conflictType);
        alert.setSeverity(ConflictAlert.SEVERITY_HIGH);
        alert.setSourceAType("CUSTOMER_QUESTION");
        alert.setSourceAContent("Test content A");
        alert.setSourceBType(null);
        alert.setSourceBContent(null);
        alert.setDetectedKeywords(List.of("keyword1"));
        alert.setStatus(status);
        alert.setCreatedAt(LocalDateTime.now());
        return alert;
    }

    private StoreKnowledgeBase buildKnowledgeBase(String title, String content) {
        return StoreKnowledgeBase.builder()
                .id(UUID.randomUUID())
                .store(testStore)
                .category("GENERAL")
                .title(title)
                .content(content)
                .keywords(List.of("keyword"))
                .isActive(true)
                .priority(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
