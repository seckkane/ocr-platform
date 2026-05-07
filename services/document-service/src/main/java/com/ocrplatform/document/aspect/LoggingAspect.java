package com.ocrplatform.document.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * Aspect qui trace automatiquement l'entrée, la sortie, la durée et les exceptions
 * de toutes les méthodes des services métier.
 * <p>
 * Avantages :
 * <ul>
 *     <li>Pas de {@code log.info("entering...")} à écrire à la main partout</li>
 *     <li>Métriques de durée gratuites pour chaque méthode service</li>
 *     <li>Traçage uniforme et cohérent</li>
 * </ul>
 *
 * <h3>Niveaux de log</h3>
 * <ul>
 *     <li>{@code DEBUG} : entrée/sortie nominale (verbeux, désactivable)</li>
 *     <li>{@code WARN}  : durée &gt; SLOW_THRESHOLD_MS (méthode lente)</li>
 *     <li>{@code ERROR} : exception propagée</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /** Au-dessus de ce seuil, on log en WARN pour identifier les méthodes lentes. */
    private static final long SLOW_THRESHOLD_MS = 500;

    /**
     * Pointcut : toutes les méthodes publiques des classes annotées {@link org.springframework.stereotype.Service}
     * dans le package {@code com.ocrplatform.document} (récursif).
     */
    @Pointcut("within(@org.springframework.stereotype.Service com.ocrplatform.document..*)")
    public void serviceLayer() {
        // marker
    }

    @Around("serviceLayer()")
    public Object logAroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
        String label = className + "." + methodName;

        log.debug("→ {}", label);

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;

            if (duration > SLOW_THRESHOLD_MS) {
                log.warn("← {} ({}ms) [SLOW]", label, duration);
            } else {
                log.debug("← {} ({}ms)", label, duration);
            }
            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("✗ {} ({}ms) — {}: {}", label, duration,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}
