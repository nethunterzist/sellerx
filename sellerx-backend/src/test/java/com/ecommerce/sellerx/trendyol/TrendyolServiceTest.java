package com.ecommerce.sellerx.trendyol;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TrendyolService.
 * Tests API call construction, error handling, and connection testing.
 */
@DisplayName("TrendyolService")
class TrendyolServiceTest extends BaseUnitTest {

    @Mock
    private RestTemplate restTemplate;

    private TrendyolService trendyolService;

    private User testUser;
    private Store testStore;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        trendyolService = new TrendyolService(restTemplate, meterRegistry);
        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);
        testStore = TestDataBuilder.store(testUser).build();
    }

    @Nested
    @DisplayName("testConnection")
    class TestConnection {

        @Test
        @DisplayName("should return success for valid connection")
        void shouldReturnSuccessForValidConnection() {
            // Given
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));

            // When
            TrendyolConnectionResult result = trendyolService.testConnection(testStore);

            // Then
            assertThat(result.isConnected())
                    .as("Connection should be successful")
                    .isTrue();
            assertThat(result.getStatusCode())
                    .as("Status code should be 200")
                    .isEqualTo(200);
            assertThat(result.getMessage())
                    .as("Message should indicate success")
                    .isEqualTo("Connection successful");
        }

        @Test
        @DisplayName("should construct proper Basic Auth header")
        void shouldConstructProperBasicAuthHeader() {
            // Given
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));

            // When
            trendyolService.testConnection(testStore);

            // Then
            ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), entityCaptor.capture(), eq(String.class));

            HttpEntity<String> capturedEntity = entityCaptor.getValue();
            String authHeader = capturedEntity.getHeaders().getFirst("Authorization");

            assertThat(authHeader)
                    .as("Authorization header should start with 'Basic '")
                    .startsWith("Basic ");

            // Decode and verify the credentials
            TrendyolCredentials creds = (TrendyolCredentials) testStore.getCredentials();
            String expectedAuth = creds.getApiKey() + ":" + creds.getApiSecret();
            String expectedEncoded = Base64.getEncoder().encodeToString(expectedAuth.getBytes());

            assertThat(authHeader)
                    .as("Authorization header should contain correctly encoded credentials")
                    .isEqualTo("Basic " + expectedEncoded);
        }

        @Test
        @DisplayName("should construct URL with seller ID")
        void shouldConstructUrlWithSellerId() {
            // Given
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));

            // When
            trendyolService.testConnection(testStore);

            // Then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(), eq(String.class));

            TrendyolCredentials creds = (TrendyolCredentials) testStore.getCredentials();
            assertThat(urlCaptor.getValue())
                    .as("URL should contain the seller ID")
                    .contains(creds.getSellerId().toString());
        }

        @Test
        @DisplayName("should handle 401 unauthorized error")
        void shouldHandle401UnauthorizedError() {
            // Given
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new org.springframework.web.client.HttpClientErrorException(HttpStatus.UNAUTHORIZED, "401 Unauthorized"));

            // When
            TrendyolConnectionResult result = trendyolService.testConnection(testStore);

            // Then
            assertThat(result.isConnected())
                    .as("Connection should not be successful for 401")
                    .isFalse();
            assertThat(result.getStatusCode())
                    .as("Status code should be 401")
                    .isEqualTo(401);
            assertThat(result.getMessage())
                    .as("Message should indicate invalid credentials")
                    .isEqualTo("Invalid API credentials");
        }

        @Test
        @DisplayName("should handle 403 forbidden error")
        void shouldHandle403ForbiddenError() {
            // Given
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new org.springframework.web.client.HttpClientErrorException(HttpStatus.FORBIDDEN, "403 Forbidden"));

            // When
            TrendyolConnectionResult result = trendyolService.testConnection(testStore);

            // Then
            assertThat(result.isConnected()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(403);
            assertThat(result.getMessage()).contains("Access forbidden");
        }

        @Test
        @DisplayName("should handle timeout error")
        void shouldHandleTimeoutError() {
            // Given
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection timeout"));

            // When
            TrendyolConnectionResult result = trendyolService.testConnection(testStore);

            // Then
            assertThat(result.isConnected()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(408);
            assertThat(result.getMessage()).contains("timeout");
        }

        @Test
        @DisplayName("should handle generic server error")
        void shouldHandleGenericServerError() {
            // Given
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new RuntimeException("Unexpected server error occurred"));

            // When
            TrendyolConnectionResult result = trendyolService.testConnection(testStore);

            // Then
            assertThat(result.isConnected()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(500);
        }

        @Test
        @DisplayName("should return error when store has null credentials")
        void shouldReturnErrorWhenCredentialsNull() {
            // Given
            testStore.setCredentials(null);

            // When
            TrendyolConnectionResult result = trendyolService.testConnection(testStore);

            // Then
            assertThat(result.isConnected()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getMessage()).contains("credentials not found");
        }
    }

    @Nested
    @DisplayName("testCredentials")
    class TestCredentials {

        @Test
        @DisplayName("should return success for valid credentials")
        void shouldReturnSuccessForValidCredentials() {
            // Given
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));

            // When
            TrendyolConnectionResult result = trendyolService.testCredentials("123456", "apiKey", "apiSecret");

            // Then
            assertThat(result.isConnected()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(200);
            assertThat(result.getSellerId()).isEqualTo("123456");
        }

        @Test
        @DisplayName("should handle 404 not found")
        void shouldHandle404NotFound() {
            // Given
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new org.springframework.web.client.HttpClientErrorException(HttpStatus.NOT_FOUND, "404 Not Found"));

            // When
            TrendyolConnectionResult result = trendyolService.testCredentials("999999", "apiKey", "apiSecret");

            // Then
            assertThat(result.isConnected()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getMessage()).contains("Seller not found");
        }
    }
}
