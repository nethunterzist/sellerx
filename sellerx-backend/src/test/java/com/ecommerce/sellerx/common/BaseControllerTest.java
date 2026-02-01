package com.ecommerce.sellerx.common;

import com.ecommerce.sellerx.auth.JwtService;
import com.ecommerce.sellerx.users.Role;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import jakarta.servlet.http.Cookie;

/**
 * Base class for controller integration tests.
 * Provides MockMvc and JWT authentication helpers.
 *
 * Usage:
 * <pre>
 * class MyControllerTest extends BaseControllerTest {
 *
 *     @Test
 *     void testEndpoint() throws Exception {
 *         // Create and authenticate a test user
 *         User user = createAndSaveTestUser("test@example.com");
 *
 *         // Perform authenticated request
 *         performWithAuth(get("/api/endpoint"), user)
 *             .andExpect(status().isOk());
 *     }
 * }
 * </pre>
 */
@AutoConfigureMockMvc
public abstract class BaseControllerTest extends BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected UserRepository userRepository;

    protected User testUser;

    /**
     * Cleans up all test users.
     * Call this AFTER deleting dependent entities (stores, products, etc.) in child class @BeforeEach.
     *
     * <pre>
     * &#64;BeforeEach
     * void setUp() {
     *     productRepository.deleteAll();
     *     storeRepository.deleteAll();
     *     cleanUpUsers();  // Must be last!
     *     TestDataBuilder.resetSequence();
     * }
     * </pre>
     */
    protected void cleanUpUsers() {
        userRepository.deleteAll();
    }

    /**
     * Creates and saves a test user with USER role.
     *
     * @param email User email (must be unique)
     * @return Saved user entity
     */
    protected User createAndSaveTestUser(String email) {
        return createAndSaveTestUser(email, Role.USER);
    }

    /**
     * Creates and saves a test user with specified role.
     *
     * @param email User email (must be unique)
     * @param role  User role (USER or ADMIN)
     * @return Saved user entity
     */
    protected User createAndSaveTestUser(String email, Role role) {
        User user = User.builder()
                .name("Test User")
                .email(email)
                .password("$2a$10$pbhA2lvr7KXc4gxwcrYbIu6rlsyi5IpASgzxG6Wcco0/VSGwR1g.K") // "password123"
                .role(role)
                .build();
        return userRepository.save(user);
    }

    /**
     * Generates JWT access token for the given user.
     *
     * @param user User to generate token for
     * @return JWT token string
     */
    protected String generateAccessToken(User user) {
        return jwtService.generateAccessToken(user).toString();
    }

    /**
     * Performs a request with JWT authentication via Authorization header.
     *
     * @param requestBuilder MockMvc request builder
     * @param user          User to authenticate as
     * @return ResultActions for further assertions
     * @throws Exception if request fails
     */
    protected ResultActions performWithAuth(MockHttpServletRequestBuilder requestBuilder, User user) throws Exception {
        String token = generateAccessToken(user);
        return mockMvc.perform(requestBuilder
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * Performs a request with JWT authentication via Cookie (access_token).
     *
     * @param requestBuilder MockMvc request builder
     * @param user          User to authenticate as
     * @return ResultActions for further assertions
     * @throws Exception if request fails
     */
    protected ResultActions performWithAuthCookie(MockHttpServletRequestBuilder requestBuilder, User user) throws Exception {
        String token = generateAccessToken(user);
        Cookie accessTokenCookie = new Cookie("access_token", token);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");

        return mockMvc.perform(requestBuilder
                .cookie(accessTokenCookie)
                .contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * Performs an unauthenticated request.
     *
     * @param requestBuilder MockMvc request builder
     * @return ResultActions for further assertions
     * @throws Exception if request fails
     */
    protected ResultActions performWithoutAuth(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder
                .contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * Converts object to JSON string.
     *
     * @param object Object to convert
     * @return JSON string representation
     * @throws Exception if serialization fails
     */
    protected String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }
}
