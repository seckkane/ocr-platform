package com.ocrplatform.document.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter HTTP qui gère l'identifiant de corrélation pour chaque requête.
 * <p>
 * Comportement :
 * <ul>
 *     <li>Si la requête contient un header {@code X-Correlation-Id}, il est utilisé tel quel</li>
 *     <li>Sinon, un nouvel UUID est généré</li>
 *     <li>L'identifiant est placé dans le {@link MDC} (visible dans tous les logs du thread)</li>
 *     <li>L'identifiant est ajouté au header de la réponse pour que le client puisse le voir</li>
 *     <li>Le {@link MDC} est nettoyé après traitement (crucial pour éviter les fuites de données entre requêtes)</li>
 * </ul>
 *
 * <h3>Pourquoi {@link OncePerRequestFilter} ?</h3>
 * Spring peut parfois invoquer un filter plusieurs fois pour une seule requête (forwards, includes).
 * {@link OncePerRequestFilter} garantit que la logique ne s'exécute qu'<b>une seule fois</b>,
 * ce qui évite de regénérer un correlationId au milieu d'une requête.
 *
 * <h3>Pourquoi @Order(HIGHEST_PRECEDENCE) ?</h3>
 * On veut que ce filter s'exécute <b>avant</b> tous les autres pour que le correlationId
 * soit dispo dans les logs dès le tout début de la requête (avant même la sécurité Spring,
 * la validation, etc.).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Nom du header HTTP standard pour le correlationId. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** Clé utilisée dans le MDC. Doit matcher le pattern Logback ({@code %X{correlationId}}). */
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. Extraire ou générer le correlationId
        String correlationId = extractOrGenerate(request);

        try {
            // 2. Mettre dans le MDC (visible dans tous les logs du thread)
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // 3. Ajouter au header de la réponse (visible côté client)
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            log.debug("Request {} {} with correlationId={}",
                    request.getMethod(), request.getRequestURI(), correlationId);

            // 4. Passer la main au filter suivant
            filterChain.doFilter(request, response);

        } finally {
            // 5. CRUCIAL : nettoyer le MDC pour éviter les fuites entre requêtes
            // (le thread va être réutilisé par Tomcat pour une autre requête)
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    /**
     * Extrait le correlationId du header {@code X-Correlation-Id} si présent,
     * sinon en génère un nouveau via {@link UUID#randomUUID()}.
     * <p>
     * Cas d'usage du correlationId entrant : un service amont (ex: API gateway) a déjà
     * généré un correlationId pour la requête utilisateur, on veut conserver le lien.
     */
    private String extractOrGenerate(HttpServletRequest request) {
        String headerValue = request.getHeader(CORRELATION_ID_HEADER);
        if (headerValue != null && !headerValue.isBlank()) {
            // Sanitize : limite à 100 chars et caractères safe
            String sanitized = headerValue.trim();
            if (sanitized.length() > 100) {
                sanitized = sanitized.substring(0, 100);
            }
            return sanitized;
        }
        return UUID.randomUUID().toString();
    }
}
