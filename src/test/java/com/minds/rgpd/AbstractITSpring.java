package com.minds.rgpd;

import com.minds.rgpd.testcontainers.TestContainersConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for Spring Boot integration tests with TestContainers support.
 * <p>
 * This class automatically:
 * - Starts a PostgreSQL container before tests
 * - Configures Spring Boot to use the container database
 * - Manages container lifecycle
 * - Eliminates need for manual docker-compose setup
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {RgpdApplication.class})
@Import(TestContainersConfiguration.class)
@DirtiesContext
@ActiveProfiles("test")
public abstract class AbstractITSpring {

}
