package com.pablohenrique.workflowengine.execution;

import com.pablohenrique.workflowengine.execution.application.WorkflowRepository;
import com.pablohenrique.workflowengine.execution.domain.Actor;
import com.pablohenrique.workflowengine.execution.domain.Workflow;
import com.pablohenrique.workflowengine.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class WorkflowConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final Actor ACTOR = new Actor("concurrent-actor", Set.of());

    @Autowired
    private WorkflowRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * RNF-001: duas operações que partem da mesma leitura não podem ambas prevalecer. A segunda a gravar
     * falha, seja pela posição já ocupada no History, seja pela versão do registro, e nada dela persiste.
     */
    @Test
    void anOperationBasedOnAStaleReadFailsInsteadOfOverwriting() throws Exception {
        UUID id = UUID.fromString(createWorkflow(activeDefinition(), 500));
        TransactionTemplate staleTransaction = new TransactionTemplate(transactionManager);
        TransactionTemplate concurrentTransaction = new TransactionTemplate(transactionManager);
        concurrentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> staleTransaction.executeWithoutResult(status -> {
            Workflow stale = repository.findById(id).orElseThrow();

            concurrentTransaction.executeWithoutResult(inner -> {
                Workflow fresh = repository.findById(id).orElseThrow();
                fresh.start(ACTOR, Instant.now());
                repository.save(fresh);
            });

            stale.cancel(ACTOR, "based on a stale read", Instant.now());
            repository.save(stale);
        })).isInstanceOfAny(OptimisticLockingFailureException.class, DataIntegrityViolationException.class);

        get_(WORKFLOWS + "/" + id, user()).andExpect(jsonPath("$.status").value("RUNNING"));
        get_(WORKFLOWS + "/" + id + "/history", user())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].type").value("STARTED"));
    }
}
