package com.pablohenrique.workflowengine.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL dos testes de integração. Por padrão sobe um container (Testcontainers), compartilhado por
 * toda a suíte. Sem Docker, pode-se apontar para um banco existente e descartável:
 *
 * <pre>
 * EWE_TEST_DB_URL=jdbc:postgresql://localhost:5432/workflow_engine_test
 * EWE_TEST_DB_USERNAME=...
 * EWE_TEST_DB_PASSWORD=...
 * </pre>
 *
 * Sem nenhuma das duas opções, os testes de integração são ignorados (e não falham).
 */
public final class TestDatabase implements ExecutionCondition {

    private static final String EXTERNAL_URL = System.getenv("EWE_TEST_DB_URL");
    private static final String IMAGE = "postgres:18-alpine";

    private static PostgreSQLContainer container;

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (EXTERNAL_URL != null) {
            return ConditionEvaluationResult.enabled("Using external database from EWE_TEST_DB_URL");
        }
        if (DockerClientFactory.instance().isDockerAvailable()) {
            return ConditionEvaluationResult.enabled("Docker is available");
        }
        return ConditionEvaluationResult.disabled(
                "Integration tests need Docker or an external PostgreSQL (EWE_TEST_DB_URL)");
    }

    static synchronized void register(DynamicPropertyRegistry registry) {
        if (EXTERNAL_URL != null) {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", () -> System.getenv("EWE_TEST_DB_USERNAME"));
            registry.add("spring.datasource.password", () -> System.getenv("EWE_TEST_DB_PASSWORD"));
            return;
        }
        if (container == null) {
            container = new PostgreSQLContainer(IMAGE);
            container.start();
        }
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }
}
