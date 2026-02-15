package com.ecommerce.sellerx.websocket;

import com.ecommerce.sellerx.auth.Jwt;
import com.ecommerce.sellerx.auth.JwtService;
import com.ecommerce.sellerx.users.Role;
import com.ecommerce.sellerx.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebSocketAuthInterceptor.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageChannel channel;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    private StompHeaderAccessor accessor;

    @BeforeEach
    void setUp() {
        accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    }

    @Test
    @DisplayName("Should parse token with Bearer prefix in Authorization header")
    void shouldParseTokenWithBearerPrefix() {
        // Given
        accessor.addNativeHeader("Authorization", "Bearer valid-token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.parseToken("valid-token")).thenReturn(jwt);
        when(jwt.isExpired()).thenReturn(false);
        when(jwt.getUserId()).thenReturn(123L);
        when(jwt.getRole()).thenReturn(Role.USER);

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        assertThat(result).isNotNull();
        verify(jwtService).parseToken("valid-token");
        verify(jwt).isExpired();
        verify(jwt).getUserId();
        verify(jwt).getRole();
    }

    @Test
    @DisplayName("Should parse token from custom token header")
    void shouldParseTokenFromTokenHeader() {
        // Given
        accessor.addNativeHeader("token", "custom-header-token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.parseToken("custom-header-token")).thenReturn(jwt);
        when(jwt.isExpired()).thenReturn(false);
        when(jwt.getUserId()).thenReturn(456L);
        when(jwt.getRole()).thenReturn(Role.USER);

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        assertThat(result).isNotNull();
        verify(jwtService).parseToken("custom-header-token");
    }

    @Test
    @DisplayName("Should parse token from X-Auth-Token header")
    void shouldParseTokenFromXAuthTokenHeader() {
        // Given
        accessor.addNativeHeader("X-Auth-Token", "x-auth-token-value");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.parseToken("x-auth-token-value")).thenReturn(jwt);
        when(jwt.isExpired()).thenReturn(false);
        when(jwt.getUserId()).thenReturn(789L);
        when(jwt.getRole()).thenReturn(Role.USER);

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        assertThat(result).isNotNull();
        verify(jwtService).parseToken("x-auth-token-value");
    }

    @Test
    @DisplayName("Should return message for CONNECT with no token")
    void shouldReturnMessageForConnectWithNoToken() {
        // Given
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then - message is returned but without authentication
        assertThat(result).isNotNull();
        verify(jwtService, never()).parseToken(anyString());
    }

    @Test
    @DisplayName("Should not set user when token is invalid")
    void shouldNotSetUserWhenTokenIsInvalid() {
        // Given
        accessor.addNativeHeader("Authorization", "Bearer invalid-token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.parseToken("invalid-token")).thenReturn(null);

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        assertThat(result).isNotNull();
        verify(jwtService).parseToken("invalid-token");
        // jwt.isExpired() should never be called since parseToken returned null
        verify(jwt, never()).isExpired();
    }

    @Test
    @DisplayName("Should not set user when token is expired")
    void shouldNotSetUserWhenTokenIsExpired() {
        // Given
        accessor.addNativeHeader("Authorization", "Bearer expired-token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.parseToken("expired-token")).thenReturn(jwt);
        when(jwt.isExpired()).thenReturn(true);

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        assertThat(result).isNotNull();
        verify(jwtService).parseToken("expired-token");
        verify(jwt).isExpired();
        // getUserId should not be called since token is expired
        verify(jwt, never()).getUserId();
    }

    @Test
    @DisplayName("Should pass through non-CONNECT commands")
    void shouldPassThroughNonConnectCommands() {
        // Given
        StompHeaderAccessor subscribeAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        Message<?> message = MessageBuilder.createMessage(new byte[0], subscribeAccessor.getMessageHeaders());

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        assertThat(result).isNotNull();
        verify(jwtService, never()).parseToken(anyString());
    }

    @Test
    @DisplayName("Should pass through SEND commands")
    void shouldPassThroughSendCommands() {
        // Given
        StompHeaderAccessor sendAccessor = StompHeaderAccessor.create(StompCommand.SEND);
        Message<?> message = MessageBuilder.createMessage(new byte[0], sendAccessor.getMessageHeaders());

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        assertThat(result).isNotNull();
        verify(jwtService, never()).parseToken(anyString());
    }

    @Test
    @DisplayName("Should pass through DISCONNECT commands")
    void shouldPassThroughDisconnectCommands() {
        // Given
        StompHeaderAccessor disconnectAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], disconnectAccessor.getMessageHeaders());

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        assertThat(result).isNotNull();
        verify(jwtService, never()).parseToken(anyString());
    }

    @Test
    @DisplayName("Should handle exception gracefully during token parsing")
    void shouldHandleExceptionGracefully() {
        // Given
        accessor.addNativeHeader("Authorization", "Bearer error-token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.parseToken("error-token")).thenThrow(new RuntimeException("Parse error"));

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then - should return message without exception propagation
        assertThat(result).isNotNull();
        verify(jwtService).parseToken("error-token");
    }

    @Test
    @DisplayName("Should prioritize Authorization header over other headers")
    void shouldPrioritizeAuthorizationHeader() {
        // Given - all three headers present
        accessor.addNativeHeader("Authorization", "Bearer auth-token");
        accessor.addNativeHeader("token", "token-header");
        accessor.addNativeHeader("X-Auth-Token", "x-auth-header");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.parseToken("auth-token")).thenReturn(jwt);
        when(jwt.isExpired()).thenReturn(false);
        when(jwt.getUserId()).thenReturn(123L);
        when(jwt.getRole()).thenReturn(Role.USER);

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then - should use Authorization header
        assertThat(result).isNotNull();
        verify(jwtService).parseToken("auth-token");
        verify(jwtService, never()).parseToken("token-header");
        verify(jwtService, never()).parseToken("x-auth-header");
    }
}
