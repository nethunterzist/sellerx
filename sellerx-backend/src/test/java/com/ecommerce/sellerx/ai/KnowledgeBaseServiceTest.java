package com.ecommerce.sellerx.ai;

import com.ecommerce.sellerx.ai.dto.CreateKnowledgeRequest;
import com.ecommerce.sellerx.ai.dto.KnowledgeBaseDto;
import com.ecommerce.sellerx.common.BaseUnitTest;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KnowledgeBaseServiceTest extends BaseUnitTest {

    @Mock
    private StoreKnowledgeBaseRepository repository;

    @Mock
    private StoreRepository storeRepository;

    private KnowledgeBaseService knowledgeBaseService;

    private Store testStore;
    private User testUser;
    private UUID storeId;
    private UUID knowledgeId;

    @BeforeEach
    void setUp() {
        knowledgeBaseService = new KnowledgeBaseService(repository, storeRepository);

        testUser = User.builder()
                .name("Test User")
                .email("test@test.com")
                .build();
        testUser.setId(1L);

        storeId = UUID.randomUUID();
        testStore = Store.builder()
                .storeName("Test Store")
                .marketplace("trendyol")
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testStore.setId(storeId);

        knowledgeId = UUID.randomUUID();
    }

    private StoreKnowledgeBase createKnowledgeEntity(String category, String title, String content) {
        StoreKnowledgeBase entity = StoreKnowledgeBase.builder()
                .store(testStore)
                .category(category)
                .title(title)
                .content(content)
                .keywords(List.of("keyword1", "keyword2"))
                .isActive(true)
                .priority(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        entity.setId(knowledgeId);
        return entity;
    }

    @Nested
    @DisplayName("getByStoreId")
    class GetByStoreId {

        @Test
        @DisplayName("should return all knowledge base items for store")
        void shouldReturnAllItems() {
            StoreKnowledgeBase entity = createKnowledgeEntity("shipping", "Kargo Politikasi", "Ucretsiz kargo");

            when(repository.findByStoreIdOrderByPriorityDesc(storeId)).thenReturn(List.of(entity));

            List<KnowledgeBaseDto> result = knowledgeBaseService.getByStoreId(storeId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Kargo Politikasi");
            assertThat(result.get(0).getCategory()).isEqualTo("shipping");
            assertThat(result.get(0).getStoreId()).isEqualTo(storeId);
        }

        @Test
        @DisplayName("should return empty list when no items exist")
        void shouldReturnEmptyList() {
            when(repository.findByStoreIdOrderByPriorityDesc(storeId)).thenReturn(List.of());

            List<KnowledgeBaseDto> result = knowledgeBaseService.getByStoreId(storeId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getByStoreIdAndCategory")
    class GetByStoreIdAndCategory {

        @Test
        @DisplayName("should return items filtered by category")
        void shouldFilterByCategory() {
            StoreKnowledgeBase entity = createKnowledgeEntity("returns", "Iade Politikasi", "15 gun iade hakki");

            when(repository.findByStoreIdAndCategoryAndIsActiveTrueOrderByPriorityDesc(storeId, "returns"))
                    .thenReturn(List.of(entity));

            List<KnowledgeBaseDto> result = knowledgeBaseService.getByStoreIdAndCategory(storeId, "returns");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo("returns");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create new knowledge base item")
        void shouldCreateNewItem() {
            CreateKnowledgeRequest request = CreateKnowledgeRequest.builder()
                    .category("shipping")
                    .title("Kargo Bilgisi")
                    .content("Ucretsiz kargo detaylari")
                    .keywords(List.of("kargo", "ucretsiz"))
                    .priority(10)
                    .build();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(repository.save(any(StoreKnowledgeBase.class))).thenAnswer(invocation -> {
                StoreKnowledgeBase saved = invocation.getArgument(0);
                saved.setId(knowledgeId);
                return saved;
            });

            KnowledgeBaseDto result = knowledgeBaseService.create(storeId, request);

            assertThat(result.getTitle()).isEqualTo("Kargo Bilgisi");
            assertThat(result.getCategory()).isEqualTo("shipping");
            assertThat(result.getPriority()).isEqualTo(10);
            assertThat(result.getIsActive()).isTrue();

            ArgumentCaptor<StoreKnowledgeBase> captor = ArgumentCaptor.forClass(StoreKnowledgeBase.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStore()).isEqualTo(testStore);
        }

        @Test
        @DisplayName("should use default priority of 0 when not specified")
        void shouldUseDefaultPriority() {
            CreateKnowledgeRequest request = CreateKnowledgeRequest.builder()
                    .category("general")
                    .title("Genel Bilgi")
                    .content("Icerlik")
                    .priority(null)
                    .build();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(repository.save(any(StoreKnowledgeBase.class))).thenAnswer(invocation -> {
                StoreKnowledgeBase saved = invocation.getArgument(0);
                saved.setId(knowledgeId);
                return saved;
            });

            KnowledgeBaseDto result = knowledgeBaseService.create(storeId, request);

            assertThat(result.getPriority()).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw when store not found")
        void shouldThrowWhenStoreNotFound() {
            UUID unknownStoreId = UUID.randomUUID();
            CreateKnowledgeRequest request = CreateKnowledgeRequest.builder()
                    .category("shipping")
                    .title("Title")
                    .content("Content")
                    .build();

            when(storeRepository.findById(unknownStoreId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> knowledgeBaseService.create(unknownStoreId, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(unknownStoreId.toString());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update existing knowledge base item")
        void shouldUpdateExistingItem() {
            StoreKnowledgeBase existing = createKnowledgeEntity("shipping", "Old Title", "Old Content");

            CreateKnowledgeRequest updateRequest = CreateKnowledgeRequest.builder()
                    .category("returns")
                    .title("Updated Title")
                    .content("Updated Content")
                    .keywords(List.of("updated"))
                    .priority(20)
                    .build();

            when(repository.findById(knowledgeId)).thenReturn(Optional.of(existing));
            when(repository.save(any(StoreKnowledgeBase.class))).thenAnswer(i -> i.getArgument(0));

            KnowledgeBaseDto result = knowledgeBaseService.update(knowledgeId, updateRequest);

            assertThat(result.getTitle()).isEqualTo("Updated Title");
            assertThat(result.getCategory()).isEqualTo("returns");
            assertThat(result.getContent()).isEqualTo("Updated Content");
            assertThat(result.getPriority()).isEqualTo(20);
        }

        @Test
        @DisplayName("should throw when knowledge item not found")
        void shouldThrowWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            CreateKnowledgeRequest request = CreateKnowledgeRequest.builder()
                    .category("shipping")
                    .title("Title")
                    .content("Content")
                    .build();

            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> knowledgeBaseService.update(unknownId, request))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete knowledge base item by id")
        void shouldDeleteById() {
            knowledgeBaseService.delete(knowledgeId);

            verify(repository).deleteById(knowledgeId);
        }
    }

    @Nested
    @DisplayName("toggleActive")
    class ToggleActive {

        @Test
        @DisplayName("should toggle active status to false")
        void shouldToggleToFalse() {
            StoreKnowledgeBase entity = createKnowledgeEntity("shipping", "Title", "Content");

            when(repository.findById(knowledgeId)).thenReturn(Optional.of(entity));
            when(repository.save(any(StoreKnowledgeBase.class))).thenAnswer(i -> i.getArgument(0));

            knowledgeBaseService.toggleActive(knowledgeId, false);

            assertThat(entity.getIsActive()).isFalse();
            verify(repository).save(entity);
        }

        @Test
        @DisplayName("should throw when item not found for toggle")
        void shouldThrowWhenNotFoundForToggle() {
            UUID unknownId = UUID.randomUUID();
            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> knowledgeBaseService.toggleActive(unknownId, false))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
