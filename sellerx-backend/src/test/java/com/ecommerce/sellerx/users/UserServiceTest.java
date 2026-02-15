package com.ecommerce.sellerx.users;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.referral.ReferralService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Tests user profile, password change, preferences, and selected store operations.
 */
@DisplayName("UserService")
class UserServiceTest extends BaseUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ReferralService referralService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private com.ecommerce.sellerx.auth.EmailVerificationService emailVerificationService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);
        testUser.setPreferences(new UserPreferences());

        testUserDto = new UserDto(
                testUser.getId(),
                testUser.getName(),
                testUser.getEmail(),
                null,
                testUser.getRole()
        );
    }

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @DisplayName("should return user DTO when user exists")
        void shouldReturnUserDtoWhenUserExists() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userMapper.toDto(testUser)).thenReturn(testUserDto);

            // When
            UserDto result = userService.getUser(1L);

            // Then
            assertThat(result)
                    .as("Returned DTO should match the test user")
                    .isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("should throw UserNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.getUser(999L))
                    .as("Should throw UserNotFoundException for non-existent user")
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("should change password when old password is correct")
        void shouldChangePasswordWhenOldPasswordCorrect() {
            // Given
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("password123");
            request.setNewPassword("newPassword456");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("newPassword456")).thenReturn("encoded-new-password");

            // When
            userService.changePassword(1L, request);

            // Then
            assertThat(testUser.getPassword())
                    .as("Password should be updated with encoded new password")
                    .isEqualTo("encoded-new-password");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when old password is incorrect")
        void shouldThrowWhenOldPasswordIncorrect() {
            // Given
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("wrongPassword");
            request.setNewPassword("newPassword456");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> userService.changePassword(1L, request))
                    .as("Should throw AccessDeniedException for incorrect old password")
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Password does not match");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw UserNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFoundForPasswordChange() {
            // Given
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("password123");
            request.setNewPassword("newPassword456");

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.changePassword(999L, request))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getPreferences")
    class GetPreferences {

        @Test
        @DisplayName("should return user preferences when they exist")
        void shouldReturnPreferencesWhenExist() {
            // Given
            UserPreferences prefs = UserPreferences.builder()
                    .language("en")
                    .theme("dark")
                    .currency("USD")
                    .build();
            testUser.setPreferences(prefs);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            UserPreferences result = userService.getPreferences(1L);

            // Then
            assertThat(result.getLanguage())
                    .as("Should return the stored language preference")
                    .isEqualTo("en");
            assertThat(result.getTheme())
                    .as("Should return the stored theme preference")
                    .isEqualTo("dark");
            assertThat(result.getCurrency())
                    .as("Should return the stored currency preference")
                    .isEqualTo("USD");
        }

        @Test
        @DisplayName("should return default preferences when user has null preferences")
        void shouldReturnDefaultPreferencesWhenNull() {
            // Given
            testUser.setPreferences(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            UserPreferences result = userService.getPreferences(1L);

            // Then
            assertThat(result)
                    .as("Should return non-null default preferences")
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("updatePreferences")
    class UpdatePreferences {

        @Test
        @DisplayName("should update only provided fields (partial update)")
        void shouldUpdateOnlyProvidedFields() {
            // Given
            UserPreferences existingPrefs = UserPreferences.builder()
                    .language("tr")
                    .theme("light")
                    .currency("TRY")
                    .build();
            testUser.setPreferences(existingPrefs);

            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language("en")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            UserPreferences result = userService.updatePreferences(1L, request);

            // Then
            assertThat(result.getLanguage())
                    .as("Language should be updated to 'en'")
                    .isEqualTo("en");
            assertThat(result.getTheme())
                    .as("Theme should remain 'light' (not provided in request)")
                    .isEqualTo("light");
            assertThat(result.getCurrency())
                    .as("Currency should remain 'TRY' (not provided in request)")
                    .isEqualTo("TRY");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should update sync interval with valid value")
        void shouldUpdateSyncIntervalWithValidValue() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .syncInterval(120)
                    .build();

            // When
            UserPreferences result = userService.updatePreferences(1L, request);

            // Then
            assertThat(result.getSyncInterval())
                    .as("Sync interval should be updated to valid value 120")
                    .isEqualTo(120);
        }

        @Test
        @DisplayName("should not update sync interval with invalid value")
        void shouldNotUpdateSyncIntervalWithInvalidValue() {
            // Given
            UserPreferences existingPrefs = UserPreferences.builder().syncInterval(60).build();
            testUser.setPreferences(existingPrefs);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .syncInterval(45) // 45 is not a valid value (0, 30, 60, 120, 300 are valid)
                    .build();

            // When
            UserPreferences result = userService.updatePreferences(1L, request);

            // Then
            assertThat(result.getSyncInterval())
                    .as("Sync interval should remain unchanged for invalid value")
                    .isEqualTo(60);
        }

        @Test
        @DisplayName("should update notification preferences")
        void shouldUpdateNotificationPreferences() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            UpdatePreferencesRequest.NotificationUpdate notifUpdate =
                    UpdatePreferencesRequest.NotificationUpdate.builder()
                            .email(false)
                            .weeklyReport(true)
                            .build();

            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .notifications(notifUpdate)
                    .build();

            // When
            UserPreferences result = userService.updatePreferences(1L, request);

            // Then
            assertThat(result.getNotifications().isEmail())
                    .as("Email notification should be disabled")
                    .isFalse();
            assertThat(result.getNotifications().isWeeklyReport())
                    .as("Weekly report should be enabled")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("selectedStore")
    class SelectedStore {

        @Test
        @DisplayName("should get selected store ID")
        void shouldGetSelectedStoreId() {
            // Given
            UUID storeId = UUID.randomUUID();
            testUser.setSelectedStoreId(storeId);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            UUID result = userService.getSelectedStoreId(1L);

            // Then
            assertThat(result)
                    .as("Should return the selected store ID")
                    .isEqualTo(storeId);
        }

        @Test
        @DisplayName("should set selected store ID")
        void shouldSetSelectedStoreId() {
            // Given
            UUID storeId = UUID.randomUUID();
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            userService.setSelectedStoreId(1L, storeId);

            // Then
            assertThat(testUser.getSelectedStoreId())
                    .as("User's selected store should be updated")
                    .isEqualTo(storeId);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should return null when no store is selected")
        void shouldReturnNullWhenNoStoreSelected() {
            // Given
            testUser.setSelectedStoreId(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            UUID result = userService.getSelectedStoreId(1L);

            // Then
            assertThat(result)
                    .as("Should return null when no store is selected")
                    .isNull();
        }

        @Test
        @DisplayName("should throw UserNotFoundException when user not found for setSelectedStoreId")
        void shouldThrowWhenUserNotFoundForSetSelectedStore() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.setSelectedStoreId(999L, UUID.randomUUID()))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("should throw DuplicateUserException when email already exists")
        void shouldThrowWhenEmailAlreadyExists() {
            // Given
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("existing@example.com");
            request.setName("Test");
            request.setPassword("password");

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.registerUser(request))
                    .as("Should throw DuplicateUserException for duplicate email")
                    .isInstanceOf(DuplicateUserException.class);
        }

        @Test
        @DisplayName("should register new user with encoded password")
        void shouldRegisterNewUserWithEncodedPassword() {
            // Given
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("new@example.com");
            request.setName("New User");
            request.setPassword("rawPassword");

            User mappedUser = User.builder()
                    .email("new@example.com")
                    .name("New User")
                    .password("rawPassword")
                    .build();

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(mappedUser);
            when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
            when(userMapper.toDto(any(User.class))).thenReturn(
                    new UserDto(1L, "New User", "new@example.com", null, Role.USER));

            // When
            UserDto result = userService.registerUser(request);

            // Then
            assertThat(mappedUser.getPassword())
                    .as("Password should be encoded before saving")
                    .isEqualTo("encodedPassword");
            assertThat(mappedUser.getRole())
                    .as("New user should have USER role")
                    .isEqualTo(Role.USER);
            verify(userRepository).save(mappedUser);
        }
    }
}
