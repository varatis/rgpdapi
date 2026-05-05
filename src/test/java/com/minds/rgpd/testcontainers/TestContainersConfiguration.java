package com.minds.rgpd.testcontainers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers configuration for Spring Boot integration tests.
 * Automatically manages PostgreSQL container lifecycle and configuration.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    /**
     * Creates and configures a PostgreSQL container for testing.
     * <p>
     * The @ServiceConnection annotation automatically configures Spring Boot
     * to use this container for database connections, eliminating the need
     * for manual configuration of database URLs, usernames, and passwords.
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass")
                .withReuse(true); // Reuse container across test runs for better performance
    }
}