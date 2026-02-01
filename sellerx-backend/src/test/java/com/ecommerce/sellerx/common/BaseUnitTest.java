package com.ecommerce.sellerx.common;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for unit tests with Mockito support.
 * Provides mock injection and verification capabilities.
 *
 * Usage:
 * <pre>
 * class MyServiceTest extends BaseUnitTest {
 *
 *     @Mock
 *     private MyRepository myRepository;
 *
 *     @InjectMocks
 *     private MyService myService;
 *
 *     @Test
 *     void testSomething() {
 *         // Given
 *         when(myRepository.findById(any())).thenReturn(Optional.of(entity));
 *
 *         // When
 *         var result = myService.doSomething();
 *
 *         // Then
 *         assertThat(result).isNotNull();
 *         verify(myRepository).findById(any());
 *     }
 * }
 * </pre>
 *
 * Note: This class uses MockitoExtension which provides:
 * - Automatic mock initialization (no need for MockitoAnnotations.openMocks())
 * - Strict stubbing (detects unused stubs)
 * - Better error messages for verification failures
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {

    /**
     * Helper method to pause execution in tests (use sparingly).
     * Prefer Awaitility for async testing instead.
     *
     * @param millis Milliseconds to wait
     */
    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
