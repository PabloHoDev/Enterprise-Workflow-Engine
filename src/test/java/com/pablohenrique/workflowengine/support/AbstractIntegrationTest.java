package com.pablohenrique.workflowengine.support;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração: aplicação completa, cadeia de segurança real e PostgreSQL real.
 * Os testes não limpam o banco; cada um cria os próprios dados com chaves únicas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(TestDatabase.class)
public abstract class AbstractIntegrationTest {

    protected static final String DEFINITIONS = "/api/v1/workflow-definitions";
    protected static final String WORKFLOWS = "/api/v1/workflows";
    protected static final String AUDIT = "/api/v1/audit-records";

    @Autowired
    protected MockMvc mockMvc;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        TestDatabase.register(registry);
    }

    protected static RequestPostProcessor admin() {
        return httpBasic("admin", "admin-secret");
    }

    protected static RequestPostProcessor manager() {
        return httpBasic("manager", "manager-secret");
    }

    protected static RequestPostProcessor finance() {
        return httpBasic("finance", "finance-secret");
    }

    protected static RequestPostProcessor user() {
        return httpBasic("user", "user-secret");
    }

    protected static String uniqueKey() {
        return "purchase-" + UUID.randomUUID();
    }

    protected ResultActions postJson(String url, RequestPostProcessor actor, String body) throws Exception {
        return mockMvc.perform(post(url).with(actor).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions post_(String url, RequestPostProcessor actor) throws Exception {
        return mockMvc.perform(post(url).with(actor));
    }

    protected ResultActions get_(String url, RequestPostProcessor actor) throws Exception {
        return mockMvc.perform(get(url).with(actor));
    }

    protected ResultActions createDefinition(String key) throws Exception {
        String body = """
                {"key": "%s", "name": "Purchase Approval", "description": "Aprovação de compras", %s}
                """.formatted(key, PurchaseApproval.structureJson());
        return postJson(DEFINITIONS, admin(), body);
    }

    /** Definição de compras com a versão 1 já ativa. */
    protected String activeDefinition() throws Exception {
        String key = uniqueKey();
        createDefinition(key).andExpect(status().isCreated());
        post_(DEFINITIONS + "/" + key + "/versions/1/activate", admin()).andExpect(status().isOk());
        return key;
    }

    protected String createWorkflow(String definitionKey, int amount) throws Exception {
        String body = """
                {"definitionKey": "%s", "variables": {"amount": %d}}
                """.formatted(definitionKey, amount);
        String response = postJson(WORKFLOWS, user(), body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    /** Workflow iniciado e submetido: aguardando decisão do gestor em PENDING_APPROVAL. */
    protected String workflowPendingApproval(String definitionKey, int amount) throws Exception {
        String id = createWorkflow(definitionKey, amount);
        post_(WORKFLOWS + "/" + id + "/start", user()).andExpect(status().isOk());
        action(id, user(), "submit").andExpect(status().isOk());
        return id;
    }

    protected ResultActions action(String workflowId, RequestPostProcessor actor, String action) throws Exception {
        return postJson(WORKFLOWS + "/" + workflowId + "/actions", actor, "{\"action\": \"" + action + "\"}");
    }
}
