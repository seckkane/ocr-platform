package com.ocrplatform.document.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.Mockito.mock;

/**
 * Fournit un JwtDecoder mock pour les tests.
 * Avec @WithMockUser, on n'a pas besoin de vrai JWT mais Spring Security
 * exige quand meme la presence du bean.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        return mock(JwtDecoder.class);
    }
}
