package com.pablohenrique.workflowengine.identity;

import com.pablohenrique.workflowengine.support.AbstractIntegrationTest;
import com.pablohenrique.workflowengine.support.BrowserSession;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    private static String randomAddress() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return "10." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(1, 255);
    }

    private String createUser(String password) throws Exception {
        String username = "u" + UUID.randomUUID().toString().substring(0, 8);
        postJson("/api/v1/users", admin(), """
                {"username": "%s", "displayName": "Test User", "password": "%s", "roles": ["USER"]}
                """.formatted(username, password)).andExpect(status().isCreated());
        return username;
    }

    @Test
    void browserLoginOpensASessionProtectedByCsrf() throws Exception {
        BrowserSession browser = new BrowserSession(mockMvc, randomAddress());

        browser.login("manager", "manager-secret")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("manager"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("USER", "MANAGER")));

        browser.perform(get("/api/v1/auth/me")).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("manager"));
        browser.perform(get(WORKFLOWS)).andExpect(status().isOk());

        // Mudança de estado sem o token CSRF é recusada, mesmo com a sessão válida.
        browser.performWithoutCsrf(post(WORKFLOWS).contentType("application/json").content("{\"definitionKey\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Invalid CSRF token"));
        browser.perform(post(WORKFLOWS).contentType("application/json").content("{\"definitionKey\":\"x\"}"))
                .andExpect(status().isUnprocessableContent());

        browser.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());
        browser.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());

        get_(AUDIT + "?actorId=manager&operation=LOGOUT", admin())
                .andExpect(jsonPath("$.content[0].outcome").value("SUCCESS"));
    }

    @Test
    void loginFailureIsGenericAndDoesNotTriggerTheBrowserBasicPrompt() throws Exception {
        BrowserSession browser = new BrowserSession(mockMvc, randomAddress());

        browser.login("manager", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
        browser.login("nobody-" + UUID.randomUUID(), "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
        browser.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));

        get_(AUDIT + "?actorId=manager&operation=LOGIN_FAILED&outcome=REJECTED", admin())
                .andExpect(jsonPath("$.content[0].detail").value("BadCredentialsException"));
    }

    @Test
    void consecutiveWrongPasswordsLockTheAccountUntilAnAdministratorUnlocksIt() throws Exception {
        String password = "a-strong-passphrase";
        String username = createUser(password);
        BrowserSession browser = new BrowserSession(mockMvc, randomAddress());

        for (int i = 0; i < 5; i++) {
            browser.login(username, "wrong-password").andExpect(status().isUnauthorized());
        }
        browser.login(username, password).andExpect(status().isUnauthorized());

        get_("/api/v1/users/" + username, admin())
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.lockedUntil").isNotEmpty());
        get_(AUDIT + "?resourceId=" + username + "&operation=ACCOUNT_LOCKED", admin())
                .andExpect(jsonPath("$.content[0].actorId").value("system"));

        post_("/api/v1/users/" + username + "/unlock", admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false));
        browser.login(username, password).andExpect(status().isOk());
        get_("/api/v1/users/" + username, admin()).andExpect(jsonPath("$.lastLoginAt").isNotEmpty());
    }

    @Test
    void repeatedLoginAttemptsFromOneOriginAreThrottled() throws Exception {
        BrowserSession browser = new BrowserSession(mockMvc, randomAddress());

        for (int i = 0; i < 10; i++) {
            browser.login("nobody-" + i, "wrong-password").andExpect(status().isUnauthorized());
        }

        browser.login("manager", "manager-secret")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.title").value("Too many login attempts"));
        new BrowserSession(mockMvc, randomAddress()).login("manager", "manager-secret").andExpect(status().isOk());
    }

    @Test
    void changingTheOwnPasswordRequiresTheCurrentOneAndEndsTheSessions() throws Exception {
        String password = "a-strong-passphrase";
        String username = createUser(password);
        BrowserSession browser = new BrowserSession(mockMvc, randomAddress());
        browser.login(username, password).andExpect(status().isOk());

        browser.perform(post("/api/v1/auth/password").contentType("application/json")
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"another-passphrase\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_CURRENT_PASSWORD"));
        browser.perform(post("/api/v1/auth/password").contentType("application/json")
                        .content("{\"currentPassword\":\"" + password + "\",\"newPassword\":\"short\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Weak password"));
        browser.perform(post("/api/v1/auth/password").contentType("application/json")
                        .content("{\"currentPassword\":\"" + password + "\",\"newPassword\":\"another-passphrase\"}"))
                .andExpect(status().isNoContent());

        browser.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        BrowserSession again = new BrowserSession(mockMvc, randomAddress());
        again.login(username, password).andExpect(status().isUnauthorized());
        again.login(username, "another-passphrase").andExpect(status().isOk());
    }

    @Test
    void integrationClientsKeepUsingBasicWithoutCsrf() throws Exception {
        postJson(WORKFLOWS, user(), "{\"definitionKey\": \"unknown\"}").andExpect(status().isUnprocessableContent());
    }

    @Test
    void deniedRequestsAreAudited() throws Exception {
        get_(AUDIT, user()).andExpect(status().isForbidden());

        get_(AUDIT + "?actorId=user&operation=ACCESS_DENIED", admin())
                .andExpect(jsonPath("$.content[0].resourceType").value("HTTP_ENDPOINT"))
                .andExpect(jsonPath("$.content[*].resourceId", hasItem("GET /api/v1/audit-records")));
    }

    @Test
    void responsesCarryBrowserSecurityHeaders() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", containsString("camera=()")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void oversizedBodiesAreRejected() throws Exception {
        String blob = "x".repeat(300 * 1024);
        postJson(WORKFLOWS, user(), "{\"definitionKey\":\"k\",\"variables\":{\"blob\":\"" + blob + "\"}}")
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.title").value("Payload too large"));
    }
}
