package com.ecommerce.sellerx.auth;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.users.Role;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Tests authentication, token refresh, and current user retrieval.
 */
@DisplayName("AuthService")
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest extends BaseUnitTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private AuthService authService;
    private User testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, userRepository, jwtService);

        testUser = TestDataBuilder.user()
                .id(1L)
                .email("test@example.com")
                .role(Role.USER)
                .build();

        // Setup SecurityContext mock
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * Helper method to create LoginRequest with email and password.
     */
    private LoginRequest createLoginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should return tokens on successful login")
        void shouldReturnTokensOnSuccessfulLogin() {
            // Given
            LoginRequest request = createLoginRequest("test@example.com", "password123");
            Jwt mockAccessToken = mock(Jwt.class);
            Jwt mockRefreshToken = mock(Jwt.class);

            when(mockAccessToken.toString()).thenReturn("access-token-string");
            when(mockRefreshToken.toString()).thenReturn("refresh-token-string");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(jwtService.generateAccessToken(testUser)).thenReturn(mockAccessToken);
            when(jwtService.generateRefreshToken(testUser)).thenReturn(mockRefreshToken);

            // When
            LoginResponse response = authService.login(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo(mockAccessToken);
            assertThat(response.getRefreshToken()).isEqualTo(mockRefreshToken);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userRepository).findByEmail("test@example.com");
            verify(jwtService).generateAccessToken(testUser);
            verify(jwtService).generateRefreshToken(testUser);
        }

        @Test
        @DisplayName("should throw BadCredentialsException for invalid credentials")
        void shouldThrowBadCredentialsExceptionForInvalidCredentials() {
            // Given
            LoginRequest request = createLoginRequest("test@example.com", "wrongpassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Bad credentials");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("should throw exception when user not found after authentication")
        void shouldThrowExceptionWhenUserNotFoundAfterAuthentication() {
            // Given
            LoginRequest request = createLoginRequest("nonexistent@example.com", "password123");

            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(NoSuchElementException.class);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userRepository).findByEmail("nonexistent@example.com");
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("should authenticate admin user successfully")
        void shouldAuthenticateAdminUserSuccessfully() {
            // Given
            User adminUser = TestDataBuilder.adminUser()
                    .id(2L)
                    .email("admin@example.com")
                    .build();
            LoginRequest request = createLoginRequest("admin@example.com", "adminpass");
            Jwt mockAccessToken = mock(Jwt.class);
            Jwt mockRefreshToken = mock(Jwt.class);

            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
            when(jwtService.generateAccessToken(adminUser)).thenReturn(mockAccessToken);
            when(jwtService.generateRefreshToken(adminUser)).thenReturn(mockRefreshToken);

            // When
            LoginResponse response = authService.login(request);

            // Then
            assertThat(response).isNotNull();
            verify(jwtService).generateAccessToken(adminUser);
        }
    }

    @Nested
    @DisplayName("refreshAccessToken")
    class RefreshAccessToken {

        @Test
        @DisplayName("should return new access token for valid refresh token")
        void shouldReturnNewAccessTokenForValidRefreshToken() {
            // Given
            String refreshTokenString = "valid-refresh-token";
            Jwt mockRefreshToken = mock(Jwt.class);
            Jwt mockNewAccessToken = mock(Jwt.class);

            when(mockRefreshToken.isExpired()).thenReturn(false);
            when(mockRefreshToken.getUserId()).thenReturn(1L);
            when(jwtService.parseToken(refreshTokenString)).thenReturn(mockRefreshToken);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(jwtService.generateAccessToken(testUser)).thenReturn(mockNewAccessToken);

            // When
            Jwt result = authService.refreshAccessToken(refreshTokenString);

            // Then
            assertThat(result).isEqualTo(mockNewAccessToken);
            verify(jwtService).parseToken(refreshTokenString);
            verify(userRepository).findById(1L);
            verify(jwtService).generateAccessToken(testUser);
        }

        @Test
        @DisplayName("should throw BadCredentialsException for expired refresh token")
        void shouldThrowBadCredentialsExceptionForExpiredRefreshToken() {
            // Given
            String expiredRefreshToken = "expired-refresh-token";
            Jwt mockRefreshToken = mock(Jwt.class);

            when(mockRefreshToken.isExpired()).thenReturn(true);
            when(jwtService.parseToken(expiredRefreshToken)).thenReturn(mockRefreshToken);

            // When/Then
            assertThatThrownBy(() -> authService.refreshAccessToken(expiredRefreshToken))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid refresh token");

            verify(jwtService).parseToken(expiredRefreshToken);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("should throw BadCredentialsException for invalid refresh token")
        void shouldThrowBadCredentialsExceptionForInvalidRefreshToken() {
            // Given
            String invalidRefreshToken = "invalid-token";

            when(jwtService.parseToken(invalidRefreshToken)).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> authService.refreshAccessToken(invalidRefreshToken))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid refresh token");

            verify(jwtService).parseToken(invalidRefreshToken);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("should throw exception when user not found during refresh")
        void shouldThrowExceptionWhenUserNotFoundDuringRefresh() {
            // Given
            String refreshTokenString = "valid-refresh-token";
            Jwt mockRefreshToken = mock(Jwt.class);

            when(mockRefreshToken.isExpired()).thenReturn(false);
            when(mockRefreshToken.getUserId()).thenReturn(999L);
            when(jwtService.parseToken(refreshTokenString)).thenReturn(mockRefreshToken);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.refreshAccessToken(refreshTokenString))
                    .isInstanceOf(NoSuchElementException.class);

            verify(jwtService).parseToken(refreshTokenString);
            verify(userRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {

        @Test
        @DisplayName("should return current user from security context")
        void shouldReturnCurrentUserFromSecurityContext() {
            // Given
            when(authentication.getPrincipal()).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            User result = authService.getCurrentUser();

            // Then
            assertThat(result).isEqualTo(testUser);
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("should return null when user not found")
        void shouldReturnNullWhenUserNotFound() {
            // Given
            when(authentication.getPrincipal()).thenReturn(999L);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            User result = authService.getCurrentUser();

            // Then
            assertThat(result).isNull();
            verify(userRepository).findById(999L);
        }

        @Test
        @DisplayName("should handle admin user correctly")
        void shouldHandleAdminUserCorrectly() {
            // Given
            User adminUser = TestDataBuilder.adminUser()
                    .id(2L)
                    .build();

            when(authentication.getPrincipal()).thenReturn(2L);
            when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

            // When
            User result = authService.getCurrentUser();

            // Then
            assertThat(result).isEqualTo(adminUser);
            assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        }
    }
}
