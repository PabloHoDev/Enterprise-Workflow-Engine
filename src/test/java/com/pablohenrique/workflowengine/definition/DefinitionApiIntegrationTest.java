package com.pablohenrique.workflowengine.definition;

import com.pablohenrique.workflowengine.support.AbstractIntegrationTest;
import com.pablohenrique.workflowengine.support.PurchaseApproval;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefinitionApiIntegrationTest extends AbstractIntegrationTest {

    private String versionBody() {
        return "{" + PurchaseApproval.structureJson() + "}";
    }

    @Test
    void createsADefinitionWithItsFirstVersionAsDraft() throws Exception {
        String key = uniqueKey();

        createDefinition(key)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(DEFINITIONS + "/" + key)))
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.activeVersion").value(nullValue()))
                .andExpect(jsonPath("$.versions", hasSize(1)))
                .andExpect(jsonPath("$.versions[0].number").value(1))
                .andExpect(jsonPath("$.versions[0].status").value("DRAFT"));

        get_(DEFINITIONS + "/" + key + "/versions/1", user())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.definitionKey").value(key))
                .andExpect(jsonPath("$.states", hasSize(5)))
                .andExpect(jsonPath("$.states[0].name").value("REQUESTED"))
                .andExpect(jsonPath("$.states[0].type").value("INITIAL"))
                .andExpect(jsonPath("$.transitions", hasSize(6)))
                .andExpect(jsonPath("$.transitions[1].action").value("approve"))
                .andExpect(jsonPath("$.transitions[1].requiredRole").value("MANAGER"))
                .andExpect(jsonPath("$.transitions[1].rules[0].field").value("amount"))
                .andExpect(jsonPath("$.transitions[1].rules[0].operator").value("LESS_THAN_OR_EQUAL"))
                .andExpect(jsonPath("$.transitions[1].rules[0].value").value("10000"));
    }

    @Test
    void onlyAdministratorsManageDefinitions() throws Exception {
        String key = uniqueKey();
        String body = """
                {"key": "%s", "name": "Purchase Approval", %s}
                """.formatted(key, PurchaseApproval.structureJson());

        postJson(DEFINITIONS, user(), body).andExpect(status().isForbidden());
        postJson(DEFINITIONS, manager(), body).andExpect(status().isForbidden());

        createDefinition(key).andExpect(status().isCreated());
        post_(DEFINITIONS + "/" + key + "/versions/1/activate", user()).andExpect(status().isForbidden());
        get_(DEFINITIONS + "/" + key, user()).andExpect(status().isOk());
    }

    @Test
    void rejectsADuplicatedKey() throws Exception {
        String key = uniqueKey();
        createDefinition(key).andExpect(status().isCreated());

        createDefinition(key)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Workflow definition key already in use"));
    }

    @Test
    void rejectsAStructureThatCannotBeExecuted() throws Exception {
        String body = """
                {
                  "key": "%s", "name": "Broken",
                  "states": [
                    {"name": "START", "type": "INITIAL"},
                    {"name": "LIMBO", "type": "INTERMEDIATE"},
                    {"name": "END", "type": "TERMINAL"}
                  ],
                  "transitions": [
                    {"action": "finish", "from": "START", "to": "END"},
                    {"action": "check", "from": "START", "to": "END",
                     "rules": [{"field": "amount", "operator": "GREATER_THAN"}]}
                  ]
                }
                """.formatted(uniqueKey());

        // A regra sem valor é recusada na conversão da requisição, antes da validação estrutural.
        postJson(DEFINITIONS, admin(), body)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Invalid workflow definition"))
                .andExpect(jsonPath("$.violations[0]")
                        .value("rule on field 'amount' with operator GREATER_THAN requires a value"));

        String structural = body.replace("\"operator\": \"GREATER_THAN\"", "\"operator\": \"EXISTS\"");
        postJson(DEFINITIONS, admin(), structural)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.violations",
                        hasItem("state 'LIMBO' is not TERMINAL and has no outgoing transition")))
                .andExpect(jsonPath("$.violations", hasItem("state 'LIMBO' is unreachable from the INITIAL state")));
    }

    @Test
    void rejectsAMalformedRequestListingTheInvalidFields() throws Exception {
        String body = """
                {"name": "No key", "states": [{"name": "", "type": "INITIAL"}], "transitions": []}
                """;

        postJson(DEFINITIONS, admin(), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("key")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("states[0].name")));

        postJson(DEFINITIONS, admin(), "{not json").andExpect(status().isBadRequest());
        postJson(DEFINITIONS, admin(), """
                {"key": "k", "name": "n", "states": [{"name": "A", "type": "SIDEWAYS"}], "transitions": []}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void versionsEvolveIndependentlyAndOnlyOneIsActive() throws Exception {
        String key = uniqueKey();
        String versions = DEFINITIONS + "/" + key + "/versions";
        createDefinition(key).andExpect(status().isCreated());

        postJson(versions, admin(), versionBody())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(versions + "/2")))
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        post_(versions + "/1/activate", admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        post_(versions + "/1/activate", admin())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid version status"));

        post_(versions + "/2/activate", admin()).andExpect(status().isOk());
        get_(DEFINITIONS + "/" + key, user())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeVersion").value(2))
                .andExpect(jsonPath("$.versions[0].status").value("INACTIVE"))
                .andExpect(jsonPath("$.versions[1].status").value("ACTIVE"));

        post_(versions + "/2/deactivate", admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
        post_(versions + "/2/deactivate", admin()).andExpect(status().isConflict());
        get_(DEFINITIONS + "/" + key, user()).andExpect(jsonPath("$.activeVersion").value(nullValue()));
    }

    @Test
    void invalidNewVersionDoesNotChangeTheDefinition() throws Exception {
        String key = uniqueKey();
        createDefinition(key).andExpect(status().isCreated());

        postJson(DEFINITIONS + "/" + key + "/versions", admin(), """
                {"states": [{"name": "ONLY", "type": "INITIAL"}], "transitions": []}
                """).andExpect(status().isUnprocessableContent());

        get_(DEFINITIONS + "/" + key, user()).andExpect(jsonPath("$.versions", hasSize(1)));
    }

    @Test
    void unknownDefinitionsAndVersionsAreNotFound() throws Exception {
        String key = uniqueKey();

        get_(DEFINITIONS + "/" + key, user())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Workflow definition not found"));
        postJson(DEFINITIONS + "/" + key + "/versions", admin(), versionBody()).andExpect(status().isNotFound());

        createDefinition(key).andExpect(status().isCreated());
        get_(DEFINITIONS + "/" + key + "/versions/9", user()).andExpect(status().isNotFound());
        post_(DEFINITIONS + "/" + key + "/versions/9/activate", admin()).andExpect(status().isNotFound());
    }

    @Test
    void listsDefinitionsInPages() throws Exception {
        String key = uniqueKey();
        createDefinition(key).andExpect(status().isCreated());

        get_(DEFINITIONS + "?size=1", user())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.page.totalElements").isNumber());
    }
}
