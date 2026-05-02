package com.ocrplatform.document.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration de l'exécution asynchrone Spring (@Async).
 * <p>
 * Active la prise en charge des annotations {@link org.springframework.scheduling.annotation.Async}
 * dans tout le contexte applicatif. Définit un executor dédié pour l'audit
 * (séparé de l'executor par défaut) afin que :
 * <ul>
 *     <li>Les tâches d'audit ne bloquent pas d'autres opérations async</li>
 *     <li>On puisse tuner le pool indépendamment</li>
 *     <li>Les threads soient nommés de façon explicite dans les logs</li>
 * </ul>
 *
 * @see com.ocrplatform.document.audit.listener
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor dédié à l'audit log.
     * <p>
     * <b>Pool size</b> : 2 core / 5 max — l'audit est rapide (1 INSERT BDD),
     * pas besoin de beaucoup de threads. Une queue de 100 absorbe les pics.
     * <p>
     * <b>Pourquoi un executor dédié ?</b> Si tous les @Async partagent le même pool,
     * un job lent peut bloquer tout. Pattern pro : 1 executor par domaine.
     */
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("audit-");
        // Si la queue est pleine ET que tous les threads sont occupés,
        // exécute la tâche dans le thread appelant (au lieu de la perdre).
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}