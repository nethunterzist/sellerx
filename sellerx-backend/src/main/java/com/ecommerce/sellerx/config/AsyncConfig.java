package com.ecommerce.sellerx.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async configuration for background tasks like store onboarding sync.
 * Configured for ~2000 stores scale.
 */
@Configuration
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Executor for store onboarding tasks.
     * - Core pool: 5 threads (handles normal load)
     * - Max pool: 10 threads (handles peak - multiple stores onboarding simultaneously)
     * - Queue: 100 tasks (buffering for burst)
     */
    @Bean(name = "onboardingExecutor")
    public Executor onboardingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("onboarding-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(loggingCallerRunsPolicy("onboardingExecutor"));
        executor.initialize();
        return executor;
    }

    /**
     * Executor for general async tasks.
     * Default executor for @Async methods without qualifier.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(loggingCallerRunsPolicy("taskExecutor"));
        executor.initialize();
        return executor;
    }

    /**
     * Creates a CallerRunsPolicy that logs a warning before delegating to the calling thread.
     * This ensures tasks are never silently dropped when the thread pool is saturated.
     */
    private RejectedExecutionHandler loggingCallerRunsPolicy(String executorName) {
        return (runnable, executor) -> {
            log.warn("Thread pool '{}' is full (pool={}, active={}, queue={}). "
                            + "Task will run on calling thread: {}",
                    executorName,
                    executor.getPoolSize(),
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    Thread.currentThread().getName());
            new ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(runnable, executor);
        };
    }
}
