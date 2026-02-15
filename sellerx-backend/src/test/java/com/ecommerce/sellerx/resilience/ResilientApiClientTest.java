package com.ecommerce.sellerx.resilience;

import com.ecommerce.sellerx.common.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ResilientApiClient")
class ResilientApiClientTest extends BaseUnitTest {

    @Mock
    private RestTemplate restTemplate;

    private ResilientApiClient client;

    @BeforeEach
    void setUp() {
        client = new ResilientApiClient(restTemplate);
    }

    @Nested
    @DisplayName("executeApiCall()")
    class ExecuteApiCall {

        @Test
        @DisplayName("should execute successful API call")
        void shouldExecuteSuccessfulApiCall() {
            // Given
            UUID storeId = UUID.randomUUID();
            String expectedResponse = "success";
            Supplier<String> apiCall = () -> expectedResponse;

            // When
            String result = client.executeApiCall(storeId, apiCall);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("should propagate server errors for retry")
        void shouldPropagateServerErrorsForRetry() {
            // Given
            UUID storeId = UUID.randomUUID();
            Supplier<String> apiCall = () -> {
                throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error");
            };

            // When/Then
            assertThatThrownBy(() -> client.executeApiCall(storeId, apiCall))
                .isInstanceOf(HttpServerErrorException.class);
        }

        @Test
        @DisplayName("should propagate connection errors for retry")
        void shouldPropagateConnectionErrorsForRetry() {
            // Given
            UUID storeId = UUID.randomUUID();
            Supplier<String> apiCall = () -> {
                throw new ResourceAccessException("Connection refused");
            };

            // When/Then
            assertThatThrownBy(() -> client.executeApiCall(storeId, apiCall))
                .isInstanceOf(ResourceAccessException.class);
        }
    }

    @Nested
    @DisplayName("executeSyncOperation()")
    class ExecuteSyncOperation {

        @Test
        @DisplayName("should execute successful sync operation")
        void shouldExecuteSuccessfulSyncOperation() {
            // Given
            UUID storeId = UUID.randomUUID();
            Integer expectedResult = 100;
            Supplier<Integer> operation = () -> expectedResult;

            // When
            Integer result = client.executeSyncOperation(storeId, operation);

            // Then
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        @DisplayName("should propagate errors from sync operation")
        void shouldPropagateErrorsFromSyncOperation() {
            // Given
            UUID storeId = UUID.randomUUID();
            Supplier<String> operation = () -> {
                throw new RuntimeException("Sync failed");
            };

            // When/Then
            assertThatThrownBy(() -> client.executeSyncOperation(storeId, operation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Sync failed");
        }
    }

    @Nested
    @DisplayName("Convenience methods")
    class ConvenienceMethods {

        @Test
        @DisplayName("get() should execute GET request")
        void getShouldExecuteGetRequest() {
            // Given
            UUID storeId = UUID.randomUUID();
            String url = "http://api.example.com/test";
            HttpEntity<?> entity = new HttpEntity<>(null);
            ResponseEntity<String> expectedResponse = ResponseEntity.ok("response");

            when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), eq(entity), eq(String.class)))
                .thenReturn(expectedResponse);

            // When
            ResponseEntity<String> result = client.get(storeId, url, entity, String.class);

            // Then
            assertThat(result.getBody()).isEqualTo("response");
            verify(restTemplate).exchange(url, HttpMethod.GET, entity, String.class);
        }

        @Test
        @DisplayName("post() should execute POST request")
        void postShouldExecutePostRequest() {
            // Given
            UUID storeId = UUID.randomUUID();
            String url = "http://api.example.com/test";
            HttpEntity<String> entity = new HttpEntity<>("request body");
            ResponseEntity<String> expectedResponse = ResponseEntity.ok("created");

            when(restTemplate.exchange(eq(url), eq(HttpMethod.POST), eq(entity), eq(String.class)))
                .thenReturn(expectedResponse);

            // When
            ResponseEntity<String> result = client.post(storeId, url, entity, String.class);

            // Then
            assertThat(result.getBody()).isEqualTo("created");
            verify(restTemplate).exchange(url, HttpMethod.POST, entity, String.class);
        }

        @Test
        @DisplayName("put() should execute PUT request")
        void putShouldExecutePutRequest() {
            // Given
            UUID storeId = UUID.randomUUID();
            String url = "http://api.example.com/test";
            HttpEntity<String> entity = new HttpEntity<>("update body");
            ResponseEntity<String> expectedResponse = ResponseEntity.ok("updated");

            when(restTemplate.exchange(eq(url), eq(HttpMethod.PUT), eq(entity), eq(String.class)))
                .thenReturn(expectedResponse);

            // When
            ResponseEntity<String> result = client.put(storeId, url, entity, String.class);

            // Then
            assertThat(result.getBody()).isEqualTo("updated");
            verify(restTemplate).exchange(url, HttpMethod.PUT, entity, String.class);
        }
    }
}
