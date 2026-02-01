package com.ecommerce.sellerx.qa;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SeniorityService")
class SeniorityServiceTest extends BaseUnitTest {

    @Mock
    private QaPatternRepository patternRepository;

    private SeniorityService seniorityService;

    private User testUser;
    private Store testStore;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        seniorityService = new SeniorityService(patternRepository);

        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testStore = TestDataBuilder.completedStore(testUser).build();
        testStore.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("recordApproval")
    class RecordApproval {

        @Test
        @DisplayName("should increment approval count when not modified")
        void shouldIncrementApprovalCount() {
            // Given
            QaPattern pattern = buildPattern(1, 0, 0, 0, QaPattern.SENIORITY_JUNIOR);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            // When
            seniorityService.recordApproval(patternId, false);

            // Then
            assertThat(pattern.getApprovalCount()).isEqualTo(1);
            assertThat(pattern.getModificationCount()).isEqualTo(0);
            assertThat(pattern.getLastHumanReview()).isNotNull();
            verify(patternRepository).save(pattern);
        }

        @Test
        @DisplayName("should increment modification count when modified")
        void shouldIncrementModificationCount() {
            // Given
            QaPattern pattern = buildPattern(1, 0, 0, 0, QaPattern.SENIORITY_JUNIOR);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            // When
            seniorityService.recordApproval(patternId, true);

            // Then
            assertThat(pattern.getApprovalCount()).isEqualTo(0);
            assertThat(pattern.getModificationCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should promote to LEARNING when occurrences >= 3")
        void shouldPromoteToLearning() {
            // Given
            QaPattern pattern = buildPattern(3, 0, 0, 0, QaPattern.SENIORITY_JUNIOR);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            // When
            seniorityService.recordApproval(patternId, false);

            // Then
            assertThat(pattern.getSeniorityLevel()).isEqualTo(QaPattern.SENIORITY_LEARNING);
        }

        @Test
        @DisplayName("should promote to SENIOR with 5+ approvals and 80%+ approval rate")
        void shouldPromoteToSenior() {
            // Given
            QaPattern pattern = buildPattern(10, 4, 0, 0, QaPattern.SENIORITY_LEARNING);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            // When - this approval makes it 5 approvals out of 5 total = 100% rate
            seniorityService.recordApproval(patternId, false);

            // Then
            assertThat(pattern.getApprovalCount()).isEqualTo(5);
            assertThat(pattern.getSeniorityLevel()).isEqualTo(QaPattern.SENIORITY_SENIOR);
        }

        @Test
        @DisplayName("should promote to EXPERT with 10+ approvals and 90%+ approval rate")
        void shouldPromoteToExpert() {
            // Given
            QaPattern pattern = buildPattern(20, 9, 0, 0, QaPattern.SENIORITY_SENIOR);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            // When - this approval makes it 10 approvals out of 10 total = 100% rate
            seniorityService.recordApproval(patternId, false);

            // Then
            assertThat(pattern.getApprovalCount()).isEqualTo(10);
            assertThat(pattern.getSeniorityLevel()).isEqualTo(QaPattern.SENIORITY_EXPERT);
        }

        @Test
        @DisplayName("should throw when pattern not found")
        void shouldThrowWhenPatternNotFound() {
            UUID patternId = UUID.randomUUID();
            when(patternRepository.findById(patternId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> seniorityService.recordApproval(patternId, false))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Pattern not found");
        }
    }

    @Nested
    @DisplayName("recordRejection")
    class RecordRejection {

        @Test
        @DisplayName("should increment rejection count")
        void shouldIncrementRejectionCount() {
            // Given
            QaPattern pattern = buildPattern(5, 3, 0, 0, QaPattern.SENIORITY_LEARNING);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            // When
            seniorityService.recordRejection(patternId);

            // Then
            assertThat(pattern.getRejectionCount()).isEqualTo(1);
            assertThat(pattern.getLastHumanReview()).isNotNull();
        }

        @Test
        @DisplayName("should disable auto-submit on rejection")
        void shouldDisableAutoSubmitOnRejection() {
            // Given
            QaPattern pattern = buildPattern(20, 10, 0, 0, QaPattern.SENIORITY_EXPERT);
            pattern.setIsAutoSubmitEligible(true);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            // When
            seniorityService.recordRejection(patternId);

            // Then
            assertThat(pattern.getIsAutoSubmitEligible()).isFalse();
            assertThat(pattern.getAutoSubmitDisabledReason()).contains("reddedildi");
        }

        @Test
        @DisplayName("should demote from EXPERT if approval rate drops below threshold after rejection")
        void shouldDemoteIfApprovalRateDrops() {
            // Given - 10 approvals, 0 rejections = EXPERT
            // After 1 rejection: 10 approvals / 11 total = 90.9% still EXPERT
            // After 2 rejections: 10 approvals / 12 total = 83.3% drops below 90% for EXPERT
            QaPattern pattern = buildPattern(20, 10, 1, 0, QaPattern.SENIORITY_EXPERT);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            // When - adding another rejection: 10 approvals / 12 total = 83.3%
            seniorityService.recordRejection(patternId);

            // Then - should drop from EXPERT to SENIOR (approval rate < 90%)
            assertThat(pattern.getRejectionCount()).isEqualTo(2);
            assertThat(pattern.getSeniorityLevel()).isEqualTo(QaPattern.SENIORITY_SENIOR);
        }
    }

    @Nested
    @DisplayName("promotePattern")
    class PromotePattern {

        @Test
        @DisplayName("should promote JUNIOR to LEARNING")
        void shouldPromoteJuniorToLearning() {
            // Given
            QaPattern pattern = buildPattern(1, 0, 0, 0, QaPattern.SENIORITY_JUNIOR);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            // When
            QaPatternDto result = seniorityService.promotePattern(patternId, testUser);

            // Then
            assertThat(result.getSeniorityLevel()).isEqualTo(QaPattern.SENIORITY_LEARNING);
        }

        @Test
        @DisplayName("should promote LEARNING to SENIOR")
        void shouldPromoteLearningToSenior() {
            QaPattern pattern = buildPattern(5, 3, 0, 0, QaPattern.SENIORITY_LEARNING);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            QaPatternDto result = seniorityService.promotePattern(patternId, testUser);

            assertThat(result.getSeniorityLevel()).isEqualTo(QaPattern.SENIORITY_SENIOR);
        }

        @Test
        @DisplayName("should not promote beyond EXPERT")
        void shouldNotPromoteBeyondExpert() {
            QaPattern pattern = buildPattern(20, 10, 0, 0, QaPattern.SENIORITY_EXPERT);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));

            QaPatternDto result = seniorityService.promotePattern(patternId, testUser);

            assertThat(result.getSeniorityLevel()).isEqualTo(QaPattern.SENIORITY_EXPERT);
            verify(patternRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("demotePattern")
    class DemotePattern {

        @Test
        @DisplayName("should demote EXPERT to SENIOR and disable auto-submit")
        void shouldDemoteExpertToSenior() {
            QaPattern pattern = buildPattern(20, 10, 0, 0, QaPattern.SENIORITY_EXPERT);
            pattern.setIsAutoSubmitEligible(true);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            QaPatternDto result = seniorityService.demotePattern(patternId, testUser, "Quality issue");

            assertThat(result.getSeniorityLevel()).isEqualTo(QaPattern.SENIORITY_SENIOR);
            assertThat(pattern.getIsAutoSubmitEligible()).isFalse();
            assertThat(pattern.getAutoSubmitDisabledReason()).isEqualTo("Quality issue");
        }

        @Test
        @DisplayName("should not demote below JUNIOR")
        void shouldNotDemoteBelowJunior() {
            QaPattern pattern = buildPattern(1, 0, 0, 0, QaPattern.SENIORITY_JUNIOR);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));

            QaPatternDto result = seniorityService.demotePattern(patternId, testUser, null);

            assertThat(result.getSeniorityLevel()).isEqualTo(QaPattern.SENIORITY_JUNIOR);
            verify(patternRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("enableAutoSubmit")
    class EnableAutoSubmit {

        @Test
        @DisplayName("should enable auto-submit for EXPERT pattern with sufficient approvals")
        void shouldEnableAutoSubmit() {
            QaPattern pattern = buildPattern(10, 5, 0, 0, QaPattern.SENIORITY_EXPERT);
            pattern.setIsAutoSubmitEligible(false);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            QaPatternDto result = seniorityService.enableAutoSubmit(patternId, testUser);

            assertThat(result.getIsAutoSubmitEligible()).isTrue();
        }

        @Test
        @DisplayName("should throw when pattern is not EXPERT")
        void shouldThrowWhenNotExpert() {
            QaPattern pattern = buildPattern(5, 3, 0, 0, QaPattern.SENIORITY_SENIOR);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));

            assertThatThrownBy(() -> seniorityService.enableAutoSubmit(patternId, testUser))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("EXPERT");
        }

        @Test
        @DisplayName("should throw when approval requirements not met")
        void shouldThrowWhenApprovalRequirementsNotMet() {
            QaPattern pattern = buildPattern(5, 1, 0, 0, QaPattern.SENIORITY_EXPERT);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));

            assertThatThrownBy(() -> seniorityService.enableAutoSubmit(patternId, testUser))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("approval requirements");
        }
    }

    @Nested
    @DisplayName("canAutoSubmit")
    class CanAutoSubmit {

        @Test
        @DisplayName("should return true for eligible expert pattern with high confidence")
        void shouldReturnTrueForEligiblePattern() {
            QaPattern pattern = buildPattern(20, 15, 0, 0, QaPattern.SENIORITY_EXPERT);
            pattern.setIsAutoSubmitEligible(true);
            pattern.setConfidenceScore(BigDecimal.valueOf(0.90));

            assertThat(seniorityService.canAutoSubmit(pattern)).isTrue();
        }

        @Test
        @DisplayName("should return false when not auto-submit eligible")
        void shouldReturnFalseWhenNotEligible() {
            QaPattern pattern = buildPattern(20, 15, 0, 0, QaPattern.SENIORITY_EXPERT);
            pattern.setIsAutoSubmitEligible(false);
            pattern.setConfidenceScore(BigDecimal.valueOf(0.90));

            assertThat(seniorityService.canAutoSubmit(pattern)).isFalse();
        }

        @Test
        @DisplayName("should return false when confidence below threshold")
        void shouldReturnFalseWhenLowConfidence() {
            QaPattern pattern = buildPattern(20, 15, 0, 0, QaPattern.SENIORITY_EXPERT);
            pattern.setIsAutoSubmitEligible(true);
            pattern.setConfidenceScore(BigDecimal.valueOf(0.50));

            assertThat(seniorityService.canAutoSubmit(pattern)).isFalse();
        }

        @Test
        @DisplayName("should return false when not EXPERT level")
        void shouldReturnFalseWhenNotExpert() {
            QaPattern pattern = buildPattern(10, 5, 0, 0, QaPattern.SENIORITY_SENIOR);
            pattern.setIsAutoSubmitEligible(true);
            pattern.setConfidenceScore(BigDecimal.valueOf(0.90));

            assertThat(seniorityService.canAutoSubmit(pattern)).isFalse();
        }
    }

    @Nested
    @DisplayName("disableAutoSubmit")
    class DisableAutoSubmit {

        @Test
        @DisplayName("should disable auto-submit with reason")
        void shouldDisableAutoSubmitWithReason() {
            QaPattern pattern = buildPattern(20, 15, 0, 0, QaPattern.SENIORITY_EXPERT);
            pattern.setIsAutoSubmitEligible(true);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            QaPatternDto result = seniorityService.disableAutoSubmit(patternId, testUser, "Too many errors");

            assertThat(result.getIsAutoSubmitEligible()).isFalse();
            assertThat(pattern.getAutoSubmitDisabledReason()).isEqualTo("Too many errors");
        }

        @Test
        @DisplayName("should use default reason when null")
        void shouldUseDefaultReasonWhenNull() {
            QaPattern pattern = buildPattern(20, 15, 0, 0, QaPattern.SENIORITY_EXPERT);
            pattern.setIsAutoSubmitEligible(true);
            UUID patternId = pattern.getId();
            when(patternRepository.findById(patternId)).thenReturn(Optional.of(pattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            seniorityService.disableAutoSubmit(patternId, testUser, null);

            assertThat(pattern.getAutoSubmitDisabledReason()).contains("Manuel olarak");
        }
    }

    // Helper
    private QaPattern buildPattern(int occurrences, int approvals, int rejections, int modifications, String seniority) {
        QaPattern pattern = new QaPattern();
        pattern.setId(UUID.randomUUID());
        pattern.setStore(testStore);
        pattern.setPatternHash("hash-" + UUID.randomUUID().toString().substring(0, 8));
        pattern.setCanonicalQuestion("Bu urun ne zaman gelir?");
        pattern.setCanonicalAnswer("Kargoya verildikten sonra 2-3 gun icerisinde teslim edilir.");
        pattern.setOccurrenceCount(occurrences);
        pattern.setApprovalCount(approvals);
        pattern.setRejectionCount(rejections);
        pattern.setModificationCount(modifications);
        pattern.setConfidenceScore(BigDecimal.ZERO);
        pattern.setSeniorityLevel(seniority);
        pattern.setIsAutoSubmitEligible(false);
        pattern.setFirstSeenAt(LocalDateTime.now().minusDays(30));
        pattern.setLastSeenAt(LocalDateTime.now());
        pattern.setUpdatedAt(LocalDateTime.now());
        return pattern;
    }
}
