package com.ecommerce.sellerx.qa;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("PatternDiscoveryService")
class PatternDiscoveryServiceTest extends BaseUnitTest {

    @Mock
    private QaPatternRepository patternRepository;

    @Mock
    private KnowledgeSuggestionRepository suggestionRepository;

    @Mock
    private TrendyolQuestionRepository questionRepository;

    @Mock
    private StoreRepository storeRepository;

    private PatternDiscoveryService patternDiscoveryService;

    private User testUser;
    private Store testStore;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        patternDiscoveryService = new PatternDiscoveryService(
                patternRepository, suggestionRepository, questionRepository, storeRepository);

        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);

        testStore = TestDataBuilder.completedStore(testUser).build();
        storeId = UUID.randomUUID();
        testStore.setId(storeId);
    }

    @Nested
    @DisplayName("checkQuestionPattern")
    class CheckQuestionPattern {

        @Test
        @DisplayName("should return existing pattern when exact hash match found")
        void shouldReturnExistingPatternOnHashMatch() {
            // Given
            TrendyolQuestion question = buildQuestion("Bu urun ne zaman gelir?");

            QaPattern existingPattern = buildPattern("Bu urun ne zaman gelir?");
            when(patternRepository.findByStoreIdAndPatternHash(eq(storeId), any()))
                    .thenReturn(Optional.of(existingPattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            // When
            QaPattern result = patternDiscoveryService.checkQuestionPattern(question);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOccurrenceCount()).isEqualTo(2); // incremented from 1
            verify(patternRepository).save(existingPattern);
        }

        @Test
        @DisplayName("should return null when no matching pattern found")
        void shouldReturnNullWhenNoMatch() {
            // Given
            TrendyolQuestion question = buildQuestion("Completely unique question about something new");

            when(patternRepository.findByStoreIdAndPatternHash(eq(storeId), any()))
                    .thenReturn(Optional.empty());
            when(patternRepository.findByStoreId(storeId)).thenReturn(List.of());

            // When
            QaPattern result = patternDiscoveryService.checkQuestionPattern(question);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should find fuzzy match when similarity is above threshold")
        void shouldFindFuzzyMatch() {
            // Given
            TrendyolQuestion question = buildQuestion("bu urun ne zaman teslim edilir");

            when(patternRepository.findByStoreIdAndPatternHash(eq(storeId), any()))
                    .thenReturn(Optional.empty());

            QaPattern similarPattern = buildPattern("bu urun ne zaman gelir teslim edilir");
            when(patternRepository.findByStoreId(storeId)).thenReturn(List.of(similarPattern));
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            // When
            QaPattern result = patternDiscoveryService.checkQuestionPattern(question);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOccurrenceCount()).isEqualTo(2); // incremented
        }

        @Test
        @DisplayName("should not match when similarity is below threshold")
        void shouldNotMatchWhenSimilarityLow() {
            // Given
            TrendyolQuestion question = buildQuestion("iade sureci nasil isliyor");

            when(patternRepository.findByStoreIdAndPatternHash(eq(storeId), any()))
                    .thenReturn(Optional.empty());

            QaPattern differentPattern = buildPattern("kargo takip numarasi nedir");
            when(patternRepository.findByStoreId(storeId)).thenReturn(List.of(differentPattern));

            // When
            QaPattern result = patternDiscoveryService.checkQuestionPattern(question);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("createPattern")
    class CreatePattern {

        @Test
        @DisplayName("should create pattern with correct initial values")
        void shouldCreatePatternWithCorrectValues() {
            // Given
            TrendyolQuestion question = buildQuestion("Renk secenekleri nelerdir?");
            String answer = "Siyah, beyaz ve mavi renk secenekleri mevcuttur.";

            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> {
                QaPattern saved = i.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // When
            QaPattern result = patternDiscoveryService.createPattern(question, answer);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCanonicalQuestion()).isEqualTo("Renk secenekleri nelerdir?");
            assertThat(result.getCanonicalAnswer()).isEqualTo(answer);
            assertThat(result.getOccurrenceCount()).isEqualTo(1);
            assertThat(result.getApprovalCount()).isEqualTo(0);
            assertThat(result.getSeniorityLevel()).isEqualTo(QaPattern.SENIORITY_JUNIOR);
            assertThat(result.getIsAutoSubmitEligible()).isFalse();
            assertThat(result.getConfidenceScore()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("incrementPatternOccurrence")
    class IncrementPatternOccurrence {

        @Test
        @DisplayName("should increment occurrence count and update last seen")
        void shouldIncrementAndUpdateTimestamp() {
            // Given
            QaPattern pattern = buildPattern("Test question");
            LocalDateTime originalLastSeen = pattern.getLastSeenAt();
            when(patternRepository.save(any(QaPattern.class))).thenAnswer(i -> i.getArgument(0));

            // When
            patternDiscoveryService.incrementPatternOccurrence(pattern);

            // Then
            assertThat(pattern.getOccurrenceCount()).isEqualTo(2);
            assertThat(pattern.getLastSeenAt()).isAfterOrEqualTo(originalLastSeen);
            verify(patternRepository).save(pattern);
        }
    }

    @Nested
    @DisplayName("analyzeStoreQuestions")
    class AnalyzeStoreQuestions {

        @Test
        @DisplayName("should skip analysis when no recent questions")
        void shouldSkipWhenNoRecentQuestions() {
            // Given
            when(questionRepository.findByStoreIdAndCreatedAtAfter(eq(storeId), any()))
                    .thenReturn(List.of());

            // When
            patternDiscoveryService.analyzeStoreQuestions(storeId);

            // Then
            verify(suggestionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create suggestion when cluster meets minimum size")
        void shouldCreateSuggestionForLargeCluster() {
            // Given
            // Create 3+ similar questions (minimum for suggestion)
            TrendyolQuestion q1 = buildQuestion("Bu urun ne zaman gelir?");
            TrendyolQuestion q2 = buildQuestion("Bu urun ne zaman gelir teslim edilir?");
            TrendyolQuestion q3 = buildQuestion("Bu urun ne zaman gelir bana?");

            when(questionRepository.findByStoreIdAndCreatedAtAfter(eq(storeId), any()))
                    .thenReturn(List.of(q1, q2, q3));
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(suggestionRepository.findByStoreIdAndStatusOrderByPriorityDescCreatedAtDesc(
                    storeId, KnowledgeSuggestion.STATUS_PENDING))
                    .thenReturn(List.of());
            when(suggestionRepository.save(any(KnowledgeSuggestion.class))).thenAnswer(i -> i.getArgument(0));

            // When
            patternDiscoveryService.analyzeStoreQuestions(storeId);

            // Then
            verify(suggestionRepository).save(any(KnowledgeSuggestion.class));
        }

        @Test
        @DisplayName("should not create suggestion when cluster is too small")
        void shouldNotCreateSuggestionForSmallCluster() {
            // Given - only 2 questions (below minimum of 3)
            TrendyolQuestion q1 = buildQuestion("Kargo ucreti ne kadar?");
            TrendyolQuestion q2 = buildQuestion("Tamamen farkli bir soru");

            when(questionRepository.findByStoreIdAndCreatedAtAfter(eq(storeId), any()))
                    .thenReturn(List.of(q1, q2));

            // When
            patternDiscoveryService.analyzeStoreQuestions(storeId);

            // Then
            verify(suggestionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getSuggestions")
    class GetSuggestions {

        @Test
        @DisplayName("should return suggestions filtered by status")
        void shouldReturnSuggestionsFilteredByStatus() {
            // Given
            KnowledgeSuggestion suggestion = buildSuggestion("Test Title");
            when(suggestionRepository.findByStoreIdAndStatusOrderByPriorityDescCreatedAtDesc(
                    storeId, KnowledgeSuggestion.STATUS_PENDING))
                    .thenReturn(List.of(suggestion));

            // When
            List<KnowledgeSuggestionDto> result = patternDiscoveryService.getSuggestions(
                    storeId, KnowledgeSuggestion.STATUS_PENDING);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSuggestedTitle()).isEqualTo("Test Title");
        }

        @Test
        @DisplayName("should return all suggestions when no status filter")
        void shouldReturnAllWhenNoStatusFilter() {
            // Given
            when(suggestionRepository.findByStoreIdOrderByPriorityDescCreatedAtDesc(storeId))
                    .thenReturn(List.of());

            // When
            List<KnowledgeSuggestionDto> result = patternDiscoveryService.getSuggestions(storeId, null);

            // Then
            assertThat(result).isEmpty();
            verify(suggestionRepository).findByStoreIdOrderByPriorityDescCreatedAtDesc(storeId);
        }
    }

    @Nested
    @DisplayName("getPendingSuggestionCount")
    class GetPendingSuggestionCount {

        @Test
        @DisplayName("should return pending count")
        void shouldReturnPendingCount() {
            when(suggestionRepository.countByStoreIdAndStatus(storeId, KnowledgeSuggestion.STATUS_PENDING))
                    .thenReturn(5L);

            assertThat(patternDiscoveryService.getPendingSuggestionCount(storeId)).isEqualTo(5L);
        }
    }

    // Helpers

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

    private QaPattern buildPattern(String canonicalQuestion) {
        QaPattern pattern = new QaPattern();
        pattern.setId(UUID.randomUUID());
        pattern.setStore(testStore);
        pattern.setPatternHash("hash-" + UUID.randomUUID().toString().substring(0, 8));
        pattern.setCanonicalQuestion(canonicalQuestion);
        pattern.setCanonicalAnswer("Default answer");
        pattern.setOccurrenceCount(1);
        pattern.setApprovalCount(0);
        pattern.setRejectionCount(0);
        pattern.setModificationCount(0);
        pattern.setConfidenceScore(BigDecimal.ZERO);
        pattern.setSeniorityLevel(QaPattern.SENIORITY_JUNIOR);
        pattern.setIsAutoSubmitEligible(false);
        pattern.setFirstSeenAt(LocalDateTime.now().minusDays(7));
        pattern.setLastSeenAt(LocalDateTime.now().minusDays(1));
        pattern.setUpdatedAt(LocalDateTime.now());
        return pattern;
    }

    private KnowledgeSuggestion buildSuggestion(String title) {
        KnowledgeSuggestion suggestion = new KnowledgeSuggestion();
        suggestion.setId(UUID.randomUUID());
        suggestion.setStore(testStore);
        suggestion.setSuggestedTitle(title);
        suggestion.setSuggestedContent("Suggested content for " + title);
        suggestion.setSampleQuestions(List.of("Question 1", "Question 2"));
        suggestion.setQuestionCount(5);
        suggestion.setAvgSimilarity(BigDecimal.valueOf(0.85));
        suggestion.setStatus(KnowledgeSuggestion.STATUS_PENDING);
        suggestion.setPriority(KnowledgeSuggestion.PRIORITY_MEDIUM);
        suggestion.setFirstSeenAt(LocalDateTime.now().minusDays(3));
        suggestion.setLastSeenAt(LocalDateTime.now());
        suggestion.setCreatedAt(LocalDateTime.now());
        suggestion.setUpdatedAt(LocalDateTime.now());
        return suggestion;
    }
}
