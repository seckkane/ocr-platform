package com.ocrplatform.document.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Configuration de l'exécution asynchrone Spring (@Async).
 * <p>
 * Active la prise en charge des annotations {@link org.springframework.scheduling.annotation.Async}
 * dans tout le contexte applicatif. Définit un executor dédié pour l'audit
 * (séparé de l'executor par défaut) avec <b>propagation du MDC</b> du thread parent
 * vers le thread enfant.
 *
 * <h3>Propagation du MDC</h3>
 * Le {@link MDC} est thread-local par design : un thread async ne voit pas le MDC
 * du thread qui a publié l'event. Sans un {@link TaskDecorator} qui copie le MDC,
 * le {@code correlationId} serait perdu dans tous les logs et events d'audit async.
 *
 * @see com.ocrplatform.document.audit.listener.AuditEventListener
 * @see com.ocrplatform.document.filter.CorrelationIdFilter
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor dédié à l'audit log avec propagation du MDC.
     * <p>
     * <b>Pool size</b> : 2 core / 5 max — l'audit est rapide (1 INSERT BDD),
     * pas besoin de beaucoup de threads. Une queue de 100 absorbe les pics.
     * <p>
     * <b>CallerRunsPolicy</b> : si la queue est pleine ET tous les threads occupés,
     * exécute la tâche dans le thread appelant. Garantit qu'aucun audit n'est perdu.
     */
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // Propagation du MDC : copie le contexte du thread parent vers le thread async
        executor.setTaskDecorator(new MdcTaskDecorator());

        executor.initialize();
        return executor;
    }

    /**
     * TaskDecorator qui copie le MDC du thread parent vers le thread async.
     * <p>
     * Sans ce décorateur, les tâches async perdraient le contexte de logging
     * (correlationId, userId, etc.) de la requête HTTP d'origine.
     */
    static class MdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // Capture le MDC du thread parent (le thread HTTP qui publie l'event)
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                } else {
                    MDC.clear();
                }
                try {
                    runnable.run();
                } finally {
                    // Restaure le MDC précédent (important pour ne pas leak entre tâches)
                    if (previous != null) {
                        MDC.setContextMap(previous);
                    } else {
                        MDC.clear();
                    }
                }
            };
        }
    }
}
