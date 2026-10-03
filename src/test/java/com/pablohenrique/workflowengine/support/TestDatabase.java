package com.pablohenrique.workflowengine.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL dos testes de integração, compartilhado por toda a suíte. A origem é escolhida nesta ordem:
 *
 * <ol>
 *   <li>banco existente e descartável, se {@code EWE_TEST_DB_URL} estiver definida
 *       (com {@code EWE_TEST_DB_USERNAME} e {@code EWE_TEST_DB_PASSWORD});</li>
 *   <li>container (Testcontainers), se houver Docker, como no CI;</li>
 *   <li>PostgreSQL embutido ({@link EmbeddedDatabase}), sem Docker ou com {@code EWE_TEST_DB=embedded}.</li>
 * </ol>
 *
 * Os testes de integração rodam sempre; nenhuma das opções exige instalação manual.
 */
final class TestDatabase {

    private static final String EXTERNAL_URL = System.getenv("EWE_TEST_DB_URL");
    private static final boolean FORCE_EMBEDDED = "embedded".equalsIgnoreCase(System.getenv("EWE_TEST_DB"));
    private static final String IMAGE = "postgres:18-alpine";

    private static PostgreSQLContainer container;
    private static EmbeddedPostgres embedded;

    private TestDatabase() {
    }

    static synchronized void register(DynamicPropertyRegistry registry) {
        if (EXTERNAL_URL != null) {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", () -> System.getenv("EWE_TEST_DB_USERNAME"));
            registry.add("spring.datasource.password", () -> System.getenv("EWE_TEST_DB_PASSWORD"));
            return;
        }
        if (container == null && embedded == null) {
            if (!FORCE_EMBEDDED && DockerClientFactory.instance().isDockerAvailable()) {
                container = new PostgreSQLContainer(IMAGE);
                container.start();
            } else {
                embedded = EmbeddedDatabase.startEphemeral();
            }
        }
        if (container != null) {
            registry.add("spring.datasource.url", container::getJdbcUrl);
            registry.add("spring.datasource.username", container::getUsername);
            registry.add("spring.datasource.password", container::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> embedded.getJdbcUrl("postgres", "postgres"));
            registry.add("spring.datasource.username", () -> "postgres");
            registry.add("spring.datasource.password", () -> "");
        }
    }
}
