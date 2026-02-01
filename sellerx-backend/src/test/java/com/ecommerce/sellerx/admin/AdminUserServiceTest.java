package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminUserDto;
import com.ecommerce.sellerx.admin.dto.AdminUserListDto;
import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.SyncStatus;
import com.ecommerce.sellerx.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminUserServiceTest extends BaseUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreRepository storeRepository;

    private AdminUserService adminUserService;

    private User testUser;
    private Store testStore;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(userRepository, storeRepository);

        testUser = User.builder()
                .name("Test User")
                .email("test@test.com")
                .password("encoded-password")
                .role(Role.USER)
                .build();
        testUser.setId(1L);

        testStore = Store.builder()
                .storeName("Test Store")
                .marketplace("trendyol")
                .user(testUser)
                .initialSyncCompleted(true)
                .syncStatus(SyncStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testStore.setId(UUID.randomUUID());

        lenient().when(storeRepository.findAllByUser(any(User.class))).thenReturn(List.of(testStore));
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("should return paginated user list")
        void shouldReturnPaginatedUserList() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);
            when(userRepository.findAll(pageable)).thenReturn(userPage);

            Page<AdminUserListDto> result = adminUserService.getAllUsers(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo("test@test.com");
            assertThat(result.getContent().get(0).getStoreCount()).isEqualTo(1);
            verify(userRepository).findAll(pageable);
        }

        @Test
        @DisplayName("should return empty page when no users exist")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(userRepository.findAll(pageable)).thenReturn(emptyPage);

            Page<AdminUserListDto> result = adminUserService.getAllUsers(pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return detailed user DTO")
        void shouldReturnDetailedUserDto() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            AdminUserDto result = adminUserService.getUserById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@test.com");
            assertThat(result.getName()).isEqualTo("Test User");
            assertThat(result.getRole()).isEqualTo(Role.USER);
            assertThat(result.getStoreCount()).isEqualTo(1);
            assertThat(result.getStores()).hasSize(1);
            assertThat(result.getStores().get(0).getStoreName()).isEqualTo("Test Store");
        }

        @Test
        @DisplayName("should throw UserNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserService.getUserById(999L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("changeUserRole")
    class ChangeUserRole {

        @Test
        @DisplayName("should change user role and return updated DTO")
        void shouldChangeUserRole() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            AdminUserDto result = adminUserService.changeUserRole(1L, Role.ADMIN);

            assertThat(result.getRole()).isEqualTo(Role.ADMIN);
            verify(userRepository).save(testUser);
            assertThat(testUser.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("should throw UserNotFoundException when user not found for role change")
        void shouldThrowWhenUserNotFoundForRoleChange() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserService.changeUserRole(999L, Role.ADMIN))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("searchUsers")
    class SearchUsers {

        @Test
        @DisplayName("should find users by email")
        void shouldFindUsersByEmail() {
            when(userRepository.findAll()).thenReturn(List.of(testUser));

            List<AdminUserListDto> result = adminUserService.searchUsers("test@");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo("test@test.com");
        }

        @Test
        @DisplayName("should find users by name (case insensitive)")
        void shouldFindUsersByNameCaseInsensitive() {
            when(userRepository.findAll()).thenReturn(List.of(testUser));

            List<AdminUserListDto> result = adminUserService.searchUsers("TEST USER");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list when no match")
        void shouldReturnEmptyWhenNoMatch() {
            when(userRepository.findAll()).thenReturn(List.of(testUser));

            List<AdminUserListDto> result = adminUserService.searchUsers("nonexistent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTotalUserCount")
    class GetTotalUserCount {

        @Test
        @DisplayName("should return total user count")
        void shouldReturnTotalUserCount() {
            when(userRepository.count()).thenReturn(42L);

            long result = adminUserService.getTotalUserCount();

            assertThat(result).isEqualTo(42L);
        }
    }
}
