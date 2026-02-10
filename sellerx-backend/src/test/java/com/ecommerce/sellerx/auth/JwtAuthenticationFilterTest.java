package com.ecommerce.sellerx.auth;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.users.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 * Tests token extraction from headers/cookies and security context setup.
 */
@DisplayName("JwtAuthenticationFilter")
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterTest extends BaseUnitTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Jwt jwt;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();

        // Default request setup
        lenient().when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Nested
    @DisplayName("Token Extraction")
    class TokenExtraction {

        @Test
        @DisplayName("should extract token from Authorization header")
        void shouldExtractTokenFromAuthorizationHeader() throws Exception {
            // Given
            String token = "valid-access-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(request.getCookies()).thenReturn(null);
            when(jwtService.parseToken(token)).thenReturn(jwt);
            when(jwt.isExpired()).thenReturn(false);
            when(jwt.getUserId()).thenReturn(1L);
            when(jwt.getRole()).thenReturn(Role.USER);
            when(jwt.isImpersonated()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtService).parseToken(token);
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should extract token from cookie when no Authorization header")
        void shouldExtractTokenFromCookieWhenNoAuthorizationHeader() throws Exception {
            // Given
            String token = "cookie-access-token";
            Cookie accessTokenCookie = new Cookie("access_token", token);
            when(request.getHeader("Authorization")).thenReturn(null);
            when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie});
            when(jwtService.parseToken(token)).thenReturn(jwt);
            when(jwt.isExpired()).thenReturn(false);
            when(jwt.getUserId()).thenReturn(2L);
            when(jwt.getRole()).thenReturn(Role.USER);
            when(jwt.isImpersonated()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtService).parseToken(token);
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(2L);
        }

        @Test
        @DisplayName("should prefer Authorization header over cookie")
        void shouldPreferAuthorizationHeaderOverCookie() throws Exception {
            // Given
            String headerToken = "header-token";
            String cookieToken = "cookie-token";
            Cookie accessTokenCookie = new Cookie("access_token", cookieToken);

            when(request.getHeader("Authorization")).thenReturn("Bearer " + headerToken);
            when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie});
            when(jwtService.parseToken(headerToken)).thenReturn(jwt);
            when(jwt.isExpired()).thenReturn(false);
            when(jwt.getUserId()).thenReturn(1L);
            when(jwt.getRole()).thenReturn(Role.USER);
            when(jwt.isImpersonated()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtService).parseToken(headerToken);
            verify(jwtService, never()).parseToken(cookieToken);
        }

        @Test
        @DisplayName("should ignore other cookies")
        void shouldIgnoreOtherCookies() throws Exception {
            // Given
            Cookie otherCookie = new Cookie("other_cookie", "some-value");
            Cookie sessionCookie = new Cookie("JSESSIONID", "session-id");

            when(request.getHeader("Authorization")).thenReturn(null);
            when(request.getCookies()).thenReturn(new Cookie[]{otherCookie, sessionCookie});

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verifyNoInteractions(jwtService);
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("No Token Scenarios")
    class NoTokenScenarios {

        @Test
        @DisplayName("should continue filter chain when no token present")
        void shouldContinueFilterChainWhenNoTokenPresent() throws Exception {
            // Given
            when(request.getHeader("Authorization")).thenReturn(null);
            when(request.getCookies()).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("should continue filter chain when Authorization header is not Bearer")
        void shouldContinueFilterChainWhenAuthorizationHeaderIsNotBearer() throws Exception {
            // Given
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");
            when(request.getCookies()).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("should continue filter chain when cookies array is empty")
        void shouldContinueFilterChainWhenCookiesArrayIsEmpty() throws Exception {
            // Given
            when(request.getHeader("Authorization")).thenReturn(null);
            when(request.getCookies()).thenReturn(new Cookie[]{});

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Invalid Token Scenarios")
    class InvalidTokenScenarios {

        @Test
        @DisplayName("should continue filter chain when token parsing fails")
        void shouldContinueFilterChainWhenTokenParsingFails() throws Exception {
            // Given
            String invalidToken = "invalid-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
            when(request.getCookies()).thenReturn(null);
            when(jwtService.parseToken(invalidToken)).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtService).parseToken(invalidToken);
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should continue filter chain when token is expired")
        void shouldContinueFilterChainWhenTokenIsExpired() throws Exception {
            // Given
            String expiredToken = "expired-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);
            when(request.getCookies()).thenReturn(null);
            when(jwtService.parseToken(expiredToken)).thenReturn(jwt);
            when(jwt.isExpired()).thenReturn(true);
            when(jwt.getUserId()).thenReturn(1L);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtService).parseToken(expiredToken);
            verify(jwt).isExpired();
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should continue filter chain when token is malformed")
        void shouldContinueFilterChainWhenTokenIsMalformed() throws Exception {
            // Given
            String malformedToken = "not.a.valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + malformedToken);
            when(request.getCookies()).thenReturn(null);
            when(jwtService.parseToken(malformedToken)).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtService).parseToken(malformedToken);
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Valid Token Scenarios")
    class ValidTokenScenarios {

        @Test
        @DisplayName("should set authentication for valid user token")
        void shouldSetAuthenticationForValidUserToken() throws Exception {
            // Given
            String validToken = "valid-user-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(request.getCookies()).thenReturn(null);
            when(jwtService.parseToken(validToken)).thenReturn(jwt);
            when(jwt.isExpired()).thenReturn(false);
            when(jwt.getUserId()).thenReturn(42L);
            when(jwt.getRole()).thenReturn(Role.USER);
            when(jwt.isImpersonated()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);

            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isEqualTo(42L);
            assertThat(authentication.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("should set authentication for valid admin token")
        void shouldSetAuthenticationForValidAdminToken() throws Exception {
            // Given
            String validToken = "valid-admin-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(request.getCookies()).thenReturn(null);
            when(jwtService.parseToken(validToken)).thenReturn(jwt);
            when(jwt.isExpired()).thenReturn(false);
            when(jwt.getUserId()).thenReturn(1L);
            when(jwt.getRole()).thenReturn(Role.ADMIN);
            when(jwt.isImpersonated()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("Impersonation Scenarios")
    class ImpersonationScenarios {

        @Test
        @DisplayName("should set impersonation attributes for impersonated token")
        void shouldSetImpersonationAttributesForImpersonatedToken() throws Exception {
            // Given
            String impersonationToken = "impersonation-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + impersonationToken);
            when(request.getCookies()).thenReturn(null);
            when(jwtService.parseToken(impersonationToken)).thenReturn(jwt);
            when(jwt.isExpired()).thenReturn(false);
            when(jwt.getUserId()).thenReturn(100L); // Target user
            when(jwt.getRole()).thenReturn(Role.USER);
            when(jwt.isImpersonated()).thenReturn(true);
            when(jwt.getImpersonatedBy()).thenReturn(1L); // Admin who is impersonating

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(request).setAttribute("impersonatedBy", 1L);
            verify(request).setAttribute("readOnly", true);
            verify(filterChain).doFilter(request, response);

            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should not set impersonation attributes for non-impersonated token")
        void shouldNotSetImpersonationAttributesForNonImpersonatedToken() throws Exception {
            // Given
            String normalToken = "normal-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + normalToken);
            when(request.getCookies()).thenReturn(null);
            when(jwtService.parseToken(normalToken)).thenReturn(jwt);
            when(jwt.isExpired()).thenReturn(false);
            when(jwt.getUserId()).thenReturn(1L);
            when(jwt.getRole()).thenReturn(Role.USER);
            when(jwt.isImpersonated()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(request, never()).setAttribute(eq("impersonatedBy"), any());
            verify(request, never()).setAttribute(eq("readOnly"), any());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle empty Bearer token")
        void shouldHandleEmptyBearerToken() throws Exception {
            // Given
            when(request.getHeader("Authorization")).thenReturn("Bearer ");
            when(request.getCookies()).thenReturn(null);
            when(jwtService.parseToken("")).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should handle multiple cookies with access_token present")
        void shouldHandleMultipleCookiesWithAccessTokenPresent() throws Exception {
            // Given
            String token = "access-token-value";
            Cookie otherCookie1 = new Cookie("session", "session-value");
            Cookie accessTokenCookie = new Cookie("access_token", token);
            Cookie otherCookie2 = new Cookie("preferences", "pref-value");

            when(request.getHeader("Authorization")).thenReturn(null);
            when(request.getCookies()).thenReturn(new Cookie[]{otherCookie1, accessTokenCookie, otherCookie2});
            when(jwtService.parseToken(token)).thenReturn(jwt);
            when(jwt.isExpired()).thenReturn(false);
            when(jwt.getUserId()).thenReturn(1L);
            when(jwt.getRole()).thenReturn(Role.USER);
            when(jwt.isImpersonated()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtService).parseToken(token);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        }

        @Test
        @DisplayName("should preserve request details in authentication")
        void shouldPreserveRequestDetailsInAuthentication() throws Exception {
            // Given
            String validToken = "valid-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(request.getCookies()).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            when(request.getSession(false)).thenReturn(null);
            when(jwtService.parseToken(validToken)).thenReturn(jwt);
            when(jwt.isExpired()).thenReturn(false);
            when(jwt.getUserId()).thenReturn(1L);
            when(jwt.getRole()).thenReturn(Role.USER);
            when(jwt.isImpersonated()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getDetails()).isNotNull();
        }
    }
}
