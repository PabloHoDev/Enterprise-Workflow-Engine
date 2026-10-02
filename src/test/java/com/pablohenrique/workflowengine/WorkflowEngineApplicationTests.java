package com.pablohenrique.workflowengine;

import com.pablohenrique.workflowengine.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkflowEngineApplicationTests extends AbstractIntegrationTest {

    @Test
    void healthCheckIsPublicAndReportsReadiness() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }

    @Test
    void operationalEndpointsAreRestrictedToAdministrators() throws Exception {
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").with(user())).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/metrics").with(admin())).andExpect(status().isOk());
    }

    @Test
    void apiRequiresAuthenticationAndAnswersWithProblemDetails() throws Exception {
        mockMvc.perform(get(WORKFLOWS))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Basic")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Authentication required"));
    }

    @Test
    void wrongCredentialsAreRejected() throws Exception {
        mockMvc.perform(get(WORKFLOWS).with(httpBasic("admin", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void everyResponseCarriesARequestId() throws Exception {
        mockMvc.perform(get(WORKFLOWS).with(user()).header("X-Request-Id", "trace-123"))
                .andExpect(header().string("X-Request-Id", "trace-123"));
        mockMvc.perform(get(WORKFLOWS).with(user()).header("X-Request-Id", "not safe\tvalue"))
                .andExpect(header().string("X-Request-Id", matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void openApiDocumentDescribesTheApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Enterprise Workflow Engine API"))
                .andExpect(jsonPath("$.paths['/api/v1/workflows/{id}/actions'].post").exists());
    }

    @Test
    void unknownResourcesAndInvalidParametersAreClientErrors() throws Exception {
        mockMvc.perform(get("/api/v1/unknown").with(user())).andExpect(status().isNotFound());
        mockMvc.perform(get(WORKFLOWS + "/not-a-uuid").with(user())).andExpect(status().isBadRequest());
        mockMvc.perform(get(WORKFLOWS + "?sort=bogus").with(user()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown sort property 'bogus'"));
    }
}
