package com.ecommerce.sellerx.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebSocketConfig.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private WebSocketAuthInterceptor authInterceptor;

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    @Mock
    private StompEndpointRegistry stompEndpointRegistry;

    @Mock
    private StompWebSocketEndpointRegistration endpointRegistration;

    @Mock
    private SockJsServiceRegistration sockJsServiceRegistration;

    @Mock
    private ChannelRegistration channelRegistration;

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @BeforeEach
    void setUp() {
        // Setup is handled by @InjectMocks
    }

    @Test
    @DisplayName("Should configure message broker with correct prefixes")
    void shouldConfigureMessageBroker() {
        // Given
        when(messageBrokerRegistry.enableSimpleBroker(any(String[].class)))
                .thenReturn(null);
        when(messageBrokerRegistry.setApplicationDestinationPrefixes(any(String[].class)))
                .thenReturn(messageBrokerRegistry);
        when(messageBrokerRegistry.setUserDestinationPrefix(anyString()))
                .thenReturn(messageBrokerRegistry);

        // When
        webSocketConfig.configureMessageBroker(messageBrokerRegistry);

        // Then
        verify(messageBrokerRegistry).enableSimpleBroker("/topic", "/queue");
        verify(messageBrokerRegistry).setApplicationDestinationPrefixes("/app");
        verify(messageBrokerRegistry).setUserDestinationPrefix("/user");
    }

    @Test
    @DisplayName("Should register STOMP endpoints with SockJS and native WebSocket")
    void shouldRegisterStompEndpoints() {
        // Given - Setup full chain mocks
        when(stompEndpointRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOriginPatterns(any(String[].class)))
                .thenReturn(endpointRegistration);
        when(endpointRegistration.withSockJS()).thenReturn(sockJsServiceRegistration);

        // When
        webSocketConfig.registerStompEndpoints(stompEndpointRegistry);

        // Then - verify both endpoints are registered
        verify(stompEndpointRegistry, times(2)).addEndpoint("/ws");
        verify(endpointRegistration, times(2)).setAllowedOriginPatterns("*");
        // SockJS only called once (for the first endpoint)
        verify(endpointRegistration, times(1)).withSockJS();
    }

    @Test
    @DisplayName("Should configure client inbound channel with auth interceptor")
    void shouldConfigureClientInboundChannel() {
        // Given
        when(channelRegistration.interceptors(any())).thenReturn(channelRegistration);

        // When
        webSocketConfig.configureClientInboundChannel(channelRegistration);

        // Then
        verify(channelRegistration).interceptors(authInterceptor);
    }
}
