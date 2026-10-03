package com.pablohenrique.workflowengine.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PostgreSQL nativo executado pela própria JVM, sem Docker (binários de io.zonky.test.postgres, mesma
 * versão maior do postgres:18-alpine). O processo é encerrado junto com a JVM.
 */
public final class EmbeddedDatabase {

    private EmbeddedDatabase() {
    }

    /** Banco descartável, em diretório temporário e porta livre. */
    public static EmbeddedPostgres startEphemeral() {
        return start(EmbeddedPostgres.builder());
    }

    /** Banco persistente para desenvolvimento: os dados sobrevivem entre execuções. */
    public static EmbeddedPostgres startPersistent(Path dataDirectory, int port) {
        stopOrphanServer(dataDirectory);
        return start(EmbeddedPostgres.builder()
                .setDataDirectory(dataDirectory)
                .setCleanDataDirectory(false)
                .setPort(port));
    }

    /**
     * Se a execução anterior foi encerrada à força (sem shutdown hook), o servidor continua rodando com
     * a pasta de dados travada. Encerra esse processo; o PostgreSQL se recupera pelo WAL ao subir.
     */
    private static void stopOrphanServer(Path dataDirectory) {
        Path pidFile = dataDirectory.resolve("postmaster.pid");
        if (!Files.exists(pidFile)) {
            return;
        }
        try {
            long pid = Long.parseLong(Files.readAllLines(pidFile).getFirst().trim());
            ProcessHandle.of(pid)
                    .filter(process -> process.info().command().orElse("").contains("postgres"))
                    .ifPresent(process -> {
                        process.destroyForcibly();
                        process.onExit().join();
                    });
        } catch (IOException | RuntimeException e) {
            // Arquivo ilegível ou processo já encerrado: o próprio PostgreSQL trata um postmaster.pid obsoleto.
        }
    }

    private static EmbeddedPostgres start(EmbeddedPostgres.Builder builder) {
        try {
            // UTF-8 explícito: no Windows o initdb adotaria a codificação do sistema (WIN1252).
            return builder
                    .setLocaleConfig("encoding", "UTF8")
                    .setLocaleConfig("locale", "C")
                    .start();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start embedded PostgreSQL", e);
        }
    }
}
