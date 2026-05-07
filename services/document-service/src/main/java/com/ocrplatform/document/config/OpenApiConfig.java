package com.ocrplatform.document.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI : metadonnees du service + schema d'auth JWT.
 * <p>
 * Avec le bean {@link SecurityScheme} declare ici, Swagger UI affichera un
 * bouton "Authorize" en haut a droite ou l'utilisateur peut coller son JWT
 * Keycloak pour tester les endpoints proteges.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_KEY = "bearerAuth";

    @Bean
    public OpenAPI documentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Document Service API")
                        .version("1.0.0")
                        .description("""
                                Service de gestion des documents pour la plateforme OCR.

                                Gere l'upload, le stockage (MinIO), les metadonnees (MySQL),
                                l'audit et la publication d'events Kafka pour le traitement OCR.
                                """)
                        .contact(new Contact()
                                .name("Issa Seck Kane")
                                .email("issaseckkane@gmail.com"))
                        .license(new License()
                                .name("Internal use")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_KEY))
                .components(new Components()
                        .addSecuritySchemes(BEARER_KEY, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenu via Keycloak (realm ocr-platform)")));
    }
}
