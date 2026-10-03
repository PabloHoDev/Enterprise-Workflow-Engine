package com.pablohenrique.workflowengine;

import com.pablohenrique.workflowengine.support.EmbeddedDatabase;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.SpringApplication;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Ambiente local sem Docker: sobe um PostgreSQL embutido e a aplicação com o perfil local.
 *
 * <pre>
 * ./mvnw spring-boot:test-run
 * </pre>
 *
 * Os dados ficam em {@code .local/postgres} e sobrevivem entre execuções (apague a pasta para recomeçar).
 * A porta é 5433 ({@code LOCAL_DB_PORT} para trocar). Com {@code DB_URL} definida, usa aquele banco.
 */
public final class LocalWorkflowEngineApplication {

    private static final String DATABASE = "workflow_engine";

    private LocalWorkflowEngineApplication() {
    }

    public static void main(String[] args) throws SQLException {
        String[] effectiveArgs = args;
        if (System.getenv("DB_URL") == null) {
            int port = Integer.parseInt(System.getenv().getOrDefault("LOCAL_DB_PORT", "5433"));
            EmbeddedPostgres postgres = EmbeddedDatabase.startPersistent(Path.of(".local", "postgres"), port);
            createDatabaseIfMissing(postgres);
            effectiveArgs = Stream.concat(Stream.of(
                    "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", DATABASE),
                    "--spring.datasource.username=postgres",
                    "--spring.datasource.password="), Arrays.stream(args)).toArray(String[]::new);
        }
        SpringApplication.from(WorkflowEngineApplication::main)
                .withAdditionalProfiles("local")
                .run(effectiveArgs);
    }

    private static void createDatabaseIfMissing(EmbeddedPostgres postgres) throws SQLException {
        try (Connection connection = postgres.getPostgresDatabase().getConnection();
             Statement statement = connection.createStatement()) {
            boolean exists = statement.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + DATABASE + "'").next();
            if (!exists) {
                statement.execute("CREATE DATABASE " + DATABASE);
            }
        }
    }
}
