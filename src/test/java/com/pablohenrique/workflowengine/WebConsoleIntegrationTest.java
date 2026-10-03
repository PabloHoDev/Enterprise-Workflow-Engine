package com.pablohenrique.workflowengine;

import com.pablohenrique.workflowengine.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O console web (build de teste em src/test/resources/static) é servido pela aplicação. */
class WebConsoleIntegrationTest extends AbstractIntegrationTest {

    @Test
    void clientSideRoutesServeTheSpaEntryPointWithoutCaching() throws Exception {
        for (String route : new String[]{"/", "/login", "/workflows", "/workflows/123", "/definitions/purchase/versions/1"}) {
            mockMvc.perform(get(route).accept("text/html"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", containsString("no-store")))
                    .andExpect(content().string(containsString("<div id=\"root\">")));
        }
    }

    @Test
    void hashedAssetsAreCacheableForAYear() throws Exception {
        mockMvc.perform(get("/assets/app-test.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")));
    }

    @Test
    void apiPathsAreNeverAnsweredByTheSpa() throws Exception {
        mockMvc.perform(get(WORKFLOWS).accept("text/html")).andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardSummaryCountsWorkflowsByStatus() throws Exception {
        createWorkflow(activeDefinition(), 100);

        get_(WORKFLOWS + "/summary", user())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.byStatus.CREATED").isNumber())
                .andExpect(jsonPath("$.byStatus.RUNNING").isNumber())
                .andExpect(jsonPath("$.byStatus.COMPLETED").isNumber())
                .andExpect(jsonPath("$.byStatus.CANCELLED").isNumber());
    }

    @Test
    void definitionListingShowsTheActiveVersion() throws Exception {
        String key = activeDefinition();

        get_(DEFINITIONS + "?size=5&sort=createdAt,desc", user())
                .andExpect(jsonPath("$.content[?(@.key == '" + key + "')].activeVersion").value(1));
    }
}
