package com.ecommerce.sellerx.auth;

import com.ecommerce.sellerx.common.BaseControllerTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.users.Role;
import com.ecommerce.sellerx.users.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Tests login, logout, token refresh, and user info endpoints.
 */
@DisplayName("AuthController")
class AuthControllerTest extends BaseControllerTest {

    private static final String LOGIN_URL = "/auth/login";
    private static final String LOGOUT_URL = "/auth/logout";
    private static final String REFRESH_URL = "/auth/refresh";
    private static final String ME_URL = "/auth/me";

    private static final String TEST_EMAIL = "auth-test@example.com";
    private static final String TEST_PASSWORD = "password123";
    // BCrypt hash for "password123"
    private static final String HASHED_PASSWORD = "$2a$10$pbhA2lvr7KXc4gxwcrYbIu6rlsyi5IpASgzxG6Wcco0/VSGwR1g.K";

    @BeforeEach
    void setUpAuth() {
        cleanUpUsers();
        TestDataBuilder.resetSequence();
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("should return access token for valid credentials")
        void shouldReturnAccessTokenForValidCredentials() throws Exception {
            // Given
            User user = User.builder()
                    .name("Test User")
                    .email(TEST_EMAIL)
                    .password(HASHED_PASSWORD)
                    .role(Role.USER)
                    .build();
            userRepository.save(user);

            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(TEST_EMAIL, TEST_PASSWORD);

            // When/Then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(cookie().exists("access_token"))
                    .andExpect(cookie().exists("refreshToken"));
        }

        @Test
        @DisplayName("should return 401 for wrong password")
        void shouldReturn401ForWrongPassword() throws Exception {
            // Given
            User user = User.builder()
                    .name("Test User")
                    .email(TEST_EMAIL)
                    .password(HASHED_PASSWORD)
                    .role(Role.USER)
                    .build();
            userRepository.save(user);

            String requestBody = """
                {
                    "email": "%s",
                    "password": "wrongpassword"
                }
                """.formatted(TEST_EMAIL);

            // When/Then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 401 for non-existent user")
        void shouldReturn401ForNonExistentUser() throws Exception {
            // Given
            String requestBody = """
                {
                    "email": "nonexistent@example.com",
                    "password": "anypassword"
                }
                """;

            // When/Then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 for missing email")
        void shouldReturn400ForMissingEmail() throws Exception {
            // Given
            String requestBody = """
                {
                    "password": "anypassword"
                }
                """;

            // When/Then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for invalid email format")
        void shouldReturn400ForInvalidEmailFormat() throws Exception {
            // Given
            String requestBody = """
                {
                    "email": "not-an-email",
                    "password": "anypassword"
                }
                """;

            // When/Then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh")
    class Refresh {

        @Test
        @DisplayName("should return new access token for valid refresh token")
        void shouldReturnNewAccessTokenForValidRefreshToken() throws Exception {
            // Given
            User user = createAndSaveTestUser(TEST_EMAIL);
            Jwt refreshToken = jwtService.generateRefreshToken(user);
            Cookie refreshCookie = new Cookie("refreshToken", refreshToken.toString());

            // When/Then
            mockMvc.perform(post(REFRESH_URL)
                            .cookie(refreshCookie)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(cookie().exists("access_token"));
        }

        @Test
        @DisplayName("should return error for missing refresh token cookie")
        void shouldReturnErrorForMissingRefreshToken() throws Exception {
            // When/Then
            // Spring throws MissingRequestCookieException which results in 500 Internal Server Error
            // (unless a custom exception handler is added for this specific exception)
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("should return 401 for invalid refresh token")
        void shouldReturn401ForInvalidRefreshToken() throws Exception {
            // Given
            Cookie invalidRefreshCookie = new Cookie("refreshToken", "invalid.token.string");

            // When/Then
            mockMvc.perform(post(REFRESH_URL)
                            .cookie(invalidRefreshCookie)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /auth/me")
    class GetCurrentUser {

        @Test
        @DisplayName("should return current user for authenticated request")
        void shouldReturnCurrentUserForAuthenticatedRequest() throws Exception {
            // Given
            User user = createAndSaveTestUser(TEST_EMAIL);

            // When/Then
            performWithAuth(get(ME_URL), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.name").isNotEmpty());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() throws Exception {
            // When/Then
            performWithoutAuth(get(ME_URL))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /auth/logout")
    class Logout {

        @Test
        @DisplayName("should clear cookies on logout")
        void shouldClearCookiesOnLogout() throws Exception {
            // Given
            User user = createAndSaveTestUser(TEST_EMAIL);

            // When/Then
            performWithAuth(post(LOGOUT_URL), user)
                    .andExpect(status().isOk())
                    .andExpect(cookie().maxAge("access_token", 0))
                    .andExpect(cookie().maxAge("refreshToken", 0));
        }

        @Test
        @DisplayName("should work even without authentication")
        void shouldWorkWithoutAuthentication() throws Exception {
            // When/Then - logout should work even if not authenticated
            performWithoutAuth(post(LOGOUT_URL))
                    .andExpect(status().isOk());
        }
    }
}
