package com.ecommerce.sellerx.users;

import com.ecommerce.sellerx.common.BaseControllerTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController.
 * Tests profile, password, preferences, and selected store endpoints.
 */
@DisplayName("UserController")
class UserControllerTest extends BaseControllerTest {

    @Autowired
    private UserRepository testUserRepository;

    @BeforeEach
    void setUpUsers() {
        testUserRepository.deleteAll();
        TestDataBuilder.resetSequence();
    }

    @Nested
    @DisplayName("GET /users/profile")
    class GetProfile {

        @Test
        @DisplayName("should return user profile for authenticated user")
        void shouldReturnProfileForAuthenticatedUser() throws Exception {
            // Given
            User user = createAndSaveTestUser("profile@example.com");

            // When/Then
            performWithAuth(get("/users/profile"), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("profile@example.com"))
                    .andExpect(jsonPath("$.name").value("Test User"));
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticated() throws Exception {
            // When/Then
            performWithoutAuth(get("/users/profile"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /users/password")
    class ChangePassword {

        @Test
        @DisplayName("should change password with correct old password")
        void shouldChangePasswordWithCorrectOldPassword() throws Exception {
            // Given
            User user = createAndSaveTestUser("pwd-change@example.com");
            String requestBody = """
                {
                    "oldPassword": "password123",
                    "newPassword": "newSecurePassword456"
                }
                """;

            // When/Then
            performWithAuth(put("/users/password").content(requestBody), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 400 for incorrect old password")
        void shouldReturn400ForIncorrectOldPassword() throws Exception {
            // Given
            User user = createAndSaveTestUser("pwd-fail@example.com");
            String requestBody = """
                {
                    "oldPassword": "wrongOldPassword",
                    "newPassword": "newSecurePassword456"
                }
                """;

            // When/Then
            performWithAuth(put("/users/password").content(requestBody), user)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Current password is incorrect"));
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticated() throws Exception {
            // Given
            String requestBody = """
                {
                    "oldPassword": "password123",
                    "newPassword": "newSecurePassword456"
                }
                """;

            // When/Then
            performWithoutAuth(put("/users/password").content(requestBody))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /users/preferences")
    class GetPreferences {

        @Test
        @DisplayName("should return default preferences for new user")
        void shouldReturnDefaultPreferencesForNewUser() throws Exception {
            // Given
            User user = createAndSaveTestUser("prefs@example.com");

            // When/Then
            performWithAuth(get("/users/preferences"), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.language").value("tr"))
                    .andExpect(jsonPath("$.theme").value("light"))
                    .andExpect(jsonPath("$.currency").value("TRY"));
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticated() throws Exception {
            // When/Then
            performWithoutAuth(get("/users/preferences"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /users/preferences")
    class UpdatePreferences {

        @Test
        @DisplayName("should update preferences with valid request")
        void shouldUpdatePreferencesWithValidRequest() throws Exception {
            // Given
            User user = createAndSaveTestUser("prefs-update@example.com");
            String requestBody = """
                {
                    "language": "en",
                    "theme": "dark",
                    "currency": "USD"
                }
                """;

            // When/Then
            performWithAuth(put("/users/preferences").content(requestBody), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.language").value("en"))
                    .andExpect(jsonPath("$.theme").value("dark"))
                    .andExpect(jsonPath("$.currency").value("USD"));
        }

        @Test
        @DisplayName("should support partial preference update")
        void shouldSupportPartialPreferenceUpdate() throws Exception {
            // Given
            User user = createAndSaveTestUser("partial-prefs@example.com");
            // First set some preferences
            String initialBody = """
                {
                    "language": "en",
                    "theme": "dark"
                }
                """;
            performWithAuth(put("/users/preferences").content(initialBody), user)
                    .andExpect(status().isOk());

            // Now update only language
            String partialBody = """
                {
                    "language": "tr"
                }
                """;

            // When/Then
            performWithAuth(put("/users/preferences").content(partialBody), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.language").value("tr"))
                    .andExpect(jsonPath("$.theme").value("dark"));
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticated() throws Exception {
            // Given
            String requestBody = """
                {
                    "language": "en"
                }
                """;

            // When/Then
            performWithoutAuth(put("/users/preferences").content(requestBody))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /users/selected-store")
    class SetSelectedStore {

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticated() throws Exception {
            // Given
            String requestBody = """
                {
                    "storeId": "00000000-0000-0000-0000-000000000001"
                }
                """;

            // When/Then
            performWithoutAuth(post("/users/selected-store").content(requestBody))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 for empty store ID")
        void shouldReturn400ForEmptyStoreId() throws Exception {
            // Given
            User user = createAndSaveTestUser("store-select@example.com");
            String requestBody = """
                {
                    "storeId": ""
                }
                """;

            // When/Then
            performWithAuth(post("/users/selected-store").content(requestBody), user)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for invalid store ID format")
        void shouldReturn400ForInvalidStoreIdFormat() throws Exception {
            // Given
            User user = createAndSaveTestUser("store-invalid@example.com");
            String requestBody = """
                {
                    "storeId": "not-a-uuid"
                }
                """;

            // When/Then
            performWithAuth(post("/users/selected-store").content(requestBody), user)
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /users (register)")
    class RegisterUser {

        @Test
        @DisplayName("should register new user with valid request")
        void shouldRegisterNewUserWithValidRequest() throws Exception {
            // Given
            String requestBody = """
                {
                    "name": "New User",
                    "email": "newuser@example.com",
                    "password": "securePassword123"
                }
                """;

            // When/Then
            performWithoutAuth(post("/users").content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("New User"))
                    .andExpect(jsonPath("$.email").value("newuser@example.com"))
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should return 400 for duplicate email")
        void shouldReturn400ForDuplicateEmail() throws Exception {
            // Given - first register a user
            createAndSaveTestUser("duplicate@example.com");

            String requestBody = """
                {
                    "name": "Duplicate User",
                    "email": "duplicate@example.com",
                    "password": "securePassword123"
                }
                """;

            // When/Then
            performWithoutAuth(post("/users").content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").value("Email is already registered."));
        }
    }
}
