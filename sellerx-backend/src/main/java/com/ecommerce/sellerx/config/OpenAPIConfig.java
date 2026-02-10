package com.ecommerce.sellerx.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger documentation configuration for SellerX API.
 *
 * Access documentation at:
 * - Swagger UI: /swagger-ui.html
 * - OpenAPI JSON: /v3/api-docs
 * - OpenAPI YAML: /v3/api-docs.yaml
 */
@Configuration
public class OpenAPIConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT access token. Obtain via POST /api/auth/login")))
                .tags(apiTags());
    }

    private Info apiInfo() {
        return new Info()
                .title("SellerX API")
                .description("""
                        SellerX E-Commerce Management Platform API.

                        ## Overview
                        SellerX is an e-commerce management platform for Turkish marketplaces (primarily Trendyol).
                        Sellers can manage products, orders, financials, and expenses across multiple stores.

                        ## Authentication
                        Most endpoints require JWT authentication. To authenticate:
                        1. Call `POST /api/auth/login` with email and password
                        2. Use the returned access token in the `Authorization: Bearer <token>` header
                        3. Access tokens expire in 1 hour; use refresh token endpoint to renew

                        ## Rate Limiting
                        - Trendyol API calls are limited to 10 requests/second
                        - No rate limiting on internal API calls

                        ## Timezone
                        All dates are in Turkey timezone (Europe/Istanbul) unless specified otherwise.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("SellerX Team")
                        .email("support@sellerx.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://sellerx.com/terms"));
    }

    private List<Server> serverList() {
        if ("prod".equals(activeProfile)) {
            return List.of(
                    new Server()
                            .url("https://api.sellerx.com")
                            .description("Production Server")
            );
        }
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("Development Server")
        );
    }

    private List<Tag> apiTags() {
        return List.of(
                new Tag().name("Authentication").description("User authentication and token management"),
                new Tag().name("Users").description("User profile and preferences"),
                new Tag().name("Stores").description("Store management and Trendyol credentials"),
                new Tag().name("Products").description("Product catalog, cost, and stock management"),
                new Tag().name("Orders").description("Order management and sync"),
                new Tag().name("Dashboard").description("Sales statistics and analytics"),
                new Tag().name("Financial").description("Financial settlements and invoices"),
                new Tag().name("Expenses").description("Store expense tracking"),
                new Tag().name("Purchasing").description("Purchase orders and supplier management"),
                new Tag().name("Webhooks").description("Trendyol webhook configuration"),
                new Tag().name("Admin").description("Admin panel operations (admin only)")
        );
    }
}
