package com.pablohenrique.workflowengine.execution;

import com.pablohenrique.workflowengine.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkflowApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void runsAPurchaseAboveTheLimitThroughFinancialValidation() throws Exception {
        String key = activeDefinition();

        String body = """
                {"definitionKey": "%s", "variables": {"amount": 15000, "requester": "ana"}}
                """.formatted(key);
        postJson(WORKFLOWS, user(), body)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(WORKFLOWS + "/")))
                .andExpect(jsonPath("$.definitionKey").value(key))
                .andExpect(jsonPath("$.definitionVersion").value(1))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.currentState").value("REQUESTED"))
                .andExpect(jsonPath("$.variables.amount").value(15000));

        String id = workflowPendingApproval(key, 15000);

        get_(WORKFLOWS + "/" + id + "/actions", user())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action", contains("approve", "send-to-finance", "reject")));

        postJson(WORKFLOWS + "/" + id + "/actions", manager(), """
                {"action": "send-to-finance", "variables": {"managerNote": "ok"}}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.currentState").value("FINANCIAL_VALIDATION"))
                .andExpect(jsonPath("$.variables.managerNote").value("ok"));

        action(id, finance(), "approve")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currentState").value("APPROVED"));

        get_(WORKFLOWS + "/" + id, user())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.variables.amount").value(15000))
                .andExpect(jsonPath("$.variables.managerNote").value("ok"));

        get_(WORKFLOWS + "/" + id + "/history", user())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sequence", contains(1, 2, 3, 4, 5)))
                .andExpect(jsonPath("$[*].type",
                        contains("CREATED", "STARTED", "TRANSITIONED", "TRANSITIONED", "TRANSITIONED")))
                .andExpect(jsonPath("$[*].toState", contains("REQUESTED", "REQUESTED", "PENDING_APPROVAL",
                        "FINANCIAL_VALIDATION", "APPROVED")))
                .andExpect(jsonPath("$[*].actorId", contains("user", "user", "user", "manager", "finance")))
                .andExpect(jsonPath("$[3].action").value("send-to-finance"))
                .andExpect(jsonPath("$[3].fromState").value("PENDING_APPROVAL"));

        get_(WORKFLOWS + "/" + id + "/actions", user()).andExpect(jsonPath("$", hasSize(0)));

        get_(AUDIT + "?resourceType=WORKFLOW&resourceId=" + id + "&sort=occurredAt,asc", admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].operation", contains("WORKFLOW_CREATED", "WORKFLOW_STARTED",
                        "WORKFLOW_ACTION_EXECUTED", "WORKFLOW_ACTION_EXECUTED", "WORKFLOW_ACTION_EXECUTED")))
                .andExpect(jsonPath("$.content[*].outcome", everyItem(is("SUCCESS"))))
                .andExpect(jsonPath("$.content[3].actorId").value("manager"))
                .andExpect(jsonPath("$.content[3].detail")
                        .value("send-to-finance: PENDING_APPROVAL -> FINANCIAL_VALIDATION"));
    }

    @Test
    void managerApprovesAPurchaseWithinTheLimit() throws Exception {
        String id = workflowPendingApproval(activeDefinition(), 800);

        action(id, manager(), "approve")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currentState").value("APPROVED"));
    }

    @Test
    void cannotCreateAWorkflowWithoutAnActiveDefinitionVersion() throws Exception {
        String draftOnly = uniqueKey();
        createDefinition(draftOnly).andExpect(status().isCreated());

        postJson(WORKFLOWS, user(), "{\"definitionKey\": \"" + draftOnly + "\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Workflow definition unavailable"));
        postJson(WORKFLOWS, user(), "{\"definitionKey\": \"" + uniqueKey() + "\"}")
                .andExpect(status().isUnprocessableContent());
        postJson(WORKFLOWS, user(), "{\"variables\": {}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("definitionKey"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
    }

    @Test
    void actionsAreOnlyAcceptedWhileRunning() throws Exception {
        String id = createWorkflow(activeDefinition(), 500);

        action(id, user(), "submit")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Operation incompatible with workflow status"));

        post_(WORKFLOWS + "/" + id + "/start", user())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));
        post_(WORKFLOWS + "/" + id + "/start", user()).andExpect(status().isConflict());
    }

    @Test
    void rejectedActionsLeaveTheWorkflowUntouchedButAreAudited() throws Exception {
        String id = workflowPendingApproval(activeDefinition(), 15000);

        action(id, user(), "approve")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Actor not authorized"));
        action(id, manager(), "approve")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Rule not satisfied"))
                .andExpect(jsonPath("$.unsatisfiedRules", contains("amount LESS_THAN_OR_EQUAL 10000")));
        action(id, manager(), "teleport")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Action not available"));
        postJson(WORKFLOWS + "/" + id + "/actions", manager(), "{}").andExpect(status().isBadRequest());

        get_(WORKFLOWS + "/" + id, user())
                .andExpect(jsonPath("$.currentState").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.status").value("RUNNING"));
        get_(WORKFLOWS + "/" + id + "/history", user()).andExpect(jsonPath("$", hasSize(3)));

        get_(AUDIT + "?resourceId=" + id + "&outcome=REJECTED&sort=occurredAt,asc", admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].actorId", contains("user", "manager", "manager")))
                .andExpect(jsonPath("$.content[*].operation", everyItem(is("WORKFLOW_ACTION_EXECUTED"))))
                .andExpect(jsonPath("$.content[0].detail", org.hamcrest.Matchers.startsWith("approve: ")));
    }

    @Test
    void finishedWorkflowsDoNotAcceptFurtherOperations() throws Exception {
        String id = workflowPendingApproval(activeDefinition(), 500);
        action(id, manager(), "reject")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currentState").value("REJECTED"));

        action(id, manager(), "approve").andExpect(status().isConflict());
        post_(WORKFLOWS + "/" + id + "/cancel", manager()).andExpect(status().isConflict());
        post_(WORKFLOWS + "/" + id + "/start", user()).andExpect(status().isConflict());
    }

    @Test
    void cancellationIsTerminalAndTraceable() throws Exception {
        String id = workflowPendingApproval(activeDefinition(), 500);

        postJson(WORKFLOWS + "/" + id + "/cancel", manager(), "{\"reason\": \"duplicated request\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.currentState").value("PENDING_APPROVAL"));

        get_(WORKFLOWS + "/" + id + "/history", user())
                .andExpect(jsonPath("$[3].type").value("CANCELLED"))
                .andExpect(jsonPath("$[3].actorId").value("manager"))
                .andExpect(jsonPath("$[3].comment").value("duplicated request"));
        get_(AUDIT + "?resourceId=" + id + "&operation=WORKFLOW_CANCELLED", admin())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].detail").value("duplicated request"));

        post_(WORKFLOWS + "/" + id + "/cancel", manager()).andExpect(status().isConflict());
        action(id, manager(), "approve").andExpect(status().isConflict());
        postJson(WORKFLOWS + "/" + id + "/cancel", manager(), "{\"reason\": \"" + "x".repeat(501) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void runningWorkflowsStayOnTheirVersionWhenTheDefinitionEvolves() throws Exception {
        String key = activeDefinition();
        String onVersion1 = workflowPendingApproval(key, 500);

        // A versão 2 simplifica o processo: aprovação direta, sem papel exigido.
        postJson(DEFINITIONS + "/" + key + "/versions", admin(), """
                {
                  "states": [{"name": "REQUESTED", "type": "INITIAL"}, {"name": "DONE", "type": "TERMINAL"}],
                  "transitions": [{"action": "finish", "from": "REQUESTED", "to": "DONE"}]
                }
                """).andExpect(status().isCreated());
        post_(DEFINITIONS + "/" + key + "/versions/2/activate", admin()).andExpect(status().isOk());

        String onVersion2 = createWorkflow(key, 500);
        get_(WORKFLOWS + "/" + onVersion2, user()).andExpect(jsonPath("$.definitionVersion").value(2));
        post_(WORKFLOWS + "/" + onVersion2 + "/start", user()).andExpect(status().isOk());
        action(onVersion2, user(), "finish")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").value("DONE"));

        get_(WORKFLOWS + "/" + onVersion1, user()).andExpect(jsonPath("$.definitionVersion").value(1));
        action(onVersion1, manager(), "finish").andExpect(status().isConflict());
        action(onVersion1, manager(), "approve")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").value("APPROVED"));
    }

    @Test
    void deactivatingAVersionStopsNewWorkflowsButNotRunningOnes() throws Exception {
        String key = activeDefinition();
        String id = workflowPendingApproval(key, 500);

        post_(DEFINITIONS + "/" + key + "/versions/1/deactivate", admin()).andExpect(status().isOk());

        postJson(WORKFLOWS, user(), "{\"definitionKey\": \"" + key + "\"}")
                .andExpect(status().isUnprocessableContent());
        action(id, manager(), "approve")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void searchesWorkflowsByDefinitionAndStatus() throws Exception {
        String key = activeDefinition();
        String created = createWorkflow(key, 100);
        String running = workflowPendingApproval(key, 200);

        get_(WORKFLOWS + "?definitionKey=" + key + "&sort=createdAt,asc", user())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].id", contains(created, running)));
        get_(WORKFLOWS + "?definitionKey=" + key + "&status=RUNNING", user())
                .andExpect(jsonPath("$.content[*].id", contains(running)))
                .andExpect(jsonPath("$.content[0].currentState").value("PENDING_APPROVAL"));
        get_(WORKFLOWS + "?status=CREATED&size=100", user())
                .andExpect(jsonPath("$.content[*].id", hasItem(created)))
                .andExpect(jsonPath("$.content[*].status", everyItem(is("CREATED"))));
        get_(WORKFLOWS + "?status=NOPE", user()).andExpect(status().isBadRequest());
    }

    @Test
    void unknownWorkflowIsNotFound() throws Exception {
        String unknown = WORKFLOWS + "/00000000-0000-0000-0000-000000000000";

        get_(unknown, user())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Workflow not found"));
        get_(unknown + "/history", user()).andExpect(status().isNotFound());
        get_(unknown + "/actions", user()).andExpect(status().isNotFound());
        post_(unknown + "/start", user()).andExpect(status().isNotFound());
        action("00000000-0000-0000-0000-000000000000", user(), "submit").andExpect(status().isNotFound());
    }

    @Test
    void auditTrailIsRestrictedToAdministratorsAndCoversDefinitionChanges() throws Exception {
        String key = activeDefinition();

        get_(AUDIT, user()).andExpect(status().isForbidden());
        get_(AUDIT, manager()).andExpect(status().isForbidden());

        get_(AUDIT + "?resourceType=WORKFLOW_DEFINITION&resourceId=" + key + "&sort=occurredAt,asc", admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].operation",
                        contains("DEFINITION_CREATED", "DEFINITION_VERSION_ACTIVATED")))
                .andExpect(jsonPath("$.content[*].actorId", everyItem(is("admin"))))
                .andExpect(jsonPath("$.content[0].occurredAt", endsWith("Z")));
        get_(AUDIT + "?actorId=nobody-" + key, admin()).andExpect(jsonPath("$.content", hasSize(0)));
    }
}
